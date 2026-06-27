package com.classmate.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A small round "?" that opens a Chinese help dialog. Lets a learning page keep only its title, a short
 * subtitle and the core actions on screen, moving the longer "how it works / what you can do" copy behind
 * a tap. Help content is product copy only — never provider/pipeline/debug text.
 */
@Composable
fun HelpHint(title: String, points: List<String>, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(26.dp).clickable { open = true },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("?", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { open = false }) { Text("知道了") } },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    points.filter { it.isNotBlank() }.forEach { Text("· $it", style = MaterialTheme.typography.bodyMedium) }
                }
            },
        )
    }
}
