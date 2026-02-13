package net.muttcode.spring.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "muttcues-test-secret-key-for-testing-only-32chars");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        String username = "testuser";
        
        String token = jwtService.generateToken(username);
        
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        String username = "testuser";
        String token = jwtService.generateToken(username);
        
        String extracted = jwtService.extractUsername(token);
        
        assertEquals(username, extracted);
    }

    @Test
    void generateToken_withRole_shouldIncludeRole() {
        String username = "testuser";
        String role = "ADMIN";
        
        String token = jwtService.generateToken(username, role);
        
        assertNotNull(token);
        assertTrue(jwtService.validateToken(token, username));
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String username = "testuser";
        String token = jwtService.generateToken(username);
        
        boolean isValid = jwtService.validateToken(token, username);
        
        assertTrue(isValid);
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidUsername() {
        String username = "testuser";
        String token = jwtService.generateToken(username);
        
        boolean isValid = jwtService.validateToken(token, "wronguser");
        
        assertFalse(isValid);
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() {
        JwtService shortExpiryService = new JwtService();
        ReflectionTestUtils.setField(shortExpiryService, "secret", "muttcues-test-secret-key-for-testing-only-32chars");
        ReflectionTestUtils.setField(shortExpiryService, "expiration", -1000L);
        
        String username = "testuser";
        String token = shortExpiryService.generateToken(username);
        
        boolean isValid = shortExpiryService.validateToken(token, username);
        
        assertFalse(isValid);
    }

    @Test
    void validateToken_withoutUsername_shouldCheckExpirationOnly() {
        String username = "testuser";
        String token = jwtService.generateToken(username);
        
        boolean isValid = jwtService.validateToken(token);
        
        assertTrue(isValid);
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        boolean isValid = jwtService.validateToken("invalid.token.here");
        
        assertFalse(isValid);
    }
}
