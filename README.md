# AccountingApp (Play Framework, Scala)

## Overview

AccountingApp is a Play Framework (Scala) web application that ingests transaction files, analyzes them, and exposes summaries and lookup tools for accounts, users, and tickers. It supports multi-file upload with robust validation to ensure data integrity (for example, uploaded files must share the same account number prefix).

## Key features

- File upload and processing
  - Upload one or more files via a web form (multipart/form-data).
  - Client-side and server-side validation to ensure all files share the same account number prefix (derived from the filename before the first "-").
  - Auto-fill of the account number field on the form when a consistent prefix is detected.
  - Server verifies the prefix consistency and ensures it matches the entered account number (if provided), returning a clear error when validation fails.
- Reports and summaries
  - Aggregates totals such as total buy, total sell, other orders, and net profit across uploaded data.
  - Displays ticker mapping for processed companies (name → ticker, market).
- Users and accounts
  - View users and their accounts; navigate to account reports and per-stock details (routes available; UI depends on the rest of the project’s views/controllers).
- Ticker management
  - Edit a stock’s ticker/market mapping via the UI.
- PostgreSQL-backed storage
  - Database connectivity is configured via app.properties (JDBC URL) or environment variables.

## Project structure (high level)

- app/controllers
  - FileUpload_Controller.scala: Handles file uploads, validation, processing, and result rendering.
  - Additional controllers: HomeController, DB_Controller, Users_Controller (see conf/routes for endpoints).
- app/views
  - files.scala.html: Upload form view with client-side validation and flash error rendering.
  - Other views for status pages, tickers, user/account pages.
- conf/routes: HTTP routing table for all endpoints.
- build.sbt: SBT build (Scala 2.13.x, Play plugin), with Docker packaging configuration.
- public: Static assets (images, JS, CSS).
- README.Docker.md: Supplemental Docker usage notes.
- compose.yaml and Dockerfile: Containerization and orchestration.
- app.properties: Example database configuration (JDBC URL).

## Prerequisites

- JDK 21 (the Docker image uses openjdk:21; Java 21 is recommended).
- SBT (preferred) or Maven (pom.xml exists, but SBT is the primary build for Play).
- PostgreSQL database (local or remote) accessible via JDBC URL.

## Getting started (local)

1) Clone the repository

   git clone https://github.com/your-org/AccountingApp.git
   cd AccountingApp

2) Configure the database

   - Option A: Edit app.properties with your JDBC URL (development use only):
     - app.properties
       DB_URL = "jdbc:postgresql://HOST:PORT/DB?user=USER&password=PWD&sslmode=require"

   - Option B: Use environment variables (recommended for production):
     - Set JAVA_OPTS or Play system properties at run time:
       -DDB_URL="jdbc:postgresql://HOST:PORT/DB?user=USER&password=PWD&sslmode=require"

   The code that opens database connections reads from this configured URL.

3) Run with SBT

   - Start the dev server:
     sbt run

   - By default Play runs on http://localhost:9000

4) Verify the app

   - Open http://localhost:9000/ to see the home page.
   - Upload page: http://localhost:9000/files
   - On upload, the server enforces filename prefix consistency across selected files, and verifies a match to the entered account number when provided.

## File upload behavior and validation

- Filenames are expected to start with an account number followed by a dash, e.g. 12345-transactions.csv
- Client-side validation (app/views/files.scala.html)
  - On file selection, JavaScript extracts the prefix before the first '-' from each file and ensures all prefixes are identical.
  - If multiple prefixes are detected, selection is blocked with an alert and the file input is cleared. If exactly one prefix is found, the account field is auto-filled.
  - The same check runs again on submit.
- Server-side validation (app/controllers/FileUpload_Controller.scala)
  - The controller derives the account prefix for each uploaded file and ensures there is at most one distinct prefix.
  - If more than one prefix is present, the request is redirected back to the form with a flash error message.
  - If an account number was submitted and it differs from the derived prefix, the upload is rejected with a flash error.
  - If validation passes, files are processed and a status page is displayed with computed totals and ticker mappings.

## Important routes (conf/routes)

- GET  /               HomeController.index
- GET  /files          FileUpload_Controller.showFormUpdateFile (upload form)
- POST /upload         FileUpload_Controller.upload (handle upload)
- GET  /tickers        FileUpload_Controller.tickers
- GET  /stocks/:name/edit  FileUpload_Controller.editStockTicker
- POST /stocks/:name/tickerStatus FileUpload_Controller.tickerPost
- Additional user/account/report routes are also defined; see conf/routes for the full list.

## Running tests

- With SBT:
  sbt test

- You can also run specific test suites if present:
  sbt "testOnly com.example.YourTest"

Build and run with Docker

There are two supported ways to containerize the app.

1) Using sbt-native-packager (configured in build.sbt)

   - Build a local Docker image:
     sbt Docker/publishLocal

   - Run with docker-compose (compose.yaml expects an image tag you built or references Dockerfile):
     docker compose up

2) Using the provided Dockerfile

   - Build:
     docker build -t accounting-app:latest .

   - Run:
     docker run -p 9000:9000 -e DB_URL="jdbc:postgresql://..." accounting-app:latest

   See also README.Docker.md for additional details.

Configuration

- Database URL
  - app.properties contains a DB_URL for development/testing. Do not commit secrets/production credentials.
  - For deployments, set DB_URL via environment variable or JVM property and avoid committing sensitive data.

- Ports
  - Default Play port: 9000 (override with -Dhttp.port=PORT)

Troubleshooting

- Flash messages not shown on upload form
  - The template reads request.flash; ensure redirects on error use .flashing("error" -> message) and the view displays request.flash.get("error").

- Mixed account numbers error on upload
  - Ensure all selected files share the same prefix before the first '-'.
  - Example good set: 12345-JAN.csv, 12345-FEB.csv; bad set: 12345-JAN.csv, 67890-FEB.csv

- Database connectivity
  - Verify DB_URL is set correctly and the database is reachable from your host/container. If using SSL, confirm sslmode is set properly.

Tech stack

- Language: Scala 2.13
- Framework: Play Framework (Scala)
- Build: SBT (primary); Maven files are present but not the preferred path for Play.
- Database: PostgreSQL
- Packaging: sbt-native-packager (Docker), Dockerfile, docker compose

Acknowledgements

- Play Framework and its ecosystem
- PostgreSQL
