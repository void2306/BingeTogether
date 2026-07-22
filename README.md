⚙️ BingeTogether Backend

The backend powering **BingeTogether**, a real-time watch party platform that enables synchronized video playback, live chat, secure authentication, room management, and cloud-based media storage.

The backend is built using **Spring Boot** and provides REST APIs, STOMP WebSocket communication, Google OAuth authentication, PostgreSQL persistence, and AWS S3 integration for uploaded MP4 videos.

🌐 **Live Demo:** [BingeTogether](https://bingetogether.vercel.app)

## 🚀 Core Features

### 🔐 Authentication
- Google OAuth Authentication
- Secure user session management
- Spring Security integration
  
### 🏠 Room Management
- Create private watch rooms
- Join rooms using shareable room codes
- Track active room members
- Leave room functionality
  
### 🎥 Real-Time Synchronization
- Play synchronization
- Pause synchronization
- Seek synchronization
- Smart synchronization request popups
- Low-latency event broadcasting using WebSockets

### 💬 Live Chat
- Instant room messaging
- STOMP WebSocket communication
- Real-time message broadcasting

### ☁️ Media Storage
- Upload MP4 videos
- Secure cloud storage using AWS S3
- Retrieve uploaded media for synchronized playback

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java |
| Framework | Spring Boot |
| Security | Spring Security + Google OAuth2 |
| Real-Time Communication | WebSocket (STOMP) |
| Database | PostgreSQL |
| ORM | Spring Data JPA + Hibernate |
| Cloud Storage | AWS S3 |
| Build Tool | Maven |
| Deployment | Railway, Docker |

## 🏗️ Backend Architecture

```
                React + Vite
                      │
      REST APIs + STOMP WebSockets
                      │
              Spring Boot Backend
              ┌────────┴────────┐
              ▼                 ▼
         PostgreSQL         AWS S3
    (Users, Rooms, Chat)  (MP4 Videos)
```

## 📡 Backend Responsibilities
The backend is responsible for:
- Authenticating users using Google OAuth
- Managing watch rooms
- Managing room members
- Synchronizing video playback
- Broadcasting real-time chat messages
- Uploading MP4 videos to AWS S3
- Persisting application data
- Handling WebSocket communication
- Maintaining room state


## ⚡ Real-Time Capabilities
- Live Chat
- Playback Synchronization
- Seek Synchronization
- Smart Sync Confirmation Requests
- Member Join/Leave Updates
- Low-latency Event Broadcasting

## 📂 Project Structure

```text
src/
│
├── config/
├── controller/
├── dto/
├── entity/
├── repository/
├── security/
├── service/
├── websocket/
├── exception/
└── BingeTogetherApplication.java
```
## 🚧 Challenges Faced
Building a real-time synchronized watch party required solving several engineering challenges:
- Synchronizing video playback between multiple users with minimal latency.
- Maintaining persistent WebSocket connections.
- Implementing secure Google OAuth authentication.
- Managing uploaded MP4 videos through AWS S3.
- Handling CORS between the React frontend and Spring Boot backend.
- Deploying the frontend on Vercel and backend on Railway.

## 🧠 What I Learned
Building BingeTogether helped me gain hands-on experience with:
- Designing RESTful APIs using Spring Boot
- Building real-time applications using STOMP WebSockets
- Implementing Google OAuth authentication
- Managing relational data using PostgreSQL
- Uploading and serving media through AWS S3
- Synchronizing application state across multiple users
- Deploying Spring Boot applications on Railway
- Structuring scalable backend architecture

## ⚙️ Environment Variables

Create an **application.properties** (or configure Railway environment variables) with values similar to:

```properties
SPRING_DATASOURCE_URL=

SPRING_DATASOURCE_USERNAME=

SPRING_DATASOURCE_PASSWORD=

GOOGLE_CLIENT_ID=

GOOGLE_CLIENT_SECRET=

AWS_ACCESS_KEY_ID=

AWS_SECRET_ACCESS_KEY=

AWS_REGION=

AWS_BUCKET_NAME=
```

## ⚙️ Getting Started
Clone the repository
```bash
git clone https://github.com/void2306/BingeTogether.git
```

### Navigate to the project
```bash
cd BingeTogether
```

### Install dependencies
```bash
mvn clean install
```

### Run the backend
```bash
mvn spring-boot:run
```

The backend will start on:
```
http://localhost:8080
```

## 🔗 Frontend

The React frontend communicates with this backend using REST APIs and STOMP WebSockets.

Frontend Repository:
👉[BingeTogether-Frontend](https://github.com/void2306/BingeTogether-Frontend)


## 🚀 Future Enhancements
- 🎭 Emoji Reactions
- 🔔 Push Notifications
- 🎵 Voice Chat
- 📜 Watch History
- 👥 Friend Invitations
- 🎞️ Playback Queue
- 🛡️ Room Moderation
- 📺 Support for additional streaming platforms

## 🔗 Links
🌐 **Live Demo:**[BingeTogether](https://bingetogether.vercel.app)
💻 **Frontend Repository:** [BingeTogether-Frontend](https://github.com/void2306/BingeTogether-Frontend)
⚙️ **Backend Repository:** [BingeTogether](https://github.com/void2306/BingeTogether)

## 👩‍💻 Developer

**Sakshi Kumari**
B.Tech Computer Science Engineering
🌐 **GitHub:** [@void2306](https://github.com/void2306)
💼 **LinkedIn:** [Sakshi Kumari](https://www.linkedin.com/in/sakshi-kumari-374bb9330/)

## ⭐ Support
If you found this project interesting, consider giving the repository a ⭐.
Feedback and contributions are always welcome!
