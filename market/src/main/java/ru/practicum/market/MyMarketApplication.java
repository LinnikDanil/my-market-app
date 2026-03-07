package ru.practicum.market;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Точка входа в приложение market.
 */
@EnableCaching
@SpringBootApplication
public class MyMarketApplication {

    /**
     * Запускает Spring Boot приложение.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(MyMarketApplication.class, args);
    }

}
