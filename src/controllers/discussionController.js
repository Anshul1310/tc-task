const axios = require("axios");
const prisma = require("../config/db");
const { indexDiscussion, findSimilarDiscussions } = require("../services/embeddingService");

const DUPLICATE_THRESHOLD = 0.85;

const { formatPreciseAddress } = require("./locationController");

// Helper: OpenCage reverse geocoding on the server
async function fetchAddressFromOpenCage(lat, lng) {
  try {
    const apiKey = process.env.OPENCAGE_API_KEY || "40c5f2b87f944bd0a563ee25eb7b3726";
    const url = `https://api.opencagedata.com/geocode/v1/json?q=${lat}+${lng}&key=${apiKey}&no_annotations=1&limit=1`;
    const response = await axios.get(url);
    const results = response.data?.results;
    if (results && results.length > 0) {
      return formatPreciseAddress(results[0]);
    }
  } catch (err) {
    console.error("Server-side OpenCage reverse geocode error:", err.message);
  }
  return null;
}

async function createDiscussion(req, res) {
  try {
    const { title, description, latitude, longitude, buildingName, createAnyway } = req.body;
    
    const images = req.files ? req.files.map((file) => `uploads/${file.filename}`) : [];

    let finalBuildingName = buildingName || null;
    const parsedLat = latitude ? parseFloat(latitude) : null;
    const parsedLng = longitude ? parseFloat(longitude) : null;

    if (!finalBuildingName && parsedLat !== null && parsedLng !== null) {
      const address = await fetchAddressFromOpenCage(parsedLat, parsedLng);
      if (address) {
        finalBuildingName = address;
      }
    }

    // Duplicate Detection via ChromaDB
    if (String(createAnyway) !== "true") {
      const matches = await findSimilarDiscussions(title, description, images[0] || null, 1);
      if (matches.length > 0 && matches[0].similarity > DUPLICATE_THRESHOLD) {
        // Find the matched discussion details
        const matchedDiscussion = await prisma.discussion.findUnique({
          where: { id: matches[0].discussionId },
          include: { createdBy: { select: { anonymousUsername: true, avatarColor: true } } }
        });

        if (matchedDiscussion) {
          return res.status(409).json({
            duplicate: true,
            matchedDiscussion,
            similarity: matches[0].similarity,
            message: "We found an existing discussion that looks similar."
          });
        }
      }
    }

    // Create the discussion in database
    const discussion = await prisma.discussion.create({
      data: {
        title,
        description,
        images,
        latitude: parsedLat,
        longitude: parsedLng,
        buildingName: finalBuildingName,
        userId: req.user.id,
      },
      include: {
        createdBy: { select: { id: true, anonymousUsername: true, avatarColor: true } }
      }
    });

    // Index discussion in ChromaDB (persistent vector storage in Python sidecar)
    indexDiscussion(discussion.id, title, description, images[0] || null);

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

    // Fetch related discussions via ChromaDB vector similarity
    let relatedDiscussions = [];
    try {
      const matches = await findSimilarDiscussions(
        discussion.title,
        discussion.description,
        discussion.images.length > 0 ? discussion.images[0] : null,
        6
      );

      const relatedIds = matches
        .filter((m) => m.discussionId !== id)
        .map((m) => m.discussionId);

      if (relatedIds.length > 0) {
        const relatedFromDb = await prisma.discussion.findMany({
          where: { id: { in: relatedIds } },
          include: {
            createdBy: { select: { id: true, anonymousUsername: true, avatarColor: true } },
            _count: { select: { comments: true } }
          }
        });

        const similarityMap = new Map(matches.map((m) => [m.discussionId, m.similarity]));

        relatedDiscussions = relatedFromDb
          .map((d) => ({
            ...d,
            similarity: similarityMap.get(d.id) || 0
          }))
          .sort((a, b) => b.similarity - a.similarity);
      }
    } catch (relErr) {
      console.error("Failed to fetch related discussions:", relErr.message);
    }

    res.json({ discussion: response, relatedDiscussions });
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
