package com.jordan.wailaixifu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jordan.wailaixifu.ui.AppRoot
import com.jordan.wailaixifu.ui.MainViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel = viewModel<MainViewModel>(factory = MainViewModel.Factory)
            // The palette belongs to the selected site, so AppRoot owns theming rather than
            // wrapping everything in one fixed theme here.
            Surface(modifier = Modifier.fillMaxSize()) {
                AppRoot(viewModel = viewModel)
            }
        }
    }
}
