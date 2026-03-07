package ru.practicum.market.service.cache.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import ru.practicum.market.domain.model.AppRole;
import ru.practicum.market.repository.AppRoleRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppRoleCacheServiceImpl")
class AppRoleCacheServiceImplTest {

    @Mock
    private AppRoleRepository roleRepository;

    @Nested
    @DisplayName("getRoleIdByName")
    class GetRoleIdByName {

        @Test
        @DisplayName("loads roles once and serves from cache")
        void test1() {
            var userRole = new AppRole();
            userRole.setId(1L);
            userRole.setName("ROLE_USER");

            var adminRole = new AppRole();
            adminRole.setId(2L);
            adminRole.setName("ROLE_ADMIN");

            when(roleRepository.findAll()).thenReturn(Flux.just(userRole, adminRole));

            var service = new AppRoleCacheServiceImpl(roleRepository);

            var userRoleId = service.getRoleIdByName("ROLE_USER").block();
            var adminRoleId = service.getRoleIdByName("ROLE_ADMIN").block();
            var userRoleIdAgain = service.getRoleIdByName("ROLE_USER").block();

            assertThat(userRoleId).isEqualTo(1L);
            assertThat(adminRoleId).isEqualTo(2L);
            assertThat(userRoleIdAgain).isEqualTo(1L);
            verify(roleRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("returns empty when role does not exist")
        void test2() {
            var userRole = new AppRole();
            userRole.setId(1L);
            userRole.setName("ROLE_USER");

            when(roleRepository.findAll()).thenReturn(Flux.just(userRole));

            var service = new AppRoleCacheServiceImpl(roleRepository);

            var missingRoleId = service.getRoleIdByName("ROLE_MANAGER").blockOptional();

            assertThat(missingRoleId).isEmpty();
            verify(roleRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("propagates repository error")
        void test3() {
            when(roleRepository.findAll()).thenReturn(Flux.error(new IllegalStateException("roles repository unavailable")));

            var service = new AppRoleCacheServiceImpl(roleRepository);

            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> service.getRoleIdByName("ROLE_USER").block())
                    .withMessage("roles repository unavailable");

            verify(roleRepository, times(1)).findAll();
        }
    }
}
