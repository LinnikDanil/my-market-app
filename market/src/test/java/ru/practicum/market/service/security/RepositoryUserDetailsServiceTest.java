package ru.practicum.market.service.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.exception.UserAlreadyExistsException;
import ru.practicum.market.domain.exception.UserNotFoundException;
import ru.practicum.market.domain.model.AppRole;
import ru.practicum.market.domain.model.AppUser;
import ru.practicum.market.repository.AppRoleRepository;
import ru.practicum.market.repository.AppUserRepository;
import ru.practicum.market.repository.AppUserRoleRepository;
import ru.practicum.market.service.cache.AppRoleCacheService;
import ru.practicum.market.service.security.model.AppPrincipal;
import ru.practicum.market.web.dto.AppUserRequestDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RepositoryUserDetailsService")
class RepositoryUserDetailsServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private AppRoleRepository roleRepository;

    @Mock
    private AppUserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AppRoleCacheService appRoleCacheService;

    @InjectMocks
    private RepositoryUserDetailsService service;

    @Nested
    @DisplayName("findByUsername")
    class FindByUsername {

        @Test
        @DisplayName("returns principal with roles")
        void test1() {
            var user = new AppUser();
            user.setId(1L);
            user.setUsername("john");
            user.setPassword("hash");

            var userRole = new AppRole();
            userRole.setId(10L);
            userRole.setName("ROLE_USER");

            var adminRole = new AppRole();
            adminRole.setId(20L);
            adminRole.setName("ROLE_ADMIN");

            when(userRepository.findByUsername("john")).thenReturn(Mono.just(user));
            when(userRoleRepository.findRoleIdsByUserId(1L)).thenReturn(Flux.just(10L, 20L));
            when(roleRepository.findByIdIn(List.of(10L, 20L))).thenReturn(Flux.just(userRole, adminRole));

            var details = service.findByUsername("john").block();

            assertThat(details).isInstanceOf(AppPrincipal.class);
            assertThat(details.getUsername()).isEqualTo("john");
            assertThat(details.getPassword()).isEqualTo("hash");
            assertThat(details.getAuthorities())
                    .extracting("authority")
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }

        @Test
        @DisplayName("throws when user not found")
        void test2() {
            when(userRepository.findByUsername("ghost")).thenReturn(Mono.empty());

            assertThatExceptionOfType(UserNotFoundException.class)
                    .isThrownBy(() -> service.findByUsername("ghost").block())
                    .withMessage("User with name = ghost not found");

            verify(userRoleRepository, never()).findRoleIdsByUserId(any());
        }

        @Test
        @DisplayName("returns principal without authorities when user has no roles")
        void test3() {
            var user = new AppUser();
            user.setId(1L);
            user.setUsername("john");
            user.setPassword("hash");

            when(userRepository.findByUsername("john")).thenReturn(Mono.just(user));
            when(userRoleRepository.findRoleIdsByUserId(1L)).thenReturn(Flux.empty());
            when(roleRepository.findByIdIn(List.of())).thenReturn(Flux.empty());

            var details = service.findByUsername("john").block();

            assertThat(details).isInstanceOf(AppPrincipal.class);
            assertThat(details.getAuthorities()).isEmpty();
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("saves user and assigns ROLE_USER")
        void test1() {
            var request = new AppUserRequestDto("new-user", "raw-password");
            var savedUser = new AppUser();
            savedUser.setId(5L);
            savedUser.setUsername("new-user");
            savedUser.setPassword("encoded-password");

            when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
            when(userRepository.save(any(AppUser.class))).thenReturn(Mono.just(savedUser));
            when(appRoleCacheService.getRoleIdByName("ROLE_USER")).thenReturn(Mono.just(100L));
            when(userRoleRepository.save(5L, 100L)).thenReturn(Mono.empty());

            service.register(Mono.just(request)).block();

            var userCaptor = ArgumentCaptor.forClass(AppUser.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getUsername()).isEqualTo("new-user");
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");

            verify(appRoleCacheService).getRoleIdByName("ROLE_USER");
            verify(userRoleRepository).save(5L, 100L);
        }

        @Test
        @DisplayName("maps duplicate user error")
        void test2() {
            var request = new AppUserRequestDto("existing", "raw");

            when(passwordEncoder.encode("raw")).thenReturn("encoded");
            when(userRepository.save(any(AppUser.class)))
                    .thenReturn(Mono.error(new DataIntegrityViolationException("duplicate")));

            assertThatExceptionOfType(UserAlreadyExistsException.class)
                    .isThrownBy(() -> service.register(Mono.just(request)).block())
                    .withMessage("User with name = existing already exists.");

            verify(userRoleRepository, never()).save(any(), any());
        }

        @Test
        @DisplayName("does not save user role relation when role id is absent in cache")
        void test3() {
            var request = new AppUserRequestDto("new-user", "raw-password");
            var savedUser = new AppUser();
            savedUser.setId(5L);
            savedUser.setUsername("new-user");
            savedUser.setPassword("encoded-password");

            when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
            when(userRepository.save(any(AppUser.class))).thenReturn(Mono.just(savedUser));
            when(appRoleCacheService.getRoleIdByName("ROLE_USER")).thenReturn(Mono.empty());

            service.register(Mono.just(request)).block();

            verify(userRoleRepository, never()).save(any(), any());
        }
    }
}
