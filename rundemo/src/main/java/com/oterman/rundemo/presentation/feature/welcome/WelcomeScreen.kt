package com.oterman.rundemo.presentation.feature.welcome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.R
import com.oterman.rundemo.ui.theme.RunBlue
import com.oterman.rundemo.ui.theme.RunBlueGradient1
import com.oterman.rundemo.ui.theme.RunBlueGradient2

private val DeepNavy = Color(0xFF0A1628)
private val MidNavy = Color(0xFF0F2440)

@Composable
fun WelcomeScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to DeepNavy,
                        0.4f to MidNavy,
                        1.0f to RunBlue
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        RunBlueGradient1.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.75f, h * 0.18f),
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = Offset(w * 0.75f, h * 0.18f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        RunBlue.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.15f, h * 0.55f),
                    radius = w * 0.50f
                ),
                radius = w * 0.50f,
                center = Offset(w * 0.15f, h * 0.55f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        RunBlueGradient2.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.6f, h * 0.85f),
                    radius = w * 0.40f
                ),
                radius = w * 0.40f,
                center = Offset(w * 0.6f, h * 0.85f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(id = R.drawable.run_duck),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DemoRun",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "一款简约而不简单的跑步数据分析应用。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            ButtonSection(
                onNavigateToRegister = onNavigateToRegister,
                onNavigateToLogin = onNavigateToLogin
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ButtonSection(
    onNavigateToRegister: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onNavigateToRegister,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = RunBlue
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "注册",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }

        OutlinedButton(
            onClick = onNavigateToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "登录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}
