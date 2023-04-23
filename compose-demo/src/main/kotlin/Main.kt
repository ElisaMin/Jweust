import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
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
        Button(onClick = {
            text = "Hello, Desktop!"
        }, modifier = Modifier.align(Alignment.Center)) {
            Text(text)
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Jweust Demo") {
        App()
    }
}
