# Task Manager API

REST API для управления задачами с JWT-аутентификацией.
Каждый пользователь видит только свои задачи, администратор — задачи всех пользователей.

## Стек

- Java 17
- Spring Boot 4.1
- Spring Security 7 (JWT, HS256)  
- Spring Data JPA / Hibernate
- PostgreSQL 17 + Flyway
- springdoc-openapi (Swagger UI)
- Maven, Docker Compose
- JUnit 5, Mockito, AssertJ

## Статус

Проект в активной разработке. Ниже — что уже работает, а что в планах.

| Модуль | Статус |
|---|---|
| Регистрация пользователя | ✅ готово |
| Аутентификация (логин, выдача JWT) | 🚧 в работе |
| CRUD задач | 📋 план |
| Разграничение доступа (своя / чужая задача) | 📋 план |
| Админский read-only доступ | 📋 план |
| Обработка ошибок (ProblemDetail) | ✅ готово |
| Swagger UI | ✅ готово |

## Быстрый старт

Требования: JDK 17+, Docker.

```bash
# 1. Поднять PostgreSQL
docker compose up -d

# 2. Запустить приложение
./mvnw spring-boot:run
```

Приложение: http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui/index.html
PostgreSQL: `localhost:5433`, база/логин/пароль — `taskmanager`

Миграции Flyway применяются автоматически при старте.

## Конфигурация

| Свойство | Назначение | Значение по умолчанию |
|---|---|---|
| `security.jwt.secret` | секрет подписи HS256, **не короче 32 байт** | dev-заглушка (см. ниже) |
| `security.jwt.expiration` | время жизни токена | `60m` |
| `spring.datasource.url` | подключение к БД | `jdbc:postgresql://localhost:5433/taskmanager` |
| `spring.jpa.hibernate.ddl-auto` | схема управляется Flyway | `validate` |

**Секрет в репозитории — только dev-заглушка.** В реальном окружении он задаётся переменной:

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
```

## Архитектура

Пакеты организованы по фичам, а не по слоям:

```
com.github.potap322.taskmanager
├── auth/            # вход и регистрация
│   └── dto/
├── user/            # пользователи
│   └── dto/
├── task/            # задачи
├── security/        # Spring Security, JWT
│   └── jwt/
└── common/          # общее: обработка ошибок
    └── exception/
```

Зависимости направлены «внутрь»: `task` знает про `security`, но не наоборот; `security` знает про `user`; доменные сущности не знают о Spring Security.

### Путь запроса

```
HTTP → SecurityFilterChain
        ├── JwtAuthenticationFilter   (достаёт пользователя из токена)
        ├── AuthorizationFilter       (проверяет правила доступа)
        └── ExceptionTranslationFilter (401/403)
     → Controller → Service → Repository
```

## Модель данных

- `users.role` — `USER` или `ADMIN` (CHECK constraint)
- `tasks.status` — `TODO`, `IN_PROGRESS`, `DONE`
- `tasks.priority` — `LOW`, `MEDIUM`, `HIGH`
- `tasks.owner_id` → `users.id`, `ON DELETE CASCADE`

Миграции: `src/main/resources/db/migration`

## API

### Регистрация

`POST /api/auth/register` — доступ без токена.

```json
{
  "username": "ivan",
  "email": "ivan@mail.ru",
  "password": "password123"
}
```

`201 Created`:

```json
{
  "id": 1,
  "username": "ivan",
  "email": "ivan@mail.ru"
}
```

Ограничения: `username` 3–50 символов `[a-zA-Z0-9_]`, `password` 8–72 символа.
Роль всегда `USER` — задать её через запрос нельзя.

| Код | Когда |
|---|---|
| 201 | пользователь создан |
| 400 | невалидные данные |
| 409 | username или email уже заняты |

### Планируемые эндпоинты

| Метод | Путь | Доступ |
|---|---|---|
| POST | `/api/auth/login` | все |
| GET | `/api/tasks` | USER — свои, ADMIN — все |
| POST | `/api/tasks` | USER, ADMIN |
| GET | `/api/tasks/{id}` | владелец или ADMIN |
| PUT | `/api/tasks/{id}` | только владелец |
| PATCH | `/api/tasks/{id}/status` | только владелец |
| DELETE | `/api/tasks/{id}` | только владелец |
| GET | `/api/admin/users` | только ADMIN |

## Формат ошибок

Все ошибки — [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) (`application/problem+json`):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/auth/register",
  "errors": {
    "password": "Password must be between 8 and 72 characters"
  }
}
```

| Код | Значение |
|---|---|
| 400 | невалидный запрос или тело |
| 401 | нет токена / токен недействителен |
| 403 | аутентифицирован, но нет прав |
| 404 | ресурс или эндпоинт не найден |
| 409 | конфликт (дубликат username/email) |
| 500 | внутренняя ошибка (детали только в логах) |

## Решения по безопасности

- **Stateless.** Сессии не используются (`SessionCreationPolicy.STATELESS`), поэтому CSRF отключён: подделывать нечего — браузер не отправляет учётные данные автоматически.
- **Роль не хранится в токене.** На каждом запросе пользователь загружается из БД, поэтому смена роли или удаление аккаунта действуют немедленно, а не после истечения токена.
- **Пароли — BCrypt.** В токен и в ответы API пароль не попадает никогда.
- **`ROLE_` префикс.** `hasRole("ADMIN")` работает только потому, что authorities имеют вид `ROLE_ADMIN`.
- **Чужая задача → 403.** Осознанный выбор: 404 скрывал бы существование ресурса, но для учебного проекта нагляднее явный отказ.
- **Администратор только читает** чужие данные. Мутации доступны исключительно владельцу.

## Тесты

```bash
./mvnw test -Dtest=JwtServiceTest      # подпись, срок, чужой пользователь
./mvnw test -Dtest=UserServiceTest     # дубликаты, нормализация, BCrypt, роль
```

> `./mvnw test` целиком пока не проходит: контекстный тест требует запущенного PostgreSQL.
> Интеграционные тесты на Testcontainers — в планах.
