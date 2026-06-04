package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun PermissionScreen(onPermissionGranted: (Boolean) -> Unit) {
    val context = LocalContext.current
    
    // Choose permissions based on SDK version
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val audioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            results[Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        onPermissionGranted(audioGranted)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PitchBlack,
                        CardGrey,
                        PitchBlack
                    )
                )
            )
            .testTag("permission_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle ambient neon blur circles in background
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.TopEnd)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(CyberViolet.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Signature Glowing Ayvan Logo
            AyvanLogo(modifier = Modifier.padding(bottom = 24.dp))

            Text(
                text = "File Access Requested",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Ayvaaan requires permission to scan your device for offline audio tracks (MP3, WAV, FLAC, AAC, M4A) and map your library.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = TextDim,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = LuxuryGrey.copy(alpha = 0.8f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberViolet.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "🔒 PRIVACY ASSURANCE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberPink,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This app compiles 100% offline. No servers, no sign-ups, no tracking of listening profiles. Your audio files stay protected on your device.",
                        fontSize = 12.sp,
                        color = TextDim,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Premium pulsing Button for permission launch
            Button(
                onClick = { launcher.launch(permissionsToRequest) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("grant_permission_button")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(CyberViolet, CyberPink)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "GRANT AUDIO ACCESS",
                        color = TextLight,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Guided offline notification
            Text(
                text = "Please authorize permission to proceed into your music library.",
                color = TextDim,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
