package io.goobi.api.job.actapro.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import jakarta.ws.rs.core.Response;

public class ActaProApiTest {

    /**
     * A 401 response means the token is expired.
     * Retrying with the same token is pointless — the test must verify
     * that retry() throws UnauthorizedException immediately without retrying.
     */
    @Test
    public void testRetryThrowsUnauthorizedExceptionOn401WithoutRetrying() throws IOException {
        AtomicInteger callCount = new AtomicInteger(0);

        try {
            ActaProApi.retry(
                    new IOException("failed"),
                    Duration.ofMillis(1),
                    5,
                    () -> {
                        callCount.incrementAndGet();
                        Response r = mock(Response.class);
                        when(r.getStatus()).thenReturn(401);
                        return r;
                    });
            fail("Expected UnauthorizedException");
        } catch (UnauthorizedException e) {
            // expected
        }

        assertEquals("retry must not repeat on 401 — token is expired", 1, callCount.get());
    }

    /**
     * Transient server errors (5xx) should still be retried as before.
     */
    @Test
    public void testRetryRetriesOnServerError() throws IOException {
        AtomicInteger callCount = new AtomicInteger(0);

        try {
            ActaProApi.retry(
                    new IOException("failed after retries"),
                    Duration.ofMillis(1),
                    3,
                    () -> {
                        callCount.incrementAndGet();
                        Response r = mock(Response.class);
                        when(r.getStatus()).thenReturn(503);
                        return r;
                    });
            fail("Expected IOException");
        } catch (UnauthorizedException e) {
            fail("Must not throw UnauthorizedException for 503");
        } catch (IOException e) {
            assertEquals("failed after retries", e.getMessage());
        }

        assertEquals("retry must attempt maxRetries times for transient server errors", 3, callCount.get());
    }

    /**
     * Retry succeeds when a transient error clears up before maxRetries is exhausted.
     */
    @Test
    public void testRetrySucceedsAfterTransientErrors() throws IOException {
        AtomicInteger callCount = new AtomicInteger(0);

        Response result = ActaProApi.retry(
                new IOException("failed"),
                Duration.ofMillis(1),
                5,
                () -> {
                    int count = callCount.incrementAndGet();
                    Response r = mock(Response.class);
                    when(r.getStatus()).thenReturn(count < 3 ? 503 : 200);
                    return r;
                });

        assertEquals(200, result.getStatus());
        assertEquals("should succeed on 3rd attempt", 3, callCount.get());
    }
}
