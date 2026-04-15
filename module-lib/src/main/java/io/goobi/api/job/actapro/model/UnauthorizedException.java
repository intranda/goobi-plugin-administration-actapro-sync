package io.goobi.api.job.actapro.model;

import java.io.IOException;

/**
 * Thrown when the ActaPro API returns HTTP 401 Unauthorized, indicating that the authentication token has expired. Unlike transient server errors,
 * retrying with the same token is pointless — the caller must re-authenticate.
 */
public class UnauthorizedException extends IOException {

    private static final long serialVersionUID = -156827742904929897L;

    public UnauthorizedException(String message) {
        super(message);
    }
}
