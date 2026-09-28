# FraudWatch - Digital Wallet Transaction Anomaly & Fraud Flagging System

FraudWatch is an enterprise transaction anomaly and fraud detection engine for digital wallets. It evaluates every transfer against configurable fraud rules in real time before affecting account balances. Flagged transactions are placed into a dedicated review queue for manual intervention (Approve / Block).

---

## Project Structure

```
fraudwatch/
├── docker-compose.yml              # MySQL 8.4 container service with healthcheck
├── README.md                       # Comprehensive documentation & troubleshooting
├── fraudwatch/                     # Spring Boot Backend (Maven project)
│   ├── pom.xml                     # Maven project descriptor (Java 21, Spring Boot 3.3.4)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/fraudwatch/
│   │   │   │   ├── FraudWatchApplication.java   # Spring Boot entry point & CORS configuration
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AccountController.java     # POST/GET accounts
│   │   │   │   │   ├── TransactionController.java # POST/GET transactions
│   │   │   │   │   ├── ReviewController.java      # Pending review, approve, block
│   │   │   │   │   ├── RuleController.java        # CRUD & enable/disable rules
│   │   │   │   │   └── DashboardController.java   # Summary & flagged count by rule
│   │   │   │   ├── entity/
│   │   │   │   │   ├── Account.java               # Wallet account entity
│   │   │   │   │   ├── Transaction.java           # Transaction entity & TransactionStatus enum
│   │   │   │   │   ├── Rule.java                  # Configurable fraud rule entity
│   │   │   │   │   ├── FlaggedTransaction.java    # Flagged record with uk_tx_rule constraint
│   │   │   │   │   └── ReviewOutcome.java         # Review outcome entity & Outcome enum
│   │   │   │   ├── repository/
│   │   │   │   │   ├── AccountRepository.java
│   │   │   │   │   ├── TransactionRepository.java # Optimized COUNT query for velocity check
│   │   │   │   │   ├── RuleRepository.java
│   │   │   │   │   ├── FlaggedTransactionRepository.java
│   │   │   │   │   └── ReviewOutcomeRepository.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── AccountService.java
│   │   │   │   │   ├── TransactionService.java   # Transaction workflow & balance management
│   │   │   │   │   ├── FraudDetectionService.java # Evaluates all enabled rules without early return
│   │   │   │   │   ├── RuleService.java
│   │   │   │   │   └── ReviewService.java        # Approve & Block logic with balance protection
│   │   │   │   └── exception/
│   │   │   │       ├── GlobalExceptionHandler.java # @RestControllerAdvice returning standard JSON
│   │   │   │       ├── NotFoundException.java
│   │   │   │       ├── BadRequestException.java
│   │   │   │       ├── InsufficientBalanceException.java
│   │   │   │       └── ConflictException.java
│   │   │   └── resources/
│   │   │       ├── application.properties    # MySQL, Flyway & JPA configuration
│   │   │       └── db/migration/
│   │   │           ├── V1__create_tables.sql # DDL for all 5 tables, FKs, indexes
│   │   │           └── V2__insert_sample_data.sql # Sample accounts & fraud rules
│   │   └── test/
│   │       └── java/com/fraudwatch/
│   │           ├── TransactionServiceTest.java    # 10 core business requirements test suite
│   │           └── FraudDetectionServiceTest.java # Threshold and velocity rule tests
└── frontend/                       # React + Vite Frontend
    ├── index.html
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.jsx                # React DOM root
        ├── App.jsx                 # Dashboard, Accounts, Transactions, Review Queue, Rules
        └── style.css               # Clean modern admin UI styling
```

---

## Technologies

- **Java**: 21 (Oracle / OpenJDK)
- **Framework**: Spring Boot 3.3.4
- **Build Tool**: Maven 3.9+
- **Database**: MySQL 8.x (managed via Flyway schema migrations)
- **Frontend**: React 18, Vite 5, Axios, Vanilla CSS
- **Containerization**: Docker Compose (MySQL 8.4)

---

## Requirements

1. **JDK 21** installed and configured in your `JAVA_HOME` environment variable.
2. **Maven 3.8+** installed (or use IntelliJ's bundled Maven).
3. **MySQL 8.x** running locally on port 3306 (or configurable via environment variables).
4. **Node.js 18+** & **npm** for running the frontend.
5. *(Optional)* **Docker & Docker Compose** for running MySQL in a container.

---

## Database Setup

### Option A: Local MySQL Server

1. Ensure MySQL is running on port `3306` (or set `DB_PORT` accordingly).
2. Connect to MySQL as `root`:
   ```sql
   CREATE DATABASE IF NOT EXISTS fraudwatch;
   ```
3. Default credentials expected:
   - Host: `localhost`
   - Port: `3306`
   - Database: `fraudwatch`
   - Username: `root`
   - Password: `root`

### Option B: Docker Compose

If you have Docker installed, start the pre-configured MySQL 8.4 container:
```bash
docker compose up -d mysql
```
This automatically initializes the `fraudwatch` database with user `root` and password `root`.

### Configurable Environment Variables

The database parameters can be overridden at runtime using environment variables:
- `DB_HOST` (Default: `localhost`)
- `DB_PORT` (Default: `3306`)
- `DB_NAME` (Default: `fraudwatch`)
- `DB_USERNAME` (Default: `root`)
- `DB_PASSWORD` (Default: `root`)

*(Note: On systems where MySQL is running on a custom port such as 3307, set `DB_PORT=3307` before launching).*

---

## Backend Setup (IntelliJ IDEA Instructions)

Follow these step-by-step instructions to run the backend in IntelliJ IDEA:

1. **Open IntelliJ IDEA**.
2. Go to **File -> Open...** and select the `fraudwatch` folder.
3. Wait for IntelliJ to detect the Maven project and finish indexing.
4. Go to **File -> Project Structure -> Project**:
   - Set **SDK** to **JDK 21**.
   - Set **Language level** to **21 - Sealed types, pattern matching, records**.
5. In the **Maven** tool window on the right:
   - Click **Reload All Maven Projects** (the circular refresh icon).
   - Expand `fraudwatch` -> `Lifecycle` -> double-click `clean`, then `install` (or `compile`).
6. Verify your MySQL instance is running and the `fraudwatch` database exists.
7. Open `fraudwatch/src/main/java/com/fraudwatch/FraudWatchApplication.java`.
8. Right-click inside `FraudWatchApplication.java` and click **Run 'FraudWatchApplication'**.
   *(If your MySQL is running on a port other than 3306, e.g. 3307, add `-DDB_PORT=3307` to VM options in your Run Configuration).*
9. Verify the console logs:
   - Flyway applies `V1__create_tables.sql` and `V2__insert_sample_data.sql`.
   - Tomcat starts on port `8080`.
   - Message: `Started FraudWatchApplication in X.XXX seconds`.
10. The backend is accessible at:
    **`http://localhost:8080`**

---

## Frontend Setup

1. Open a new terminal window.
2. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
3. Install dependencies:
   ```bash
   npm install
   ```
4. Start the Vite development server:
   ```bash
   npm run dev
   ```
5. The frontend will be available at:
   **`http://localhost:5173`**

---

## Core Business Rules & Demonstration Procedure

Follow this test walk-through in the UI or via REST API to observe all fraud rules and review mechanics:

### 1. Normal Transaction (Auto-Completes)
- **Action**: Transfer `₹5,000.00` from `ACC1001` (Balance: ₹100,000) to `ACC1002` (Balance: ₹50,000).
- **Result**:
  - No rules triggered.
  - Status: `COMPLETED`.
  - `ACC1001` balance debited to `₹95,000.00`.
  - `ACC1002` balance credited to `₹55,000.00`.

### 2. Large Amount Anomaly (Exceeds Threshold)
- **Action**: Transfer `₹60,000.00` from `ACC1001` to `ACC1002`.
- **Threshold Rule**: Rule `AMOUNT_THRESHOLD` has threshold `₹50,000.00`.
- **Result**:
  - Status: `FLAGGED`.
  - Triggered Rule: `AMOUNT_THRESHOLD` ("Transaction amount 60000.00 exceeds threshold 50000.00").
  - **Critical Rule**: Account balances are **NOT** modified (`ACC1001` remains ₹95,000.00).
  - Transaction appears in the **Review Queue**.

### 3. Approve Flagged Transaction
- **Action**: Go to **Review Queue** tab and click **Approve Transfer** on the flagged transaction.
- **Result**:
  - Status changes to `APPROVED`.
  - Sender balance debited to `₹35,000.00`.
  - Receiver balance credited to `₹115,000.00`.
  - Flag marked as `resolved: true`.
  - `ReviewOutcome` record created with `APPROVED`.
  - Any subsequent attempt to review or block returns `HTTP 409 Conflict`.

### 4. Rapid Repeated Transfers (Velocity Rule)
- **Action**: From `ACC1003` to `ACC1004`, submit transfers in quick succession:
  - Transfer 1: `₹100.00` -> `COMPLETED`
  - Transfer 2: `₹100.00` -> `COMPLETED`
  - Transfer 3: `₹100.00` -> `COMPLETED`
  - Transfer 4: `₹100.00` -> **`FLAGGED`**
- **Result**:
  - Transfer 4 exceeds the threshold of 3 transfers within 60 seconds.
  - Status: `FLAGGED`.
  - Reason: `"Account ACC1003 exceeded velocity limit: 3 transactions in the last 60 seconds (threshold: 3)"`.
  - Balances are untouched for Transfer 4.

### 5. Multiple Rules Triggered Simultaneously
- **Action**: Submit another transfer from `ACC1003` of `₹60,000.00` within the 60-second window.
- **Result**:
  - Rule 1 (`AMOUNT_THRESHOLD`) triggers because ₹60,000.00 > ₹50,000.00.
  - Rule 2 (`VELOCITY`) triggers because sender has made >= 3 transactions in the window.
  - Both `FlaggedTransaction` records are created and linked to the same transaction.
  - The review queue clearly displays **both** triggered anomalies.

### 6. Block Flagged Transaction
- **Action**: Go to **Review Queue** and click **Block Transfer** on the transaction.
- **Result**:
  - Status changes to `BLOCKED`.
  - **Most Important**: Balances are **NOT** modified for either sender or receiver.
  - `ReviewOutcome` record created with `BLOCKED`.
  - Any subsequent attempt to approve or block returns `HTTP 409 Conflict`.

---

## REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/accounts` | Create a new account |
| `GET` | `/api/accounts` | List all accounts |
| `GET` | `/api/accounts/{id}` | Get account by ID |
| `POST` | `/api/transactions` | Submit transaction (executes fraud check & balance updates) |
| `GET` | `/api/transactions` | List all transactions (supports `?status=FLAGGED`) |
| `GET` | `/api/transactions/{id}` | Get transaction details and flagged rules |
| `GET` | `/api/transactions/flagged` | List all flagged transactions |
| `GET` | `/api/reviews/pending` | List pending review queue items |
| `POST` | `/api/reviews/{id}/approve` | Approve flagged transaction & apply balance changes |
| `POST` | `/api/reviews/{id}/block` | Block flagged transaction (balances untouched) |
| `GET` | `/api/rules` | List all fraud detection rules |
| `POST` | `/api/rules` | Create a new fraud detection rule |
| `PUT` | `/api/rules/{id}` | Update existing rule thresholds / configuration |
| `PATCH` | `/api/rules/{id}/enabled` | Enable or disable a rule |
| `GET` | `/api/dashboard/summary` | Dashboard statistics (counts by status) |
| `GET` | `/api/dashboard/flagged-by-rule` | Counts of flags grouped by rule code |

---

## MySQL Troubleshooting

- **MySQL service is not running**:
  - Start via Windows Services (`services.msc` -> start `MySQL80` or `MySQL84`).
  - Or use Docker: `docker compose up -d mysql`.
- **Port 3306 is occupied or using a custom port (e.g., 3307)**:
  - If another MySQL instance is running on port 3307, start Spring Boot with:
    `$env:DB_PORT="3307"; mvn spring-boot:run`
    or pass `-DDB_PORT=3307` in IntelliJ Run Configuration.
- **Access denied for user 'root'@'localhost'**:
  - Verify your MySQL password in `application.properties` or provide environment variable `DB_PASSWORD=your_password`.
- **Database 'fraudwatch' does not exist**:
  - Connect to MySQL and run:
    ```sql
    CREATE DATABASE fraudwatch;
    ```
- **Flyway migration fails**:
  - Check whether manual table alterations were made. If necessary, drop the database and recreate it so Flyway can re-apply cleanly:
    ```sql
    DROP DATABASE fraudwatch;
    CREATE DATABASE fraudwatch;
    ```

---

## Common IntelliJ & Runtime Errors

1. **JDK Version Error (class file has wrong version / unsupported class file version)**:
   - Solution: Go to `File` -> `Project Structure` -> `Project` and ensure SDK is set to JDK 21. Also check `Settings` -> `Build, Execution, Deployment` -> `Compiler` -> `Java Compiler` and verify target bytecode version is 21.
2. **Maven Dependency Error (Cannot resolve symbol / Unresolved dependencies)**:
   - Solution: In IntelliJ, open the Maven tool window and click the **Reload All Maven Projects** button.
3. **Port 8080 already in use**:
   - Solution: A previous Spring Boot process is still running. In PowerShell, find and kill it:
     ```powershell
     Get-NetTCPConnection -LocalPort 8080 | Select-Object -ExpandProperty OwningProcess | Stop-Process -Force
     ```
4. **Port 5173 already in use (Frontend)**:
   - Solution: Stop the previous Vite process, or Vite will automatically ask to select the next available port (e.g. 5174).
5. **CORS Error (`Access to XMLHttpRequest has been blocked by CORS policy`)**:
   - Solution: The backend has centralized CORS configuration in `FraudWatchApplication.java` allowing `http://localhost:5173` with all standard HTTP methods (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`). Ensure frontend runs on `http://localhost:5173`.
6. **npm install failure**:
   - Solution: Verify Node.js is installed (`node -v`). Delete `frontend/node_modules` and `package-lock.json` if corrupted, then run `npm install`.
7. **Backend not reachable from Frontend**:
   - Solution: Confirm the Spring Boot application is running and listening on `http://localhost:8080`.
