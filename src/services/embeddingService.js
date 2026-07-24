const axios = require("axios");
const fs = require("fs");
const path = require("path");
const FormData = require("form-data");

let EMBEDDING_URL = process.env.EMBEDDING_SERVICE_URL;
if (!EMBEDDING_URL) {
  EMBEDDING_URL = "http://localhost:5050";
}

function getAbsolutePath(imagePath) {
  if (!imagePath) {
    return null;
  }
  return path.join(__dirname, "../", imagePath);
}

async function indexDiscussion(discussionId, title, description, imagePath) {
  try {
    const form = new FormData();
    form.append("discussion_id", String(discussionId));
    form.append("title", title);
    form.append("description", description);

    if (imagePath) {
      const absolutePath = getAbsolutePath(imagePath);
      if (absolutePath !== null) {
        if (fs.existsSync(absolutePath)) {
          form.append("file", fs.createReadStream(absolutePath));
        }
      }
    }

    const response = await axios.post(EMBEDDING_URL + "/discussions/index", form, {
      headers: form.getHeaders()
    });

    return response.data;
  } catch (error) {
    console.error("Index discussion error:", error.message);
    return null;
  }
}

async function findSimilarDiscussions(title, description, imagePath, limit) {
  try {
    let matchLimit = 5;
    if (limit !== undefined && limit !== null) {
      matchLimit = limit;
    }

    const form = new FormData();
    if (title) {
      form.append("title", title);
    }
    if (description) {
      form.append("description", description);
    }
    form.append("limit", String(matchLimit));

    if (imagePath) {
      const absolutePath = getAbsolutePath(imagePath);
      if (absolutePath !== null) {
        if (fs.existsSync(absolutePath)) {
          form.append("file", fs.createReadStream(absolutePath));
        }
      }
    }

    const response = await axios.post(EMBEDDING_URL + "/discussions/find_similar", form, {
      headers: form.getHeaders()
    });

    if (response.data && response.data.matches) {
      return response.data.matches;
    }
    return [];
  } catch (error) {
    console.error("Find similar discussions error:", error.message);
    return [];
  }
}

async function deleteDiscussionVector(discussionId) {
  try {
    const response = await axios.delete(EMBEDDING_URL + "/discussions/" + discussionId);
    return response.data;
  } catch (error) {
    console.error("Delete discussion vector error:", error.message);
    return null;
  }
}

module.exports = {
  indexDiscussion,
  findSimilarDiscussions,
  deleteDiscussionVector
};
