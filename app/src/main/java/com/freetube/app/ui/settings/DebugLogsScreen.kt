package com.freetube.app.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load logs
    LaunchedEffect(Unit) {
        isLoading = true
        logs = getAppLogs()
        isLoading = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Debug Logs",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        // Copy all logs
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("FreeTube Logs", logs.joinToString("\n"))
                        clipboard.setPrimaryClip(clip)
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy logs")
                    }
                    IconButton(onClick = {
                        // Refresh logs
                        logs = getAppLogs()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            logs.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No logs available")
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(logs) { log ->
                        LogItem(log = log)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItem(log: String) {
    val color = when {
        log.contains(" E ") || log.contains("Error") -> Color.Red
        log.contains(" W ") || log.contains("Warning") -> Color(0xFFFFA500)
        log.contains(" D ") -> Color.Cyan
        log.contains(" I ") -> Color.Green
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Text(
        text = log,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        color = color,
        modifier = Modifier.padding(vertical = 1.dp)
    )
}

private fun getAppLogs(): List<String> {
    val logs = mutableListOf<String>()
    try {
        // Get logs from logcat for FreeTube app
        val process = Runtime.getRuntime().exec(arrayOf(
            "logcat", "-d", "-t", "500", 
            "*:V"
        ))
        
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?
        
        while (reader.readLine().also { line = it } != null) {
            line?.let { 
                // Filter for our app's logs
                if (it.contains("freetube", ignoreCase = true) || 
                    it.contains("YouTubeService", ignoreCase = true) ||
                    it.contains("ExoPlayer", ignoreCase = true) ||
                    it.contains("NewPipe", ignoreCase = true) ||
                    it.contains("ShortsScreen", ignoreCase = true) ||
                    it.contains("HomeScreen", ignoreCase = true) ||
                    it.contains("PlayerScreen", ignoreCase = true) ||
                    it.contains("Error", ignoreCase = true) ||
                    it.contains("Exception", ignoreCase = true)) {
                    logs.add(it)
                }
            }
        }
        reader.close()
        process.waitFor()
    } catch (e: Exception) {
        logs.add("Error reading logs: ${e.message}")
    }
    
    return logs.takeLast(200).reversed() // Most recent first
}
