-- ──────────────────────────────────────────────
-- PostgreSQL Init Script
--
-- Runs automatically when the DB container starts
-- for the first time. Enables the pgvector extension.
-- ──────────────────────────────────────────────

CREATE EXTENSION IF NOT EXISTS vector;
