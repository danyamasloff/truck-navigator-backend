# TruckNavigator Backend - Сервер аналитической системы грузоперевозок

Бэкенд-сервер для аналитической подсистемы автоматизированной системы управления (АСУ) грузовыми автомобильными перевозками в России. Система предоставляет REST API для управления маршрутами, водителями, транспортом и грузами с продвинутой аналитикой и оптимизацией.

## 🚀 Основные возможности

### 📍 Интеллектуальная маршрутизация

-   **Построение оптимальных маршрутов** с учетом габаритов, веса и типа груза
-   **Интеграция с GraphHopper** для точной навигации по дорогам России
-   **Геокодирование адресов** с поддержкой российских адресов
-   **Расчет времени и расстояния** с учетом дорожных ограничений

### 🔍 Предиктивная аналитика

-   **Анализ погодных рисков** на маршруте с интеграцией OpenWeatherMap
-   **Оценка качества дорожного покрытия** и рельефа местности
-   **Прогнозирование рисков** для различных типов грузов
-   **Мониторинг состояния транспортных средств**

### 💰 Экономический анализ

-   **Динамический расчет стоимости рейса** в реальном времени
-   **Учет цен на топливо** с актуальными данными
-   **Расчет платных дорог** и дополнительных расходов
-   **Анализ рентабельности** и оптимизация затрат

### ⚖️ Комплаенс и безопасность

-   **Контроль режима труда и отдыха (РТО)** водителей
-   **Мониторинг весогабаритных ограничений**
-   **Контроль требований** к перевозке специфических грузов
-   **JWT-аутентификация** и авторизация пользователей

## 🛠 Технологический стек

### Backend

-   **Java 17** - основной язык разработки
-   **Spring Boot 3.2.4** - фреймворк приложения
-   **Spring Security** - безопасность и аутентификация
-   **Spring Data JPA** - работа с базой данных
-   **Hibernate Spatial** - поддержка геопространственных данных

### База данных

-   **PostgreSQL** - основная СУБД
-   **PostGIS** - расширение для геопространственных данных
-   **Hibernate** - ORM для работы с БД

### Внешние интеграции

-   **GraphHopper** - маршрутизация и геокодирование
-   **OpenWeatherMap API** - погодные данные
-   **Fuel Price API** - актуальные цены на топливо

### Инфраструктура

-   **Docker & Docker Compose** - контейнеризация
-   **Maven** - сборка проекта
-   **Swagger/OpenAPI 3** - документация API
-   **Caffeine** - кэширование
-   **Logback** - логирование

## 🏗 Архитектура проекта

```
ru.maslov.trucknavigator/
├── 🎛 controller/           # REST API контроллеры
│   ├── AuthController       # Аутентификация и авторизация
│   ├── RouteController      # Управление маршрутами
│   ├── DriverController     # Управление водителями
│   ├── VehicleController    # Управление транспортом
│   ├── CargoController      # Управление грузами
│   ├── GeocodingController  # Геокодирование адресов
│   ├── WeatherController    # Погодная аналитика
│   └── RouteAnalyticsController # Аналитика маршрутов
│
├── 🏢 service/              # Бизнес-логика
│   ├── analytics/           # Аналитические сервисы
│   ├── compliance/          # Комплаенс и соответствие
│   ├── RouteService         # Сервис маршрутов
│   ├── DriverService        # Сервис водителей
│   └── VehicleService       # Сервис транспорта
│
├── 🗄 repository/           # Репозитории данных
├── 🏛 entity/               # JPA сущности
├── 📦 dto/                  # Объекты передачи данных
│   ├── routing/             # DTO для маршрутизации
│   ├── analytics/           # DTO для аналитики
│   ├── auth/                # DTO для аутентификации
│   └── weather/             # DTO для погодных данных
│
├── 🔌 integration/          # Внешние интеграции
│   ├── graphhopper/         # GraphHopper API
│   ├── openweather/         # OpenWeatherMap API
│   └── fuelprice/           # Fuel Price API
│
├── 🔒 security/             # Настройки безопасности
├── ⚙️ config/               # Конфигурация приложения
├── 🗺 mapper/               # MapStruct маппинг
└── ⚠️ exception/            # Обработка исключений
```

## 📡 API Endpoints

### 🔐 Аутентификация

```http
POST /api/auth/login          # Вход в систему
POST /api/auth/register       # Регистрация пользователя
POST /api/auth/logout         # Выход из системы
POST /api/auth/refresh        # Обновление токена
```

### 🗺 Маршруты

```http
GET    /api/routes                    # Список маршрутов
POST   /api/routes                    # Создание маршрута
GET    /api/routes/{id}               # Детали маршрута
PUT    /api/routes/{id}               # Обновление маршрута
DELETE /api/routes/{id}               # Удаление маршрута
POST   /api/routes/calculate          # Расчет маршрута
POST   /api/routes/{id}/optimize      # Оптимизация маршрута
```

### 👨‍💼 Водители

```http
GET    /api/drivers                   # Список водителей
POST   /api/drivers                   # Добавление водителя
GET    /api/drivers/{id}              # Профиль водителя
PUT    /api/drivers/{id}              # Обновление профиля
POST   /api/drivers/{id}/analyze-rest-time  # Анализ РТО
GET    /api/drivers/{id}/performance  # Показатели эффективности
```

### 🚛 Транспортные средства

```http
GET    /api/vehicles                  # Парк транспорта
POST   /api/vehicles                  # Добавление ТС
GET    /api/vehicles/{id}             # Информация о ТС
PUT    /api/vehicles/{id}             # Обновление данных ТС
POST   /api/vehicles/{id}/maintenance # Техобслуживание
```

### 📦 Грузы

```http
GET    /api/cargo                     # Список грузов
POST   /api/cargo                     # Создание груза
GET    /api/cargo/{id}                # Детали груза
PUT    /api/cargo/{id}                # Обновление груза
POST   /api/cargo/{id}/assign         # Назначение на маршрут
```

### 🌍 Геокодирование

```http
GET    /api/geocoding/search          # Поиск адресов
GET    /api/geocoding/reverse         # Обратное геокодирование
POST   /api/geocoding/batch           # Пакетное геокодирование
```

### 🌤 Погода и аналитика

```http
GET    /api/weather/route/{routeId}   # Погода на маршруте
GET    /api/analytics/route/{routeId} # Аналитика маршрута
GET    /api/analytics/driver/{driverId} # Аналитика водителя
GET    /api/analytics/fleet           # Аналитика парка
```

## 🚀 Быстрый старт

### Предварительные требования

-   **Java 17+**
-   **Maven 3.6+**
-   **Docker & Docker Compose**
-   **PostgreSQL 14+** (если запуск без Docker)

### 🐳 Запуск через Docker Compose (рекомендуется)

1. **Клонирование репозитория:**

    ```bash
    git clone https://github.com/danyamasloff/truck-navigator-backend.git
    cd truck-navigator-backend
    ```

2. **Настройка переменных окружения:**

    ```bash
    # Создайте файл .env на основе примера
    cp .env.example .env
    # Отредактируйте .env файл с вашими настройками
    ```

3. **Сборка и запуск:**

    ```bash
    # Сборка приложения
    mvn clean package -DskipTests

    # Запуск всех сервисов
    docker-compose up -d
    ```

4. **Проверка работоспособности:**

    ```bash
    # API документация
    http://localhost:8080/swagger-ui.html

    # Health check
    curl http://localhost:8080/actuator/health
    ```

### 💻 Локальная разработка

1. **Настройка базы данных:**

    ```bash
    # Запуск только PostgreSQL
    docker-compose up -d postgres
    ```

2. **Настройка конфигурации:**

    ```bash
    # Скопируйте application-dev.properties.example
    cp src/main/resources/application-dev.properties.example \
       src/main/resources/application-dev.properties
    ```

3. **Запуск приложения:**
    ```bash
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
    ```

## 🔧 Конфигурация

### Основные настройки

```properties
# Порт сервера
server.port=8080

# База данных
spring.datasource.url=jdbc:postgresql://localhost:5432/truck_navigator
spring.datasource.username=truck_user
spring.datasource.password=truck_password

# JWT
app.jwt.secret=your-secret-key
app.jwt.expiration=86400000

# GraphHopper
app.graphhopper.api-key=your-graphhopper-key
app.graphhopper.base-url=https://graphhopper.com/api/1

# OpenWeatherMap
app.weather.api-key=your-openweather-key
app.weather.base-url=https://api.openweathermap.org/data/2.5
```

### Профили окружения

-   **dev** - разработка (подробное логирование, H2 для тестов)
-   **prod** - продакшн (оптимизированное логирование, PostgreSQL)
-   **test** - тестирование (in-memory база, моки внешних сервисов)

## 📊 Мониторинг и метрики

Приложение предоставляет endpoints для мониторинга через Spring Boot Actuator:

```http
GET /actuator/health          # Состояние приложения
GET /actuator/metrics         # Метрики производительности
GET /actuator/info            # Информация о приложении
GET /actuator/prometheus      # Метрики для Prometheus
```

## 🧪 Тестирование

```bash
# Запуск всех тестов
mvn test

# Запуск интеграционных тестов
mvn test -Dtest="*IntegrationTest"

# Запуск с покрытием кода
mvn test jacoco:report
```

## 📚 Документация API

После запуска приложения документация API доступна по адресам:

-   **Swagger UI**: http://localhost:8080/swagger-ui.html
-   **OpenAPI JSON**: http://localhost:8080/v3/api-docs
-   **OpenAPI YAML**: http://localhost:8080/v3/api-docs.yaml

## 🤝 Разработка

### Стиль кода

-   Используется **Google Java Style Guide**
-   **Lombok** для уменьшения boilerplate кода
-   **MapStruct** для маппинга между DTO и Entity
-   **Валидация** через Bean Validation (JSR-303)

### Структура коммитов

Проект использует **Conventional Commits**:

-   `feat:` - новая функциональность
-   `fix:` - исправление ошибок
-   `refactor:` - рефакторинг кода
-   `docs:` - изменения в документации
-   `test:` - добавление тестов

## 📄 Лицензия

Проект распространяется под лицензией **MIT**. См. файл [LICENSE](LICENSE) для подробностей.

## 👨‍💻 Автор

**Данила Маслов** - [GitHub](https://github.com/danyamasloff)

---

_Для вопросов и предложений создавайте Issues в репозитории проекта._
