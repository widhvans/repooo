package com.freetube.app.ui.shorts

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.freetube.app.data.extractor.ExtractorHelper
import com.freetube.app.data.models.VideoInfo
import com.freetube.app.ui.theme.YouTubeRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    onChannelClick: (String) -> Unit,
    viewModel: ShortsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            
            uiState.error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Failed to load Shorts",
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.refresh() }) {
                        Text("Retry")
                    }
                }
            }
            
            uiState.shorts.isNotEmpty() -> {
                val pagerState = rememberPagerState(
                    initialPage = 0,
                    pageCount = { uiState.shorts.size }
                )
                
                LaunchedEffect(pagerState.currentPage) {
                    viewModel.onPageChanged(pagerState.currentPage)
                }
                
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    ShortItem(
                        video = uiState.shorts[page],
                        isActive = page == pagerState.currentPage,
                        onChannelClick = { onChannelClick(uiState.shorts[page].channelId) }
                    )
                }
            }
            
            else -> {
                Text(
                    text = "No Shorts available",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun ShortItem(
    video: VideoInfo,
    isActive: Boolean,
    onChannelClick: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(isActive) }
    var isLiked by remember { mutableStateOf(false) }
    var isDisliked by remember { mutableStateOf(false) }
    var streamUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingStream by remember { mutableStateOf(false) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 1f
        }
    }
    
    // Load stream URL when active
    LaunchedEffect(isActive, video.id) {
        if (isActive && streamUrl == null && !isLoadingStream) {
            isLoadingStream = true
            try {
                val videoUrl = ExtractorHelper.buildVideoUrl(video.id)
                // Fetch stream data from NewPipe
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val streamInfo = org.schabi.newpipe.extractor.stream.StreamInfo.getInfo(
                        org.schabi.newpipe.extractor.ServiceList.YouTube,
                        videoUrl
                    )
                    
                    // Get best video stream
                    val bestStream = streamInfo.videoStreams
                        .filter { it.isVideoOnly == false }
                        .maxByOrNull { it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 }
                        ?: streamInfo.videoStreams.firstOrNull()
                    
                    streamUrl = bestStream?.content
                }
            } catch (e: Exception) {
                android.util.Log.e("ShortsScreen", "Failed to load stream: ${e.message}", e)
            }
            isLoadingStream = false
        }
    }
    
    // Play video when stream URL is available
    LaunchedEffect(streamUrl, isActive) {
        if (isActive && streamUrl != null) {
            val mediaItem = MediaItem.fromUri(streamUrl!!)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } else if (!isActive) {
            exoPlayer.pause()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Video player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Overlay gradient at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )
        
        // Right side action buttons
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Like
            ActionButton(
                icon = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                label = video.formattedLikeCount,
                onClick = { isLiked = !isLiked; isDisliked = false },
                tint = if (isLiked) YouTubeRed else Color.White
            )
            
            // Dislike
            ActionButton(
                icon = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                label = "Dislike",
                onClick = { isDisliked = !isDisliked; isLiked = false },
                tint = if (isDisliked) YouTubeRed else Color.White
            )
            
            // Comments
            ActionButton(
                icon = Icons.Outlined.Comment,
                label = "Comments",
                onClick = { /* TODO: Show comments */ }
            )
            
            // Share
            ActionButton(
                icon = Icons.Outlined.Share,
                label = "Share",
                onClick = { /* TODO: Share */ }
            )
            
            // More
            ActionButton(
                icon = Icons.Default.MoreVert,
                label = "",
                onClick = { /* TODO: More options */ }
            )
        }
        
        // Bottom info
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 24.dp, end = 80.dp)
        ) {
            // Channel
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onChannelClick)
            ) {
                AsyncImage(
                    model = video.channelAvatarUrl,
                    contentDescription = video.channelName,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "@${video.channelName}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Button(
                    onClick = { /* TODO: Subscribe */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Subscribe",
                        color = Color.Black,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Title
            Text(
                text = video.title,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Music/Sound indicator (placeholder)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Sound",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Original audio - ${video.channelName}",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
        if (label.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
