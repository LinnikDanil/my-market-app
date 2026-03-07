package ru.practicum.payments.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("payments SecurityConfig")
class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    @DisplayName("jwt converter extracts scope, realm roles and configured client roles")
    void shouldExtractAllAuthorities() {
        ReflectionTestUtils.setField(securityConfig, "rolesClientId", "payments-api");

        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("scope", "payments.read")
                .claim("realm_access", Map.of("roles", List.of("REALM_ROLE")))
                .claim("resource_access", Map.of("payments-api", Map.of("roles", List.of("SERVICE"))))
                .build();

        var authentication = securityConfig.jwtAuthenticationConverter().convert(jwt);
        var authorities = authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList();

        assertThat(authorities)
                .contains("SCOPE_payments.read", "REALM_ROLE", "SERVICE");
    }

    @Test
    @DisplayName("jwt converter ignores missing role claims")
    void shouldHandleMissingRoleClaims() {
        ReflectionTestUtils.setField(securityConfig, "rolesClientId", "payments-api");

        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("scope", "payments.read")
                .build();

        var authentication = securityConfig.jwtAuthenticationConverter().convert(jwt);
        var authorities = authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList();

        assertThat(authorities)
                .containsExactly("SCOPE_payments.read");
    }
}
