package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Distinctive color accents for Smoking Barrel theme
object LedgerColors {
    val DarkSlateBg = Color(0xFF121824)
    val CardBg = Color(0xFF1E293B)
    val MetallicSilver = Color(0xFFE2E8F0)
    val NeonGreen = Color(0xFF10B981)
    val AmberOrange = Color(0xFFF59E0B)
    val SoftRed = Color(0xFFEF4444)
    val TechBlue = Color(0xFF3B82F6)
    val SlateGrayText = Color(0xFF94A3B8)
    val DeepInk = Color(0xFF0F172A)
    
    val FireGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFF97316), Color(0xFFEF4444))
    )
    val GreenGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF34D399), Color(0xFF059669))
    )
    val TechGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF60A5FA), Color(0xFF2563EB))
    )
}

@Composable
fun StatusBadge(
    statusText: String,
    modifier: Modifier = Modifier
) {
    val isReceived = statusText.contains("received", ignoreCase = true) || 
                     statusText.contains("paid", ignoreCase = true)
    val isPending = statusText.contains("yet to Receive", ignoreCase = true) || 
                    statusText.contains("pending", ignoreCase = true)

    val bgColor = when {
        isReceived -> LedgerColors.NeonGreen.copy(alpha = 0.15f)
        isPending -> LedgerColors.AmberOrange.copy(alpha = 0.15f)
        else -> LedgerColors.TechBlue.copy(alpha = 0.15f)
    }

    val textColor = when {
        isReceived -> LedgerColors.NeonGreen
        isPending -> LedgerColors.AmberOrange
        else -> LedgerColors.TechBlue
    }

    val label = if (isPending) "Pending Collection" else statusText

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    gradient: Brush,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .height(115.dp)
            .clickable { onClick() }
    } else {
        modifier
            .fillMaxWidth()
            .height(115.dp)
    }
    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LedgerColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (onClick != null) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = LedgerColors.SlateGrayText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    color = LedgerColors.MetallicSilver,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = LedgerColors.SlateGrayText,
                        fontSize = 11.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun PartnerProgressRow(
    partnerName: String,
    amount: Double,
    total: Double,
    modifier: Modifier = Modifier,
    isExpanded: Boolean? = null
) {
    val percentage = if (total > 0) (amount / total) else 0.0
    val progressColor = when (partnerName) {
        "Karthi" -> Color(0xFFEC4899)
        "Pradeeph" -> Color(0xFFF59E0B)
        "Sankar" -> Color(0xFF3B82F6)
        "Ravi Shankar" -> Color(0xFF10B981)
        else -> LedgerColors.TechBlue
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(progressColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = partnerName,
                    color = LedgerColors.MetallicSilver,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (isExpanded != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse details" else "Expand details",
                        tint = LedgerColors.SlateGrayText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Rs. ${String.format("%,.2f", amount)}",
                    color = LedgerColors.MetallicSilver,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format("%.1f", percentage * 100)}%",
                    color = LedgerColors.SlateGrayText,
                    fontSize = 11.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        // Progress Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(LedgerColors.DeepInk)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = percentage.toFloat().coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(progressColor)
            )
        }
    }
}
