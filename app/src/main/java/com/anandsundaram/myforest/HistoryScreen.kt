package com.anandsundaram.myforest

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anandsundaram.myforest.ui.DailyFocusStat
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    dailyStats: List<DailyFocusStat>,
    totalMinutes: Int,
    focusedDays: Int
) {
    val colorScheme = MaterialTheme.colorScheme
    val backgroundBrush = remember(colorScheme) {
        Brush.verticalGradient(
            colors = listOf(
                colorScheme.background,
                colorScheme.surfaceVariant
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "History",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Daily focus totals",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            MetricsRow(totalMinutes = totalMinutes, focusedDays = focusedDays)

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Last 7 days",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DailyBarChart(dailyStats = dailyStats.takeLast(7))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (dailyStats.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No sessions yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Start a focus session to grow your history.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(dailyStats.reversed()) { stat ->
                        DailyStatCard(stat)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricsRow(totalMinutes: Int, focusedDays: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard(title = "Total Minutes", value = totalMinutes.toString(), modifier = Modifier.weight(1f))
        MetricCard(title = "Focused Days", value = focusedDays.toString(), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DailyBarChart(dailyStats: List<DailyFocusStat>) {
    val maxMinutes = dailyStats.maxOfOrNull { it.totalMinutes }?.coerceAtLeast(1) ?: 1
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    val formatter = remember { DateTimeFormatter.ofPattern("EEE") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            if (dailyStats.isEmpty()) return@Canvas

            val spacing = size.width / (dailyStats.size * 2f)
            val barWidth = spacing
            dailyStats.forEachIndexed { index, stat ->
                val barHeight = (stat.totalMinutes / maxMinutes.toFloat()) * size.height
                val x = spacing + index * spacing * 2
                drawLine(
                    color = barColor,
                    start = Offset(x, size.height),
                    end = Offset(x, size.height - barHeight),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            dailyStats.forEach { stat ->
                Text(
                    text = formatter.format(stat.date),
                    style = MaterialTheme.typography.labelLarge,
                    color = labelColor,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
private fun DailyStatCard(stat: DailyFocusStat) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    val growth = if (stat.totalMinutes > 0) {
        (stat.successMinutes / stat.totalMinutes.toFloat()).coerceIn(0.2f, 1f)
    } else {
        0.2f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tree(growth = growth, size = 72.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatter.format(stat.date),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stat.totalMinutes} minutes • ${stat.sessions} sessions",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
