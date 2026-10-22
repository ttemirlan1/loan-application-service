# Loan Application Service

Учебный микросервис для приёма заявок на микрокредит, хранения данных в PostgreSQL и асинхронного скоринга через RabbitMQ.

Цель проекта — продемонстрировать разработку REST API на Spring Boot, работу с транзакциями и статусами, интеграцию с брокером сообщений, обработку ошибок, тестирование и запуск через Docker Compose.

## Возможности

- Создание заявки и получение её по ID.
- Получение списка заявок с фильтрами `status` и `clientPin`, пагинацией и сортировкой по `createdAt DESC`.
- Отмена заявки с проверкой допустимого перехода статуса.
- Проверка входных данных с помощью Jakarta Bean Validation и централизованная обработка ошибок.
- Асинхронный скоринг: событие отправляется в RabbitMQ после коммита транзакции создания заявки.
- Защита конкурентных изменений статуса с помощью блокировки записи PostgreSQL (`findByIdForUpdate`).
- Повторная отправка зависших заявок `NEW` планировщиком (при наличии последних локальных изменений).
- Ограниченные повторы обработки RabbitMQ и Dead Letter Queue для неуспешных сообщений (при наличии последних локальных изменений).
- Unit-тесты бизнес-логики и проверка состояния приложения через Actuator.

## Технологии

| Компонент | Технология |
|---|---|
| Язык | Java 17 |
| Framework | Spring Boot 4.1.1, Spring Web MVC |
| Persistence | Spring Data JPA, Hibernate |
| База данных | PostgreSQL 16 |
| Брокер сообщений | RabbitMQ 3.13 с Management UI |
| Валидация | Jakarta Bean Validation |
| Сборка | Maven Wrapper |
| Тестирование | JUnit 5, Mockito |
| Инфраструктура | Docker, Docker Compose |

## Архитектура и обработка заявки

```text
Client
  |
  | POST /api/v1/loan-applications
  v
REST Controller -> LoanApplicationService -> PostgreSQL (NEW)
                           |
                           | AFTER_COMMIT event
                           v
                   LoanMessagePublisher
                           |
                           v
                    RabbitMQ Exchange
                           |
                           v
                    loan.scoring.queue
                           |
                           v
                   LoanScoringConsumer
                           |
                           v
                 Transactional process()
                           |
                           v
                   PostgreSQL (APPROVED / REJECTED)
```

1. REST API валидирует запрос и сохраняет заявку со статусом `NEW`.
2. После успешного коммита `LoanApplicationEventListener` инициирует публикацию `LoanScoringMessage` в RabbitMQ.
3. Consumer получает сообщение и вызывает транзакционную обработку скоринга.
4. Запись заявки блокируется через `findByIdForUpdate()`. Обработка продолжается только для статуса `NEW`.
5. Скоринг возвращает `APPROVED` либо `REJECTED` с причиной отказа. Результат сохраняется в PostgreSQL.
6. Клиент получает итоговый статус через `GET /api/v1/loan-applications/{id}`.

Скоринг выполняется асинхронно. Ответ `201 Created` не означает, что скоринг уже завершён.

## Быстрый старт через Docker Compose

**Требования:** установлен Docker с поддержкой Compose; свободны локальные порты `8080`, `5432`, `5672`, `15672`.

1. Создайте файл `.env` в корне проекта:

   ```dotenv
   POSTGRES_DB=loan_db
   POSTGRES_USER=postgres
   POSTGRES_PASSWORD=change_me_local_password
   RABBITMQ_USER=guest
   RABBITMQ_PASSWORD=guest
   ```

   Значения приведены только для локального запуска. Файл `.env` не нужно коммитить.

2. Запустите приложение и зависимости:

   ```bash
   docker compose up --build -d
   docker compose ps
   ```

3. Проверьте health endpoint:

   ```bash
   curl http://localhost:8080/actuator/health
   ```

   При исправной конфигурации ожидается ответ с `"status": "UP"`.

4. Для диагностики:

   ```bash
   docker compose logs -f app
   ```

5. Для остановки:

   ```bash
   docker compose down
   ```

   Именованные volumes сохраняются. **`docker compose down -v` удаляет данные** PostgreSQL и RabbitMQ в volumes.

### Локальные адреса

| Сервис | Адрес |
|---|---|
| REST API | `http://localhost:8080` |
| Actuator Health | `http://localhost:8080/actuator/health` |
| RabbitMQ Management | `http://localhost:15672` |
| PostgreSQL | `localhost:5432` |

В `docker-compose.yml` порты опубликованы только на `127.0.0.1` хост-машины. Для входа в RabbitMQ Management используются `RABBITMQ_USER` и `RABBITMQ_PASSWORD` из `.env`.

## REST API

Базовый URL: `http://localhost:8080/api/v1/loan-applications`.

| Метод | Endpoint | Описание |
|---|---|---|
| `POST` | `/api/v1/loan-applications` | Создание заявки; `201 Created`, заголовок `Location` |
| `GET` | `/api/v1/loan-applications/{id}` | Получение по ID; `200 OK` |
| `GET` | `/api/v1/loan-applications` | Список, фильтры и пагинация; `200 OK` |
| `PATCH` | `/api/v1/loan-applications/{id}/cancel` | Отмена при допустимом переходе статуса; `200 OK` |

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

После создания заявка сначала получает `NEW`; позже consumer может перевести её в `APPROVED` или `REJECTED`.

### Фильтрация и пагинация

```http
GET http://localhost:8080/api/v1/loan-applications?status=APPROVED&clientPin=12345678901234&page=0&size=20
```

Сортировка по умолчанию: новые заявки первыми (`createdAt DESC`). Для ручной проверки готов файл [`requests.http`](requests.http), который можно открыть в HTTP Client IntelliJ IDEA.

### Ошибки API

| HTTP | Причина |
|---|---|
| `400 Bad Request` | Ошибочные параметры или нарушение ограничений DTO |
| `404 Not Found` | Заявка не найдена |
| `409 Conflict` | Активная заявка клиента или запрещённый переход статуса |
| `500 Internal Server Error` | Непредвиденная ошибка сервиса |

Проверка наличия активной заявки в сервисе сама по себе **не гарантирует** защиту от двух одновременных `POST`. Для этой гарантии нужен уникальный индекс PostgreSQL и корректное преобразование ошибки ограничения в `409` — см. раздел «Перед сдачей».

## Статусы и правила переходов

| Текущий статус | Действие | Новый статус |
|---|---|---|
| `NEW` | Начало скоринга | `IN_REVIEW` |
| `IN_REVIEW` | Скоринг пройден | `APPROVED` |
| `IN_REVIEW` | Скоринг не пройден | `REJECTED` |
| `NEW` / `IN_REVIEW` | Отмена (если разрешена валидатором) | `CANCELLED` |

Завершённые состояния `APPROVED`, `REJECTED` и `CANCELLED` не должны перезаписываться скорингом или отменой.

Для защиты от гонки между `cancel()` и скорингом обе операции чтения и изменения статуса выполняются в транзакции с пессимистической блокировкой `findByIdForUpdate()`. При повторной доставке сообщения скоринг пропускает заявку, если её статус уже не `NEW`.

В текущей реализации переходы `NEW -> IN_REVIEW -> APPROVED/REJECTED` могут выполняться в одной транзакции: промежуточный `IN_REVIEW` обычно не виден другим запросам.

## Правила учебного скоринга

Используются два упрощённых бизнес-ограничения:

1. Допустимая сумма рассчитывается как `monthlyIncome × maxPaymentIncomeRatio × termMonths`. Если `amount` больше этого значения, заявка отклоняется.
2. Если `amount` превышает `maxAmount`, заявка отклоняется.

Конфигурация:

```yaml
loan:
  scoring:
    max-amount: 300000
    max-payment-income-ratio: 0.5
```

Если обе проверки пройдены — статус `APPROVED`. Модель учебная: она не рассчитывает проценты по кредиту, кредитную историю и иные реальные факторы риска.

## RabbitMQ: очереди и обработка ошибок

### Основной маршрут

| Назначение | Имя |
|---|---|
| Exchange | `loan.scoring.exchange` |
| Queue | `loan.scoring.queue` |
| Routing key | `loan.scoring.requested` |

### Повторные попытки и DLQ

Для обработки временных ошибок consumer предусмотрены ограниченные попытки и Dead Letter Queue:

| Назначение | Имя |
|---|---|
| Dead Letter Exchange | `loan.scoring.dlx` |
| Dead Letter Queue | `loan.scoring.dlq` |
| Dead letter routing key | `loan.scoring.failed` |

Пример настроек **Spring Boot 4.x** в `application.yaml` (при условии, что DLX/DLQ созданы в `RabbitMqConfig`):

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true
          max-retries: 2
          initial-interval: 1000ms
          multiplier: 2.0
          max-interval: 10000ms
        default-requeue-rejected: false
```

При `max-retries: 2` сообщение обрабатывается первоначально и может быть повторно обработано ещё дважды. После исчерпания попыток сообщение отклоняется; при настроенном DLX оно направляется в `loan.scoring.dlq` для диагностики. Ошибки consumer должны пробрасываться, а не только логироваться.

**Примечание:** при изменении аргументов уже существующей `loan.scoring.queue` RabbitMQ может сообщить о конфликте объявления (`PRECONDITION_FAILED`). В локальном тестовом окружении очередь можно пересоздать только после проверки, что в ней нет важных сообщений.

## Восстановление зависших заявок NEW

`LoanScoringRetryJob` предназначен для ситуации, когда заявка сохранена, но сообщение не удалось отправить в RabbitMQ:

1. Планировщик запускается примерно раз в минуту.
2. Он выбирает до 100 заявок в статусе `NEW`, созданных более 5 минут назад.
3. Повторно отправляет `LoanScoringMessage` через существующий `LoanMessagePublisher`.
4. Consumer проверяет статус под блокировкой; повторно доставленные сообщения для завершённых заявок пропускаются.

Пример параметра:

```yaml
loan:
  scoring:
    retry:
      fixed-delay-ms: 60000
```

Это **упрощённое восстановление**, а не гарантия доставки: без отметки последней отправки, ограничения попыток и publisher confirms одно сообщение может публиковаться многократно. Для production-подхода следует рассмотреть Transactional Outbox, учёт попыток и мониторинг DLQ.

## Проверка отказоустойчивости

### RabbitMQ недоступен при создании заявки

1. При работающем приложении выполните `docker compose stop rabbitmq`.
2. Создайте заявку: при доступном PostgreSQL она должна сохраниться как `NEW`, даже если отправка сообщения завершится ошибкой.
3. Выполните `docker compose start rabbitmq`.
4. После достижения порога в 5 минут планировщик должен повторить публикацию.
5. Проверьте итоговый статус через `GET /api/v1/loan-applications/{id}` и логи приложения.

### Ошибка во время обработки сообщения

На локальном стенде временно сымитируйте исключение в consumer (затем обязательно уберите его). После исчерпания попыток проверьте наличие сообщения в `loan.scoring.dlq` через RabbitMQ Management UI.

## Тестирование

Требуется JDK 17. Unit-тесты реализованы с JUnit 5 и Mockito.

**Windows (PowerShell):**

```powershell
.\mvnw.cmd "-Dtest=LoanApplicationServiceTest,ScoringServiceTest,LoanStatusTransitionValidatorTest" test
```

**Полный прогон тестов:**

```powershell
.\mvnw.cmd clean test
```

**Linux/macOS:**

```bash
./mvnw clean test
```

Полный запуск может требовать настроенных переменных окружения/доступных зависимостей для контекстных или интеграционных тестов. Для расследования падений используйте `target/surefire-reports/`. Dockerfile собирает приложение с пропуском тестов, поэтому тесты перед сдачей необходимо запускать отдельно.

## Перед сдачей: контроль критических замечаний

Следующие пункты необходимо **проверить в локальной версии проекта и БД** перед Pull Request `dev -> master`. Их наличие в README само по себе не означает, что изменения уже закоммичены или применены.

- [ ] `cancel()` и обработка скоринга используют `findByIdForUpdate()` внутри транзакции; тест с параллельными операциями подтверждает корректный переход.
- [ ] Для активных заявок создан частичный уникальный индекс PostgreSQL и нарушение индекса возвращает `409 Conflict`:

  ```sql
  CREATE UNIQUE INDEX uq_loan_active_client_pin
  ON loan_application (client_pin)
  WHERE status IN ('NEW', 'IN_REVIEW');
  ```

  Перед созданием индекса нужно устранить возможные уже существующие дубликаты. При `ddl-auto: update` такой индекс не гарантируется автоматически.

- [ ] В `CreateLoanApplicationRequestDto` есть ограничения на положительные `termMonths` и `monthlyIncome`, диапазон срока, формат и длину номера телефона, `@Size` для ФИО и ограничения точности денежных значений (`@Digits`).
- [ ] `@EnableScheduling` и `LoanScoringRetryJob` действительно подключены, а повторная публикация работает после восстановления RabbitMQ.
- [ ] В `RabbitMqConfig` объявлены DLX, DLQ и binding, retry включён, consumer не подавляет исключения.
- [ ] Все тесты и проверка Docker Compose завершаются успешно; изменения есть в ветке `dev` и оформлены Pull Request в `master`.

## Ограничения

Проект учебный и не подключается к продуктивным банковским системам. Для локальной разработки используется `spring.jpa.hibernate.ddl-auto=update` вместо версионированных миграций Flyway/Liquibase. Отправка события после коммита и повторная публикация `NEW` не дают exactly-once доставки. Для промышленной эксплуатации понадобятся миграции БД, надёжная публикация через outbox/confirm, управление повторными попытками и мониторинг ошибок.
