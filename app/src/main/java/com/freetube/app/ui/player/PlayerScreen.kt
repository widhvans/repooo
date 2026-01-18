package com.freetube.app.ui.player

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.freetube.app.data.models.CommentInfo
import com.freetube.app.ui.components.VideoCard
import com.freetube.app.ui.theme.YouTubeRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    videoId: String,
    onBackClick: () -> Unit,
    onChannelClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Share video function
    fun shareVideo(title: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Check out: $title\nhttps://www.youtube.com/watch?v=$videoId")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share video"))
    }
    
    // Quality selection dialog
    if (uiState.showQualityDialog) {
        QualitySelectionDialog(
            currentQuality = uiState.selectedQuality,
            availableQualities = viewModel.availableQualities,
            onQualitySelected = { viewModel.setQuality(it) },
            onDismiss = { viewModel.hideQualityDialog() }
        )
    }
    
    // Speed selection dialog
    if (uiState.showSpeedDialog) {
        SpeedSelectionDialog(
            currentSpeed = uiState.playbackSpeed,
            speeds = viewModel.playbackSpeeds,
            onSpeedSelected = { viewModel.setPlaybackSpeed(it) },
            onDismiss = { viewModel.hideSpeedDialog() }
        )
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Video Player
        item {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                VideoPlayer(
                    streamUrl = viewModel.getBestStreamUrl(),
                    playbackSpeed = uiState.playbackSpeed,
                    isLive = uiState.video?.isLive ?: false,
                    onBackClick = onBackClick,
                    onQualityClick = { viewModel.showQualityDialog() },
                    onSpeedClick = { viewModel.showSpeedDialog() },
                    onCaptionsClick = { /* TODO: Captions */ },
                    onPositionChanged = { viewModel.updateWatchPosition(it) }
                )
            }
        }
        
        // Video info
        uiState.video?.let { video ->
            item {
                VideoInfoSection(
                    video = video,
                    isDescriptionExpanded = uiState.isDescriptionExpanded,
                    isInWatchLater = uiState.isInWatchLater,
                    onToggleDescription = { viewModel.toggleDescriptionExpanded() },
                    onChannelClick = { onChannelClick(video.channelId) },
                    onWatchLaterClick = { viewModel.toggleWatchLater() },
                    onShareClick = { shareVideo(video.title) },
                    onDownloadClick = { /* TODO: Implement download */ },
                    onSpeedClick = { viewModel.showSpeedDialog() }
                )
            }
            
            // Divider
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            // Comments section header
            item {
                CommentsHeader(
                    commentCount = uiState.comments.size,
                    isLoading = uiState.isLoadingComments
                )
            }
            
            // Comments (show first 3)
            items(uiState.comments.take(3)) { comment ->
                CommentItem(
                    comment = comment,
                    onChannelClick = { onChannelClick(comment.authorChannelId) }
                )
            }
            
            // See all comments button
            if (uiState.comments.size > 3) {
                item {
                    TextButton(
                        onClick = { /* TODO: Show all comments */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text("See all ${uiState.comments.size} comments")
                    }
                }
            }
            
            // Related videos header
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Related Videos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Related videos
            items(
                items = uiState.relatedVideos,
                key = { it.id }
            ) { relatedVideo ->
                VideoCard(
                    video = relatedVideo,
                    onClick = { onVideoClick(relatedVideo.id) },
                    isCompact = true,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        
        // Error state
        uiState.error?.let { error ->
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error loading video",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBackClick) {
                        Text("Go Back")
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoInfoSection(
    video: com.freetube.app.data.models.VideoInfo,
    isDescriptionExpanded: Boolean,
    isInWatchLater: Boolean,
    onToggleDescription: () -> Unit,
    onChannelClick: () -> Unit,
    onWatchLaterClick: () -> Unit,
    onShareClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onSpeedClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = video.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Stats
        Text(
            text = "${video.formattedViewCount} views • ${video.uploadDate}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionButton(
                icon = Icons.Default.ThumbUp,
                label = video.formattedLikeCount,
                onClick = { /* TODO: Like */ }
            )
            ActionButton(
                icon = Icons.Default.ThumbDown,
                label = "Dislike",
                onClick = { /* TODO: Dislike */ }
            )
            ActionButton(
                icon = Icons.Default.Share,
                label = "Share",
                onClick = onShareClick
            )
            ActionButton(
                icon = Icons.Default.Download,
                label = "Download",
                onClick = onDownloadClick
            )
            ActionButton(
                icon = if (isInWatchLater) Icons.Filled.WatchLater else Icons.Outlined.WatchLater,
                label = "Save",
                onClick = onWatchLaterClick
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        HorizontalDivider()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Channel info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onChannelClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = video.channelAvatarUrl,
                contentDescription = video.channelName,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.channelName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Button(
                onClick = { /* TODO: Subscribe */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = YouTubeRed
                )
            ) {
                Text("Subscribe")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleDescription)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = video.cleanDescription,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (video.cleanDescription.length > 150) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isDescriptionExpanded) "Show less" else "Show more",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun CommentsHeader(
    commentCount: Int,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Comments",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        if (isLoading) {
            Spacer(modifier = Modifier.width(8.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
        } else {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "($commentCount)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CommentItem(
    comment: CommentInfo,
    onChannelClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        AsyncImage(
            model = comment.authorAvatarUrl,
            contentDescription = comment.authorName,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onChannelClick),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (comment.isAuthorChannelOwner) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = comment.publishedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (comment.isPinned) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = comment.cleanText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.ThumbUp,
                    contentDescription = "Like",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = comment.formattedLikeCount,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (comment.isHearted) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Hearted",
                        modifier = Modifier.size(14.dp),
                        tint = YouTubeRed
                    )
                }
                
                if (comment.replyCount > 0) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${comment.replyCount} replies",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun QualitySelectionDialog(
    currentQuality: String,
    availableQualities: List<String>,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Video Quality") },
        text = {
            Column {
                availableQualities.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQualitySelected(quality) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = quality == currentQuality,
                            onClick = { onQualitySelected(quality) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = quality)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SpeedSelectionDialog(
    currentSpeed: Float,
    speeds: List<Float>,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback Speed") },
        text = {
            Column {
                speeds.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeedSelected(speed) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = speed == currentSpeed,
                            onClick = { onSpeedSelected(speed) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (speed == 1f) "Normal" else "${speed}x"
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
