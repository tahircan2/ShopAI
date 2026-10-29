<div align="center">

# ✨ ShopAI

**A multi-vendor e-commerce platform with a built-in, multi-agent AI shopping assistant.**

![Angular](https://img.shields.io/badge/Angular_19-DD0031?style=flat-square&logo=angular&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.3-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-009688?style=flat-square&logo=fastapi&logoColor=white)
![LangGraph](https://img.shields.io/badge/LangGraph-1C3C3C?style=flat-square&logo=langchain&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL_8-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker_Compose-2496ED?style=flat-square&logo=docker&logoColor=white)

<img src="docs/screenshots/home.png" alt="ShopAI home page" width="900">

</div>

## Highlights

| | |
|---|---|
| 🤖 **Agentic assistant** | A LangGraph supervisor routes each message to 10 specialised agents: search, filter, cart, checkout, orders, analytics, FAQ, navigation and more |
| 🛡️ **Human-in-the-loop** | Sensitive actions (checkout, cart changes) need explicit user approval before a tool runs |
| 👥 **3 roles, 3 dashboards** | Customer, Seller and Admin, each with analytics, charts and audit logs |
| 🔍 **Instant search** | Typesense full-text search with typo tolerance |
| 🔐 **Security-hardened** | JWT in HttpOnly cookies, Bucket4j / Redis rate limiting, internal service key between Java and Python, Flyway-versioned schema |

## Screenshots

| Product catalog | Product detail |
|:---:|:---:|
| <img src="docs/screenshots/products.png" width="440"> | <img src="docs/screenshots/product-detail.png" width="440"> |
| **AI assistant** | **Customer dashboard** |
| <img src="docs/screenshots/ai-chat.png" width="440"> | <img src="docs/screenshots/dashboard.png" width="440"> |
| **Admin panel** | **Seller panel** |
| <img src="docs/screenshots/admin.png" width="440"> | <img src="docs/screenshots/seller.png" width="440"> |

## Architecture

```mermaid
graph LR
    A["Angular 19 SPA"] -->|REST + SSE| B["Spring Boot 3.3 · Java 21"]
    B -->|internal key| C["FastAPI + LangGraph"]
    B --> D[(MySQL 8)]
    B --> E[(Typesense)]
    C --> F[(Redis)]
    C -->|LLM| G["OpenAI GPT-4o-mini"]
    B --> H["Cloudinary"]
```

| Service | Folder | Role |
|---|---|---|
| Frontend | [`frontend/`](frontend) | Angular 19, standalone components, signals, Chart.js |
| Backend | [`backend/`](backend) | REST API, auth, orders, search indexing (21 controllers, 25 migrations) |
| AI service | [`ai-service/`](ai-service) | Supervisor + agents + tools, rate-limited, structured logs |

## Quick start

```bash
git clone https://github.com/tahircan2/ShopAI.git && cd ShopAI
cp .env.example .env
cp ai-service/.env.example ai-service/.env     # add OPENAI_API_KEY
# create backend/.env (JWT_SECRET, AI_INTERNAL_KEY, TYPESENSE_API_KEY, Cloudinary, SMTP)
docker compose up --build
```

Open **http://localhost**. API docs: http://localhost:8080/swagger-ui.html

Demo accounts are seeded by the Flyway migrations (see `V5__seed_data.sql`); they are for local development only.

## Docs

[Technical report](docs/TECHNICAL_REPORT.md) · [PDF](docs/ShopAI_Technical_Report.pdf) · [Docker guide](docs/DOCKER_GUIDE.md)
