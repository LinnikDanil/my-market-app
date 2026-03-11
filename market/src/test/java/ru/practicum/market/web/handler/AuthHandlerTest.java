package ru.practicum.market.web.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import ru.practicum.market.service.ItemService;
import ru.practicum.market.service.security.CurrentUserService;
import ru.practicum.market.service.security.RepositoryUserDetailsService;
import ru.practicum.market.web.filter.RouteExceptionFilter;
import ru.practicum.market.web.filter.RouteLoggingFilter;
import ru.practicum.market.web.view.PageRenderHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration.class
})
@Import({
        AuthHandlerTest.TestRoutes.class,
        AuthHandler.class,
        RouteLoggingFilter.class,
        RouteExceptionFilter.class,
        PageRenderHelper.class
})
@DisplayName("AuthHandler")
class AuthHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private RepositoryUserDetailsService repositoryUserDetailsService;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @TestConfiguration
    static class TestRoutes {
        @Bean
        WebSessionServerCsrfTokenRepository csrfTokenRepository() {
            return new WebSessionServerCsrfTokenRepository();
        }

        @Bean
        LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }

        @Bean
        RouterFunction<ServerResponse> routes(
                AuthHandler authHandler,
                RouteLoggingFilter logging,
                RouteExceptionFilter errors
        ) {
            return RouterFunctions.route()
                    .GET("/login", authHandler::login)
                    .GET("/registerform", authHandler::registerForm)
                    .GET("/access-denied", authHandler::accessDenied)
                    .POST("/register", authHandler::register)
                    .build()
                    .filter(logging.logging())
                    .filter(errors.errors());
        }
    }

    @Test
    @DisplayName("register returns redirect for valid payload")
    void registerShouldRedirectForValidPayload() {
        when(repositoryUserDetailsService.register(any())).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "new-user").with("password", "strong-pass"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/login");

        verify(repositoryUserDetailsService).register(any());
    }

    @Test
    @DisplayName("register returns 400 when username is blank")
    void registerShouldReturnBadRequestForBlankUsername() {
        webTestClient.post()
                .uri("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "   ").with("password", "strong-pass"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_HTML);

        verify(repositoryUserDetailsService, never()).register(any());
    }

    @Test
    @DisplayName("register returns 400 when password is too short")
    void registerShouldReturnBadRequestForShortPassword() {
        webTestClient.post()
                .uri("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "new-user").with("password", "1234"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_HTML);

        verify(repositoryUserDetailsService, never()).register(any());
    }
}
