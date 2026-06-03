package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainAppContainer
import com.example.ui.screens.PermissionScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MusicViewModel
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val musicViewModel: MusicViewModel = viewModel()
            val themeMode by musicViewModel.themeMode.collectAsState()
            val accentColor by musicViewModel.accentColor.collectAsState()
            val customAccentHex by musicViewModel.customAccentHex.collectAsState()
            val dynamicColors by musicViewModel.dynamicColors.collectAsState()
            val artworkDominantColor by musicViewModel.artworkDominantColor.collectAsState()

            MyApplicationTheme(
                themeMode = themeMode,
                accentColorName = accentColor,
                customAccentHex = customAccentHex,
                dynamicColor = dynamicColors,
                artworkDominantColor = artworkDominantColor
            ) {
                // Check initial permission status on launch
                val permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                
                val isPermissionInitiallyGranted = ContextCompat.checkSelfPermission(
                    this,
                    permissionToCheck
                ) == PackageManager.PERMISSION_GRANTED
                
                LaunchedEffect(Unit) {
                    musicViewModel.updatePermissionStatus(isPermissionInitiallyGranted)
                }

                // Core App Flow State Controllers
                var showSplash by remember { mutableStateOf(true) }
                val hasPermission by musicViewModel.permissionGranted.collectAsState()

                // Render screens based on sequence
                if (showSplash) {
                    SplashScreen(
                        onSplashFinished = {
                            showSplash = false
                        }
                    )
                } else if (!hasPermission) {
                    PermissionScreen(
                        onPermissionGranted = { wasGranted ->
                            // Update State Flow, triggers recomposition transitions automatically
                            musicViewModel.updatePermissionStatus(wasGranted)
                        }
                    )
                } else {
                    MainAppContainer(viewModel = musicViewModel)
                }
            }
        }
    }
}
