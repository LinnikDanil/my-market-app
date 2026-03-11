package ru.practicum.market.service.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import ru.practicum.market.service.security.model.AppPrincipal;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CurrentUserService")
class CurrentUserServiceTest {

    private final CurrentUserService service = new CurrentUserService();

    @Mock
    private ServerRequest request;

    @Nested
    @DisplayName("currentUserId")
    class CurrentUserId {

        @Test
        @DisplayName("returns current user id for authenticated principal")
        void test1() {
            var principal = new AppPrincipal(
                    15L,
                    "john",
                    "hash",
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(principal, "credentials", principal.getAuthorities());

            doReturn(Mono.just(authentication)).when(request).principal();

            var currentUserId = service.currentUserId(request).block();

            assertThat(currentUserId).isEqualTo(15L);
        }

        @Test
        @DisplayName("fails when principal is not AppPrincipal")
        void test2() {
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    "john",
                    "credentials",
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            doReturn(Mono.just(authentication)).when(request).principal();

            assertThatExceptionOfType(ClassCastException.class)
                    .isThrownBy(() -> service.currentUserId(request).block());
        }
    }

    @Nested
    @DisplayName("currentUserIdIfAuthenticated")
    class CurrentUserIdIfAuthenticated {

        @Test
        @DisplayName("returns user id for authenticated AppPrincipal")
        void test1() {
            var principal = new AppPrincipal(
                    25L,
                    "mike",
                    "hash",
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(principal, "credentials", principal.getAuthorities());

            doReturn(Mono.just(authentication)).when(request).principal();

            var currentUserId = service.currentUserIdIfAuthenticated(request).blockOptional();

            assertThat(currentUserId).contains(25L);
        }

        @Test
        @DisplayName("returns empty when principal is not AppPrincipal")
        void test2() {
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    "mike",
                    "credentials",
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            doReturn(Mono.just(authentication)).when(request).principal();

            var currentUserId = service.currentUserIdIfAuthenticated(request).blockOptional();

            assertThat(currentUserId).isEmpty();
        }

        @Test
        @DisplayName("returns empty when request principal is absent")
        void test3() {
            when(request.principal()).thenReturn(Mono.empty());

            var currentUserId = service.currentUserIdIfAuthenticated(request).blockOptional();

            assertThat(currentUserId).isEmpty();
        }

        @Test
        @DisplayName("fails when request principal is not Authentication")
        void test4() {
            Principal principal = () -> "anonymous";
            doReturn(Mono.just(principal)).when(request).principal();

            assertThatExceptionOfType(ClassCastException.class)
                    .isThrownBy(() -> service.currentUserIdIfAuthenticated(request).blockOptional());
        }
    }
}
