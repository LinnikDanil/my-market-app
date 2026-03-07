package ru.practicum.market.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@WebFluxTest
@Import({SecurityConfig.class, SecurityConfigTest.TestRoutes.class})
@DisplayName("SecurityConfig")
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ReactiveUserDetailsService userDetailsService;

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private ServerOAuth2AuthorizedClientRepository authorizedClientRepository;

    @TestConfiguration
    static class TestRoutes {

        @Bean
        RouterFunction<ServerResponse> testRoutes() {
            return RouterFunctions.route()
                    .GET("/items", request -> ServerResponse.ok().bodyValue("items"))
                    .GET("/cart/items", request -> ServerResponse.ok().bodyValue("cart"))
                    .POST("/cart/items", request -> ServerResponse.ok().bodyValue("updated"))
                    .GET("/admin", request -> ServerResponse.ok().bodyValue("admin"))
                    .POST("/buy", request -> ServerResponse.ok().bodyValue("buy"))
                    .GET("/access-denied", request -> ServerResponse.status(403).bodyValue("denied"))
                    .GET("/login", request -> ServerResponse.ok().bodyValue("login"))
                    .build();
        }
    }

    @Test
    @DisplayName("anonymous user can access public items page")
    void shouldAllowAnonymousAccessToItems() {
        webTestClient.get()
                .uri("/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("items");
    }

    @Test
    @DisplayName("anonymous user is redirected to login for protected cart")
    void shouldRedirectAnonymousToLoginForCart() {
        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/login");
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("user role can access cart")
    void userShouldAccessCart() {
        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("cart");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("admin role can access admin endpoint")
    void adminShouldAccessAdmin() {
        webTestClient.get()
                .uri("/admin")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("admin");
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("user role is denied for admin endpoint")
    void userShouldBeDeniedForAdmin() {
        webTestClient.get()
                .uri("/admin")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/access-denied");
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("post without csrf is forbidden")
    void shouldReturnForbiddenWithoutCsrf() {
        webTestClient.post()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("post with csrf is allowed")
    void shouldAllowPostWithCsrf() {
        webTestClient.mutateWith(csrf())
                .post()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("updated");
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("logout clears session cookies")
    void logoutShouldExpireSessionCookies() {
        webTestClient.mutateWith(csrf())
                .post()
                .uri("/logout")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Set-Cookie", ".*Max-Age=0.*");
    }
}
