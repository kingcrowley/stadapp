package cc.stadapp.stad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cc.stadapp.stad.ui.HomeScreen
import cc.stadapp.stad.ui.theme.StadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StadTheme {
                HomeScreen()
            }
        }
    }
}
