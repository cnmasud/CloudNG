package com.cloudng.app.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cloudng.app.ui.settings.SettingsEvent
import com.cloudng.app.ui.settings.SettingsViewModel
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var page by remember { mutableIntStateOf(0) }

    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.Cloud,
            title = "Welcome to CloudNG",
            body = "A fast, flexible proxy client. Connect to your servers securely from anywhere.",
            accent = Color(0xFF80DEEA)
        ),
        OnboardingPage(
            icon = Icons.Default.Security,
            title = "Privacy First",
            body = "CloudNG does not log your traffic. Your connection data stays on your device.",
            accent = Color(0xFF69F0AE)
        ),
        OnboardingPage(
            icon = Icons.Default.Lock,
            title = "VPN Permission",
            body = "CloudNG needs to create a VPN tunnel to route your traffic through your chosen proxy server. " +
                    "This is required for all traffic routing and kill-switch features.",
            accent = Color(0xFF82B1FF)
        )
    )

    val accentColor by animateColorAsState(
        pages[page].accent,
        animationSpec = tween(600),
        label = "accent"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        accentColor.copy(alpha = 0.07f)
                    )
                )
            )
    ) {
        Scaffold(containerColor = Color.Transparent) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))

                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.18f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = pages[page].icon,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = accentColor
                        )
                    }
                }

                Spacer(Modifier.height(36.dp))

                Text(
                    text = pages[page].title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = pages[page].body,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )

                Spacer(Modifier.weight(1f))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    pages.indices.forEach { i ->
                        val width by animateDpAsState(
                            if (i == page) 28.dp else 8.dp,
                            animationSpec = tween(300),
                            label = "indicatorW"
                        )
                        val color by animateColorAsState(
                            if (i == page) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            animationSpec = tween(300),
                            label = "indicatorC"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(width, 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                        )
                    }
                }

                Spacer(Modifier.height(36.dp))

                Button(
                    onClick = {
                        if (page < pages.lastIndex) {
                            page++
                        } else {
                            viewModel.onEvent(SettingsEvent.SetOnboardingComplete)
                            onFinish()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text(
                        if (page < pages.lastIndex) "Continue" else "Get Started",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF0D1B2A)
                    )
                }

                if (page < pages.lastIndex) {
                    TextButton(
                        onClick = {
                            viewModel.onEvent(SettingsEvent.SetOnboardingComplete)
                            onFinish()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Skip",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val accent: Color
)
