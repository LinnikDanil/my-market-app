# My Market App

Учебный проект: веб-приложение витрины интернет-магазина на Spring Boot (реактивный стек). Для него используется упрощённая реализация корзины (без пользователей) и сервиса платежей. Витрина показывает товары с поиском, сортировкой и пагинацией, позволяет добавлять их в корзину и оформлять заказы. Используются Thymeleaf-шаблоны, схемой базы данных управляет Liquibase.
## Стек и архитектура
- Java 21, Gradle, Spring Boot 3.5 (WebFlux, Data R2DBC, Validation, Thymeleaf).
- PostgreSQL с миграциями Liquibase; реактивный доступ через R2DBC, в тестах — Testcontainers.
- Redis для кеширования витрины, карточек товара и состава корзины.
- Отдельный сервис платежей (Spring WebFlux) с REST API для холда, подтверждения и отмены платежей.
- Embedded Netty как встроенный веб-сервер.
- Log4j2 для логирования.
- Docker/Docker Compose для контейнеризации приложения и базы.

## Основные возможности
- Витрина товаров: поиск по названию/описанию, сортировка по имени/цене и пагинация; изменение количества в корзине прямо со страницы. Данные списка кешируются в Redis.
- Страница товара: подробности и управление количеством выбранного товара. Карточка товара кешируется в Redis.
- Корзина: список выбранных товаров, изменение количества, удаление и отображение общей суммы. Данные о товарах берутся из кеша Redis.
- Оформление заказа: создание заказа из корзины, просмотр списка заказов и деталей заказа. При покупке выполняется REST-вызов в сервис платежей (hold → confirm или cancel при ошибке).
- Админ-панель: загрузка товаров из Excel и загрузка изображений для существующих карточек.

## Маршруты интерфейса
- `GET /` или `/items` — витрина товаров с параметрами `search`, `sort`, `pageNumber`, `pageSize`.
- `POST /items` — изменение количества товара на витрине (`action=PLUS|MINUS`).
- `GET /items/{id}` — страница товара.
- `POST /items/{id}` — изменение количества на странице товара (`action=PLUS|MINUS`).
- `GET /cart/items` и `POST /cart/items` — просмотр и изменение корзины (`action=PLUS|MINUS|DELETE`).
- `POST /buy` — оформление заказа.
- `GET /orders` и `GET /orders/{id}` — список и карточка заказа (флаг `newOrder=true` подсвечивает свежий заказ).
- `GET /admin` — админ-панель; `POST /admin/items/upload` — загрузка товаров из Excel; `POST /admin/items/{id}/image` — загрузка изображения.

## Сервис платежей (REST API)
- `GET /api/payments/balance` — текущий баланс.
- `POST /api/payments/balance` — пополнение баланса.
- `POST /api/payments/hold` — заморозка суммы (возвращает `paymentId`).
- `POST /api/payments/confirm/{paymentId}` — подтверждение платежа.
- `POST /api/payments/cancel/{paymentId}` — отмена платежа.
Для сервиса платежей доступен Swagger UI (спецификация в браузере), где можно посмотреть методы и выполнить запрос пополнения баланса.

## Подготовка окружения
Понадобятся Java 21, Docker Compose и доступный экземпляр PostgreSQL. Переменные среды, которые читает приложение:

- `DATASOURCE_SCHEMA_NAME` — схема БД.
- `SPRING_R2DBC_URL`, `SPRING_R2DBC_USERNAME`, `SPRING_R2DBC_PASSWORD` — параметры подключения через R2DBC.
- `SPRING_LIQUIBASE_URL`, `SPRING_LIQUIBASE_USER`, `SPRING_LIQUIBASE_PASSWORD` — параметры подключения Liquibase (JDBC).
- `SPRING_WEBFLUX_MULTIPART_MAX_IN_MEMORY_SIZE`, `SPRING_WEBFLUX_MULTIPART_MAX_DISK_USAGE_PER_PART`, `SPRING_WEBFLUX_MULTIPART_MAX_PARTS` — ограничения на загрузку файлов.
- `SPRING_CODEC_MAX_IN_MEMORY_SIZE` — лимит размера буфера кодеков WebFlux.
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_TTL` — настройки Redis и TTL кеша товаров.
- `IMAGE_PATH` — каталог для сохранения загружаемых изображений (создаётся автоматически).
- `LOGGING_PRACTICUM_LEVEL` — уровень логирования пакета `ru.practicum`.
- `PAYMENTS_BASE_URL` — базовый URL сервиса платежей.

Для локальной разработки можно взять значения из `src/main/resources/application-local.yaml`.

## Локальный запуск с Gradle
1. Запустите PostgreSQL (например, `docker compose up -d db` использует порт `5433` на хосте).
2. Экспортируйте переменные среды, совпадающие с вашим подключением к БД и путями загрузки.
3. Самостоятельно создайте схему с названием, указанным в `DATASOURCE_SCHEMA_NAME`
4. Соберите и запустите приложение:
   ```bash
   ./gradlew clean bootRun
   ```
   или упакуйте JAR и запустите:
   ```bash
   ./gradlew clean build
   java -jar build/libs/my-market-app-0.0.1.jar
   ```
5. Приложение будет доступно на http://localhost:8080, миграции Liquibase применяются автоматически.

## Запуск в Docker
Полностью контейнеризованный вариант (приложение + БД + Redis + сервис платежей). В `docker-compose.yml` уже заданы `SPRING_R2DBC_*`, `SPRING_LIQUIBASE_*`, `REDIS_*`, `PAYMENTS_BASE_URL` и ограничения WebFlux — при необходимости скорректируйте их под свою БД и размеры загрузок:
```bash
./gradlew clean build
docker compose up --build
```
Веб-интерфейс остаётся на `http://localhost:8080`, база доступна на `localhost:5433`, Redis — на `localhost:6379`, сервис платежей — на `http://localhost:8081`.

## Тесты
- Юнит-тесты и Spring Boot tests: `./gradlew test`
- Интеграционные тесты (суффикс `IT`, Testcontainers): `./gradlew integrationTest`

Для прогона интеграционных тестов необходим Docker.

## Полезные директории
- Шаблоны интерфейса — `src/main/resources/templates`.
- Миграции БД — `src/main/resources/db/changelog`.
- Локальный пример настроек — `src/main/resources/application-local.yaml`.
- Основной код приложения — `src/main/java/ru/practicum/market`.
- Тестовые товары для загрузки из excel — `dev/Items.xlsx`.
