package ru.practicum.market.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import ru.practicum.payments.api.DefaultApi;
import ru.practicum.payments.integration.client.ApiClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class WebClientConfig {

    /**
     * Создает настроенный WebClient для интеграции с payments-сервисом.
     * Включает:
     * - connection pool,
     * - сетевые timeout-ы,
     * - OAuth2 client_credentials,
     * - диагностическое логирование запросов/ответов.
     */
    @Bean
    public WebClient paymentsWebClient(
            @Value("${integration.payments.baseUrl}") String baseUrl,
            @Value("${integration.payments.oauth2.registration-id}") String oauth2RegistrationId,
            @Value("${integration.payments.web-client.max-in-memory-size}") DataSize maxInMemorySize,
            @Value("${integration.payments.web-client.pool.name}") String poolName,
            @Value("${integration.payments.web-client.pool.max-connections}") int maxConnections,
            @Value("${integration.payments.web-client.pool.pending-acquire-timeout}") Duration pendingAcquireTimeout,
            @Value("${integration.payments.web-client.pool.max-idle-time}") Duration maxIdleTime,
            @Value("${integration.payments.web-client.timeouts.connect-timeout-millis}") int connectTimeoutMillis,
            @Value("${integration.payments.web-client.timeouts.response-timeout}") Duration responseTimeout,
            @Value("${integration.payments.web-client.timeouts.read-timeout}") Duration readTimeout,
            @Value("${integration.payments.web-client.timeouts.write-timeout}") Duration writeTimeout,
            ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {

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

        // Фильтр автоматически получает access token и подставляет Authorization: Bearer <token>.
        var oauth2Client = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        // ID регистрации клиента должен совпадать с spring.security.oauth2.client.registration.*.
        oauth2Client.setDefaultClientRegistrationId(oauth2RegistrationId);

        return WebClient.builder()
                // Базовый URL для всех запросов generated API-клиента в payments.
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .filter(logResponse())
                .filter(oauth2Client)
                // Лимит буфера на ответ; нужен для защиты от избыточного потребления памяти.
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(Math.toIntExact(maxInMemorySize.toBytes())))
                .build();
    }

    /**
     * Создает типобезопасный API-клиент payments поверх общего WebClient.
     */
    @Bean
    public DefaultApi paymentsApi(
            @Value("${integration.payments.baseUrl}") String baseUrl,
            WebClient paymentsWebClient) {
        var apiClient = new ApiClient(paymentsWebClient).setBasePath(baseUrl);
        return new DefaultApi(apiClient);
    }

    /**
     * Менеджер OAuth2-клиентов для flow client_credentials.
     * Отвечает за получение и переиспользование access token для межсервисных запросов.
     */
    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrations,
            ServerOAuth2AuthorizedClientRepository authorizedClients) {
        // Поддерживаем только machine-to-machine авторизацию.
        var authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();
        // Используем стандартный менеджер Spring Security.
        var authorizedClientManager = new DefaultReactiveOAuth2AuthorizedClientManager(
                // Источник client-id/secret/token-uri и остальных параметров провайдера.
                clientRegistrations,
                // Репозиторий хранения/кэширования токенов.
                authorizedClients);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
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
