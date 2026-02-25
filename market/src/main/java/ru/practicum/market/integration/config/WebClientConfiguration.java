package ru.practicum.market.integration.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class WebClientConfiguration {

    /**
     * Создает WebClient для интеграции с сервисом платежей.
     */
    @Bean
    public WebClient paymentsWebClient(
            @Value("${integration.payments.baseUrl}") String baseUrl,
            @Value("${integration.payments.web-client.pool.name}") String poolName,
            @Value("${integration.payments.web-client.pool.max-connections}") int maxConnections,
            @Value("${integration.payments.web-client.pool.pending-acquire-timeout}") Duration pendingAcquireTimeout,
            @Value("${integration.payments.web-client.pool.max-idle-time}") Duration maxIdleTime,
            @Value("${integration.payments.web-client.timeouts.connect-timeout-millis}") int connectTimeoutMillis,
            @Value("${integration.payments.web-client.timeouts.response-timeout}") Duration responseTimeout,
            @Value("${integration.payments.web-client.timeouts.read-timeout}") Duration readTimeout,
            @Value("${integration.payments.web-client.timeouts.write-timeout}") Duration writeTimeout) {
        ConnectionProvider provider = ConnectionProvider.builder(poolName)
                .maxConnections(maxConnections)
                .pendingAcquireTimeout(pendingAcquireTimeout)
                .maxIdleTime(maxIdleTime)
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                // Таймаут установки TCP-соединения
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                // Таймаут получения ответа целиком
                .responseTimeout(responseTimeout)
                // Таймауты чтения/записи на уровне канала
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeout.toMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeout.toMillis(), TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    /**
     * Фильтр логирования исходящих запросов в платежный сервис.
     */
    private ExchangeFilterFunction logRequest() {
        return (request, next) -> {
            log.debug("Payments Request: {} {}", request.method(), request.url());
            return next.exchange(request);
        };
    }

    /**
     * Фильтр логирования ответов платежного сервиса.
     */
    private ExchangeFilterFunction logResponse() {
        return (request, next) -> next.exchange(request)
                .doOnNext(resp -> log.debug("Payments Response: {}", resp.statusCode()));
    }
}
