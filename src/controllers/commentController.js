const prisma = require("../config/db");

async function addComment(req, res) {
  try {
    const discussionId = parseInt(req.params.id);
    const text = req.body.text;
    const userId = req.user.id;

    if (!text || typeof text !== "string" || text.trim() === "") {
      return res.status(400).json({ error: "Comment text is required" });
    }

    const discussion = await prisma.discussion.findUnique({
      where: { id: discussionId }
    });

    if (!discussion) {
      return res.status(404).json({ error: "Discussion not found" });
    }

    const comment = await prisma.comment.create({
      data: {
        text: text.trim(),
        image: null,
        userId: userId,
        discussionId: discussionId
      },
      include: {
        author: {
          select: {
            id: true,
            anonymousUsername: true,
            avatarColor: true
          }
        }
      }
    });

    return res.status(201).json({ comment: comment });
  } catch (error) {
    console.error("Add comment error:", error.message);
    return res.status(500).json({ error: "Failed to add comment" });
  }
}

module.exports = {
  addComment
};
