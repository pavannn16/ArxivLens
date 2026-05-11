package com.arxivlens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.arxivlens.ui.navigation.ArxivLensNavGraph
import com.arxivlens.ui.theme.ArxivLensTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ArxivLensApp
        setContent {
            ArxivLensTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ArxivLensNavGraph(app = app)
                }
            }
        }
    }
}
