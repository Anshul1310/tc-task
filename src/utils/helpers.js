
function formatError(error) {
  if (error.code === "P2002") {
    return "A record with this value already exists";
  }
  if (error.code === "P2025") {
    return "Record not found";
  }
  if (error.code === "LIMIT_FILE_SIZE") {
    return "File too large. Maximum size is 5MB";
  }
  if (error.code === "LIMIT_UNEXPECTED_FILE") {
    return "Too many files or wrong field name";
  }
  return error.message || "An unexpected error occurred";
}

module.exports = { formatError };
