package ru.practicum.market;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.practicum.market.util.PostgresContainer;

@SpringBootTest
@Testcontainers
@ImportTestcontainers(PostgresContainer.class)
@ActiveProfiles("test")
class MyMarketAppApplicationIT {

    @Test
    void contextLoads() {
    }

}
