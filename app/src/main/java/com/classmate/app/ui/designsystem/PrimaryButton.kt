package com.classmate.app.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateShapes

/**
 * Brand-colored CTA, 52 dp tall. Loading state shows an inline spinner so
 * the button position never shifts.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val colors = LocalClassMateColors.current
    val shapes = LocalClassMateShapes.current
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled && !loading,
        shape = shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.brandPrimary,
            contentColor = colors.fgOnAccent,
            // Disabled CTA: muted brand tint background + dimmed white text.
            // Avoids the "broken" feel of M3's default light-gray fill.
            disabledContainerColor = colors.brandPrimary.copy(alpha = 0.32f),
            disabledContentColor = colors.fgOnAccent
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = colors.fgOnAccent,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.size(8.dp))
            }
            Text(text)
        }
    }
}

/**
 * Outlined secondary CTA. Same height as [PrimaryButton] to align in rows.
 */
@Composable
fun OutlinedActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = LocalClassMateColors.current
    val shapes = LocalClassMateShapes.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colors.brandPrimary
        )
    ) {
        Text(text)
    }
}
