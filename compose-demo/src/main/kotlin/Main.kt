import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

@Composable
@Preview
fun App() = Box(Modifier.fillMaxSize()) {
    var text by remember { mutableStateOf("Hello, Jweust!") }

    MaterialTheme {
        Row(modifier = Modifier.align(Alignment.Center)) {
            Button(content = {
                Text(text)
            }, onClick = {
                text = "Hello, Desktop!"
            })
            Button(content = {
                Text("ERROR")
            }, onClick = {
                throw IllegalStateException("ERROR")
            })
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Jweust Demo") {
        App()
    }
}
