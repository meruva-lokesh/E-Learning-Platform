package com.examly.springapp.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Gives clear answers for upload problems that Spring raises before our code runs (a file over the size limit,
 * or no file in the request). Runs before GlobalExceptionHandler, which would otherwise answer "500".
 * The body has the same shape as every other error: status, error, message, path.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class VideoUploadExceptionAdvice {

    private ResponseEntity<Map<String, Object>> reply(HttpStatus status, String message, HttpServletRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> tooLarge(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return reply(HttpStatus.PAYLOAD_TOO_LARGE, "The file is larger than the upload limit set on the server", req);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> missingPart(MissingServletRequestPartException ex, HttpServletRequest req) {
        return reply(HttpStatus.BAD_REQUEST, "Choose a video file to upload", req);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> notMultipart(MultipartException ex, HttpServletRequest req) {
        return reply(HttpStatus.BAD_REQUEST, "The upload could not be read. Send the video as a form upload.", req);
    }
}
