# AI-Powered Document Intelligence Platform

A distributed, microservices-based document processing platform designed to handle large PDFs, Word documents, and text files. Built with Spring Boot 3, Spring Cloud Gateway, Apache Kafka, PostgreSQL, Spring AI, and React, the system processes documents asynchronously, extracts structured metadata, runs MapReduce summarization, flags compliance risks, and provides interactive Q&A.

---

## Why I Built This

Most AI document tools struggle when dealing with long, multi-page business files:
1. Shoving 40-page PDFs into a single prompt frequently exceeds token windows or triggers `429 Too Many Requests` rate limits.
2. Synchronous file uploads freeze the browser and timeout when multiple users submit documents at once.
3. Monolithic designs tightly couple heavy CPU text extraction and LLM rate-limited calls with quick metadata queries and user authentication.

This platform addresses these challenges with an **event-driven microservices architecture**:
- **API Gateway** acts as a unified reverse proxy and routing layer for client requests.
- **Document Service** rapidly ingests files and offloads processing work via **Apache Kafka**.
- **AI Analysis Service** consumes events, extracts text asynchronously, counts tokens locally (JTokkit CL100K), and runs a **MapReduce chunking strategy** with exponential backoff retries.
- **Database-per-Service Isolation** ensures each domain maintains its own database schema (`docintel_auth`, `docintel_docs`, `docintel_ai`).

---

## Architecture & System Flow

```
                           [ React UI (Port 80 / 5173) ]
                                         │
                         HTTP REST       │ (e.g. /api/auth, /api/documents, /api/analysis)
                                         ▼
                     ┌───────────────────────────────────────┐
                     │    API Gateway (Spring Cloud 8080)    │
                     └───────┬──────────────┬──────────────┬─┘
                             │              │              │
           /api/auth/**      │              │              │ /api/analysis/**
         ┌───────────────────┘              │              └──────────────────┐
         ▼                                  ▼                                 ▼
┌──────────────────┐               ┌──────────────────┐              ┌──────────────────┐
│   Auth Service   │               │ Document Service │              │    AI Service    │
│   (Port 8081)    │               │   (Port 8082)    │              │   (Port 8083)    │
└────────┬─────────┘               └────────┬─────────┘              └────────┬─────────┘
         │                                  │   ▲                             │   ▲
         │                                  │   │                             │   │
         │ PostgreSQL                       │   │ Kafka Topics                │   │
         │ (docintel_auth)                  │   │                             │   │
         ▼                                  │   └─────────────────────────┐   │   │
┌──────────────────┐                        │   "document-status"         │   │   │
│ docintel_auth DB │                        ▼ (Completed / Failed)        │   │   │
└──────────────────┘                  ┌───────────────┐                   │   │   │
                                      │ Kafka Broker  │                   │   │   │
                                      │ (Topic Queue) │                   │   │   │
                                      └───────┬───────┘                   │   │   │
                                              │ "document-processing"     │   │   │
                                              ▼ (Uploaded event)          │   │   │
                                      ┌───────────────────────────────────┘   │   │
                                      │                                       │   │
                                      │   PostgreSQL (docintel_docs)          │   │ PostgreSQL
                                      ▼                                       ▼   │ (docintel_ai)
                             ┌──────────────────┐                    ┌──────────────────┐
                             │ docintel_docs DB │                    │  docintel_ai DB  │
                             └──────────────────┘                    └──────────────────┘
                                      │                                       │
                                      └──────── Shared Disk Volume ───────────┘
                                                  ( /app/uploads )
```

### Event-Driven Processing Flow
1. **Upload:** User uploads a file via the React UI to `POST /api/documents/upload`.
2. **Gateway Route:** API Gateway routes the request to `document-service` (port 8082).
3. **Storage & Event Publish:** `document-service` persists the file to disk (`/app/uploads`), creates a record in `docintel_docs` with `PENDING` status, and emits a `DocumentUploadedEvent` to the Kafka topic `document-processing`. It immediately returns a `201 Created` tracking response.
4. **Async AI Processing:** `ai-service` consumes the message from `document-processing`:
   - Extracts raw text using Apache PDFBox, Apache POI, or Apache Tika.
   - Calculates tokens locally with JTokkit CL100K tokenizer.
   - Executes recursive MapReduce chunking with Spring AI and retry backoff.
   - Saves structured entities, summary, and audit risks into `docintel_ai`.
5. **Status Update:** `ai-service` publishes a `DocumentStatusUpdatedEvent` to the `document-status` topic.
6. **Completion:** `document-service` consumes the status event and updates the document status to `COMPLETED` or `FAILED`.

---

## Microservices Breakdown

| Service | Port | Responsibility | Database |
| :--- | :---: | :--- | :--- |
| **API Gateway** | `8080` | Reverse proxy, dynamic path routing, global CORS, 50MB request buffering | None |
| **Auth Service** | `8081` | User registration, authentication, BCrypt hashing, JWT issuance and validation | `docintel_auth` |
| **Document Service** | `8082` | File upload, storage management, document metadata search, Kafka upload events | `docintel_docs` |
| **AI Analysis Service** | `8083` | Text extraction, MapReduce summarization, structured JSON parsing, RAG Q&A, diffing | `docintel_ai` |
| **Common Library** | N/A | Shared DTOs, Kafka event contracts, enums, exception handlers, JWT validator | Shared dependency |

---

## Core Features

- **Asynchronous Document Ingestion:** Uploads return immediately with a `PENDING` tracking state while background Kafka workers handle document extraction and analysis.
- **Token-Aware MapReduce Summarization:** Files exceeding token limits are split into overlapping chunks, analyzed individually, and recursively merged into a clean executive summary.
- **Structured Data Extraction:** Pulls document attributes (vendor names, dates, amounts, reference numbers, line items) directly into structured JSON.
- **Audit & Inconsistency Detection:** Scans text for missing critical clauses, compliance risks, and discrepancies, assigning an overall confidence score.
- **In-Memory RAG Document Q&A:** Answers user queries by tokenizing questions, scoring document chunks for keyword density, and feeding only the most relevant sections to the model.
- **Document Version Diffing:** Generates hierarchical outlines of two different document versions to highlight added, removed, and modified clauses.
- **Role-Based Access Control:** Secure JWT authentication with separated roles (`EMPLOYEE` vs `ADMIN`).

---

## Tech Stack

### Microservices Backend
- **Java 21** & **Spring Boot 3.5.4**
- **Spring Cloud Gateway 4.2.0** (API Gateway)
- **Spring AI 1.0.0** (OpenAI / Groq / Gemini compatible)
- **Apache Kafka** (confluentinc cp-kafka 7.5.0)
- **PostgreSQL 16** with database-per-service isolation
- **Spring Security** with **JJWT 0.11.5**
- **Document Parsers:** Apache PDFBox 3.0.4, Apache POI 5.4.1, Apache Tika 3.2.1
- **Tokenization:** JTokkit (CL100K tokenizer for local, offline token counting)

### Frontend
- **React 19** with **Vite**
- **Tailwind CSS v4**
- **React Router v7**
- **Lucide Icons** & **Axios**

---

## Project Structure

```
ai-doc-intelligence-platform/
├── backend/
│   ├── pom.xml                     # Parent multi-module POM
│   ├── common-lib/                 # Shared DTOs, Kafka events, enums, JWT validator
│   │   ├── pom.xml
│   │   └── src/main/java/com/enterprise/common/
│   ├── api-gateway/                # Spring Cloud Gateway (Port 8080)
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/main/resources/application.yml
│   ├── auth-service/               # Authentication & User Management (Port 8081)
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/main/java/com/enterprise/auth/
│   ├── document-service/           # Uploads & Document Metadata (Port 8082)
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/main/java/com/enterprise/document/
│   └── ai-service/                 # Text extraction, MapReduce, RAG Q&A (Port 8083)
│       ├── Dockerfile
│       ├── pom.xml
│       └── src/main/java/com/enterprise/ai/
├── frontend/                       # React 19 + Tailwind CSS Frontend (Port 80 / 5173)
│   ├── Dockerfile
│   ├── package.json
│   └── src/
├── docker/
│   └── postgres/
│       └── init-databases.sql      # Auto-creates docintel_auth, docintel_docs, docintel_ai
├── docker-compose.yml              # Microservices stack orchestration
└── README.md
```

---

## Getting Started

### Prerequisites
- [Docker](https://www.docker.com/) & Docker Compose installed
- *(Optional for manual local run)* Java 21, Maven 3.9+, Node.js 20+

---

### Option 1: Running with Docker Compose (Recommended)

1. **Clone the repository:**
   ```bash
   git clone https://github.com/kadamatharva2722/ai-doc-intelligence-platform.git
   cd ai-doc-intelligence-platform
   ```

2. **Configure your environment variables in `.env` (or pass inline):**
   ```env
   POSTGRES_PASSWORD=postgrespassword
   OPENAI_API_KEY=your_groq_or_openai_or_gemini_api_key_here
   JWT_SECRET=super_secret_jwt_key_that_is_at_least_32_characters_long_12345
   SPRING_AI_OPENAI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai/
   ```

3. **Start all microservices and infrastructure:**
   ```bash
   docker compose up --build
   ```

4. **Access the application:**
   - **Frontend UI:** `http://localhost`
   - **API Gateway:** `http://localhost:8080/api`
   - **Auth Service:** `http://localhost:8081`
   - **Document Service:** `http://localhost:8082`
   - **AI Analysis Service:** `http://localhost:8083`
   - **PostgreSQL:** `localhost:5432` (`docintel_auth`, `docintel_docs`, `docintel_ai`)
   - **Kafka:** `localhost:9092`

---

### Option 2: Running Services Manually for Development

#### 1. Start Infrastructure (Postgres & Kafka)
```bash
docker compose up -d postgres zookeeper kafka
```

#### 2. Build the Maven Multi-Module Backend
From the root directory:
```bash
cd backend
mvn clean install -DskipTests
```

#### 3. Run Individual Microservices
You can start each service in separate terminal windows:
```bash
# Terminal 1: Auth Service
cd backend/auth-service
mvn spring-boot:run

# Terminal 2: Document Service
cd backend/document-service
mvn spring-boot:run

# Terminal 3: AI Service
cd backend/ai-service
mvn spring-boot:run

# Terminal 4: API Gateway
cd backend/api-gateway
mvn spring-boot:run
```

#### 4. Run Frontend
```bash
cd frontend
npm install
npm run dev
```
*Frontend runs on `http://localhost:5173` and routes API requests to `http://localhost:8080/api` through the API Gateway.*

---

## API Overview

All requests are unified through the **API Gateway** (`http://localhost:8080`):

| Method | Endpoint | Routed To | Description | Auth Required |
| :--- | :--- | :--- | :--- | :---: |
| `POST` | `/api/auth/register` | `auth-service:8081` | Register new user account (`EMPLOYEE` / `ADMIN`) | No |
| `POST` | `/api/auth/login` | `auth-service:8081` | Authenticate and obtain JWT bearer token | No |
| `POST` | `/api/documents/upload` | `document-service:8082` | Upload PDF/DOCX/TXT file for async processing | Yes |
| `GET` | `/api/documents` | `document-service:8082` | Fetch all documents owned by user (or all if admin) | Yes |
| `GET` | `/api/documents/{id}` | `document-service:8082` | Get document metadata and processing status | Yes |
| `GET` | `/api/documents/search` | `document-service:8082` | Filter documents by filename, format, date range | Yes |
| `GET` | `/api/documents/stats` | `document-service:8082` | Get document count statistics | Yes |
| `GET` | `/api/analysis/{id}` | `ai-service:8083` | Retrieve AI summary, structured JSON, & audit issues | Yes |
| `POST` | `/api/analysis/{id}/chat` | `ai-service:8083` | Ask questions against document via RAG context | Yes |
| `GET` | `/api/analysis/{id}/chat` | `ai-service:8083` | Retrieve Q&A history for document | Yes |
| `POST` | `/api/analysis/compare` | `ai-service:8083` | Compare two documents and generate diff report | Yes |
| `GET` | `/api/analysis/dashboard` | `ai-service:8083` | Aggregated dashboard metrics across platform | Yes |

---

## Environment Variables

| Variable | Description | Default (Local Dev) |
| :--- | :--- | :--- |
| `POSTGRES_PASSWORD` | PostgreSQL database password | `postgrespassword` |
| `OPENAI_API_KEY` | API Key for LLM provider (Groq / Gemini / OpenAI) | None |
| `SPRING_AI_OPENAI_BASE_URL` | Base URL for OpenAI-compatible endpoint | `https://generativelanguage.googleapis.com/v1beta/openai/` |
| `JWT_SECRET` | Secret key for signing HS256 JWT tokens (>= 32 chars) | Dev fallback key |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker host & port | `localhost:9092` (or `kafka:29092` in Docker) |
| `AUTH_SERVICE_URL` | Gateway target URL for Auth Service | `http://auth-service:8081` |
| `DOCUMENT_SERVICE_URL` | Gateway target URL for Document Service | `http://document-service:8082` |
| `AI_SERVICE_URL` | Gateway target URL for AI Service | `http://ai-service:8083` |

---

## License

This project is licensed under the MIT License - feel free to use, modify, and build upon it.
