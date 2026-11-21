package com.streamvault.feature.live.sports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.streamvault.core.ui.design.BrowseColors
import com.streamvault.core.ui.image.ChannelLogoBadge
import com.streamvault.core.ui.interaction.TvClickableSurface
import com.streamvault.feature.live.R

private val tileShape = RoundedCornerShape(14.dp)
private val sportsAccent get() = if (BrowseColors.current.isLight) Color(0xFF007579) else Color(0xFF51E4DC)

@Composable
internal fun sportsCardBorder(shape: Shape, selected: Boolean = false) = ClickableSurfaceDefaults.border(
    border = Border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) sportsAccent else BrowseColors.Outline), shape = shape),
    focusedBorder = Border(BorderStroke(3.dp, sportsAccent), shape = shape)
)

@Composable
internal fun LeagueBadge(label: String, sport: SportType?, active: Boolean, onClick: () -> Unit) {
    val (icon, ink) = when (sport) {
        SportType.FOOTBALL -> Icons.Default.SportsSoccer to Color(0xFF293B72)
        SportType.BASKETBALL -> Icons.Default.SportsBasketball to Color(0xFFAD491D)
        SportType.AMERICAN_FOOTBALL -> Icons.Default.SportsFootball to Color(0xFF825634)
        SportType.BASEBALL -> Icons.Default.SportsBaseball to Color(0xFFAF3154)
        SportType.MOTORSPORT -> Icons.Default.SportsMotorsports to Color(0xFFA83443)
        SportType.HOCKEY -> Icons.Default.SportsHockey to Color(0xFF116271)
        SportType.CRICKET -> Icons.Default.SportsCricket to Color(0xFF5E447F)
        SportType.TENNIS -> Icons.Default.SportsTennis to Color(0xFF3E723C)
        null -> Icons.Default.Apps to Color(0xFF006E70)
        else -> Icons.Default.EmojiEvents to Color(0xFF785627)
    }
    Column(modifier = Modifier.width(78.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        TvClickableSurface(
            onClick = onClick, modifier = Modifier.size(44.dp).semantics { selected = active; contentDescription = label },
            shape = ClickableSurfaceDefaults.shape(CircleShape), border = sportsCardBorder(CircleShape, active),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f)
        ) {
            Box(Modifier.fillMaxSize().clip(CircleShape).background(
                Brush.linearGradient(listOf(Color.White, Color(0xFFCFDAE6)))), contentAlignment = Alignment.Center) {
                Icon(icon, null, modifier = Modifier.size(30.dp), tint = ink)
            }
        }
        Text(label, fontSize = 10.sp, lineHeight = 11.sp, textAlign = TextAlign.Center,
            color = if (active) sportsAccent else BrowseColors.TextPrimary,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

internal data class SportsFeedTile(val broadcast: SportsBroadcast, val fixture: SportsFeedLabel?)

@Composable
internal fun SportsFeedCard(league: SportsLeague, feed: SportsFeedTile, onClick: () -> Unit) {
    val channel = feed.broadcast.channel
    val action = stringResource(R.string.live_sports_play_channel_content_description, channel.name)
    val fixture = feed.fixture
    TvClickableSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 116.dp).semantics { contentDescription = action },
        shape = ClickableSurfaceDefaults.shape(tileShape),
        border = sportsCardBorder(tileShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (BrowseColors.current.isLight) BrowseColors.SurfaceElevated else Color(0xFF202532),
            focusedContainerColor = if (BrowseColors.current.isLight) BrowseColors.SurfaceEmphasis else Color(0xFF2C3544)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.015f)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(league.displayName, modifier = Modifier.weight(1f), color = BrowseColors.TextSecondary,
                    fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                fixture?.feed?.let { source ->
                    Text(source, modifier = Modifier.widthIn(max = 102.dp), color = BrowseColors.TextSecondary,
                        fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(Modifier.fillMaxWidth().heightIn(min = 43.dp), verticalArrangement = Arrangement.Center) {
                if (fixture == null) {
                    Text(channel.name, fontWeight = FontWeight.SemiBold, color = BrowseColors.TextPrimary,
                        fontSize = 15.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                } else {
                    TeamLine(fixture.firstTeam, sportsAccent)
                    TeamLine(fixture.secondTeam, BrowseColors.MediaWarning)
                }
            }
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(BrowseColors.Divider))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChannelLogoBadge(channelName = channel.name, logoUrl = channel.logoUrl, shape = CircleShape,
                    modifier = Modifier.size(20.dp))
                Text(fixture?.schedule ?: stringResource(R.string.nav_live_tv), modifier = Modifier.weight(1f),
                    color = if (fixture?.schedule != null) sportsAccent else BrowseColors.TextSecondary,
                    fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp), tint = BrowseColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun TeamLine(name: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(17.dp).background(accent.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
            Text(name.take(1), color = accent, fontSize = 10.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text(name, color = BrowseColors.TextPrimary, fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp, lineHeight = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
