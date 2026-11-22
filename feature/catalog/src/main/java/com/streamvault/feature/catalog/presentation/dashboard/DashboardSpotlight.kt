package com.streamvault.feature.catalog.presentation.dashboard

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.streamvault.core.ui.image.rememberCrossfadeImageModel
import com.streamvault.core.ui.interaction.TvButton

internal val DashboardCanvas = Color(0xFF100E13)
private val DashboardGold = Color(0xFFF3BF74)

@Composable
internal fun DashboardSpotlight(
    @DrawableRes brandArtwork: Int,
    title: String,
    summary: String,
    artworkUrl: String? = null,
    channelLogo: Boolean = false,
    metadata: List<String> = emptyList(),
    progress: Float? = null,
    actions: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth().background(DashboardCanvas)) {
        val wide = maxWidth >= 570.dp
        val minimumHeight = if (wide) 268.dp else 248.dp
        val backgroundGradient = Brush.horizontalGradient(
            0f to DashboardCanvas,
            0.25f to DashboardCanvas,
            0.53f to DashboardCanvas.copy(alpha = 0.7f),
            0.84f to Color.Transparent,
        )
        Box(Modifier.matchParentSize().background(
            Brush.horizontalGradient(listOf(DashboardCanvas, Color(0xFF29212D)))
        )) {
            if (!artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = rememberCrossfadeImageModel(artworkUrl),
                    contentDescription = null,
                    contentScale = if (channelLogo) ContentScale.Fit else ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth(if (wide) 0.80f else 1f)
                        .fillMaxHeight()
                        .then(if (channelLogo) Modifier.padding(horizontal = 52.dp, vertical = 42.dp) else Modifier),
                )
            } else if (wide) {
                Image(
                    painter = painterResource(brandArtwork),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 52.dp).size(132.dp),
                )
            }
            Box(Modifier.fillMaxSize().background(if (wide) backgroundGradient else
                Brush.horizontalGradient(listOf(DashboardCanvas.copy(alpha = 0.95f), DashboardCanvas.copy(alpha = 0.55f)))))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                0f to Color.Transparent,
                0.6f to Color.Transparent,
                1f to DashboardCanvas,
            )))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(if (wide) 0.57f else 1f)
                .heightIn(min = minimumHeight)
                .padding(start = 23.dp, end = 18.dp, top = 25.dp, bottom = 19.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
        ) {
            Text(
                title, color = Color(0xFFFFF6ED),
                fontSize = if (wide) 37.sp else 29.sp,
                lineHeight = if (wide) 41.sp else 33.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (metadata.isNotEmpty()) {
                Text(
                    metadata.joinToString("  ·  "), style = MaterialTheme.typography.labelLarge, color = DashboardGold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (summary.isNotBlank()) {
                Text(
                    summary, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE2D9E1),
                    maxLines = if (wide) 3 else 2, overflow = TextOverflow.Ellipsis,
                )
            }
            progress?.let {
                LinearProgressIndicator(
                    progress = { it }, color = DashboardGold, trackColor = Color.White.copy(alpha = 0.24f),
                    modifier = Modifier.width(156.dp),
                )
            }
            FlowRow(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { actions() }
        }
    }
}

@Composable
internal fun DashboardSpotlightButton(
    label: String,
    icon: ImageVector,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(11.dp)
    TvButton(
        onClick = onClick,
        shape = ButtonDefaults.shape(shape),
        scale = ButtonDefaults.scale(focusedScale = 1.025f),
        colors = ButtonDefaults.colors(
            containerColor = if (primary) DashboardGold else Color(0xD13A313F),
            contentColor = if (primary) DashboardCanvas else Color.White,
            focusedContainerColor = if (primary) Color(0xFFFFD59A) else Color(0xFF4A3A47),
            focusedContentColor = if (primary) DashboardCanvas else Color.White,
        ),
        border = ButtonDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, Color(0xFFFFE5C7)), shape = shape),
        ),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}
