package com.examly.springapp.exception;

import com.examly.springapp.model.ErrorLog;
import com.examly.springapp.repository.ErrorLogRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Exception FUNNELLING: every handler below does nothing except choose a status and a client message,
 * then hands over to the single {@link #build} method. That one method logs the problem, writes the
 * ErrorLogs row and builds the JSON body {status, error, message, path}, so all errors look the same.
 *
 * @author Team Lead
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ErrorLogRepo errorLogRepo;

    public GlobalExceptionHandler(ErrorLogRepo errorLogRepo) {
        this.errorLogRepo = errorLogRepo;
    }

    private Map<String, Object> body(HttpStatus status, String message, HttpServletRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", req.getRequestURI());
        return body;
    }

    /** The one funnel: logs, optionally stores in ErrorLogs, and builds the response. */
    private ResponseEntity<Map<String, Object>> build(HttpStatus status, Exception ex, String message,
                                                      HttpServletRequest req, boolean persist, HttpHeaders headers) {
        if (status.is5xxServerError()) {
            log.error("{} on {}", ex.getClass().getSimpleName(), req.getRequestURI(), ex);
        } else {
            log.warn("{} on {} -> {}", ex.getClass().getSimpleName(), req.getRequestURI(), message);
        }
        if (persist) {
            try {
                errorLogRepo.save(new ErrorLog(status.value(), ex.getClass().getSimpleName(), message, req.getRequestURI()));
            } catch (Exception logFailure) {
                log.warn("Could not persist error log: {}", logFailure.getMessage());
            }
        }
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (headers != null) {
            builder.headers(headers);
        }
        return builder.body(body(status, message, req));
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, Exception ex, String message, HttpServletRequest req) {
        return build(status, ex, message, req, true, null);
    }

    private String fieldMessages(BindingResult result) {
        StringBuilder sb = new StringBuilder();
        result.getFieldErrors()
                .forEach(f -> sb.append(f.getField()).append(": ").append(f.getDefaultMessage()).append("; "));
        return sb.toString().trim();
    }

    // ---- our own hierarchy: not found, duplicate, invalid, database, upstream API ----
    @ExceptionHandler(AppException.class)
    public ResponseEntity<Map<String, Object>> app(AppException ex, HttpServletRequest req) {
        return build(ex.getStatus(), ex, ex.getClientMessage(), req);
    }

    /** Rate limit hits are returned but not written to the ErrorLogs table, so an attacker cannot flood it. */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> tooMany(RateLimitExceededException ex, HttpServletRequest req) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", String.valueOf(Math.max(1, ex.getRetryAfterSeconds())));
        return build(HttpStatus.TOO_MANY_REQUESTS, ex, ex.getMessage(), req, false, headers);
    }

    // ---- request validation (400) ----
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex, fieldMessages(ex.getBindingResult()), req);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> bindFailure(BindException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex, fieldMessages(ex.getBindingResult()), req);
    }

    /** Thrown for @Min/@NotBlank on path variables and request params of @Validated controllers. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> constraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            String path = v.getPropertyPath().toString();
            String name = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            sb.append(name).append(": ").append(v.getMessage()).append("; ");
        }
        return build(HttpStatus.BAD_REQUEST, ex, sb.toString().trim(), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex, "Invalid value for parameter '" + ex.getName() + "'", req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> missingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex, "Missing request parameter '" + ex.getParameterName() + "'", req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> illegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex, ex.getMessage(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex, "Malformed request body", req);
    }

    // ---- routing problems ----
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> noResource(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex, "The requested resource was not found", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> methodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex, ex.getMessage(), req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> unsupportedMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex, "Content type is not supported", req);
    }

    // ---- security (401 / 403) ----
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> unauthorized(AuthenticationException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex, "Invalid credentials", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> forbidden(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex, ex.getMessage() == null ? "Access denied" : ex.getMessage(), req);
    }

    // ---- persistence errors that were not wrapped by a service (409 / 500) ----
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex, "The request conflicts with existing data", req);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> database(DataAccessException ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex, "A database error occurred", req);
    }

    // ---- last resort ----
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> generic(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex, "Something went wrong. Please try again later.", req);
    }
}
