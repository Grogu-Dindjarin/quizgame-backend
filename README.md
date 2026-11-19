# Quizzle — Backend

Spring Boot REST API for the Quizzle quiz application.

GitHub repository: https://github.com/Grogu-Dindjarin/quizgame-backend

## Table of contents

1. [Introduction](#introduction)
2. [Prerequisites](#prerequisites)
3. [Project structure and technologies](#project-structure-and-technologies)
4. [Getting started](#getting-started)
5. [Running tests](#running-tests)
6. [Test users and roles](#test-users-and-roles)
7. [API overview](#api-overview)

## Introduction

Quizzle is a quiz and flashcard application. This web API stores all data (users, categories, questions, quiz attempts and badges) and is used by the React frontend ([quizgame-frontend](https://github.com/Grogu-Dindjarin/quizgame-frontend)).

Main features:

- Register and log in with a JWT token (roles `USER` and `ADMIN`)
- Play a quiz per category, subcategory and difficulty level
- Submit a quiz: the score is calculated and saved, and badges are awarded
- View the leaderboard
- Manage your profile and upload an avatar image (upload + download of files)
- Admin: create, update and delete categories, subcategories, questions and answers, and add an image to a question

## Prerequisites

- Java 21 (LTS)
- Maven 3.9+
- MySQL 8.0, **or** Docker (to start MySQL with Docker Compose)

## Project structure and technologies

```
src/main/java/com/bramengel/quizgame/
├── config/         # WebConfig (serves uploaded files on /uploads/**)
├── controller/     # REST controllers
├── dto/            # Request and response DTOs
├── exception/      # Custom exceptions + GlobalExceptionHandler
├── model/          # JPA entities
├── repository/     # Spring Data JPA repositories
├── security/       # JWT filter, SecurityConfig, UserDetailsServiceImpl
└── service/        # Business logic
src/main/resources/
├── application.properties   # Database, JWT and upload configuration
└── data.sql                 # Test data, loaded on every start
docs/diagrams/      # Class diagrams and sequence diagrams (PNG)
```

Technologies:

- **Spring Boot 3** — framework for the REST API (web, security, validation, data JPA).
- **Maven** — builds the project and downloads all dependencies listed in `pom.xml`.
- **Spring Data JPA / Hibernate** — maps the Java entities to database tables; the application is not tied to one database system.
- **MySQL** — database for running the application; **H2** — in-memory database for the tests.
- **Spring Security + JWT (jjwt)** — authentication with a token and authorization per role.

## Getting started

### 1. Start the database

**Option A — Docker Compose (recommended).** Make sure Docker is running, then run this command in the project root (the folder with `docker-compose.yml`):

```bash
docker compose up -d
```

This starts MySQL 8.0 on port `3306` with the database `quizgame` and root password `rootpassword`. These values already match `src/main/resources/application.properties`, so no configuration is needed.

**Option B — Existing MySQL installation.** Create the database:

```sql
CREATE DATABASE quizgame;
```

Then set your own MySQL username and password in `src/main/resources/application.properties`:

```properties
spring.datasource.username=<your-user>
spring.datasource.password=<your-password>
```

### 2. Start the application

```bash
mvn spring-boot:run
```

The API is available at `http://localhost:8080`.

Note: the database is recreated on every start (`spring.jpa.hibernate.ddl-auto=create-drop`) and filled with the test data from `data.sql`. Uploaded avatars and question images are saved in the folders `uploads/avatars` and `uploads/questions` in the project root.

## Running tests

```bash
mvn test
```

The tests use an H2 in-memory database, so MySQL is not needed. Tools:

- **JUnit 5** and **Mockito** — unit tests for `AuthService` and `QuizAttemptService` (Arrange / Act / Assert)
- **Spring Boot Test** and **MockMvc** — integration tests for `AuthController` and `QuizAttemptController`

To measure line coverage with JaCoCo (report in `target/site/jacoco/index.html`):

```bash
mvn org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report
```

## Test users and roles

Two accounts are loaded via `data.sql`:

| Email             | Password  | Role  |
| ----------------- | --------- | ----- |
| admin@quizzle.nl  | welkom123 | ADMIN |
| speler@quizzle.nl | welkom123 | USER  |

- **USER** — play quizzes, view results, badges and leaderboard, manage own profile and avatar.
- **ADMIN** — everything a USER can do, plus managing categories, subcategories, questions and answers.

## API overview

Authentication uses a JWT in the `Authorization: Bearer <token>` header. The full documentation is in the API documentation (PDF) and the Postman collection.

| Method | Path                                         | Access        | Description                         |
| ------ | -------------------------------------------- | ------------- | ----------------------------------- |
| POST   | `/api/auth/register`                         | Public        | Register a new user                 |
| POST   | `/api/auth/login`                            | Public        | Log in and receive a JWT            |
| GET    | `/api/categories`, `/api/categories/{id}`    | Public        | List / get categories               |
| POST, PUT, DELETE | `/api/categories[/{id}]`          | ADMIN         | Manage categories                   |
| GET    | `/api/subcategories`, `/api/subcategories/{id}`, `/api/subcategories/category/{categoryId}` | Public | List / get subcategories |
| POST, PUT, DELETE | `/api/subcategories[/{id}]`       | ADMIN         | Manage subcategories                |
| GET    | `/api/questions`, `/api/questions/{id}`, `/api/questions/subcategory/{subcategoryId}` | Public | List / get questions |
| POST, PUT, DELETE | `/api/questions[/{id}]`           | ADMIN         | Manage questions                    |
| POST   | `/api/questions/{id}/image`       | ADMIN         | Upload question image (max 1 MB)    |
| GET    | `/api/questions/{questionId}/answers[/{answerId}]` | Public  | List / get answers                  |
| POST, PUT, DELETE | `/api/questions/{questionId}/answers[/{answerId}]` | ADMIN | Manage answers            |
| POST   | `/api/quiz-attempts`                         | USER / ADMIN  | Submit a quiz attempt               |
| GET, PUT | `/api/profile`                             | USER / ADMIN  | Get / update own profile            |
| POST   | `/api/profile/avatar`                        | USER / ADMIN  | Upload avatar (multipart, field `file`) |
| GET    | `/uploads/avatars/{filename}`                | Public        | Download an uploaded avatar         |
| GET    | `/uploads/questions/{filename}`              | Public        | Download a question image           |
| GET    | `/api/leaderboard`                           | Public        | Leaderboard                         |
| GET    | `/api/badges`, `/api/badges/me`              | USER / ADMIN  | All badges / own earned badges      |
