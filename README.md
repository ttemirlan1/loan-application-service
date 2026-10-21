# Loan Application Service

Учебный микросервис для приёма заявок на микрокредит, их хранения в PostgreSQL и асинхронной обработки через RabbitMQ.

## Возможности

- Создание и получение заявки по идентификатору.
- Получение списка заявок с фильтрами по статусу и ПИН клиента, пагинацией и сортировкой по дате создания (новые первыми).
- Отмена заявки с проверкой допустимости перехода статуса.
- Валидация входных данных, обработка ошибок API (400, 404, 409 и 500).
- Асинхронный скоринг: после создания заявки событие публикуется через RabbitMQ, consumer обрабатывает заявку и обновляет её статус.
- Unit-тесты бизнес-логики.
- Проверка доступности приложения через Spring Boot Actuator.

## Технологии

- Java 17, Spring Boot 4.1.1, Spring Web MVC
- Spring Data JPA / Hibernate, Bean Validation
- PostgreSQL 16
- RabbitMQ 3.13 (Management UI)
- Maven, Docker и Docker Compose
- JUnit / Mockito

## Быстрый старт через Docker Compose

**Требования:** Docker с поддержкой Compose. Порты `8080`, `5432`, `5672` и `15672` на локальном компьютере должны быть свободны.

1. Создайте файл `.env` в корне проекта (он игнорируется Git):

   ```dotenv
   POSTGRES_DB=loan_db
   POSTGRES_USER=postgres
   POSTGRES_PASSWORD=change_me_local_password
   RABBITMQ_USER=guest
   RABBITMQ_PASSWORD=guest
   ```

   Значения — пример для локальной разработки. Не используйте эти учебные пароли для реального окружения.

2. Запустите сервисы:

   ```bash
   docker compose up --build -d
   docker compose ps
   ```

3. Проверьте приложение:

   ```bash
   curl http://localhost:8080/actuator/health
   ```

   Если приложение и его зависимости исправны, в ответе будет `"status": "UP"`.

4. При необходимости посмотрите логи:

   ```bash
   docker compose logs -f app
   ```

5. Для остановки:

   ```bash
   docker compose down
   ```

   Данные в именованных Docker volumes сохраняются. Команда `docker compose down -v` удалит их.

### Доступные адреса

| Сервис | Адрес |
|---|---|
| REST API | `http://localhost:8080` |
| Health | `http://localhost:8080/actuator/health` |
| RabbitMQ Management | `http://localhost:15672` |
| PostgreSQL | `localhost:5432` |

Веб-интерфейс RabbitMQ использует учётные данные из `.env`. Все указанные порты в `docker-compose.yml` привязаны к `127.0.0.1` на хосте.

## REST API

Базовый путь: `/api/v1/loan-applications`.

| Метод | Маршрут | Назначение |
|---|---|---|
| `POST` | `/api/v1/loan-applications` | Создать заявку; HTTP `201` и заголовок `Location` |
| `GET` | `/api/v1/loan-applications/{id}` | Получить заявку; HTTP `200` |
| `GET` | `/api/v1/loan-applications?status=APPROVED&clientPin=12345678901234&page=0&size=20` | Получить страницу заявок с фильтрами; HTTP `200` |
| `PATCH` | `/api/v1/loan-applications/{id}/cancel` | Отменить заявку при разрешённом переходе; HTTP `200` |

### Пример создания заявки

```http
POST http://localhost:8080/api/v1/loan-applications
Content-Type: application/json

{
  "clientPin": "12345678901234",
  "clientFullName": "Иванов Иван Иванович",
  "phone": "+996700123456",
  "amount": 120000.00,
  "termMonths": 12,
  "monthlyIncome": 30000.00
}
```

Поле `clientPin` должно содержать ровно 14 цифр, а `amount` — быть больше нуля. Для остальных обязательных полей действуют ограничения из `CreateLoanApplicationRequestDto`.

Создание заявки и обработка скоринга **асинхронны**: `POST` возвращает созданную заявку, а её статус может измениться чуть позже. Проверяйте результат запросом `GET /api/v1/loan-applications/{id}`.

Для проверки API можно открыть [requests.http](requests.http) в HTTP Client IntelliJ IDEA и выполнить запросы по очереди.

## RabbitMQ и скоринг

После сохранения заявки сервис публикует событие для скоринга. Сообщение отправляется после коммита транзакции; consumer читает очередь и выполняет оценку заявки.

| Параметр | Значение |
|---|---|
| Exchange | `loan.scoring.exchange` |
| Queue | `loan.scoring.queue` |
| Routing key | `loan.scoring.requested` |

Настройки из `application.yaml` для учебного скоринга:

- `loan.scoring.max-amount = 300000`
- `loan.scoring.max-payment-income-ratio = 0.5`

Для диагностики используйте RabbitMQ Management UI (`http://localhost:15672`) и `docker compose logs -f app`. Если consumer работает быстро, число сообщений в очереди может оставаться равным нулю: это само по себе **не** означает, что публикации не было.

## Статусы и ошибки

Основные статусы заявки: `NEW`, `IN_REVIEW`, `APPROVED`, `REJECTED`, `CANCELLED`.

- `400 Bad Request` — некорректный запрос или нарушение ограничений DTO.
- `404 Not Found` — заявка с указанным идентификатором отсутствует.
- `409 Conflict` — конфликт бизнес-правил (например, активная заявка клиента или запрещённый переход статуса).
- `500 Internal Server Error` — неожиданная ошибка на сервере.

Дублирующий `POST` для того же ПИН не обязательно вернёт `409`: проверка активной заявки зависит от её **текущего** статуса, который может быстро измениться после скоринга.

## Тестирование

Для локального запуска Maven требуется JDK 17. В Windows используйте:

```powershell
.\mvnw.cmd clean verify
```

На Linux/macOS:

```bash
./mvnw clean verify
```

В Dockerfile сборка образа выполняется с `-DskipTests`, поэтому запускайте тесты отдельно перед сдачей.

## Ограничения

Проект учебный, не интегрирован с продуктивными банковскими системами. Схема базы данных обновляется Hibernate (`ddl-auto: update`) для простоты локального запуска; это не стратегия миграций для production.
