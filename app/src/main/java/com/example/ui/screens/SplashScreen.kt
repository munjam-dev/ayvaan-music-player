package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // Animation States
    val infiniteTransition = rememberInfiniteTransition(label = "SplashTransition")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    val fadeAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        fadeAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(1000, easing = EaseInOutCubic)
        )
        // Splash lasts 2.2 seconds before transitioning
        delay(2200)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CyberViolet.copy(alpha = 0.45f),
                        PitchBlack
                    ),
                    center = Offset.Unspecified,
                    radius = 1200f
                )
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(fadeAnim.value)
        ) {
            // Pulse Glow card wrapper
            Box(
                modifier = Modifier
                    .scale(pulseScale)
                    .alpha(pulseAlpha),
                contentAlignment = Alignment.Center
            ) {
                AyvanLogo()
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Header with glorious neon shadows
            Text(
                text = "Ayvaaan",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                style = MaterialTheme.typography.displayLarge.copy(
                    shadow = Shadow(
                        color = CyberCyan,
                        offset = Offset(0f, 0f),
                        blurRadius = 45f
                    )
                ),
                color = TextLight,
                modifier = Modifier.testTag("app_logo_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Premium tagline
            Text(
                text = "FEEL MUSIC BEYOND SOUND.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp,
                color = CyberCyan.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun AyvanLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(110.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CyberViolet.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    radius = 200f
                ),
                shape = CircleShape
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_ayvan_logo),
            contentDescription = "Ayvan Logo",
            modifier = Modifier.fillMaxSize()
        )
    }
}
