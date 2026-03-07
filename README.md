# My Market App

Полноценный мультипроект интернет-магазина на Spring Boot WebFlux:

- `market` — витрина, корзина, заказы, регистрация и авторизация пользователей.
- `payments` — RESTful-сервис платежей.

Приложение использует Spring Security на двух уровнях:

- пользовательская авторизация в `market` (логин/пароль, роли, CSRF, logout с очисткой сессии/cookies);
- межсервисная OAuth2-авторизация (`Client Credentials`) между `market` и `payments` через Keycloak.

## Функциональность

- Витрина товаров: поиск, сортировка, пагинация, карточка товара.
- Анонимный пользователь может только просматривать витрину и карточки товара.
- Авторизованный пользователь получает доступ к корзине, заказам и покупке.
- Корзина и заказы привязаны к текущему пользователю (`user_id` в БД).
- Кнопка покупки активна только при достаточном балансе; при недоступности платежного сервиса показывается сообщение.
- Админ-функции (`ROLE_ADMIN`):
    - загрузка товаров из Excel (`dev/Items.xlsx`),
    - загрузка изображений товаров.
- Кеширование в Redis:
    - карточка товара,
    - страница витрины,
    - данные товаров для корзины.

## Безопасность

### 1. Пользовательская авторизация в `market`

- Form Login: `GET /login`, `POST /login`.
- Регистрация: `GET /registerform`, `POST /register`.
- Logout: `POST /logout`.
- Пароли в БД хранятся в BCrypt.
- При logout очищаются `SecurityContext`, `WebSession`, cookies `SESSION` и `JSESSIONID`.

Маршруты `market` по доступу:

- Публичные: `/`, `/items/**`, `/login`, `/register`, `/registerform`, `/images/**`, `/access-denied`.
- Только `ROLE_USER`/`ROLE_ADMIN`: `/cart/**`, `/orders/**`, `/buy/**`.
- Только `ROLE_ADMIN`: `/admin/**`.

### 2. Межсервисная OAuth2-авторизация

- `market` работает как OAuth2 client (`client_credentials`) и получает токен в Keycloak.
- `payments` работает как OAuth2 resource server (JWT).
- Все endpoint-ы `payments` требуют аутентификацию.
- Бизнес-endpoint-ы `payments` дополнительно требуют authority `SERVICE`.
- Если `market` неавторизован в Keycloak, запросы в `payments` отклоняются (401/403), а в UI покупка становится
  недоступной.

## Технологии

- Java 21
- Gradle 8
- Spring Boot 3.5 (WebFlux, Security, OAuth2 Client/Resource Server, Thymeleaf)
- Spring Data R2DBC + PostgreSQL
- Liquibase
- Redis
- Keycloak 24 (Docker)
- OpenAPI Generator
- JUnit 5, Spring Boot Test, Spring Security Test, Testcontainers

## Структура репозитория

- `market` — основное web-приложение витрины.
- `payments` — сервис платежей.
- `docker/keycloak/realm-export.json` — preconfigured realm/clients/roles для OAuth2.
- `docker-compose.yml` — инфраструктура и запуск сервисов.
- `dev/Items.xlsx` — пример Excel для загрузки товаров.

## Быстрый старт (рекомендуемый способ)

### Требования

- Docker + Docker Compose
- Java 21 (для сборки jar перед запуском контейнеров)

### 1) Собрать проекты

```bash
bash ./gradlew clean build
```

### 2) Поднять окружение

```bash
docker compose up --build
```

### 3) Доступные сервисы

- Market UI: <http://localhost:8081/items>
- Payments API (Swagger UI): <http://localhost:8082/swagger-ui.html>
- Keycloak: <http://localhost:8080>
- PostgreSQL: `localhost:5433`
- Redis: `localhost:6379`

### 4) Тестовые пользователи `market`

Создаются миграциями Liquibase:

- `user / user` — `ROLE_USER`
- `admin / admin` — `ROLE_ADMIN`

Также можно зарегистрировать нового пользователя через `/registerform` (ему назначается `ROLE_USER`).

## Локальный запуск (приложения из IDE/Gradle)

1. Поднимите инфраструктуру:

```bash
docker compose up -d db redis keycloak
```

2. Запустите `payments` (профиль `local`) в первом терминале:

```bash
bash ./gradlew :payments:bootRun --args='--spring.profiles.active=local'
```

3. Запустите `market` (профиль `local`) во втором терминале:

```bash
bash ./gradlew :market:bootRun --args='--spring.profiles.active=local'
```

Параметры для `local` уже описаны в:

- `market/src/main/resources/application-local.yaml`
- `payments/src/main/resources/application-local.yaml`

## Проверка OAuth2 вручную

Получить access token от Keycloak для `market-service`:

```bash
curl -s -X POST 'http://localhost:8080/realms/market-app/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=client_credentials' \
  -d 'client_id=market-service' \
  -d 'client_secret=NAUYDbr59boHTAr3CzX1BXeXfc9T75lg'
```

Проверить защищённый endpoint `payments`:

```bash
curl -i 'http://localhost:8082/api/payments/balance/1'
# ожидаемо: 401

curl -i 'http://localhost:8082/api/payments/balance/1' \
  -H 'Authorization: Bearer <ACCESS_TOKEN>'
# ожидаемо: 200
```

## API `payments`

- `GET /api/payments/balance/{userId}` — баланс пользователя
- `POST /api/payments/balance/{userId}` — пополнение баланса
- `POST /api/payments/hold/{userId}` — hold суммы
- `POST /api/payments/confirm/{paymentId}` — подтверждение hold
- `POST /api/payments/cancel/{paymentId}` — отмена hold

Контракт: `payments/src/main/resources/api-spec.yaml`.

Особенности реализации `payments`:

- баланс ведётся отдельно для каждого `userId`;
- стартовый баланс пользователя: `5000`;
- данные баланса/hold хранятся in-memory и сбрасываются при перезапуске сервиса.

## Импорт товаров и изображений

1. Войдите как `admin / admin`.
2. Откройте `/admin`.
3. Загрузите Excel с листом `Items` и колонками:
    - `Title`
    - `Description`
    - `Price`
4. При необходимости загрузите изображение для выбранного товара.

## Тесты

```bash
# все unit + slice тесты обоих модулей
bash ./gradlew test

# unit/slice тесты по модулям
bash ./gradlew :market:test :payments:test

# интеграционные тесты market (Testcontainers)
bash ./gradlew :market:integrationTest
```

Для `integrationTest` нужен Docker.

## Полезные команды

```bash
# пересобрать всё
./gradlew clean build

# поднять только инфраструктуру
docker compose up -d db redis keycloak

# остановить окружение
docker compose down
```
