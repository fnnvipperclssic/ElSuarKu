package com.example.elsuarku.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.elsuarku.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 3-screen onboarding flow for first-time users.
 *
 * Screens:
 *  1. Secure Voting — explains encryption & anonymity
 *  2. Transparent — explains real-time audit & verification
 *  3. Get Started — call to action, sign in button
 *
 * Shown only once (persisted in DataStore/SessionManager).
 */
@Composable
fun OnboardingScreen(onGetStarted: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPage(
            icon = Icons.Filled.Lock,
            title = "Pemilihan Aman & Terenkripsi",
            subtitle = "Setiap suara dienkripsi dengan AES-256-GCM\ndan disimpan secara anonim.\nPrivasi Anda adalah prioritas kami.",
            accentColor = EmeraldGreen
        ),
        OnboardingPage(
            icon = Icons.Filled.Visibility,
            title = "Transparan & Terverifikasi",
            subtitle = "Pantau hasil secara real-time.\nSetiap suara tercatat di audit trail\nyang tidak dapat diubah.",
            accentColor = DeepBlue
        ),
        OnboardingPage(
            icon = Icons.Filled.HowToVote,
            title = "Siap Memilih?",
            subtitle = "Gunakan hak pilih Anda dengan aman.\nSuara Anda menentukan arah organisasi.",
            accentColor = StatusWarning
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepBlue, DeepBlueDark)
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            Spacer(Modifier.height(24.dp))

            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                repeat(3) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) pages[index].accentColor
                                else OnDeepBlue.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action buttons
            if (pagerState.currentPage < 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onGetStarted) {
                        Text("Lewati", color = OnDeepBlue.copy(alpha = 0.6f))
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pages[pagerState.currentPage].accentColor
                        )
                    ) {
                        Text("Lanjut", color = ColorWhite)
                    }
                }
            } else {
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusWarning),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "Mulai Sekarang",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = DeepBlueDark
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val accentColor: androidx.compose.ui.graphics.Color
)

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon with colored circle background
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = page.accentColor.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = page.accentColor,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(Modifier.height(48.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = ColorWhite,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = OnDeepBlue.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        )
    }
}

// Needed for the button text
private val ColorWhite = androidx.compose.ui.graphics.Color.White
