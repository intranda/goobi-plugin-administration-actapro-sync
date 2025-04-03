package io.goobi.api.job.actapro.model;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class AuthenticationTokenTest {

    private AuthenticationToken authenticationToken;

    @Before
    public void setUp() {
        authenticationToken = new AuthenticationToken();
        authenticationToken.setAccessToken("testAccessToken");
        authenticationToken.setRefreshToken("testRefreshToken");
        authenticationToken.setTokenType("Bearer");
        authenticationToken.setExpiresIn(3600);
    }

    @Test
    public void testAccessToken() {
        assertEquals("testAccessToken", authenticationToken.getAccessToken());
    }

    @Test
    public void testRefreshToken() {
        assertEquals("testRefreshToken", authenticationToken.getRefreshToken());
    }

    @Test
    public void testTokenType() {
        assertEquals("Bearer", authenticationToken.getTokenType());
    }

    @Test
    public void testExpiresIn() {
        assertEquals(3600, authenticationToken.getExpiresIn());
    }

}
