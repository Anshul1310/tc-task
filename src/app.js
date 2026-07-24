const express = require("express");
const cors = require("cors");
const path = require("path");


require("dotenv").config({ path: path.join(__dirname, "../.env") });

const app = express();


app.use(express.json());

app.use(express.urlencoded({ extended: true }));

app.use(cors());

app.use("/uploads", express.static(path.join(__dirname, "uploads")));


app.get("/health", (req, res) => {
  res.json({ status: "ok", timestamp: new Date().toISOString() });
});


const authRoutes = require("./routes/authRoutes");
const discussionRoutes = require("./routes/discussionRoutes");
const commentRoutes = require("./routes/commentRoutes");
const communitySearchRoutes = require("./routes/communitySearchRoutes");
const locationRoutes = require("./routes/locationRoutes");

app.use("/auth", authRoutes);
app.use("/discussions", discussionRoutes);
app.use("/", commentRoutes);
app.use("/community", communitySearchRoutes);
app.use("/location", locationRoutes);




module.exports = app;
