# Мікросервіс для обміну даними між симуляторами (Варгейм)

> **Курсова робота** з дисципліни **«Компоненти програмної інженерії»**
> **Тема роботи №12:** Мікросервіс для обміну даними між симуляторами.

---

## 1. Загальний опис проєкту

Даний проєкт є серверним брокером (REST API) для тактичного варгейму (агентного моделювання бойових дій на базі архітектури типу *Project Mesa*). Мікросервіс виступає незалежним арбітром часу та координатором між двома автономними симуляторами протиборчих сторін (**Blue Force** та **Red Force**), а також веб-панеллю оперативного спостереження (**Дашборд Командира**).

---

## 2. Стек технологій

| Компонент | Технології |
| :--- | :--- |
| **Серверний мікросервіс** | **Java 21 LTS**, **Spring Boot 3.4**, Spring Data JPA, Hibernate Validator, **Lombok** |
| **База даних** | **PostgreSQL** |
| **Документація API** | **OpenAPI 3 / Swagger UI** (`springdoc-openapi`) |
| **Тестування** | **JUnit 5**, **Mockito**, Spring MockMvc, **Testcontainers** (PostgreSQL) |
| **Архітектурні діаграми** | **PlantUML** |
| **Mock-симулятори** | **Python 3** |
| **Фронтенд** | **Next.js**, React, TailwindCSS, HTML5 |
| **Контейнеризація** | **Docker**, **Docker Compose** |

---

## 3. Архітектура системи

Проєкт спроєктовано за принципами **Шарової модульної архітектури** з виділенням ізольованого ядра предметної області:

```text
software-components-coursework/
├── backend/                  # Серверний мікросервіс на Spring Boot 3.4
├── simulators/               # Mock-симулятори поведінки військ на Python 3
├── frontend/                 # Веб-дашборд Командира на Next.js
└── docs/                     # Проєктна документація курсової роботи
```


---

## 4. Запуск

Потрібні лише Docker і Docker Compose. Мікросервіс і PostgreSQL піднімаються однією командою:

```bash
docker compose up -d --build
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- Контейнер мікросервісу обмежено 512 MB RAM (JVM використовує до 75% ліміту), PostgreSQL обмежено 256 MB.

Демонстраційний прогін Mesa-симуляторів (Blue та Red Force) проти запущеного брокера:

```bash
docker compose --profile demo run --rm simulators --url=http://backend:8080 --ticks=30
```

Зупинка зі збереженням даних БД: `docker compose down`. Повне очищення разом із томом БД: `docker compose down -v`.

### Тести

```bash
cd backend && ./mvnw test
```

Потрібен запущений Docker: інтеграційні тести піднімають PostgreSQL 16 через Testcontainers.

| Рівень | Інструменти | Що покриває |
| :--- | :--- | :--- |
| Unit | JUnit 5 + Mockito | Туман війни, `UnitState`, бар'єрна синхронізація тіків, життєвий цикл сесії |
| Web slice | `@WebMvcTest` + MockMvc | контролери, валідація, API-ключі та ролі, коди помилок |
| Інтеграційні | `@SpringBootTest` / `@DataJpaTest` + Testcontainers | повний цикл REST → ядро → PostgreSQL, snapshot'и, журнал подій, Reset, відновлення зі snapshot'а |

### API-ключі (заголовок `X-API-KEY`)

| Ключ за замовчуванням | Роль | Доступ |
| :--- | :--- | :--- |
| `blue-secret-token` | Blue Force | реєстрація симулятора, відправка своїх юнітів, свій Туман війни, статус |
| `red-secret-token` | Red Force | те саме для Red Force |
| `admin-secret-token` | Адміністратор | керування життєвим циклом (init, pause, resume, reset, restore), повна картина поля бою, snapshot'и, журнал подій |

Ключі перевизначаються змінними середовища `WARGAME_BLUE_API_KEY`, `WARGAME_RED_API_KEY`, `WARGAME_ADMIN_API_KEY`.

### Порядок роботи симулятора

1. `POST /api/v1/simulators/register`: реєстрація сторони (без неї відправка юнітів повертає 409).
2. Адміністратор викликає `POST /api/v1/simulation/init`.
3. Кожен тік: `POST /api/v1/simulation/ticks/submit`, потім опитування `GET /api/v1/simulation/ticks/{tick}/fog-of-war`. Поки суперник не відправив свій хід, endpoint повертає 409.
4. Якщо жодна зі сторін не відповідає довше за тайм-аут (10 с), симуляція автоматично переходить у `PAUSED`.
