const prisma = require("../config/db");

async function addComment(req, res) {
  try {
    const discussionId = parseInt(req.params.id);
    const text = req.body.text;
    const userId = req.user.id;

    let image = null;
    if (req.file) {
      image = "uploads/" + req.file.filename;
    }

    const discussion = await prisma.discussion.findUnique({
      where: { id: discussionId }
    });

    if (!discussion) {
      return res.status(404).json({ error: "Discussion not found" });
    }

    const comment = await prisma.comment.create({
      data: {
        text: text,
        image: image,
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

async function addReply(req, res) {
  try {
    const commentId = parseInt(req.params.id);
    const text = req.body.text;
    const userId = req.user.id;

    const comment = await prisma.comment.findUnique({
      where: { id: commentId }
    });

    if (!comment) {
      return res.status(404).json({ error: "Comment not found" });
    }

    const reply = await prisma.reply.create({
      data: {
        text: text,
        userId: userId,
        commentId: commentId
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

    return res.status(201).json({ reply: reply });
  } catch (error) {
    console.error("Add reply error:", error.message);
    return res.status(500).json({ error: "Failed to add reply" });
  }
}

module.exports = {
  addComment,
  addReply
};
