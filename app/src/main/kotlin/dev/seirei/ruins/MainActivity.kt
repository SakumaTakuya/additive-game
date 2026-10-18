package dev.seirei.ruins

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.core.view.WindowCompat
import dev.seirei.core.ui.theme.SeireiTheme

/** 単一Activity（設計書05 §2）。ナビゲーションとディープリンク着地は E8-01 で実装する。 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val darkTheme = isSystemInDarkTheme()
            SeireiTheme(darkTheme = darkTheme) {
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "遺跡に住みつく精霊（${FlavorConfig.NAME}）",
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                    )
                }
            }
        }
    }
}
