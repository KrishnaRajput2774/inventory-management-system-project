# Inventory Management System (IMS) + AI Integration

A full-stack Inventory Management System built with **Spring Boot** for both backend and frontend.  
This project follows a **monorepo structure**, where the backend provides secure REST APIs and the frontend delivers a responsive web interface.

Additionally, the system integrates **AI-powered query support** using **LLaMA 3** and **Phi-4** models via **Ollama** and **LangChain4j**, enabling natural language interaction with the inventory data.

---

## 🚀 Features
### Backend
- Spring Boot REST APIs
- JWT-based authentication & authorization
- Role-based access (Admin, User)
- Database integration with PostgreSQL/MySQL
- CRUD operations for inventory items, users, and transactions
- AI-powered queries with **LLaMA 3** and **Phi-4** models

### Frontend
- Spring Boot MVC with Thymeleaf
- Styled using Tailwind CSS
- Interactive dashboard for inventory tracking
- Add to Cart, Wishlist, and Order management
- User and Admin login/registration

---

## 🤖 AI Integration
- **Ollama** used for model management and inference
- **LangChain4j** used for prompt building and orchestration
- Supports intelligent queries like:
    - “Show me items low in stock”
    - “Predict demand for next month”
    - “Summarize sales in the last 7 days”

---

## 📂 Project Structure
inventory-management-system/
│── backend/ # REST API + AI integration
│── frontend/ # UI (Thymeleaf + Tailwind)
│── README.md


---

## 🛠️ Tech Stack
- **Backend:** Java, Spring Boot, Spring Security, JPA/Hibernate
- **Frontend:** Spring Boot MVC, Thymeleaf, Tailwind CSS
- **Database:** PostgreSQL / MySQL
- **AI:** Ollama, LangChain4j, LLaMA 3, Phi-4
- **Build Tools:** Maven
- **Version Control:** Git & GitHub

---
