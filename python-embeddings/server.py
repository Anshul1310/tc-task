

import os
import tempfile
from typing import Optional

import chromadb
from fastapi import FastAPI, File, Form, UploadFile
from fastembed import ImageEmbedding, TextEmbedding
from pydantic import BaseModel

# ── Initialize FastAPI ────────────────────────
app = FastAPI(
    title="Embedding & ChromaDB Service",
    description="FastEmbed + Persistent ChromaDB for vector similarity search",
)

# ── Persistent ChromaDB Client ────────────────
# Data is saved locally in ./chroma_db folder
chroma_path = os.path.join(os.path.dirname(__file__), "chroma_db")
chroma_client = chromadb.PersistentClient(path=chroma_path)

# Create/get collections using cosine distance
text_collection = chroma_client.get_or_create_collection(
    name="discussions_text", metadata={"hnsw:space": "cosine"}
)
image_collection = chroma_client.get_or_create_collection(
    name="discussions_image", metadata={"hnsw:space": "cosine"}
)

# ── FastEmbed Models ──────────────────────────
print("[FastEmbed] Loading text model: BAAI/bge-small-en-v1.5 ...")
text_model = TextEmbedding("BAAI/bge-small-en-v1.5")

print("[FastEmbed] Loading image model: Qdrant/clip-ViT-B-32-vision ...")
image_model = ImageEmbedding("Qdrant/clip-ViT-B-32-vision")
print("[FastEmbed] Models and ChromaDB ready.")


# ── Health Check ──────────────────────────────
@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "text_count": text_collection.count(),
        "image_count": image_collection.count(),
    }


# ── Helper: Save Uploaded File to Temp Path ────
def save_temp_file(file: UploadFile) -> str:
    temp_dir = tempfile.gettempdir()
    temp_path = os.path.join(temp_dir, file.filename)
    with open(temp_path, "wb") as f:
        f.write(file.file.read())
    return temp_path


# ── Index Discussion ──────────────────────────
# Stores text and/or image embeddings in ChromaDB
@app.post("/discussions/index")
def index_discussion(
    discussion_id: str = Form(...),
    title: str = Form(...),
    description: str = Form(...),
    file: Optional[UploadFile] = File(None),
):
    # 1. Text embedding
    text_content = title + ". " + description
    text_emb = list(text_model.embed([text_content]))[0].tolist()

    text_collection.upsert(
        ids=[str(discussion_id)],
        embeddings=[text_emb],
        documents=[text_content],
        metadatas=[{"discussion_id": int(discussion_id)}],
    )

    # 2. Image embedding (if provided)
    if file:
        temp_path = save_temp_file(file)
        try:
            image_emb = list(image_model.embed([temp_path]))[0].tolist()
            image_collection.upsert(
                ids=[str(discussion_id)],
                embeddings=[image_emb],
                metadatas=[{"discussion_id": int(discussion_id)}],
            )
        finally:
            if os.path.exists(temp_path):
                os.remove(temp_path)

    return {"status": "indexed", "discussion_id": discussion_id}


# ── Find Similar Discussions ──────────────────
# Queries ChromaDB for duplicate check or search
@app.post("/discussions/find_similar")
def find_similar_discussions(
    title: Optional[str] = Form(None),
    description: Optional[str] = Form(None),
    file: Optional[UploadFile] = File(None),
    limit: int = Form(5),
):
    scores = {}

    # Query text collection
    if title or description:
        text_content = (title or "") + ". " + (description or "")
        text_emb = list(text_model.embed([text_content]))[0].tolist()

        if text_collection.count() > 0:
            res = text_collection.query(
                query_embeddings=[text_emb],
                n_results=min(limit, text_collection.count()),
            )

            ids = res.get("ids", [[]])[0]
            distances = res.get("distances", [[]])[0]

            for d_id, dist in zip(ids, distances):
                # Cosine similarity = 1.0 - distance
                similarity = round(max(0.0, 1.0 - float(dist)), 4)
                d_int = int(d_id)
                scores[d_int] = max(scores.get(d_int, 0.0), similarity)

    # Query image collection
    if file:
        temp_path = save_temp_file(file)
        try:
            image_emb = list(image_model.embed([temp_path]))[0].tolist()

            if image_collection.count() > 0:
                res = image_collection.query(
                    query_embeddings=[image_emb],
                    n_results=min(limit, image_collection.count()),
                )

                ids = res.get("ids", [[]])[0]
                distances = res.get("distances", [[]])[0]

                for d_id, dist in zip(ids, distances):
                    similarity = round(max(0.0, 1.0 - float(dist)), 4)
                    d_int = int(d_id)
                    scores[d_int] = max(scores.get(d_int, 0.0), similarity)
        finally:
            if os.path.exists(temp_path):
                os.remove(temp_path)

    # Sort matches by similarity descending
    matches = [
        {"discussionId": d_id, "similarity": sim}
        for d_id, sim in sorted(scores.items(), key=lambda item: item[1], reverse=True)
    ]

    return {"matches": matches[:limit]}


# ── Delete Discussion Vector ──────────────────
@app.delete("/discussions/{discussion_id}")
def delete_discussion(discussion_id: str):
    try:
        text_collection.delete(ids=[str(discussion_id)])
    except Exception:
        pass

    try:
        image_collection.delete(ids=[str(discussion_id)])
    except Exception:
        pass

    return {"status": "deleted", "discussion_id": discussion_id}
