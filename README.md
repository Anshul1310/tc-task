# Campus Lost & Found System

A backend system for NIT Trichy students to report lost and found items, with **AI-powered similarity matching** using text and image embeddings.

When someone reports a lost item, the system automatically searches all found items for potential matches (and vice versa). Users can also search by text description or by uploading an image.

## Tech Stack

| Technology | Purpose |
|-----------|---------|
| Node.js + Express | Backend server |
| PostgreSQL | Database |
| Prisma ORM | Database queries |
| pgvector | Vector similarity search |
| DAuth | NIT Trichy SSO authentication |
| Multer | Image file uploads |
| Hugging Face API | Text & image embeddings |

---

## Folder Structure

```
tc-backend/
│
├── src/
│   ├── config/
│   │   └── db.js                 # Prisma client (single DB connection)
│   │
│   ├── controllers/              # Handle requests → call services → send responses
│   │   ├── authController.js     # Login, callback, logout, getCurrentUser
│   │   ├── itemController.js     # CRUD for lost/found items
│   │   └── searchController.js   # Text and image similarity search
│   │
│   ├── middleware/               # Functions that run before route handlers
│   │   ├── authMiddleware.js     # Checks if user is logged in
│   │   └── uploadMiddleware.js   # Multer config for image uploads
│   │
│   ├── routes/                   # Maps URLs → controller functions
│   │   ├── authRoutes.js         # /auth/*
│   │   ├── itemRoutes.js         # /items/*
│   │   └── searchRoutes.js       # /search/*
│   │
│   ├── services/                 # External API integrations (swappable)
│   │   ├── dauthService.js       # DAuth OAuth2 API calls
│   │   └── embeddingService.js   # Hugging Face text/image embeddings
│   │
│   ├── prisma/
│   │   └── vectors.js            # Raw SQL for pgvector similarity queries
│   │
│   ├── uploads/                  # Multer saves images here (gitignored)
│   ├── utils/
│   │   └── helpers.js            # Error formatting utilities
│   │
│   ├── app.js                    # Express app configuration
│   └── server.js                 # HTTP server entry point
│
├── prisma/
│   ├── schema.prisma             # Database models (User, Item, Status)
│   └── add-vector-columns.sql    # pgvector columns (run after Prisma migrate)
│
├── init.sql                      # Enables pgvector extension on DB init
├── Dockerfile                    # Container build for the backend
├── docker-compose.yml            # Orchestrates backend + PostgreSQL
├── .env.example                  # Environment variable template
└── package.json
```

---

## Setup

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and Docker Compose
- [Node.js](https://nodejs.org/) v18+ (for local development)
- A [Hugging Face](https://huggingface.co/settings/tokens) API token (free tier works)
- A DAuth client registered at [auth.delta.nitt.edu](https://auth.delta.nitt.edu)

### 1. Clone and configure

```bash
git clone <your-repo-url>
cd tc-backend

# Copy the env template and fill in your values
cp .env.example .env
```

Edit `.env` with your actual credentials:

```env
PORT=3000
NODE_ENV=development
DATABASE_URL=postgresql://postgres:password@localhost:5432/lostandfound?schema=public

# Get these from auth.delta.nitt.edu
DAUTH_CLIENT_ID=your_client_id
DAUTH_CLIENT_SECRET=your_client_secret
DAUTH_REDIRECT_URI=http://localhost:3000/auth/callback

# Get from https://huggingface.co/settings/tokens
HUGGINGFACE_API_KEY=hf_xxxxxxxxxxxxx

SESSION_SECRET=any-random-string-here
```

### 2. Start with Docker (recommended)

```bash
# Start PostgreSQL + Backend
docker compose up

# In a new terminal, run Prisma migrations
docker compose exec app npx prisma migrate dev --name init

# Add pgvector columns (Prisma can't do this)
docker compose exec db psql -U postgres -d lostandfound -f /docker-entrypoint-initdb.d/init.sql
docker compose exec db psql -U postgres -d lostandfound -c "$(cat prisma/add-vector-columns.sql)"
```

### 3. Start locally (without Docker)

```bash
# Install dependencies
npm install

# Make sure PostgreSQL is running with pgvector installed
# Then run migrations
npx prisma migrate dev --name init

# Add vector columns
psql -U postgres -d lostandfound -f prisma/add-vector-columns.sql

# Generate Prisma client
npx prisma generate

# Start the server
npm run dev
```

The server will be running at `http://localhost:3000`.

---

## DAuth Configuration

DAuth is NIT Trichy's OAuth2 SSO by Delta Force.

### How to register your app

1. Go to [auth.delta.nitt.edu](https://auth.delta.nitt.edu)
2. Create an account with your webmail
3. Navigate to **Clients** → Register your application
4. Set **Homepage URL** to `http://localhost:3000`
5. Set **Callback URL** to `http://localhost:3000/auth/callback`
6. Copy the **Client ID** and **Client Secret** into your `.env`

### How the login flow works

```
Browser → GET /auth/login
         ↓ (redirect to DAuth)
DAuth login page → User enters credentials
         ↓ (redirect back with ?code=xxx)
Browser → GET /auth/callback?code=xxx
         ↓ (server exchanges code for token)
Server → POST auth.delta.nitt.edu/api/oauth/token
         ↓ (server fetches user profile)
Server → POST auth.delta.nitt.edu/api/resources/user
         ↓ (create/find user in DB, save to session)
Browser ← { user: { id, name, email } }
```

---

## pgvector Setup

### What is pgvector?

pgvector is a PostgreSQL extension that adds support for storing and querying vector embeddings. It lets you do similarity search directly in your database.

### Why raw SQL?

Prisma doesn't support the `vector` column type natively. So we:

1. Define the regular columns (title, description, etc.) in `schema.prisma`
2. Add vector columns using raw SQL (`prisma/add-vector-columns.sql`)
3. Query vectors using `prisma.$queryRawUnsafe()` in `src/prisma/vectors.js`

### Vector dimensions

| Model | Dimensions | Column |
|-------|-----------|--------|
| BAAI/bge-small-en-v1.5 | 384 | `textEmbedding` |
| openai/clip-vit-base-patch32 | 512 | `imageEmbedding` |

---

## API Endpoints

### Authentication

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/auth/login` | No | Redirects to DAuth login page |
| GET | `/auth/callback` | No | DAuth redirects here after login |
| POST | `/auth/logout` | Yes | Destroys the session |
| GET | `/auth/me` | Yes | Returns the logged-in user |

### Items

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/items` | No | List all items (optional `?status=LOST`) |
| GET | `/items/:id` | No | Get a single item |
| POST | `/items` | Yes | Create a lost/found item (multipart form) |
| PUT | `/items/:id` | Yes | Update own item |
| DELETE | `/items/:id` | Yes | Delete own item |
| PATCH | `/items/:id/claim` | Yes | Mark item as CLAIMED |

### Search

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/search/text` | No | Search by text description |
| POST | `/search/image` | No | Search by uploaded image |

### Health

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Returns `{ status: "ok" }` |

---

## API Examples

### Create a Lost Item

```bash
curl -X POST http://localhost:3000/items \
  -F "title=Black Wallet" \
  -F "description=Black leather wallet with college ID inside" \
  -F "category=Wallet" \
  -F "location=Library" \
  -F "dateLostOrFound=2025-01-15" \
  -F "status=LOST" \
  -F "images=@/path/to/wallet.jpg" \
  --cookie "connect.sid=your-session-cookie"
```

**Response:**

```json
{
  "item": {
    "id": 1,
    "title": "Black Wallet",
    "description": "Black leather wallet with college ID inside",
    "category": "Wallet",
    "location": "Library",
    "dateLostOrFound": "2025-01-15T00:00:00.000Z",
    "status": "LOST",
    "images": ["uploads/1719756000000-482910384.jpg"],
    "userId": 1,
    "createdAt": "2025-01-15T10:30:00.000Z",
    "updatedAt": "2025-01-15T10:30:00.000Z"
  },
  "matches": [
    {
      "itemId": 5,
      "title": "Brown Wallet Found",
      "similarity": 0.87
    }
  ]
}
```

### Text Search

```bash
curl -X POST http://localhost:3000/search/text \
  -H "Content-Type: application/json" \
  -d '{"query": "Black wallet near library"}'
```

**Response:**

```json
{
  "matches": [
    { "itemId": 1, "title": "Black Wallet", "status": "LOST", "similarity": 0.95 },
    { "itemId": 5, "title": "Wallet Found at Library", "status": "FOUND", "similarity": 0.82 }
  ]
}
```

### Image Search

```bash
curl -X POST http://localhost:3000/search/image \
  -F "image=@/path/to/photo.jpg"
```

**Response:**

```json
{
  "matches": [
    { "itemId": 3, "title": "Blue Backpack", "status": "FOUND", "similarity": 0.91 },
    { "itemId": 7, "title": "Dark Blue Bag", "status": "LOST", "similarity": 0.78 }
  ]
}
```

---

## Similarity Search Workflow

Here's what happens when you create a new item:

```
User creates "Lost: Black Wallet"
        ↓
1. Item is saved to PostgreSQL (normal Prisma insert)
        ↓
2. Text embedding is generated:
   "Black Wallet. Black leather wallet with college ID"
    → Sent to Hugging Face BGE-small model
    → Returns 384 floats like [0.032, -0.15, 0.87, ...]
        ↓
3. Image embedding is generated (if image uploaded):
   wallet.jpg → Sent to Hugging Face CLIP model
    → Returns 512 floats like [0.12, 0.45, -0.33, ...]
        ↓
4. Both vectors stored in PostgreSQL pgvector columns:
   UPDATE "Item" SET "textEmbedding" = '[0.032, ...]'::vector
        ↓
5. Similarity search runs against FOUND items:
   SELECT id, 1 - ("textEmbedding" <=> query_vector) AS similarity
   FROM "Item"
   WHERE status = 'FOUND'
   ORDER BY "textEmbedding" <=> query_vector
   LIMIT 10
        ↓
6. Returns: [{ itemId: 5, similarity: 0.87 }, ...]
```

### How cosine similarity works

- Two vectors pointing in the same direction → similarity = 1.0 (identical meaning)
- Two vectors at 90° → similarity = 0.0 (unrelated)
- Two vectors pointing opposite → similarity = -1.0 (opposite meaning)

pgvector's `<=>` operator returns **cosine distance** (1 - similarity), so we compute `1 - distance` to get similarity.

---

## Migrating to Local Models

The embedding service (`src/services/embeddingService.js`) is the **only file** that talks to Hugging Face. To switch to local models:

1. Set up a local inference server (e.g., using Python + FastAPI + sentence-transformers)
2. Change the API URLs in `embeddingService.js` from Hugging Face to `http://localhost:8000`
3. Everything else (controllers, routes, database) stays the same

```javascript
// Before (Hugging Face)
const HF_API_URL = "https://api-inference.huggingface.co/models";

// After (local server)
const HF_API_URL = "http://localhost:8000/models";
```

---

## License

ISC
