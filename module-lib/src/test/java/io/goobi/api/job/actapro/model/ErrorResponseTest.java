package io.goobi.api.job.actapro.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class ErrorResponseTest {

    private ErrorResponse errorResponse;

    @Before
    public void setUp() {
        errorResponse = new ErrorResponse();
    }

    @Test
    public void testErrorType() {
        errorResponse.setErrorType("Not Found");
        assertEquals("Not Found", errorResponse.getErrorType());
    }

    @Test
    public void testMessage() {
        errorResponse.setMessage("Resource not found");
        assertEquals("Resource not found", errorResponse.getMessage());
    }

    @Test
    public void testStatus() {
        errorResponse.setStatus(404);
        assertEquals(Integer.valueOf(404), errorResponse.getStatus());
    }

    @Test
    public void testTimestamp() {
        Long timestamp = System.currentTimeMillis();
        errorResponse.setTimestamp(timestamp);
        assertEquals(timestamp, errorResponse.getTimestamp());
    }

    @Test
    public void testChainSetters() {
        Long timestamp = System.currentTimeMillis();
        errorResponse.errorType("Not Found")
                .message("Resource not found")
                .status(404)
                .timestamp(timestamp);

        assertEquals("Not Found", errorResponse.getErrorType());
        assertEquals("Resource not found", errorResponse.getMessage());
        assertEquals(Integer.valueOf(404), errorResponse.getStatus());
        assertEquals(timestamp, errorResponse.getTimestamp());
    }

    @Test
    public void testToString() {
        errorResponse.setErrorType("Not Found");
        errorResponse.setMessage("Resource not found");
        errorResponse.setStatus(404);
        errorResponse.setTimestamp(System.currentTimeMillis());

        String result = errorResponse.toString();
        assertTrue(result.contains("errorType: Not Found"));
        assertTrue(result.contains("message: Resource not found"));
        assertTrue(result.contains("status: 404"));
    }
}
