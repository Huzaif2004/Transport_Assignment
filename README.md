# Transporter Assignment on Lanes

A Spring Boot REST API that assigns transporters to freight lanes. Given the quotes that transporters have submitted for lanes and a limit on how many transporters may be used, it returns the cheapest assignment that covers every lane.

There are two endpoints: one to submit lanes and transporter quotes (stored in MySQL), and one to compute the optimized assignment from the stored data. The assignment is computed by an exact branch-and-bound solver.

## Table of contents

1. [The problem](#the-problem)
2. [Tech stack](#tech-stack)
3. [Project structure](#project-structure)
4. [Configuration](#configuration)
5. [Running locally](#running-locally)
6. [API reference](#api-reference)
7. [Data model](#data-model)
8. [How the solver works](#how-the-solver-works)
9. [Testing](#testing)
10. [Deployment notes](#deployment-notes)

## The problem

A **lane** is a route from an origin to a destination (for example Mumbai to Delhi). **Transporters** quote a price for the lanes they operate, and a transporter does not have to quote every lane.

Given a limit `maxTransporters` (K), the application:

1. **Covers every lane.** Each lane must be assigned to a transporter who quoted it. If this is impossible within the limit, the API returns an error.
2. **Uses at most K transporters.**
3. **Minimizes the total cost**, where each lane is carried by the cheapest transporter among the selected group.

## Tech stack

Based on the source code:

- Java, Spring Boot (REST controllers, `@SpringBootApplication`)
- Spring Data JPA with Hibernate
- MySQL (`com.mysql.cj.jdbc.Driver`)
- Jakarta Bean Validation
- JUnit 5 for tests

The build file (`pom.xml` or Gradle script) was not part of the sources used to write this document, so Java and Spring Boot versions are not listed here. Check your build file for them.

## Project structure

```
src/
├── main/
│   ├── java/com/transport/assignment/
│   │   ├── TransportAssignmentApplication.java
│   │   ├── controller/
│   │   │   ├── TransporterController.java       POST /api/transporters/input
│   │   │   └── AssignmentController.java        POST /api/assignments/calculate
│   │   ├── service/
│   │   │   ├── InputService.java                validates and saves the dataset
│   │   │   └── AssignmentService.java           builds the quote matrix, calls the solver
│   │   ├── solver/
│   │   │   ├── AssignmentSolver.java            solver interface
│   │   │   ├── QuoteMatrix.java                 solver input (lane ids, transporter ids, quotes)
│   │   │   ├── AssignmentResult.java            solver output
│   │   │   ├── LaneAssignment.java
│   │   │   └── implementation/
│   │   │       ├── BranchAndBoundAssignmentSolver.java   used by the API (@Primary)
│   │   │       └── BruteForceAssignmentSolver.java       reference implementation
│   │   ├── entity/                              Lane, Transporter, LaneQuote (JPA)
│   │   ├── repository/                          LaneRepository, TransporterRepository, LaneQuoteRepository
│   │   ├── dto/
│   │   │   ├── request/                         InputRequest, LaneRequest, TransporterRequest,
│   │   │   │                                    LaneQuoteRequest, AssignmentRequest
│   │   │   └── response/                        InputSavedResponse, AssignmentResponse,
│   │   │                                        LaneAssignmentResponse, ApiErrorResponse
│   │   ├── exception/                           GlobalExceptionHandler and custom exceptions
│   │   └── config/                              (empty)
│   └── resources/
│       ├── application.properties               reads database settings from environment variables
│       └── application.properties.example      example values for a local setup
└── test/java/com/transport/assignment/solver/implementation/
    ├── BranchAndBoundAssignmentSolverTest.java
    └── AssignmentSolverBenchmarkTest.java
```

The layers are: controller → service → repository (JPA) for data, and service → `AssignmentSolver` for the optimization. The service converts database rows into a `QuoteMatrix`, so the solver has no dependency on JPA or Spring MVC types.

## Configuration

`application.properties` reads the following **environment variables**. None has a default value, so all four must be set or the application will not start.

| Variable | Used for |
|---|---|
| `DB_URL` | JDBC URL of the MySQL database |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `PORT` | HTTP port the server listens on |

Other settings in `application.properties`:

| Property | Value |
|---|---|
| `spring.jpa.hibernate.ddl-auto` | `update` (tables are created or updated automatically) |
| `spring.jpa.show-sql` | `false` |
| `spring.jpa.open-in-view` | `false` |
| `spring.datasource.driver-class-name` | `com.mysql.cj.jdbc.Driver` |

`application.properties.example` shows a local configuration with hard-coded values: database `transport_assignment` on `localhost:3306`, port `8080`, and `show-sql=true`.

## Running locally

**Prerequisites:** a JDK, a Maven or Gradle installation matching your build file, and a running MySQL server.

1. **Create or choose a MySQL user.** The example URL contains `createDatabaseIfNotExist=true`, so the database `transport_assignment` is created automatically if the user is allowed to create databases. Otherwise create it yourself:
   ```sql
   CREATE DATABASE transport_assignment;
   ```
2. **Set the environment variables** (values below follow `application.properties.example`):

   macOS / Linux:
   ```bash
   export DB_URL="jdbc:mysql://localhost:3306/transport_assignment?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
   export DB_USERNAME="your_user"
   export DB_PASSWORD="your_password"
   export PORT=8080
   ```
   Windows PowerShell:
   ```powershell
   $env:DB_URL="jdbc:mysql://localhost:3306/transport_assignment?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
   $env:DB_USERNAME="your_user"
   $env:DB_PASSWORD="your_password"
   $env:PORT="8080"
   ```
3. **Start the application** (Maven shown; use the Gradle equivalent if the project uses Gradle):
   ```bash
   mvn spring-boot:run
   ```
   On first start Hibernate creates the tables listed under [Data model](#data-model).
4. **Call the API** at `http://localhost:8080` (see below). The API can be tested with Postman or curl; no UI is included.

## API reference

Both endpoints accept and return JSON.

### 1. Submit input data

`POST /api/transporters/input`

Replaces the stored dataset with the lanes, transporters and quotes in the request. Any previously stored lanes, transporters and quotes are deleted first, in the same transaction.

**Request body**

```json
{
  "lanes": [
    { "id": 1, "origin": "Chandigarh", "destination": "Shimla" }
  ],
  "transporters": [
    {
      "id": 1,
      "name": "Transporter X",
      "laneQuotes": [ { "laneId": 1, "quote": 1000 } ]
    }
  ]
}
```

| Field | Rules |
|---|---|
| `lanes[].id` | required, positive, unique |
| `lanes[].origin`, `lanes[].destination` | required, not blank |
| `transporters[].id` | required, positive, unique |
| `transporters[].name` | required, not blank |
| `transporters[].laneQuotes` | required (may be an empty list) |
| `laneQuotes[].laneId` | required, positive, must refer to a lane in the same request |
| `laneQuotes[].quote` | required, greater than zero |
| other | `lanes` and `transporters` must both be non-empty; a transporter cannot quote the same lane twice |

**Success response (200)**

```json
{
  "status": "success",
  "message": "Input data saved successfully."
}
```

### 2. Get the optimized assignment

`POST /api/assignments/calculate`

Reads the stored dataset and computes the cheapest assignment that uses at most `maxTransporters` transporters and covers every lane.

**Request body**

```json
{ "maxTransporters": 3 }
```

`maxTransporters` must be at least 1. If it is larger than the number of transporters, the number of transporters is used instead.

**Success response (200)**

```json
{
  "status": "success",
  "totalCost": 7500,
  "assignments": [
    { "laneId": 1, "transporterId": 1 },
    { "laneId": 2, "transporterId": 1 },
    { "laneId": 3, "transporterId": 2 },
    { "laneId": 4, "transporterId": 2 },
    { "laneId": 5, "transporterId": 3 }
  ],
  "selectedTransporters": [1, 2, 3]
}
```

- `assignments` contains one entry per lane, in ascending lane-id order.
- `selectedTransporters` lists, in ascending order, only the transporters that were assigned at least one lane.

### Error responses

All errors use this shape:

```json
{
  "status": "error",
  "message": "No feasible assignment found",
  "errors": []
}
```

`errors` is populated only for request validation failures (one entry per invalid field, in the form `field: message`).

| HTTP status | Cause |
|---|---|
| 400 | Bean validation failed (message `Validation failed.`), malformed JSON (`Malformed request body.`), or invalid input data (empty lists, duplicate or unknown ids, duplicate quote for a lane, quote not greater than zero) |
| 404 | `calculate` was called before any data was stored (`No input data found. Submit transporter data first.`) |
| 422 | No assignment exists within the limit, for example a lane nobody quoted or a limit that is too small to cover all lanes |
| 500 | Unexpected error (`An unexpected error occurred.`) or an internal inconsistency in the stored data |


## Data model

Tables are created by Hibernate (`ddl-auto=update`).

| Table | Columns |
|---|---|
| `lanes` | `id` (primary key, supplied by the client), `origin`, `destination` (both not null) |
| `transporters` | `id` (primary key, supplied by the client), `name` (not null) |
| `lane_quotes` | `id` (auto-increment primary key), `transporter_id` (foreign key), `lane_id` (foreign key), `quote` (not null); unique on (`transporter_id`, `lane_id`) |

Lane and transporter ids come from the request body and are not generated by the database.

## How the solver works

`AssignmentService` reads all lanes and transporters sorted by id, reads all quotes, and builds a `QuoteMatrix`: one row per transporter, one column per lane, and `-1` where a transporter did not quote a lane. It then calls the `AssignmentSolver` bean. `BranchAndBoundAssignmentSolver` is annotated `@Primary`, so it is the implementation Spring injects.

### Algorithm (`BranchAndBoundAssignmentSolver`)

1. **Group size.** Let `k = min(maxTransporters, number of transporters)`. The solver only considers groups of exactly `k` transporters. Adding a transporter to a group can never increase the cost, so a group of `k` is always at least as good as any smaller group.
2. **Enumerate groups.** Transporters are sorted by id and combinations of `k` are built one transporter at a time.
3. **Lower bound.** For each partial group, the solver computes a best-case total cost: for every lane, the cheapest quote among the transporters already selected and those still available.
4. **Pruning.** A branch is abandoned when
   - some lane has no quote among the selected and remaining transporters (the bound is `Long.MAX_VALUE`), or
   - the best-case total is greater than the best complete plan found so far.
5. **Evaluating a full group.** Each lane is given to the cheapest selected transporter that quoted it; on equal quotes the lower transporter id wins. If any lane has no quote in the group, the group is rejected.
6. **Choosing between plans.** The lowest total cost wins. If two plans have the same cost, the plan whose lane-by-lane transporter ids compare lower is kept.
7. **Result.** `selectedTransporters` is the set of transporters that received at least one lane, in ascending order. If no group covers all lanes, `NoFeasibleAssignmentException` is thrown and the API returns 422.

### Interpreting the requirements

| Requirement in the assignment | Implementation |
|---|---|
| Cost minimization | The exact minimum total cost for the allowed group size is found |
| Maximize transporter usage up to the limit | Groups of `min(limit, number of transporters)` are searched; the response lists the transporters that are actually used |
| Full lane coverage | Required; if it cannot be satisfied within the limit the API returns 422 |


## Deployment notes

- Provide `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and `PORT` as environment variables in the hosting environment; the application does not start without them.
- The database user needs permission to create and alter tables because `ddl-auto` is set to `update`.
- The database must be reachable from the application using the JDBC URL provided in `DB_URL`.

