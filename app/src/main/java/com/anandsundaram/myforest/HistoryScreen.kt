package com.anandsundaram.myforest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HistoryScreen(history: List<FocusSession>) {
    LazyColumn {
        items(history) {
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (it.isSuccess) {
                        Tree(growth = 1f)
                    } else {
                        WitheredTree()
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        if (it.isSuccess) {
                            Text("Focused for ${it.durationMinutes} minutes")
                        } else {
                            Text("Focused for ${it.actualDurationMinutes} of ${it.durationMinutes} minutes")
                        }
                        Text("on ${it.date}")
                    }
                }
            }
        }
    }
}
