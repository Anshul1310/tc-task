package com.anshul.campuscare.ui.components

// ──────────────────────────────────────────────
// Item Card
//
// A card that shows a preview of a lost or found item.
// Used in the home screen list. Shows:
//   - Thumbnail image (or placeholder)
//   - Title
//   - Category and location
//   - Status chip (LOST / FOUND / CLAIMED)
//   - Time since posted
// ──────────────────────────────────────────────

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.anshul.campuscare.data.model.Item
import com.anshul.campuscare.data.network.ApiClient
import com.anshul.campuscare.ui.theme.TextSecondary

@Composable
fun ItemCard(
    item: Item,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClick(item.id) },
        shape = RoundedCornerShape(size = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Thumbnail Image ───────────────
            if (item.images.isNotEmpty()) {
                val imageUrl: String = ApiClient.BASE_URL + item.images[0]
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Image of ${item.title}",
                    modifier = Modifier
                        .size(size = 80.dp)
                        .clip(shape = RoundedCornerShape(size = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder when there's no image
                Card(
                    modifier = Modifier
                        .size(size = 80.dp)
                        .clip(shape = RoundedCornerShape(size = 12.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .size(size = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📦",
                            fontSize = 28.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(width = 12.dp))

            // ── Item Details ──────────────────
            Column(
                modifier = Modifier.weight(weight = 1f)
            ) {
                // Title
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(height = 4.dp))

                // Category
                Text(
                    text = item.category,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(height = 2.dp))

                // Location
                Text(
                    text = "📍 ${item.location}",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(height = 8.dp))

                // Status chip
                StatusChip(status = item.status)
            }
        }
    }
}
