-- ──────────────────────────────────────────────
-- Add Vector Columns to Discussion Table
--
-- Run this AFTER `npx prisma db push`
-- ──────────────────────────────────────────────

-- Add vector columns
ALTER TABLE "Discussion" ADD COLUMN IF NOT EXISTS "textEmbedding" vector(384);
ALTER TABLE "Discussion" ADD COLUMN IF NOT EXISTS "imageEmbedding" vector(512);

-- Create IVFFlat indexes for fast cosine similarity search
CREATE INDEX IF NOT EXISTS idx_discussion_text_embedding
  ON "Discussion" USING ivfflat ("textEmbedding" vector_cosine_ops)
  WITH (lists = 100);

CREATE INDEX IF NOT EXISTS idx_discussion_image_embedding
  ON "Discussion" USING ivfflat ("imageEmbedding" vector_cosine_ops)
  WITH (lists = 100);
