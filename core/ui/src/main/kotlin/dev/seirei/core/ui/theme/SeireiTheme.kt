package dev.seirei.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// TODO(パレット確定): replace Material 3 defaults with approved spirit-quality and stone swatches.
private val SeireiLightColorScheme = lightColorScheme()
private val SeireiDarkColorScheme = darkColorScheme()

/** Uses fixed Material 3 colors until the palette is finalized; dark is the default. */
@Composable
fun SeireiTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) SeireiDarkColorScheme else SeireiLightColorScheme,
        typography = Typography(),
        content = content,
    )
}
