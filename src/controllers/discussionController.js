const axios = require("axios");
const prisma = require("../config/db");
const { indexDiscussion, findSimilarDiscussions } = require("../services/embeddingService");
const { formatPreciseAddress } = require("./locationController");

const DUPLICATE_THRESHOLD = 0.85;

async function fetchAddressFromOpenCage(lat, lng) {
  try {
    let apiKey = process.env.OPENCAGE_API_KEY;
    if (!apiKey) {
      apiKey = "40c5f2b87f944bd0a563ee25eb7b3726";
    }
    const url = "https://api.opencagedata.com/geocode/v1/json?q=" + lat + "+" + lng + "&key=" + apiKey + "&no_annotations=1&limit=1";
    const response = await axios.get(url);
    if (response.data && response.data.results && response.data.results.length > 0) {
      return formatPreciseAddress(response.data.results[0]);
    }
  } catch (err) {
    console.error("Server-side OpenCage reverse geocode error:", err.message);
  }
  return null;
}

async function createDiscussion(req, res) {
  try {
    const title = req.body.title;
    const description = req.body.description;
    const latitude = req.body.latitude;
    const longitude = req.body.longitude;
    const buildingName = req.body.buildingName;
    const createAnyway = req.body.createAnyway;

    let images = [];
    if (req.files) {
      for (let i = 0; i < req.files.length; i = i + 1) {
        images.push("uploads/" + req.files[i].filename);
      }
    }

    let finalBuildingName = null;
    if (buildingName) {
      finalBuildingName = buildingName;
    }

    let parsedLat = null;
    if (latitude) {
      parsedLat = parseFloat(latitude);
    }

    let parsedLng = null;
    if (longitude) {
      parsedLng = parseFloat(longitude);
    }

    if (!finalBuildingName && parsedLat !== null && parsedLng !== null) {
      const address = await fetchAddressFromOpenCage(parsedLat, parsedLng);
      if (address) {
        finalBuildingName = address;
      }
    }

    let firstImage = null;
    if (images.length > 0) {
      firstImage = images[0];
    }

    if (String(createAnyway) !== "true") {
      const matches = await findSimilarDiscussions(title, description, firstImage, 1);
      if (matches.length > 0) {
        const topMatch = matches[0];
        if (topMatch.similarity > DUPLICATE_THRESHOLD) {
          const matchedDiscussion = await prisma.discussion.findUnique({
            where: { id: topMatch.discussionId },
            include: {
              createdBy: {
                select: {
                  anonymousUsername: true,
                  avatarColor: true
                }
              }
            }
          });

          if (matchedDiscussion) {
            return res.status(409).json({
              duplicate: true,
              matchedDiscussion: matchedDiscussion,
              similarity: topMatch.similarity,
              message: "We found an existing discussion that looks similar."
            });
          }
        }
      }
    }

    const discussion = await prisma.discussion.create({
      data: {
        title: title,
        description: description,
        images: images,
        latitude: parsedLat,
        longitude: parsedLng,
        buildingName: finalBuildingName,
        userId: req.user.id
      },
      include: {
        createdBy: {
          select: {
            id: true,
            anonymousUsername: true,
            avatarColor: true
          }
        }
      }
    });

    indexDiscussion(discussion.id, title, description, firstImage);

    return res.status(201).json({ discussion: discussion });
  } catch (error) {
    console.error("Create discussion error:", error.message);
    return res.status(500).json({ error: "Failed to create discussion" });
  }
}

async function getAllDiscussions(req, res) {
  try {
    const discussions = await prisma.discussion.findMany({
      include: {
        createdBy: {
          select: {
            id: true,
            anonymousUsername: true,
            avatarColor: true
          }
        },
        _count: {
          select: {
            comments: true
          }
        }
      },
      orderBy: {
        createdAt: "desc"
      }
    });

    return res.json({ discussions: discussions });
  } catch (error) {
    console.error("Get discussions error:", error.message);
    return res.status(500).json({ error: "Failed to fetch discussions" });
  }
}

async function getTrendingDiscussions(req, res) {
  try {
    const sevenDaysAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);
    const discussions = await prisma.discussion.findMany({
      include: {
        createdBy: {
          select: {
            id: true,
            anonymousUsername: true,
            avatarColor: true
          }
        },
        _count: {
          select: {
            comments: true
          }
        }
      },
      where: {
        createdAt: {
          gte: sevenDaysAgo
        }
      }
    });

    let scoredDiscussions = [];
    for (let i = 0; i < discussions.length; i = i + 1) {
      const item = discussions[i];
      const commentsCount = item._count.comments;
      const score = (item.upvoteCount * 2) + commentsCount;
      const itemWithScore = Object.assign({}, item, { score: score });
      scoredDiscussions.push(itemWithScore);
    }

    scoredDiscussions.sort(function (a, b) {
      return b.score - a.score;
    });

    const topTrending = scoredDiscussions.slice(0, 20);

    return res.json({ discussions: topTrending });
  } catch (error) {
    console.error("Get trending error:", error.message);
    return res.status(500).json({ error: "Failed to fetch trending discussions" });
  }
}

async function getDiscussionById(req, res) {
  try {
    const id = parseInt(req.params.id);
    const discussion = await prisma.discussion.findUnique({
      where: { id: id },
      include: {
        createdBy: {
          select: {
            id: true,
            anonymousUsername: true,
            avatarColor: true
          }
        },
        comments: {
          include: {
            author: {
              select: {
                id: true,
                anonymousUsername: true,
                avatarColor: true
              }
            },
            replies: {
              include: {
                author: {
                  select: {
                    id: true,
                    anonymousUsername: true,
                    avatarColor: true
                  }
                }
              },
              orderBy: {
                createdAt: "asc"
              }
            }
          },
          orderBy: {
            createdAt: "asc"
          }
        },
        upvotes: {
          where: {
            userId: req.user.id
          },
          select: {
            id: true
          }
        },
        _count: {
          select: {
            comments: true
          }
        }
      }
    });

    if (!discussion) {
      return res.status(404).json({ error: "Discussion not found" });
    }

    let hasUpvoted = false;
    if (discussion.upvotes.length > 0) {
      hasUpvoted = true;
    }

    const response = Object.assign({}, discussion, { hasUpvoted: hasUpvoted });
    delete response.upvotes;

    let relatedDiscussions = [];
    try {
      const matches = await findSimilarDiscussions(
        discussion.title,
        discussion.description,
        null,
        6
      );

      const RELATED_THRESHOLD = 0.78;
      let relatedIds = [];
      let similarityMap = {};

      for (let i = 0; i < matches.length; i = i + 1) {
        const match = matches[i];
        if (match.discussionId !== id) {
          if (match.similarity >= RELATED_THRESHOLD) {
            relatedIds.push(match.discussionId);
            similarityMap[match.discussionId] = match.similarity;
          }
        }
      }

      if (relatedIds.length > 0) {
        const relatedFromDb = await prisma.discussion.findMany({
          where: {
            id: {
              in: relatedIds
            }
          },
          include: {
            createdBy: {
              select: {
                id: true,
                anonymousUsername: true,
                avatarColor: true
              }
            },
            _count: {
              select: {
                comments: true
              }
            }
          }
        });

        for (let i = 0; i < relatedFromDb.length; i = i + 1) {
          const relItem = relatedFromDb[i];
          let simVal = 0;
          if (similarityMap[relItem.id] !== undefined) {
            simVal = similarityMap[relItem.id];
          }
          const relWithSim = Object.assign({}, relItem, { similarity: simVal });
          relatedDiscussions.push(relWithSim);
        }

        relatedDiscussions.sort(function (a, b) {
          return b.similarity - a.similarity;
        });
      }
    } catch (relErr) {
      console.error("Failed to fetch related discussions:", relErr.message);
    }

    return res.json({ discussion: response, relatedDiscussions: relatedDiscussions });
  } catch (error) {
    console.error("Get discussion error:", error.message);
    return res.status(500).json({ error: "Failed to fetch discussion" });
  }
}

async function toggleUpvote(req, res) {
  try {
    const discussionId = parseInt(req.params.id);
    const userId = req.user.id;

    const existing = await prisma.upvote.findUnique({
      where: {
        userId_discussionId: {
          userId: userId,
          discussionId: discussionId
        }
      }
    });

    if (existing) {
      await prisma.upvote.delete({
        where: { id: existing.id }
      });
      await prisma.discussion.update({
        where: { id: discussionId },
        data: {
          upvoteCount: {
            decrement: 1
          }
        }
      });
      return res.json({ message: "Upvote removed", upvoted: false });
    } else {
      await prisma.upvote.create({
        data: {
          userId: userId,
          discussionId: discussionId
        }
      });
      await prisma.discussion.update({
        where: { id: discussionId },
        data: {
          upvoteCount: {
            increment: 1
          }
        }
      });
      return res.json({ message: "Upvoted", upvoted: true });
    }
  } catch (error) {
    console.error("Toggle upvote error:", error.message);
    return res.status(500).json({ error: "Failed to toggle upvote" });
  }
}

module.exports = {
  createDiscussion,
  getAllDiscussions,
  getTrendingDiscussions,
  getDiscussionById,
  toggleUpvote
};
