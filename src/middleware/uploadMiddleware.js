// ──────────────────────────────────────────────
// Upload Middleware (Multer)
//
// Multer handles multipart/form-data, which is
// how browsers send file uploads.
//
// This config:
//   - Saves files to src/uploads/
//   - Renames files to avoid collisions (timestamp + random)
//   - Only allows image files (jpg, png, webp, gif)
//   - Limits file size to 5MB
//   - Allows up to 5 images per upload
// ──────────────────────────────────────────────

const multer = require("multer");
const path = require("path");

// Where to save + how to name the files
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, path.join(__dirname, "../uploads"));
  },
  filename: (req, file, cb) => {
    // e.g., 1719756000000-482910384.jpg
    const uniqueName = `${Date.now()}-${Math.round(Math.random() * 1e9)}`;
    const ext = path.extname(file.originalname);
    cb(null, `${uniqueName}${ext}`);
  },
});

// Only allow image files
function fileFilter(req, file, cb) {
  const allowedTypes = ["image/jpeg", "image/png", "image/webp", "image/gif"];

  if (allowedTypes.includes(file.mimetype)) {
    cb(null, true);
  } else {
    cb(new Error("Only image files (jpg, png, webp, gif) are allowed"), false);
  }
}

const upload = multer({
  storage,
  fileFilter,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB per file
});

// Middleware: accept up to 5 images under the field name "images"
const uploadImages = upload.array("images", 5);

module.exports = { uploadImages };
