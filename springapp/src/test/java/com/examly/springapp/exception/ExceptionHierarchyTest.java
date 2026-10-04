package com.examly.springapp.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.HttpStatus;

/**
 * Checks the status each exception type carries and that low-level database errors are wrapped.
 *
 * @author Team Lead
 */
class ExceptionHierarchyTest {

    @Test
    void everyExceptionCarriesItsHttpStatus() {
        assertEquals(HttpStatus.NOT_FOUND, new ResourceNotFoundException("x").getStatus());
        assertEquals(HttpStatus.CONFLICT, new DuplicateResourceException("x").getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, new InvalidRequestException("x").getStatus());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, new RateLimitExceededException("x", 5).getStatus());
        assertEquals(HttpStatus.BAD_GATEWAY, new ApiCommunicationException("x", null).getStatus());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, new DatabaseOperationException("x", null).getStatus());
    }

    @Test
    void allExceptionsShareTheAppExceptionRoot() {
        assertTrue(new ResourceNotFoundException("x") instanceof AppException);
        assertTrue(new RateLimitExceededException("x", 1) instanceof AppException);
        assertTrue(new ResourceNotFoundException("x") instanceof RuntimeException);
    }

    @Test
    void internalDetailsAreHiddenFromClients() {
        assertEquals("A database error occurred", new DatabaseOperationException("SQL detail", null).getClientMessage());
        assertEquals("An upstream service is unavailable", new ApiCommunicationException("key=abc", null).getClientMessage());
        assertEquals("Course not found with id 5", new ResourceNotFoundException("Course not found with id 5").getClientMessage());
    }

    @Test
    void guardWrapsDataAccessExceptionAndKeepsTheCause() {
        DataRetrievalFailureException low = new DataRetrievalFailureException("connection lost");
        DatabaseOperationException wrapped = assertThrows(DatabaseOperationException.class,
                () -> DatabaseOperationException.guard("loading things", () -> {
                    throw low;
                }));
        assertSame(low, wrapped.getCause());
        assertTrue(wrapped.getMessage().contains("loading things"));
    }

    @Test
    void guardLetsOurOwnExceptionsThroughUnchanged() {
        assertThrows(ResourceNotFoundException.class,
                () -> DatabaseOperationException.guard("loading", () -> {
                    throw new ResourceNotFoundException("missing");
                }));
        assertEquals("ok", DatabaseOperationException.guard("loading", () -> "ok"));
    }
}
