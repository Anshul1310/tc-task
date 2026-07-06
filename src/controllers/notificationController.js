const prisma = require("../config/db");

async function getUserNotifications(req, res) {
  try {
    const notifications = await prisma.notification.findMany({
      where: { receiverId: req.user.id },
      orderBy: { createdAt: "desc" },
    });
    res.json({ notifications });
  } catch (error) {
    console.error("Get notifications error:", error.message);
    res.status(500).json({ error: "Failed to fetch notifications" });
  }
}

async function markAsRead(req, res) {
  try {
    const notificationId = parseInt(req.params.id);

    // Verify ownership
    const notification = await prisma.notification.findUnique({
      where: { id: notificationId }
    });

    if (!notification || notification.receiverId !== req.user.id) {
      return res.status(404).json({ error: "Notification not found" });
    }

    const updated = await prisma.notification.update({
      where: { id: notificationId },
      data: { isRead: true }
    });

    res.json({ notification: updated });
  } catch (error) {
    console.error("Mark notification as read error:", error.message);
    res.status(500).json({ error: "Failed to update notification" });
  }
}

module.exports = {
  getUserNotifications,
  markAsRead
};
