package com.anshul.campuscare.ui.components

// ──────────────────────────────────────────────
// Status Chip
//
// A small colored label that shows the item's status.
// Colors:  Red = LOST, Green = FOUND, Grey = CLAIMED
// ──────────────────────────────────────────────

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anshul.campuscare.ui.theme.ClaimedColor
import com.anshul.campuscare.ui.theme.ClaimedColorLight
import com.anshul.campuscare.ui.theme.FoundColor
import com.anshul.campuscare.ui.theme.FoundColorLight
import com.anshul.campuscare.ui.theme.LostColor
import com.anshul.campuscare.ui.theme.LostColorLight

@Composable
fun StatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    // Pick colors based on the status string
    val backgroundColor: Color = when (status) {
        "LOST" -> LostColorLight
        "FOUND" -> FoundColorLight
        "CLAIMED" -> ClaimedColorLight
        else -> ClaimedColorLight
    }

    val textColor: Color = when (status) {
        "LOST" -> LostColor
        "FOUND" -> FoundColor
        "CLAIMED" -> ClaimedColor
        else -> ClaimedColor
    }

    Box(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = 12.dp))
            .background(color = backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
