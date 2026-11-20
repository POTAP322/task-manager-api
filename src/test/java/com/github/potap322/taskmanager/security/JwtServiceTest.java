package com.github.potap322.taskmanager.security;

import com.github.potap322.taskmanager.user.User;
import com.github.potap322.taskmanager.user.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-for-unit-tests-0123456789";

    private JwtService service(Duration expiration) {
        return new JwtService(new JwtProperties(SECRET, expiration));
    }

    private UserPrincipal principal(String username) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPassword("irrelevant");
        user.setRole(UserRole.USER);
        return new UserPrincipal(user);
    }

    @Test
    void tokenCarriesUsernameAndIsValid() {
        JwtService jwt = service(Duration.ofMinutes(30));
        String token = jwt.generateToken(principal("bob"));

        assertThat(jwt.extractUsername(token)).isEqualTo("bob");
        assertThat(jwt.isTokenValid(token, principal("bob"))).isTrue();
    }

    @Test
    void tokenOfAnotherUserIsRejected() {
        JwtService jwt = service(Duration.ofMinutes(30));
        String token = jwt.generateToken(principal("bob"));

        assertThat(jwt.isTokenValid(token, principal("alice"))).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = service(Duration.ofSeconds(-1));
        String token = jwt.generateToken(principal("bob"));

        assertThat(jwt.isTokenValid(token, principal("bob"))).isFalse();
    }
}