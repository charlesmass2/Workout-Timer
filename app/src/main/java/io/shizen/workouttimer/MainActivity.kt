package io.shizen.workouttimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import io.shizen.workouttimer.ui.AppRoot
import io.shizen.workouttimer.ui.theme.WT
import io.shizen.workouttimer.ui.theme.WorkoutTimerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            WorkoutTimerTheme {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(WT.Bg)) {
                    AppRoot(viewModel)
                }
            }
        }
    }
}
