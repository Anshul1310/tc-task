
const axios = require("axios");

function formatPreciseAddress(result) {
  if (!result) return null;

  const components = result.components || {};


  const specificSpot =
    components.building ||
    components.hostel ||
    components.amenity ||
    components.residential ||
    components.hall ||
    components.suburb ||
    components.neighbourhood ||
    components.road ||
    components.pedestrian;

  const campusOrCity =
    components.college ||
    components.university ||
    components.city_district ||
    components.suburb ||
    components.city;

  if (specificSpot) {
    if (campusOrCity && !specificSpot.toLowerCase().includes(campusOrCity.toLowerCase())) {
      return `${specificSpot}, ${campusOrCity}`;
    }
    return specificSpot;
  }

  return result.formatted;
}

async function reverseGeocode(req, res) {
  try {
    const { lat, lng } = req.query;
    if (!lat || !lng) {
      return res.status(400).json({ error: "lat and lng parameters are required" });
    }

    const apiKey = process.env.OPENCAGE_API_KEY || "40c5f2b87f944bd0a563ee25eb7b3726";
    // no_annotations=1 for clean response, limit=1 for top match
    const url = `https://api.opencagedata.com/geocode/v1/json?q=${lat}+${lng}&key=${apiKey}&no_annotations=1&limit=1`;

    const response = await axios.get(url);
    const results = response.data?.results;

    if (results && results.length > 0) {
      const preciseAddress = formatPreciseAddress(results[0]);
      return res.json({ address: preciseAddress });
    } else {
      return res.json({ address: `Lat: ${lat}, Lng: ${lng}` });
    }
  } catch (error) {
    console.error("OpenCage reverse geocode error:", error.message);
    res.status(500).json({ error: "Failed to reverse geocode location" });
  }
}

module.exports = { reverseGeocode, formatPreciseAddress };
