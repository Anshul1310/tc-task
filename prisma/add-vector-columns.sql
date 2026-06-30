-- ──────────────────────────────────────────────
-- Add Vector Columns to Item Table
--
-- Run this AFTER `npx prisma migrate dev`
--
-- WHY MANUAL?
-- Prisma doesn't have a native Vector type, so
-- these columns can't be defined in schema.prisma.
-- We add them with raw SQL instead.
--
-- DIMENSIONS:
-- textEmbedding:  384 dims (from BAAI/bge-small-en-v1.5)
-- imageEmbedding: 512 dims (from openai/clip-vit-base-patch32)
--
-- INDEXES:
-- IVFFlat indexes speed up similarity searches.
-- "lists = 100" means vectors are clustered into
-- 100 groups. PostgreSQL searches the nearest
-- clusters instead of scanning every row.
-- ──────────────────────────────────────────────

-- Add vector columns
ALTER TABLE "Item" ADD COLUMN IF NOT EXISTS "textEmbedding" vector(384);
ALTER TABLE "Item" ADD COLUMN IF NOT EXISTS "imageEmbedding" vector(512);

-- Create IVFFlat indexes for fast cosine similarity search
-- NOTE: IVFFlat requires at least some data to build.
-- If the table is empty, use HNSW instead or add data first.
CREATE INDEX IF NOT EXISTS idx_item_text_embedding
  ON "Item" USING ivfflat ("textEmbedding" vector_cosine_ops)
  WITH (lists = 100);

CREATE INDEX IF NOT EXISTS idx_item_image_embedding
  ON "Item" USING ivfflat ("imageEmbedding" vector_cosine_ops)
  WITH (lists = 100);
