const axios = require("axios");
const prisma = require("../config/db");
const { findSimilarDiscussions } = require("../services/embeddingService");

async function textSearch(req, res) {
  try {
    const query = req.body.query;

    if (!query) {
      return res.status(400).json({ error: "Query text is required" });
    }

    const matches = await findSimilarDiscussions(query, null, null, 10);
    return res.json({ matches: matches });
  } catch (error) {
    console.error("Text search error:", error.message);
    return res.status(500).json({ error: "Text search failed" });
  }
}

async function imageSearch(req, res) {
  try {
    if (!req.file) {
      return res.status(400).json({ error: "Image file is required" });
    }

    const imagePath = "uploads/" + req.file.filename;
    const matches = await findSimilarDiscussions(null, null, imagePath, 10);

    return res.json({ matches: matches });
  } catch (error) {
    console.error("Image search error:", error.message);
    return res.status(500).json({ error: "Image search failed" });
  }
}

async function ragSearch(req, res) {
  try {
    let query = req.body.query;
    if (!query) {
      query = req.query.query;
    }

    if (!query || typeof query !== "string" || query.trim() === "") {
      return res.status(400).json({ error: "Search query is required" });
    }

    const cleanQuery = query.trim();
    const SIMILARITY_THRESHOLD = 0.55;

    const vectorMatches = await findSimilarDiscussions(cleanQuery, null, null, 10);

    let topMatches = [];
    for (let i = 0; i < vectorMatches.length; i = i + 1) {
      const matchItem = vectorMatches[i];
      if (matchItem.similarity >= SIMILARITY_THRESHOLD) {
        topMatches.push(matchItem);
      }
    }

    if (topMatches.length === 0 && vectorMatches.length > 0) {
      topMatches = vectorMatches.slice(0, 3);
    }

    let discussionIds = [];
    let similarityMap = {};
    for (let i = 0; i < topMatches.length; i = i + 1) {
      const item = topMatches[i];
      discussionIds.push(item.discussionId);
      similarityMap[item.discussionId] = item.similarity;
    }

    let discussions = [];
    if (discussionIds.length > 0) {
      const dbDiscussions = await prisma.discussion.findMany({
        where: {
          id: {
            in: discussionIds
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
                }
              }
            }
          },
          _count: {
            select: {
              comments: true
            }
          }
        }
      });

      for (let i = 0; i < dbDiscussions.length; i = i + 1) {
        const d = dbDiscussions[i];
        let simVal = 0;
        if (similarityMap[d.id] !== undefined) {
          simVal = similarityMap[d.id];
        }
        const dWithSim = Object.assign({}, d, { similarity: simVal });
        discussions.push(dWithSim);
      }

      discussions.sort(function (a, b) {
        return b.similarity - a.similarity;
      });
    }

    let contextText = "";
    if (discussions.length > 0) {
      for (let i = 0; i < discussions.length; i = i + 1) {
        const d = discussions[i];
        const indexNumber = i + 1;
        contextText = contextText + "\n[Campus Discussion #" + indexNumber + "]\n";
        contextText = contextText + "Title: " + d.title + "\n";
        if (d.buildingName) {
          contextText = contextText + "Location/Department: " + d.buildingName + "\n";
        }
        contextText = contextText + "Post Description: " + d.description + "\n";
        if (d.comments && d.comments.length > 0) {
          contextText = contextText + "Student Comments & Discussion Updates:\n";
          for (let j = 0; j < d.comments.length; j = j + 1) {
            const c = d.comments[j];
            let authorName = "Student";
            if (c.author && c.author.anonymousUsername) {
              authorName = c.author.anonymousUsername;
            }
            contextText = contextText + "  - Comment by " + authorName + ": \"" + c.text + "\"\n";
            if (c.replies) {
              for (let k = 0; k < c.replies.length; k = k + 1) {
                const r = c.replies[k];
                let replyAuthorName = "Student";
                if (r.author && r.author.anonymousUsername) {
                  replyAuthorName = r.author.anonymousUsername;
                }
                contextText = contextText + "    * Reply by " + replyAuthorName + ": \"" + r.text + "\"\n";
              }
            }
          }
        } else {
          contextText = contextText + "Comments: No comments posted yet.\n";
        }
      }
    }

    const groqApiKey = process.env.GROQ_API_KEY;
    let aiAnswer = "";

    if (groqApiKey) {
      const systemPrompt = "You are a concise, insightful, and encouraging Senior Academic & Campus Mentor at NIT Trichy. Your job is to analyze campus community posts, descriptions, and comments, and deliver a crisp summary for the student.";

      let retrievedDataText = "No specific matching posts retrieved.";
      if (contextText.length > 0) {
        retrievedDataText = contextText;
      }

      const userPrompt = "Student Query: \"" + cleanQuery + "\"\n\nRETRIEVED CAMPUS COMMUNITY DATA:\n" + retrievedDataText + "\n\nINSTRUCTIONS:\n1. Keep response crisp (80 to 120 words).\n2. Give highest priority to retrieved posts and comments.\n3. Conclude with a direct recommendation.";

      const groqModelsToTry = [
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "mixtral-8x7b-32768"
      ];

      for (let i = 0; i < groqModelsToTry.length; i = i + 1) {
        const modelName = groqModelsToTry[i];
        try {
          const groqRes = await axios.post(
            "https://api.groq.com/openai/v1/chat/completions",
            {
              model: modelName,
              messages: [
                { role: "system", content: systemPrompt },
                { role: "user", content: userPrompt }
              ],
              temperature: 0.6,
              max_tokens: 500
            },
            {
              headers: {
                "Authorization": "Bearer " + groqApiKey,
                "Content-Type": "application/json"
              }
            }
          );

          if (groqRes.data && groqRes.data.choices && groqRes.data.choices.length > 0) {
            const choice = groqRes.data.choices[0];
            if (choice.message && choice.message.content) {
              const responseText = choice.message.content.trim();
              if (responseText.length > 30) {
                aiAnswer = responseText;
                break;
              }
            }
          }
        } catch (err) {
          let errMsg = err.message;
          if (err.response && err.response.data && err.response.data.error) {
            errMsg = err.response.data.error.message;
          }
          console.error("Groq model " + modelName + " call failed:", errMsg);
        }
      }
    }

    if (!aiAnswer && discussions.length > 0) {
      let topSummaries = [];
      for (let i = 0; i < discussions.length; i = i + 1) {
        const d = discussions[i];
        let bName = "Campus";
        if (d.buildingName) {
          bName = d.buildingName;
        }
        let summary = "• \"" + d.title + "\" (" + bName + "): " + d.description;
        if (d.comments && d.comments.length > 0) {
          summary = summary + " [Comment: \"" + d.comments[0].text + "\"]";
        }
        topSummaries.push(summary);
      }
      aiAnswer = "Hello! Based on student posts and comments regarding \"" + cleanQuery + "\":\n\n" + topSummaries.join("\n") + "\n\nBased on community feedback, you can proceed with confidence!";
    }

    return res.json({
      answer: aiAnswer,
      matches: discussions
    });
  } catch (error) {
    console.error("RAG search error:", error.message);
    return res.status(500).json({ error: "RAG search failed" });
  }
}

module.exports = {
  textSearch,
  imageSearch,
  ragSearch
};
