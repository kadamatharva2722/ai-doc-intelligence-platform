# AI-Powered Document Intelligence Platform

A full-stack document processing platform designed to handle large PDFs, Word documents, and text files. Built with Spring Boot 3, Apache Kafka, PostgreSQL, Spring AI, and React, the system processes documents asynchronously, extracts structured metadata, runs MapReduce summarization, flags document risks, and provides interactive Q&A.

---

## Why I Built This

Most AI document tools struggle when dealing with long, multi-page business files:
1. Shoving 40-page PDFs into a single prompt frequently exceeds token windows or triggers `429 Too Many Requests` rate limits.
2. Synchronous file uploads freeze the browser and timeout when multiple users submit documents at once.
3. Raw LLM outputs often hallucinate schemas instead of providing consistent, parseable metadata.

This platform addresses these issues by decoupling file uploads from AI processing using **Kafka**, estimating token volume locally before dispatching requests, and applying a **MapReduce chunking strategy** with exponential backoff retries.

---

## Architecture & Processing Flow

```
[ Browser / React UI ]
         |
         |  1. POST /api/documents/upload (Multipart)
         v
[ Spring Boot REST API ] ----> [ Local Disk Storage (./uploads) ]
         |
         |  2. Publish "document-uploaded" event
         v
[ Apache Kafka Topic ]
         |
         |  3. Async Consumption
         v
[ Kafka Consumer Worker ]
         |
         +---> 4. Text Extraction (Apache PDFBox / Apache POI / Tika)
         |
         +---> 5. Local Token Counting (JTokkit CL100K)
         |
         +---> 6. Chunking & MapReduce LLM Pipeline (Spring AI + Llama 3.3 / Gemini)
         |          - Per-chunk summarization & metadata extraction
         |          - Recursive consolidation & risk scoring
         |
         +---> 7. Persist Results (PostgreSQL)
                    - DocumentAnalysis
                    - Status: PROCESSING -> COMPLETED
```

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

### Backend
- **Java 21** & **Spring Boot 3.5.4**
- **Spring AI (OpenAI / Groq / Gemini compatible)**
- **Apache Kafka** (confluentinc cp-kafka 7.5.0)
- **PostgreSQL 16** (Spring Data JPA / Hibernate)
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
│   ├── src/
│   │   ├── main/java/com/enterprise/docintel/
│   │   │   ├── ai/            # DocumentAiService (MapReduce, RAG, prompt templates)
│   │   │   ├── config/        # Kafka & Spring AI configuration
│   │   │   ├── controller/    # Auth, Document, and Analysis REST endpoints
│   │   │   ├── dto/           # Request/Response data contracts
│   │   │   ├── entity/        # JPA Entities (User, Document, DocumentAnalysis, etc.)
│   │   │   ├── repository/    # Spring Data JPA repositories & Specifications
│   │   │   ├── security/      # JWT filter, auth entrypoint, WebSecurityConfig
│   │   │   └── service/       # File storage, Kafka producer/consumer, business logic
│   │   └── main/resources/
│   │       ├── application.yml
│   │       └── schema.sql
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/        # Layout, sidebar, top nav
│   │   ├── pages/             # Dashboard, Upload, Document Drawer, Details, Compare, Auth
│   │   └── services/          # Axios instance with JWT interceptors
│   ├── Dockerfile
│   └── package.json
├── docker-compose.yml         # Multi-container orchestration (DB, Kafka, Zookeeper, App)
└── .gitignore
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

2. **Create your local `.env` file at the root:**
   ```env
   POSTGRES_PASSWORD=postgrespassword
   OPENAI_API_KEY=your_groq_or_openai_or_gemini_api_key_here
   JWT_SECRET=super_secret_jwt_key_that_is_at_least_32_characters_long_12345
   ```

3. **Start the containers:**
   ```bash
   docker compose up --build
   ```

4. **Access the application:**
   - Frontend UI: `http://localhost`
   - Backend API: `http://localhost:8080/api`
   - PostgreSQL: `localhost:5432` (db: `docintel`, user: `postgres`)
   - Kafka: `localhost:9092`

---

### Option 2: Running Services Manually

#### 1. Start Infrastructure (Postgres & Kafka)
```bash
docker compose up -d postgres zookeeper kafka
```

#### 2. Run the Backend
```bash
cd backend
mvn clean spring-boot:run
```
*The backend runs on port 8080.*

#### 3. Run the Frontend
```bash
cd frontend
npm install
npm run dev
```
*The dev server runs on `http://localhost:5173`.*

---

## API Overview

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :---: |
| `POST` | `/api/auth/register` | Register new user account (`EMPLOYEE` / `ADMIN`) | No |
| `POST` | `/api/auth/login` | Authenticate and obtain JWT bearer token | No |
| `POST` | `/api/documents/upload` | Upload PDF/DOCX/TXT file for async processing | Yes |
| `GET` | `/api/documents` | Fetch all documents owned by user (or all if admin) | Yes |
| `GET` | `/api/documents/{id}` | Get document metadata and processing status | Yes |
| `GET` | `/api/documents/search` | Filter documents by filename, format, date range | Yes |
| `GET` | `/api/documents/nl-search` | Natural language semantic search query | Yes |
| `GET` | `/api/analysis/{id}` | Retrieve AI summary, structured JSON, & issues | Yes |
| `POST` | `/api/analysis/{id}/chat` | Ask questions against document via RAG context | Yes |
| `GET` | `/api/analysis/{id}/chat` | Retrieve Q&A history for document | Yes |
| `POST` | `/api/analysis/compare` | Compare two documents and generate diff report | Yes |
| `GET` | `/api/analysis/dashboard` | Aggregated metrics (uploads, requests, totals) | Yes |

---

## Environment Variables

| Variable | Description | Default (Local Dev) |
| :--- | :--- | :--- |
| `POSTGRES_PASSWORD` | PostgreSQL database password | `postgrespassword` |
| `OPENAI_API_KEY` | API Key for LLM provider (Groq / Gemini / OpenAI) | None |
| `JWT_SECRET` | Secret key for signing HS256 JWT tokens (>= 32 chars) | Dev fallback key |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka broker host & port | `localhost:9092` |

---

## License

This project is licensed under the MIT License - feel free to use, modify, and build upon it.
