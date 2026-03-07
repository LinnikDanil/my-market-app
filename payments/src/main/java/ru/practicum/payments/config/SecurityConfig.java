package ru.practicum.payments.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    /**
     * Client ID resource-server'а, из которого извлекаются client roles в JWT:
     * resource_access.{clientId}.roles.
     */
    @Value("${security.jwt.roles-client-id}")
    private String rolesClientId;

    /**
     * Основная цепочка безопасности для payments:
     * отключает CSRF для API и требует JWT-аутентификацию для всех endpoint-ов.
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                // Для stateless API с Bearer-токенами CSRF не требуется.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Все маршруты защищены и доступны только аутентифицированным клиентам.
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        // Явно подключаем наш converter, чтобы roles из Keycloak стали GrantedAuthority.
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter())
                        ))
                );
        return http.build();
    }

    /**
     * Конвертер JWT -> Authentication.
     * Собирает authorities из:
     * - стандартных scope (scp/scope),
     * - realm roles (realm_access.roles),
     * - client roles (resource_access.{rolesClientId}.roles).
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }

    /**
     * Объединяет все источники ролей в единый набор GrantedAuthority.
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // Стандартные authorities из scope: например, SCOPE_openid.
        var authorities = new ArrayList<>(new JwtGrantedAuthoritiesConverter().convert(jwt));

        // Realm роли из Keycloak: realm_access.roles.
        extractRealmRoles(jwt).stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        // Client роли текущего resource server: resource_access.{rolesClientId}.roles.
        extractClientRoles(jwt).stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        return authorities;
    }

    /**
     * Извлекает роли уровня realm.
     */
    private Collection<String> extractRealmRoles(Jwt jwt) {
        var realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> realmMap) {
            var roles = realmMap.get("roles");
            if (roles instanceof Collection<?> roleCollection) {
                return roleCollection.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .toList();
            }
        }
        return java.util.List.of();
    }

    /**
     * Извлекает client roles для настроенного resource-server client id.
     */
    private Collection<String> extractClientRoles(Jwt jwt) {
        var resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess instanceof Map<?, ?> resourceMap) {
            var clientAccess = resourceMap.get(rolesClientId);
            if (clientAccess instanceof Map<?, ?> clientMap) {
                var roles = clientMap.get("roles");
                if (roles instanceof Collection<?> roleCollection) {
                    return roleCollection.stream()
                            .filter(Objects::nonNull)
                            .map(Object::toString)
                            .toList();
                }
            }
        }
        return java.util.List.of();
    }
}
