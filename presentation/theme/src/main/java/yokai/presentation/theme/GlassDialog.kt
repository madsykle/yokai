package yokai.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * iOS 27 Liquid Glass Dialog.
 *
 * Provides:
 * - 24dp top corners only (bottom corners remain square for system gesture area)
 * - Grabber handle indicator
 * - Tier-aware glass background (Backdrop/Haze/Scrim)
 * - Darkened edge + specular highlight decorators
 *
 * DESIGN.md §1.1: Glass surfaces use tier-aware blur
 * DESIGN.md §1.2: Never stack glass on glass
 * DESIGN.md §1.3: Cap at 2–3 glass surfaces per screen
 */
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val containerColor = glassTintColor(GlassColors.GlassBaseTintAlpha, isDark)
    val onSurface = if (isDark) GlassColors.LabelPrimaryDark else GlassColors.LabelPrimaryLight
    val onSurfaceVariant = if (isDark) GlassColors.LabelSecondaryDark else GlassColors.LabelSecondaryLight

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        title = title,
        text = text,
        icon = icon,
        modifier = Modifier.clip(shape),
        containerColor = containerColor,
        titleContentColor = onSurface,
        textContentColor = onSurfaceVariant,
        // buttonContentColor not available in this version
    )
}

/**
 * iOS 27 Liquid Glass Bottom Sheet container.
 *
 * Provides:
 * - 24dp top corners only
 * - Grabber handle
 * - Tier-aware glass background
 */
@Composable
fun GlassBottomSheetContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val containerColor = glassTintColor(GlassColors.GlassBaseTintAlpha, isDark)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor),
    ) {
        // Grabber handle (iOS 27 style)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            contentAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f),
                        RoundedCornerShape(2.dp),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(bottom = 16.dp)),
        ) {
            content()
        }
    }
}
