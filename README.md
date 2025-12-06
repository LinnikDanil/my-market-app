# My Market App

Веб-приложение витрины интернет-магазина на Spring Boot. Показывает товары с поиском, сортировкой и пагинацией, позволяет добавлять их в корзину, оформлять заказы и управлять каталогом через административную страницу. Используются Thymeleaf-шаблоны из макета спринта, базой данных управляет Liquibase.

## Стек и архитектура
- Java 21, Gradle, Spring Boot 3.5 (Web MVC, Data JPA, Validation, Thymeleaf).
- PostgreSQL с миграциями Liquibase; в тестах — Testcontainers.
- Log4j2 для логирования.
- Docker/Docker Compose для контейнеризации приложения и базы.

## Основные возможности
- Витрина товаров: поиск по названию/описанию, сортировка по имени/цене и пагинация; изменение количества в корзине прямо со страницы.
- Страница товара: подробности и управление количеством выбранного товара.
- Корзина: список выбранных товаров, изменение количества, удаление и отображение общей суммы.
- Оформление заказа: создание заказа из корзины, просмотр списка заказов и деталей заказа.
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

## Подготовка окружения
Понадобятся Java 21, Docker Compose и доступный экземпляр PostgreSQL. Переменные среды, которые читает приложение:

- `DATASOURCE_SCHEMA_NAME` — схема БД.
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` — параметры подключения к PostgreSQL.
- `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE`, `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE` — ограничения на загрузку файлов.
- `IMAGE_PATH` — каталог для сохранения загружаемых изображений (создаётся автоматически).
- `LOGGING_PRACTICUM_LEVEL` — уровень логирования пакета `ru.practicum`.

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
Полностью контейнеризованный вариант (приложение + БД):
```bash
./gradlew clean build
docker compose up --build
```
Веб-интерфейс остаётся на `http://localhost:8080`, база доступна на `localhost:5433`.

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
