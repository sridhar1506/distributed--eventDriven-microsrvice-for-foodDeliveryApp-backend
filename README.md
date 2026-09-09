# Food Booking Application (Backend Focused)

This application is built using **Spring Cloud & Kafka**. Engineered for scalability, it utilizes **Spring Cloud OpenFeign** for robust inter-service synchronous communication and **Apache Kafka** for high-performance asynchronous domain event streaming. 

Optimized with **Java Virtual Threads** for maximum throughput and secured by a sophisticated **JWT + Internal Handshake** mechanism, the platform is fully observable via a modern **PLG + Tempo** stack. It represents a production-grade blueprint for new age, enterprise microservices development.

## 🚀 Key Features

- **Microservices Ecosystem**: Fully distributed architecture with Service Discovery (Eureka) and API Gateway.
- **Hybrid Communication**: Optimized use of Synchronous (OpenFeign) and Asynchronous (Kafka) patterns.
- **Enterprise Security**: Edge JWT validation and internal service-to-service handshake protection.
- **Advanced Observability**: Distributed tracing (Tempo), Log aggregation (Loki), and Metrics (Prometheus).
- **Modern Java**: Leveraging Project Loom (Virtual Threads) for reactive-like performance on a thread-per-request model.

## 📂 Documentation & Architecture

For a detailed deep dive into the system architecture, mermaid diagrams, and inter-service communication maps, please see:

👉 **[Architecture Deep Dive](architecture.md)**

---

## 🛠️ Tech Stack

- **Java 25** & **Spring Boot 4.0.5**
- **Spring Cloud** (Gateway, Eureka, OpenFeign)
- **Apache Kafka**
- **MySQL**
- **Observability**: Prometheus, Grafana Loki, Tempo, OpenTelemetry
