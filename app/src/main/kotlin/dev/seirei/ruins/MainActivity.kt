package dev.seirei.ruins

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

/** 単一Activity（設計書05 §2）。ナビゲーションとディープリンク着地は E8-01 で実装する。 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Text("遺跡に住みつく精霊（${FlavorConfig.NAME}）")
            }
        }
    }
}
