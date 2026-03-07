package ru.practicum.market.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.DelegatingServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.SecurityContextServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.WebSessionServerLogoutHandler;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    /**
     * Главная цепочка безопасности для market UI.
     * Настраивает:
     * - CSRF для HTML-форм,
     * - правила доступа по маршрутам,
     * - form login/logout,
     * - обработку AccessDenied,
     * - OAuth2 client для межсервисных вызовов.
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                            WebSessionServerCsrfTokenRepository csrfTokenRepository,
                                                            ServerCsrfTokenRequestAttributeHandler csrfTokenRequestHandler,
                                                            RedirectServerLogoutSuccessHandler redirectServerLogoutSuccessHandler,
                                                            ServerLogoutHandler logoutHandler) {
        http
                .csrf(csrf -> csrf
                        // Храним CSRF-токен в WebSession.
                        .csrfTokenRepository(csrfTokenRepository)
                        // Позволяем корректно извлекать токен из request attributes/multipart.
                        .csrfTokenRequestHandler(csrfTokenRequestHandler)
                )
                .authorizeExchange(exchanges -> exchanges
                        // Публичные страницы и статика.
                        .pathMatchers("/", "/login", "/register", "/registerform",
                                "/access-denied", "/items/**", "/images/**").permitAll()
                        // Пользовательские операции корзины/заказов.
                        .pathMatchers("/cart/**", "/orders/**", "/buy/**").hasAnyRole("USER", "ADMIN")
                        // Админские маршруты.
                        .pathMatchers("/admin/**").hasRole("ADMIN")
                        // Любой неописанный маршрут требует аутентификацию.
                        .anyExchange().authenticated()
                )
                .formLogin(form -> form
                        // Кастомная страница логина (в WebFlux не генерируется HTML-форма автоматически).
                        .loginPage("/login")
                        .authenticationSuccessHandler(
                                // После успешного входа возвращаем пользователя на домашнюю страницу.
                                new RedirectServerAuthenticationSuccessHandler("/")
                        )
                )
                .logout(logout -> logout
                        // Единая точка выхода из приложения.
                        .logoutUrl("/logout")
                        // Комбинированный logout handler очищает SecurityContext/Session/cookies.
                        .logoutHandler(logoutHandler)
                        // После выхода перенаправляем пользователя на главную.
                        .logoutSuccessHandler(redirectServerLogoutSuccessHandler)
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedHandler((exchange, denied) -> {
                            // Для UI удобнее redirect, чем JSON 403.
                            exchange.getResponse().setStatusCode(HttpStatus.SEE_OTHER);
                            exchange.getResponse().getHeaders().setLocation(URI.create("/access-denied"));
                            return exchange.getResponse().setComplete();
                        })
                )
                // Включает инфраструктуру OAuth2 client в Spring Security WebFlux.
                .oauth2Client(withDefaults());

        return http.build();
    }

    /**
     * Кодировщик паролей пользователей market.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Уровень strength=11 даёт сбалансированную стойкость/производительность для dev-сценария.
        return new BCryptPasswordEncoder(11);
    }

    /**
     * Репозиторий CSRF-токенов в WebSession.
     */
    @Bean
    public WebSessionServerCsrfTokenRepository csrfTokenRepository() {
        return new WebSessionServerCsrfTokenRepository();
    }

    /**
     * Обработчик извлечения CSRF-токена из request-а.
     */
    @Bean
    public ServerCsrfTokenRequestAttributeHandler csrfTokenRequestHandler() {
        ServerCsrfTokenRequestAttributeHandler requestHandler = new ServerCsrfTokenRequestAttributeHandler();
        // Нужен для форм с multipart (например, загрузка изображений).
        requestHandler.setTokenFromMultipartDataEnabled(true);
        return requestHandler;
    }

    /**
     * Реактивный authentication manager для form login.
     */
    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager(
            ReactiveUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        var manager = new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        // Сверка пользовательского пароля с hash из БД.
        manager.setPasswordEncoder(passwordEncoder);
        return manager;
    }

    /**
     * Обработчик редиректа после logout.
     */
    @Bean
    public RedirectServerLogoutSuccessHandler redirectServerLogoutSuccessHandler() {
        RedirectServerLogoutSuccessHandler logoutSuccessHandler = new RedirectServerLogoutSuccessHandler();
        // После выхода пользователь попадает на главную.
        logoutSuccessHandler.setLogoutSuccessUrl(URI.create("/"));
        return logoutSuccessHandler;
    }

    /**
     * Комбинированный logout handler:
     * очищает SecurityContext, WebSession и session cookies.
     */
    @Bean
    public ServerLogoutHandler logoutHandler() {
        var securityContextLogoutHandler = new SecurityContextServerLogoutHandler();
        var webSessionLogoutHandler = new WebSessionServerLogoutHandler();
        var expireCookiesLogoutHandler = (ServerLogoutHandler) (exchange, authentication) -> {
            exchange.getExchange().getResponse().addCookie(expiredCookie("SESSION"));
            exchange.getExchange().getResponse().addCookie(expiredCookie("JSESSIONID"));
            return Mono.empty();
        };

        return new DelegatingServerLogoutHandler(
                securityContextLogoutHandler,
                webSessionLogoutHandler,
                expireCookiesLogoutHandler
        );
    }

    /**
     * Создает cookie с нулевым TTL для принудительного удаления на клиенте.
     */
    private ResponseCookie expiredCookie(String name) {
        return ResponseCookie.from(name, "")
                .path("/")
                .httpOnly(true)
                .maxAge(Duration.ZERO)
                .build();
    }
}
