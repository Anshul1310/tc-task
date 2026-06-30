// ──────────────────────────────────────────────
// Prisma Client Singleton
//
// Why a singleton? If we create a new PrismaClient()
// in every file, we'd open too many database
// connections. This file creates ONE client and
// every other file imports it.
// ──────────────────────────────────────────────

const { PrismaClient } = require("@prisma/client");

const prisma = new PrismaClient();

module.exports = prisma;
