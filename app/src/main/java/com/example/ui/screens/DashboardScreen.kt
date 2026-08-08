package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.MessageEntity
import com.example.data.db.MessageSender
import com.example.data.db.MessageType
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val allMessages by viewModel.allMessages.collectAsState()
    var selectedTimeframe by remember { mutableStateOf("This Week") } // "This Week", "Last 30 Days", "All Time"

    // Calculate Analytics
    val totalMessages = allMessages.size
    val userMessagesCount = allMessages.count { it.sender == MessageSender.USER }
    val imageCount = allMessages.count { it.type == MessageType.IMAGE }
    val websiteCount = allMessages.count { it.type == MessageType.WEBSITE }
    val fileCount = allMessages.count { it.type == MessageType.FILE_ANALYSIS }
    val textCount = allMessages.count { it.type == MessageType.TEXT }

    // Estimated time spent: 1.5 mins per user message + 1 min per AI message
    val estimatedMinutesSpent = (userMessagesCount * 1.5 + (totalMessages - userMessagesCount) * 1.0).toInt()
    val formattedTimeSpent = if (estimatedMinutesSpent < 60) {
        "${estimatedMinutesSpent}m"
    } else {
        "${estimatedMinutesSpent / 60}h ${estimatedMinutesSpent % 60}m"
    }

    // Weekly distribution calculation (Mon - Sun)
    val dayCounts = remember(allMessages) {
        val counts = IntArray(7) // Mon=0, Tue=1 ... Sun=6
        val calendar = Calendar.getInstance()
        allMessages.forEach { msg ->
            calendar.timeInMillis = msg.timestamp
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Sun=1, Mon=2...
            val index = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
            if (index in 0..6) {
                counts[index]++
            }
        }
        counts.toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Activity Dashboard",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Timeframe Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("This Week", "Last 30 Days", "All Time").forEach { timeframe ->
                        FilterChip(
                            selected = selectedTimeframe == timeframe,
                            onClick = { selectedTimeframe = timeframe },
                            label = { Text(timeframe, style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = if (selectedTimeframe == timeframe) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            // Summary KPI Cards Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Total Messages",
                            value = "$totalMessages",
                            subtitle = "$userMessagesCount sent by you",
                            icon = Icons.Default.Chat,
                            iconColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Images Generated",
                            value = "$imageCount",
                            subtitle = "Visual art outputs",
                            icon = Icons.Default.Image,
                            iconColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Websites Created",
                            value = "$websiteCount",
                            subtitle = "Interactive HTML pages",
                            icon = Icons.Default.Code,
                            iconColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Est. Time Spent",
                            value = formattedTimeSpent,
                            subtitle = "Active AI engagement",
                            icon = Icons.Default.Schedule,
                            iconColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Weekly Activity Bar Chart
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Weekly Activity Distribution",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "Total: $totalMessages msgs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom Bar Chart Canvas
                        WeeklyBarChart(
                            dayCounts = dayCounts,
                            primaryColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Usage Breakdown Donut Chart Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Mode Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Donut Canvas
                            DonutChart(
                                textCount = textCount,
                                imageCount = imageCount,
                                websiteCount = websiteCount,
                                fileCount = fileCount,
                                modifier = Modifier.size(130.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Legend List
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                LegendItem(
                                    color = Color(0xFF4285F4),
                                    title = "Text Conversations",
                                    count = textCount,
                                    total = totalMessages
                                )
                                LegendItem(
                                    color = Color(0xFFEA4335),
                                    title = "Image Generation",
                                    count = imageCount,
                                    total = totalMessages
                                )
                                LegendItem(
                                    color = Color(0xFF34A853),
                                    title = "Website Creation",
                                    count = websiteCount,
                                    total = totalMessages
                                )
                                LegendItem(
                                    color = Color(0xFFFBBC05),
                                    title = "File Analysis",
                                    count = fileCount,
                                    total = totalMessages
                                )
                            }
                        }
                    }
                }
            }

            // Recent Activity Section
            item {
                Text(
                    text = "Recent Interactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val recentMsgs = allMessages.takeLast(5).reversed()
            if (recentMsgs.isEmpty()) {
                item {
                    Text(
                        text = "No recent activity logged.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(recentMsgs, key = { it.id }) { msg ->
                    ActivityLogItem(msg = msg)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun WeeklyBarChart(
    dayCounts: List<Int>,
    primaryColor: Color,
    labelColor: Color
) {
    val maxCount = (dayCounts.maxOrNull() ?: 1).coerceAtLeast(1)
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val width = size.width
        val height = size.height - 30.dp.toPx() // leave space for bottom day labels
        val barWidth = 28.dp.toPx()
        val spaceBetween = (width - (barWidth * 7)) / 8

        dayCounts.forEachIndexed { index, count ->
            val barHeight = (count.toFloat() / maxCount) * height
            val x = spaceBetween + index * (barWidth + spaceBetween)
            val y = height - barHeight

            // Draw background track bar
            drawRoundRect(
                color = primaryColor.copy(alpha = 0.12f),
                topLeft = Offset(x, 0f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(12.dp.toPx())
            )

            // Draw filled bar with gradient
            if (barHeight > 0) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor,
                            primaryColor.copy(alpha = 0.6f)
                        )
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }

            // Draw value text above bar if count > 0
            if (count > 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    "$count",
                    x + barWidth / 2,
                    (y - 6.dp.toPx()).coerceAtLeast(12.dp.toPx()),
                    android.graphics.Paint().apply {
                        color = primaryColor.hashCode()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                )
            }

            // Draw Day Label below bar
            drawContext.canvas.nativeCanvas.drawText(
                days[index],
                x + barWidth / 2,
                size.height - 4.dp.toPx(),
                android.graphics.Paint().apply {
                    color = labelColor.hashCode()
                    textSize = 11.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}

@Composable
private fun DonutChart(
    textCount: Int,
    imageCount: Int,
    websiteCount: Int,
    fileCount: Int,
    modifier: Modifier = Modifier
) {
    val total = (textCount + imageCount + websiteCount + fileCount).toFloat().coerceAtLeast(1f)
    val angles = listOf(
        (textCount / total) * 360f,
        (imageCount / total) * 360f,
        (websiteCount / total) * 360f,
        (fileCount / total) * 360f
    )
    val colors = listOf(
        Color(0xFF4285F4),
        Color(0xFFEA4335),
        Color(0xFF34A853),
        Color(0xFFFBBC05)
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 22.dp.toPx()
        var startAngle = -90f

        angles.forEachIndexed { index, sweepAngle ->
            if (sweepAngle > 0f) {
                drawArc(
                    color = colors[index],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle - 3f, // 3deg gap between slices
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    title: String,
    count: Int,
    total: Int
) {
    val percentage = if (total > 0) ((count.toFloat() / total) * 100).toInt() else 0

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "$count ($percentage%)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ActivityLogItem(msg: MessageEntity) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val (icon, iconColor, label) = when (msg.type) {
        MessageType.IMAGE -> Triple(Icons.Default.Image, MaterialTheme.colorScheme.secondary, "Image Generation")
        MessageType.WEBSITE -> Triple(Icons.Default.Code, MaterialTheme.colorScheme.tertiary, "Website Creation")
        MessageType.FILE_ANALYSIS -> Triple(Icons.Default.Description, MaterialTheme.colorScheme.error, "File Analysis")
        MessageType.TEXT -> Triple(Icons.Default.ChatBubble, MaterialTheme.colorScheme.primary, "Text Chat")
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(Date(msg.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = msg.content.ifBlank { msg.attachmentName ?: "AI Response" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
