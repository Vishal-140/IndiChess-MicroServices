# IndiChess - Microservices♟️

**IndiChess** is a real-time, microservices-based multiplayer chess application. It features a modern, responsive React frontend and a robust Spring Boot backend architecture designed for scalability and real-time interaction.

---

## Project Overview

**IndiChess** allows users to sign up, log in, and play chess against other players in real-time.

### Key Features
*   **Authentication**: Secure Login/Signup using JWT (JSON Web Tokens) stored in HTTP-Only cookies.
*   **Matchmaking**: Automatic pairing of players based on game mode (Standard, Blitz, Rapid).
*   **Real-Time Gameplay**: Instant move updates using WebSockets (STOMP over SockJS).
*   **Game Modes**: Support for different chess time controls.
*   **Microservices Architecture**: Decoupled services for better maintainability and scalability, managed behind an API Gateway.

### High-Level Architecture
The project follows a microservices pattern:
1.  **Frontend (React)**: The user interface.
2.  **API Gateway**: The single entry point for all client requests, handling routing and initial authentication checks.
3.  **Service Registry (Eureka)**: Tracks available service instances.
4.  **Backend Services**:
    *   `USER-SERVICE`: Manages users and authentication.
    *   `MATCHMAKING-SERVICE`: Handles the matchmaking queue and pairing logic.
    *   `GAME-SERVICE`: Manages game state, move validation, and persistence.

---

## Tech Stack

### Backend
*   **Language**: Java 17+
*   **Framework**: Spring Boot 3.x
*   **Security**: Spring Security, JWT (JJWT)
*   **Communication**: REST API (OpenFeign), WebSockets (STOMP)
*   **Database**: MySQL (JPA/Hibernate)
*   **Service Discovery**: Netflix Eureka
*   **Gateway**: Spring Cloud Gateway

### Frontend
*   **Framework**: React.js
*   **Styling**: CSS (Modern, Responsive)
*   **HTTP Client**: Axios
*   **Real-Time**: SockJS, StompJS

---

## 📂 Project Structure

```
IndiChess-MicroServices/
├── SERVICE-REGISTRY/       # Netflix Eureka Server
├── API-GATEWAY/            # Spring Cloud Gateway (Entry Point)
├── USER-SERVICE/           # User Management & Auth
├── MATCHMAKING-SERVICE/    # Queue & Match Creation
├── GAME-SERVICE/           # Game Logic & State
└── indichessfrontend/      # React Application
```

### Service Descriptions
*   **SERVICE-REGISTRY**: Acts as a phonebook. Services register themselves here so they can find each other dynamically.
*   **API-GATEWAY**: Routes incoming traffic to the correct service (e.g., `/login` -> `USER-SERVICE`). It also validates JWT tokens in headers/cookies.
*   **USER-SERVICE**: Handles `User` entities, password hashing (BCrypt), and JWT generation.
*   **MATCHMAKING-SERVICE**: Maintains a queue of waiting players. When two players match, it requests `GAME-SERVICE` to create a new game.
*   **GAME-SERVICE**: The core source of truth for the game. Validates moves, checks for checkmate/draw, and broadcasts state updates to players.

---

## 🔌 Backend Services & API Endpoints

### Service Details
*   **User Service** (Port `8081`): Handles Auth & Profiles. DB: `users_db`
*   **Matchmaking Service** (Port `8082`): Handles Queue & Pairs. DB: `matchmaking_db`
*   **Game Service** (Port `8083`): Handles Game Logic. DB: `games_db`

### 🔗 Consolidated API Endpoints

| Service | Endpoint | Method | Purpose | Protected |
| :--- | :--- | :--- | :--- | :--- |
| **User** | `/signup` | POST | Register a new user | No |
| **User** | `/login` | POST | Login and receive JWT cookie | No |
| **User** | `/logout` | POST | Clear auth cookie | Yes |
| **User** | `/home` | GET | Verify auth status | Yes |
| **Match** | `/matchmaking/join` | POST | User joins queue | Yes |
| **Match** | `/matchmaking/check` | GET | Poll for match status | Yes |
| **Match** | `/matchmaking/cancel` | POST | Leave queue | Yes |
| **Game** | `/games/{gameId}` | GET | Get full game state | Yes |
| **Game** | `/games/{gameId}/move` | POST | Make a move (REST) | Yes |
| **Game** | `/game/create` | POST | Internal (Matchmaking -> Game) | Internal |

---

## API Gateway
*   **Port**: `8060`
*   **Purpose**: Routing and Security.

**Routing Rules:**
*   `/signup`, `/login`, `/logout`, `/home` ➝ `USER-SERVICE`
*   `/matchmaking/**` ➝ `MATCHMAKING-SERVICE`
*   `/games/**` ➝ `GAME-SERVICE`
*   `/game/ws/**` ➝ `GAME-SERVICE` (WebSocket)

**Security Flow:**
1.  Gateway intercepts request.
2.  Extracts JWT from `Authorization` header or `jwt` cookie.
3.  Validates signature and expiration.
4.  Injects `X-USER-ID` header into the downstream request so microservices know who is calling.

---

## WebSocket & Real-Time Flow

**Connection:**
*   Frontend connects to `http://localhost:8060/game/ws` (routed to Game Service).
*   Protocol: WebSocket (SockJS fallback).

**Topics:**
*   `/topic/game/{gameId}`: Clients subscribe to this to receive game updates.

**Move Flow:**
1.  Player makes a move in React UI.
2.  Frontend sends STOMP message to `/app/game/{gameId}/move`.
3.  `GameSocketController` receives the move.
4.  `GameService` validates logic (turn, piece usage, check).
5.  If valid, `GameService` updates DB and **broadcasts** new state to `/topic/game/{gameId}`.
6.  Both clients receive the update and re-render the board.

---

## Frontend (React)

### Key Pages
*   **Home (`/`)**: Landing page.
*   **Login/Signup**: Auth forms.
*   **Dashboard (`/home`)**: User stats and "Play" button.
*   **Game (`/game/{gameId}`)**: The chessboard.

### Key Logic
*   **`AuthWrapper.js`**: Protects routes. If no auth cookie/token is found, redirects to login.
*   **`Board.js`**: Renders the chessboard, handles drag-and-drop, and highlights valid moves.
*   **`GameContainer.js`**: Manages the WebSocket connection. Listens for incoming moves and updates the `Board`.

---

## Data Flow: The "Play" Journey

1.  **User Login**: User POSTs credentials to `/login`. Gateway proxies to User Service. User Service validates and sets an **HTTP-Only Cookie**.
2.  **Matchmaking**:
    *   User clicks "Play".
    *   Frontend request `/matchmaking/join`.
    *   Frontend polls `/matchmaking/check` every few seconds.
3.  **Match Found**:
    *   User 2 joins queue. Matchmaking Service pairs them.
    *   Matchmaking Service calls `GameService` (via Feign) to create a Game.
    *   `GameService` returns `gameId`.
    *   Next poll to `/matchmaking/check` returns this `gameId`.
4.  **Game Start**:
    *   Frontend redirects to `/game/{gameId}`.
    *   Frontend fetches initial state (GET `/games/{gameId}`).
    *   Frontend opens WebSocket connection.
5.  **Gameplay**:
    *   White moves.
    *   Backend validates and broadcasts new FEN.
    *   Black receives update, board updates.

---

## How to Run Locally

### Prerequisites
*   Java JDK 17+
*   Node.js & npm
*   MySQL Server (Running on port 3306)

### 1. Database Setup
Create the required databases in MySQL:
```sql
CREATE DATABASE users_db;
CREATE DATABASE matchmaking_db;
CREATE DATABASE games_db;
```

### 2. Start Backend Services
Open 5 terminal tabs. Run the services **in this specific order**:

**Terminal 1: Service Registry**
```bash
cd SERVICE-REGISTRY
./mvnw spring-boot:run
```
*(Wait for it to start on port 8761)*

**Terminal 2: API Gateway**
```bash
cd API-GATEWAY
./mvnw spring-boot:run
```
*(Port 8060)*

**Terminal 3: User Service**
```bash
cd USER-SERVICE
./mvnw spring-boot:run
```
*(Port 8081)*

**Terminal 4: Matchmaking Service**
```bash
cd MATCHMAKING-SERVICE
./mvnw spring-boot:run
```
*(Port 8082)*

**Terminal 5: Game Service**
```bash
cd GAME-SERVICE
./mvnw spring-boot:run
```
*(Port 8083)*

### 3. Start Frontend
**Terminal 6**
```bash
cd indichessfrontend
npm install
npm start
```
*(Runs on http://localhost:3000)*
