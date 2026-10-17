package com.enterprise.docintel.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenValidatorTest {

    private final String secret = "thisIsASecretKeyForTestingThatIsLongEnough1234567890";
    private final JwtTokenValidator validator = new JwtTokenValidator(secret);

    @Test
    void testValidateInvalidToken() {
        assertFalse(validator.validateToken("invalid.token.here"));
    }

    @Test
    void testValidateNullOrEmptyToken() {
        assertFalse(validator.validateToken(null));
        assertFalse(validator.validateToken(""));
        assertFalse(validator.validateToken("   "));
    }
}
