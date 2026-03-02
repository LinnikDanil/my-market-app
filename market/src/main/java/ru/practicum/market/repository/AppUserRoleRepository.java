package ru.practicum.market.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import ru.practicum.market.domain.model.AppUserRole;

@Repository
public interface AppUserRoleRepository extends org.springframework.data.repository.Repository<AppUserRole, Void> {

    @Query("select role_Id from user_roles where user_id = :userId")
    Flux<Long> findRoleIdsByUserId(Long userId);
}
