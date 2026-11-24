package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.OutgoingAnimationState
import com.example.data.telephony.PhoneNumberInfoHelper
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalPictureTheme

@Composable
fun CallingOrbitAnimation(
    centerText: String,
    accentColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 250.dp,
    photoUri: String? = null
) {
    val transition = rememberInfiniteTransition(label = "calling_orbit")

    val outerRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rotation"
    )

    val innerRotation by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rotation"
    )

    val haloScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = this.size.minDimension
            val haloRadius = minDim * 0.49f * haloScale
            val outerArcRadius = minDim * 0.35f
            val innerArcRadius = minDim * 0.28f

            // Soft luminous radial halo matching the InCallActivity screenshot
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFDE68A).copy(alpha = 0.42f),
                        accentColor.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = haloRadius
                ),
                radius = haloRadius
            )

            // Faint full rings
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = outerArcRadius,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.28f),
                radius = innerArcRadius,
                style = Stroke(width = 2.dp.toPx())
            )

            // Outer rotating white/gold arcs
            rotate(degrees = outerRotation) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.9f),
                            Color(0xFFFCD34D).copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerArcRadius, center.y - outerArcRadius),
                    size = Size(outerArcRadius * 2, outerArcRadius * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Inner counter-rotating arc
            rotate(degrees = innerRotation) {
                drawArc(
                    color = secondaryColor.copy(alpha = 0.9f),
                    startAngle = 40f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(center.x - innerArcRadius, center.y - innerArcRadius),
                    size = Size(innerArcRadius * 2, innerArcRadius * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(size * 0.44f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f))
                .border(BorderStroke(2.dp, Color.White.copy(alpha = 0.55f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.displayLarge,
                fontSize = 42.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            if (!photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = centerText,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun RingingStatusPill(
    statusText: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "ringing_chevrons")
    val alphaPhase by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chevron_alpha"
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.18f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = ">>>",
                fontFamily = JetBrainsMonoFontFamily,
                color = Color.White.copy(alpha = alphaPhase),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun OutgoingCallOverlay(
    state: OutgoingAnimationState,
    modifier: Modifier = Modifier
) {
    val pictureTheme = LocalPictureTheme.current
    val strings = LocalAppStrings.current
    val simAccent = if (state.slotIndex == 0) pictureTheme.sim1Accent else pictureTheme.sim2Accent
    val carrier = PhoneNumberInfoHelper.detectCarrierAndRegion(state.number)
    val formatted = PhoneNumberInfoHelper.formatDisplayNumber(state.number)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF476378),
                        Color(0xFF3E4B3C),
                        Color(0xFF1F2B28)
                    )
                )
            )
            .testTag("outgoing_call_animation_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            RingingStatusPill(statusText = strings.ringingStatus)

            Text(
                text = state.displayName ?: state.number,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "$formatted • ${state.simLabel} • ${carrier.substringBefore(" ·")}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.86f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            CallingOrbitAnimation(
                centerText = (state.displayName?.firstOrNull() ?: state.number.firstOrNull() ?: '0')
                    .uppercaseChar()
                    .toString(),
                accentColor = simAccent,
                secondaryColor = Color(0xFFFDE68A),
                size = 260.dp,
                photoUri = state.photoUri
            )
        }
    }
}
