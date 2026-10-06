package com.examly.springapp.exception;

import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;

/**
 * Exception WRAPPING: service implementations catch low-level persistence errors
 * ({@link DataAccessException}) and rethrow them as this exception with the original as the cause,
 * so the stack trace is kept in the logs while the controller layer only ever sees our own hierarchy.
 * Answered with 500 and a generic message (SQL details are never sent to the client).
 *
 * @author Team Lead
 */
public class DatabaseOperationException extends AppException {
    public DatabaseOperationException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }

    @Override
    public String getClientMessage() {
        return "A database error occurred";
    }

    /** Runs a repository call that returns a value and wraps any {@link DataAccessException} thrown by it. */
    public static <T> T guard(String action, Supplier<T> operation) {
        try {
            return operation.get();
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Database error while " + action, e);
        }
    }

    /** Same as {@link #guard(String, Supplier)} for repository calls that return nothing. */
    public static void guardVoid(String action, Runnable operation) {
        try {
            operation.run();
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Database error while " + action, e);
        }
    }
}
