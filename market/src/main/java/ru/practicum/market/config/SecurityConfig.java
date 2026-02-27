package ru.practicum.market.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;

import java.net.URI;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    // Защищаем пароли шифрованием
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSessionServerCsrfTokenRepository csrfTokenRepository() {
        return new WebSessionServerCsrfTokenRepository();
    }

    @Bean
    public ServerCsrfTokenRequestAttributeHandler csrfTokenRequestHandler() {
        ServerCsrfTokenRequestAttributeHandler requestHandler = new ServerCsrfTokenRequestAttributeHandler();
        // Включаем чтение CSRF-токена из multipart/form-data (нужно для upload-форм).
        requestHandler.setTokenFromMultipartDataEnabled(true);
        return requestHandler;
    }

    // Создаём in-memory пользователя
    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails user = User.withUsername("user")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build();
        return new MapReactiveUserDetailsService(user);
    }

    // Настраиваем поведение при выходе
    @Bean
    public RedirectServerLogoutSuccessHandler redirectServerLogoutSuccessHandler() {
        RedirectServerLogoutSuccessHandler logoutSuccessHandler = new RedirectServerLogoutSuccessHandler();
        // При выходе перенаправляем его на домашнюю страницу
        logoutSuccessHandler.setLogoutSuccessUrl(URI.create("/"));
        return logoutSuccessHandler;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                            WebSessionServerCsrfTokenRepository csrfTokenRepository,
                                                            ServerCsrfTokenRequestAttributeHandler csrfTokenRequestHandler,
                                                            RedirectServerLogoutSuccessHandler redirectServerLogoutSuccessHandler) {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(csrfTokenRequestHandler)
            )
            .authorizeExchange(exchanges -> exchanges
            .pathMatchers("/", "/login", "/items/**", "/images/**").permitAll()
            .anyExchange().authenticated()
        )
            // Настраиваем форму логина
            .formLogin(form -> form
                // URL страницы логина
                .loginPage("/login")
                .authenticationSuccessHandler(
                    // В случае успешного логина, перенаправляем на домашнюю страницу
                    new RedirectServerAuthenticationSuccessHandler("/")
                )
            )
            // Настраиваем обработку при выходе
            .logout(logout -> logout
                // URL страницы выхода
                .logoutUrl("/logout")
                .logoutSuccessHandler(redirectServerLogoutSuccessHandler)
            );
            // OAuth2 Client для WebClient
//            .oauth2Client(withDefaults());

        return http.build();
    }
}