package fr.augustine.androgustinecopilote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.augustine.androgustinecopilote.ui.CopilotRoute
import fr.augustine.androgustinecopilote.ui.theme.AndroGustineCopiloteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroGustineCopiloteTheme {
                CopilotRoute()
            }
        }
    }
}
