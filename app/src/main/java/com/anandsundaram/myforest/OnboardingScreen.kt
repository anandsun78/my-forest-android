package com.anandsundaram.myforest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onGetStarted: () -> Unit
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
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to MyForest",
                style = MaterialTheme.typography.headlineLarge,
                color = colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Grow focus sessions and track your daily momentum.",
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onBackground.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "How it works",
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "1. Pick a focus duration and tap Plant.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "2. Stay in the app while the tree grows.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "3. Review your daily totals and trends in History.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Required permissions: Usage Access, Notifications, and Battery Optimization disabled.",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(onClick = onGetStarted) {
                Text("Get Started")
            }
        }
    }
}
