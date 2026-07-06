// ──────────────────────────────────────────────
// Vector Helpers (pgvector raw SQL)
//
// WHY RAW SQL?
// Prisma doesn't support pgvector's vector type
// or its similarity operators natively. So we
// write raw SQL queries here.
//
// HOW PGVECTOR SIMILARITY WORKS:
//
// pgvector adds a special "vector" column type to
// PostgreSQL. It provides operators for distance:
//
//   <=>  Cosine distance    (1 - cosine_similarity)
//   <->  L2 (Euclidean) distance
//   <#>  Inner product distance
//
// We use cosine distance (<=>). Lower = more similar.
// To get cosine SIMILARITY (0-1 scale), we compute:
//   similarity = 1 - cosine_distance
//
// The IVFFlat index speeds up searches by
// clustering vectors into "lists" and only
// searching nearby clusters instead of all rows.
// ──────────────────────────────────────────────

const prisma = require("../config/db");

// Find items similar to a given item by comparing embeddings.
// type: "text" or "image" — which embedding column to compare.
// searchStatus: "LOST" or "FOUND" — which items to search against.
async function findSimilarItems(itemId, searchStatus, type = "text", limit = 10) {
  const column =
    type === "image" ? '"imageEmbedding"' : '"textEmbedding"';

  // This query:
  // 1. Gets the embedding of the given item
  // 2. Finds all items with the opposite status
  // 3. Ranks them by cosine similarity (highest first)
  // 4. Returns the top N matches
  const matches = await prisma.$queryRawUnsafe(`
    SELECT
      id AS "itemId",
      title,
      1 - (${column} <=> (
        SELECT ${column} FROM "Item" WHERE id = $1
      )) AS similarity
    FROM "Item"
    WHERE id != $1
      AND status = $2::text::"Status"
      AND ${column} IS NOT NULL
    ORDER BY ${column} <=> (
      SELECT ${column} FROM "Item" WHERE id = $1
    ) ASC
    LIMIT $3
  `, itemId, searchStatus, limit);

  // Convert BigInt ids and Decimal similarity to plain numbers
  return matches.map((m) => ({
    itemId: Number(m.itemId),
    title: m.title,
    similarity: parseFloat(Number(m.similarity).toFixed(4)),
  }));
}

// Search by a raw embedding vector (for the /search endpoints).
// Used when a user provides a text query or uploads an image.
async function searchByVector(embedding, type = "text", limit = 10) {
  const column =
    type === "image" ? '"imageEmbedding"' : '"textEmbedding"';
  const vector = `[${embedding.join(",")}]`;

  const matches = await prisma.$queryRawUnsafe(`
    SELECT
      id AS "itemId",
      title,
      status,
      1 - (${column} <=> $1::vector) AS similarity
    FROM "Item"
    WHERE ${column} IS NOT NULL
    ORDER BY ${column} <=> $1::vector ASC
    LIMIT $2
  `, vector, limit);

  return matches.map((m) => ({
    itemId: Number(m.itemId),
    title: m.title,
    status: m.status,
    similarity: parseFloat(Number(m.similarity).toFixed(4)),
  }));
}

// ── Discussions Similarity ──────────────────
async function findSimilarDiscussions(textEmbedding, imageEmbedding, limit = 5) {
  let query = "";
  const params = [];

  // Determine which query to run based on available embeddings
  if (textEmbedding && imageEmbedding) {
    const textVector = `[${textEmbedding.join(",")}]`;
    const imageVector = `[${imageEmbedding.join(",")}]`;
    query = `
      SELECT
        id AS "discussionId",
        title,
        description,
        0.7 * (1 - ("textEmbedding" <=> $1::vector)) + 0.3 * (1 - ("imageEmbedding" <=> $2::vector)) AS similarity
      FROM "Discussion"
      WHERE "textEmbedding" IS NOT NULL AND "imageEmbedding" IS NOT NULL
      ORDER BY 0.7 * (1 - ("textEmbedding" <=> $1::vector)) + 0.3 * (1 - ("imageEmbedding" <=> $2::vector)) DESC
      LIMIT $3
    `;
    params.push(textVector, imageVector, limit);
  } else if (textEmbedding) {
    const textVector = `[${textEmbedding.join(",")}]`;
    query = `
      SELECT
        id AS "discussionId",
        title,
        description,
        1 - ("textEmbedding" <=> $1::vector) AS similarity
      FROM "Discussion"
      WHERE "textEmbedding" IS NOT NULL
      ORDER BY "textEmbedding" <=> $1::vector ASC
      LIMIT $2
    `;
    params.push(textVector, limit);
  } else if (imageEmbedding) {
    const imageVector = `[${imageEmbedding.join(",")}]`;
    query = `
      SELECT
        id AS "discussionId",
        title,
        description,
        1 - ("imageEmbedding" <=> $1::vector) AS similarity
      FROM "Discussion"
      WHERE "imageEmbedding" IS NOT NULL
      ORDER BY "imageEmbedding" <=> $1::vector ASC
      LIMIT $2
    `;
    params.push(imageVector, limit);
  } else {
    return [];
  }

  const matches = await prisma.$queryRawUnsafe(query, ...params);

  return matches.map((m) => ({
    discussionId: Number(m.discussionId),
    title: m.title,
    description: m.description,
    similarity: parseFloat(Number(m.similarity).toFixed(4)),
  }));
}

module.exports = { findSimilarItems, searchByVector, findSimilarDiscussions };
