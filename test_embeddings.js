const { pipeline, env } = require("@xenova/transformers");

async function test() {
  console.log("Loading text model...");
  const textExtractor = await pipeline("feature-extraction", "Xenova/all-MiniLM-L6-v2");
  const textOutput = await textExtractor("Hello world", { pooling: "mean", normalize: true });
  console.log("Text shape:", textOutput.dims);
  console.log("Text sample:", Array.from(textOutput.data).slice(0, 5));

  console.log("Loading image model...");
  const imageExtractor = await pipeline("image-feature-extraction", "Xenova/clip-vit-base-patch32");
  const imgUrl = "https://picsum.photos/200/300";
  const imageOutput = await imageExtractor(imgUrl);
  console.log("Image shape:", imageOutput.dims);
  console.log("Image sample:", Array.from(imageOutput.data).slice(0, 5));
}

test().catch(console.error);
