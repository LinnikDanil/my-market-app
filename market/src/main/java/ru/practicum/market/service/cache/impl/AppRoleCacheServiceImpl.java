package ru.practicum.market.service.cache.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.model.AppRole;
import ru.practicum.market.repository.AppRoleRepository;
import ru.practicum.market.service.cache.AppRoleCacheService;

import java.util.Map;

@Service
@Slf4j
public class AppRoleCacheServiceImpl implements AppRoleCacheService {

    private final Mono<Map<String, Long>> roleIdsByNameCache;

    public AppRoleCacheServiceImpl(AppRoleRepository roleRepository) {
        this.roleIdsByNameCache = Mono.defer(() -> {
                    log.info("Loading app roles into in-memory cache");
                    return roleRepository.findAll()
                            .collectMap(AppRole::getName, AppRole::getId);
                })
                .cache();
    }

    @PostConstruct
    public void warmUpCache() {
        roleIdsByNameCache
                .doOnSuccess(map -> log.info("App roles cache warmed up, size={}", map.size()))
                .doOnError(e -> log.error("Failed to warm up app roles cache", e))
                .subscribe();
    }

    @Override
    public Mono<Long> getRoleIdByName(String name) {
        return roleIdsByNameCache.flatMap(roleIds -> Mono.justOrEmpty(roleIds.get(name)));
    }
}
