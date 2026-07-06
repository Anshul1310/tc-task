const prisma = require("../config/db");
const { getTextEmbedding, getImageEmbedding } = require("../services/embeddingService");
const { findSimilarDiscussions } = require("../prisma/vectors");

const DUPLICATE_THRESHOLD = 0.90;

// Helper to asynchronously update vector embeddings using raw SQL
async function storeDiscussionEmbeddings(discussionId, textEmbedding, imageEmbedding) {
  try {
    if (textEmbedding && imageEmbedding) {
      const textVector = `[${textEmbedding.join(",")}]`;
      const imageVector = `[${imageEmbedding.join(",")}]`;
      await prisma.$executeRawUnsafe(
        `UPDATE "Discussion" SET "textEmbedding" = $1::vector, "imageEmbedding" = $2::vector WHERE id = $3`,
        textVector,
        imageVector,
        discussionId
      );
    } else if (textEmbedding) {
      const textVector = `[${textEmbedding.join(",")}]`;
      await prisma.$executeRawUnsafe(
        `UPDATE "Discussion" SET "textEmbedding" = $1::vector WHERE id = $2`,
        textVector,
        discussionId
      );
    } else if (imageEmbedding) {
      const imageVector = `[${imageEmbedding.join(",")}]`;
      await prisma.$executeRawUnsafe(
        `UPDATE "Discussion" SET "imageEmbedding" = $1::vector WHERE id = $2`,
        imageVector,
        discussionId
      );
    }
  } catch (error) {
    console.error("Failed to store discussion embeddings:", error.message);
  }
}

async function createDiscussion(req, res) {
  try {
    const { title, description, latitude, longitude, buildingName, createAnyway } = req.body;
    
    const images = req.files ? req.files.map((file) => `uploads/${file.filename}`) : [];
    const textInput = `${title}. ${description}`;
    
    // Generate embeddings
    let textEmbedding = null;
    let imageEmbedding = null;
    
    try {
      textEmbedding = await getTextEmbedding(textInput);
    } catch (err) {
      console.error("Text embedding failed:", err.message);
    }

    if (images.length > 0) {
      try {
        imageEmbedding = await getImageEmbedding(images[0]);
      } catch (err) {
        console.error("Image embedding failed:", err.message);
      }
    }

    // Duplicate Detection
    if (String(createAnyway) !== "true") {
      const matches = await findSimilarDiscussions(textEmbedding, imageEmbedding, 1);
      if (matches.length > 0 && matches[0].similarity > DUPLICATE_THRESHOLD) {
        // Find the matched discussion details
        const matchedDiscussion = await prisma.discussion.findUnique({
          where: { id: matches[0].discussionId },
          include: { createdBy: { select: { anonymousUsername: true, avatarColor: true } } }
        });

        return res.status(409).json({
          duplicate: true,
          matchedDiscussion,
          similarity: matches[0].similarity,
          message: "We found an existing discussion that looks similar."
        });
      }
    }

    // Create the discussion
    const discussion = await prisma.discussion.create({
      data: {
        title,
        description,
        images,
        latitude: latitude ? parseFloat(latitude) : null,
        longitude: longitude ? parseFloat(longitude) : null,
        buildingName,
        userId: req.user.id,
      },
      include: {
        createdBy: { select: { id: true, anonymousUsername: true, avatarColor: true } }
      }
    });

    // Store embeddings asynchronously
    storeDiscussionEmbeddings(discussion.id, textEmbedding, imageEmbedding);

    res.status(201).json({ discussion });
  } catch (error) {
    console.error("Create discussion error:", error.message);
    res.status(500).json({ error: "Failed to create discussion" });
  }
}

async function getAllDiscussions(req, res) {
  try {
    const discussions = await prisma.discussion.findMany({
      include: {
        createdBy: { select: { id: true, anonymousUsername: true, avatarColor: true } },
        _count: { select: { comments: true } }
      },
      orderBy: { createdAt: "desc" },
    });
    res.json({ discussions });
  } catch (error) {
    console.error("Get discussions error:", error.message);
    res.status(500).json({ error: "Failed to fetch discussions" });
  }
}

async function getTrendingDiscussions(req, res) {
  try {
    // Basic trending: fetch all, sort in memory by (upvotes*2 + comments)
    const discussions = await prisma.discussion.findMany({
      include: {
        createdBy: { select: { id: true, anonymousUsername: true, avatarColor: true } },
        _count: { select: { comments: true } }
      },
      // Limit to recent discussions for trending
      where: {
        createdAt: { gte: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000) } // Last 7 days
      }
    });

    const trending = discussions
      .map(d => ({
        ...d,
        score: (d.upvoteCount * 2) + d._count.comments
      }))
      .sort((a, b) => b.score - a.score)
      .slice(0, 20); // Top 20

    res.json({ discussions: trending });
  } catch (error) {
    console.error("Get trending error:", error.message);
    res.status(500).json({ error: "Failed to fetch trending discussions" });
  }
}

async function getDiscussionById(req, res) {
  try {
    const id = parseInt(req.params.id);
    const discussion = await prisma.discussion.findUnique({
      where: { id },
      include: {
        createdBy: { select: { id: true, anonymousUsername: true, avatarColor: true } },
        comments: {
          include: {
            author: { select: { id: true, anonymousUsername: true, avatarColor: true } },
            replies: {
              include: { author: { select: { id: true, anonymousUsername: true, avatarColor: true } } },
              orderBy: { createdAt: "asc" }
            }
          },
          orderBy: { createdAt: "asc" }
        },
        upvotes: {
          where: { userId: req.user.id },
          select: { id: true }
        },
        _count: { select: { comments: true } }
      }
    });

    if (!discussion) {
      return res.status(404).json({ error: "Discussion not found" });
    }

    // Attach hasUpvoted flag for the current user
    const response = {
      ...discussion,
      hasUpvoted: discussion.upvotes.length > 0
    };
    delete response.upvotes;

    res.json({ discussion: response });
  } catch (error) {
    console.error("Get discussion error:", error.message);
    res.status(500).json({ error: "Failed to fetch discussion" });
  }
}

async function toggleUpvote(req, res) {
  try {
    const discussionId = parseInt(req.params.id);
    const userId = req.user.id;

    const existing = await prisma.upvote.findUnique({
      where: { userId_discussionId: { userId, discussionId } }
    });

    if (existing) {
      // Remove upvote
      await prisma.upvote.delete({ where: { id: existing.id } });
      await prisma.discussion.update({
        where: { id: discussionId },
        data: { upvoteCount: { decrement: 1 } }
      });
      res.json({ message: "Upvote removed", upvoted: false });
    } else {
      // Add upvote
      await prisma.upvote.create({
        data: { userId, discussionId }
      });
      await prisma.discussion.update({
        where: { id: discussionId },
        data: { upvoteCount: { increment: 1 } }
      });
      res.json({ message: "Upvoted", upvoted: true });
    }
  } catch (error) {
    console.error("Toggle upvote error:", error.message);
    res.status(500).json({ error: "Failed to toggle upvote" });
  }
}

module.exports = {
  createDiscussion,
  getAllDiscussions,
  getTrendingDiscussions,
  getDiscussionById,
  toggleUpvote
};
