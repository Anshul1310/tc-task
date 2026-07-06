const prisma = require("../config/db");

// Helper to create notifications
async function createNotification(receiverId, type, title, message, discussionId = null, commentId = null) {
  try {
    await prisma.notification.create({
      data: {
        receiverId,
        type,
        title,
        message,
        discussionId,
        commentId,
      }
    });
  } catch (error) {
    console.error("Failed to create notification:", error.message);
  }
}

async function addComment(req, res) {
  try {
    const discussionId = parseInt(req.params.id);
    const { text } = req.body;
    const image = req.file ? `uploads/${req.file.filename}` : null;
    const userId = req.user.id;

    const discussion = await prisma.discussion.findUnique({
      where: { id: discussionId },
      include: { createdBy: { select: { anonymousUsername: true } } }
    });

    if (!discussion) {
      return res.status(404).json({ error: "Discussion not found" });
    }

    const comment = await prisma.comment.create({
      data: {
        text,
        image,
        userId,
        discussionId
      },
      include: {
        author: { select: { id: true, anonymousUsername: true, avatarColor: true } }
      }
    });

    // Notify discussion author if it's not their own comment
    if (discussion.userId !== userId) {
      createNotification(
        discussion.userId,
        "COMMENT",
        "New Comment",
        `${comment.author.anonymousUsername} commented on your discussion.`,
        discussionId,
        comment.id
      );
    }

    res.status(201).json({ comment });
  } catch (error) {
    console.error("Add comment error:", error.message);
    res.status(500).json({ error: "Failed to add comment" });
  }
}

async function addReply(req, res) {
  try {
    const commentId = parseInt(req.params.id);
    const { text } = req.body;
    const userId = req.user.id;

    const comment = await prisma.comment.findUnique({
      where: { id: commentId },
      include: { author: { select: { anonymousUsername: true } } }
    });

    if (!comment) {
      return res.status(404).json({ error: "Comment not found" });
    }

    const reply = await prisma.reply.create({
      data: {
        text,
        userId,
        commentId
      },
      include: {
        author: { select: { id: true, anonymousUsername: true, avatarColor: true } }
      }
    });

    // Notify comment author if it's not their own reply
    if (comment.userId !== userId) {
      createNotification(
        comment.userId,
        "REPLY",
        "New Reply",
        `${reply.author.anonymousUsername} replied to your comment.`,
        comment.discussionId,
        comment.id
      );
    }

    res.status(201).json({ reply });
  } catch (error) {
    console.error("Add reply error:", error.message);
    res.status(500).json({ error: "Failed to add reply" });
  }
}

module.exports = {
  addComment,
  addReply
};
