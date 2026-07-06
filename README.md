🎬 BingeTogether
Watch movies together with your friends in real-time.
BingeTogether is a full-stack web application that lets multiple users join a common room, synchronize video playback, and chat while watching content together.

🚀 Features

- 👤 User Authentication (JWT)
- 🎥 Create & Join Watch Rooms
- ▶️ Real-time Video Synchronization
- 💬 Live Chat using WebSockets (STOMP)
- 🔄 Host controls (Play/Pause/Seek)
- 🔐 Secure Backend APIs
- 🌐 Responsive UI
- ☁️ AWS Deployment
- 🚇 ngrok Proxy for HTTPS ↔ HTTP communication

🛠 Tech Stack
Frontend
- React.js
- HTML
- CSS
- JavaScript

Backend
- Spring Boot
- Java
- Spring Security
- JWT Authentication
- Spring WebSocket (STOMP)

Database
- MySQL

DevOps
- Docker
- AWS EC2
- ngrok
- Git
- GitHub

📂 Project Structure
BingeTogether
│
├── frontend/
├── backend/
├── docker-compose.yml
├── Dockerfile
└── README.md

Getting Started

### Clone Repository

```bash
git clone https://github.com/void2306/BingeTogether.git
```

### Backend

```bash
cd BingeTogether
mvn spring-boot:run
```

### Frontend

 ```bash
cd frontend
npm install
npm run dev
```
-Architecture
React Frontend
       │
 REST API + WebSocket
       │
Spring Boot Backend
       │
      MySQL
       │
    AWS EC2

🚧 Challenges Faced
One of the biggest deployment challenges was handling communication between:
Frontend (HTTPS on Vercel)
Backend (HTTP on AWS EC2)
This caused Mixed Content errors, preventing WebSocket connections.
Solution:
- Used **ngrok** to create a secure HTTPS tunnel.
- Routed requests through ngrok.
- Successfully established secure WebSocket communication.

⚡Real-Time Capabilities
- Live Room Synchronization
- Instant Chat
- WebSocket Communication
- Shared Video Controls
- Low-latency Updates

🔮 Future Improvements
- 🤖 AI Movie Recommendation
- 🎭 Emotion Detection
- 🍿 Watch History
- 🎵 Voice Chat
- 📱 Mobile Responsive Improvements
- 🎬 Netflix/Youtube Integration

💡 Why BingeTogether?
Watching movies remotely with friends often lacks synchronization and interaction.

BingeTogether solves this by enabling synchronized playback, live messaging, and collaborative watch rooms, creating a shared viewing experience regardless of location.
