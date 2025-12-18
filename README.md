# 🎧 Audio Cartographer

**Audio Cartographer** is a Spring Boot–based backend service for analyzing audio tracks and extracting meaningful musical and emotional features.

The project is designed as a foundation for deeper audio analysis, combining digital signal processing (DSP) techniques with higher-level semantic interpretation.

---

## ✨ Features (current stage)

- Spring Boot REST API
- Health check endpoint
- Audio file upload via multipart/form-data
- Clean layered architecture (Controller → Service → DTO)
- Ready for audio signal processing integration

---

## 🚀 Planned features

- BPM (tempo) detection  
- Energy and loudness analysis (RMS)
- Spectral features (brightness, centroid)
- Emotional heuristics based on musical characteristics
- Caching of analysis results
- Simple frontend for visualization

---

## 🛠 Tech Stack

- **Java 17**
- **Spring Boot 3**
- **Spring Web**
- **Spring Data JPA**
- **H2 Database**
- **Maven**
- *(planned)* TarsosDSP for audio analysis

---

## ▶️ Running the project

```bash
./mvnw spring-boot:run
```

The application will start on:

http://localhost:8080


Health check:

```
GET /health
```


📦 API (current)
Upload audio for analysis (stub)
POST /api/analyze
Content-Type: multipart/form-data


Returns a JSON response (analysis logic will be extended).

📌 Project status

🚧 Work in progress
This repository is an evolving proof-of-concept focused on audio analysis and creative technology.

👩‍💻 Author

Created by Daria Konstantinova

