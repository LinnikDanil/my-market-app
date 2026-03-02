package ru.practicum.market.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.model.AppUserRole;

@Repository
public interface AppUserRoleRepository extends org.springframework.data.repository.Repository<AppUserRole, Void> {

    @Query("select role_Id from user_roles where user_id = :userId")
    Flux<Long> findRoleIdsByUserId(Long userId);

    @Query("insert into user_roles (user_id, role_id) values (:userId, :roleId)")
    Mono<Void> save(Long userId, Long roleId);
}
