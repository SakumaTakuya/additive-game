package dev.seirei.core.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(name = "Seirei components · dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SeireiThemeComponentsDarkPreview() {
    SeireiTheme(darkTheme = true) {
        SeireiThemeComponentsContent()
    }
}

@Preview(name = "Seirei components · light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SeireiThemeComponentsLightPreview() {
    SeireiTheme(darkTheme = false) {
        SeireiThemeComponentsContent()
    }
}

@Composable
private fun SeireiThemeComponentsContent() {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("遺跡の灯り") },
        text = {
            Column {
                Text(
                    text = "小さな精霊たちと、もう一度探索に出ますか？",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("編成", style = MaterialTheme.typography.titleSmall)
                        Text("隊長 1体 · 留守番 2体", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text("探索に出る")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {}) { Text("灯す") }
        },
        dismissButton = {
            TextButton(onClick = {}) { Text("戻る") }
        },
    )
}
