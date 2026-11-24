package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.OutgoingAnimationState
import com.example.data.telephony.PhoneNumberInfoHelper
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalPictureTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CallingOrbitAnimation(
    centerText: String,
    accentColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 250.dp,
    photoUri: String? = null
) {
    val context = LocalContext.current
    val transition = rememberInfiniteTransition(label = "calling_orbit_premium")

    val primaryOrbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "primary_orbit"
    )

    val counterOrbit by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_orbit"
    )

    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "acoustic_wave_phase"
    )

    val auroraBreath by transition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_breath"
    )

    val goldWarm = secondaryColor
    val champagne = Color(0xFFFFF3C4)
    val emeraldGlow = Color(0xFF34D399)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val minDim = this.size.minDimension
                    val center = Offset(this.size.width * 0.5f, this.size.height * 0.5f)
                    val avatarRadius = minDim * 0.23f
                    val bezelInnerRadius = minDim * 0.275f
                    val mainRingRadius = minDim * 0.345f
                    val outerArcRadius = minDim * 0.415f
                    val maxRippleRadius = minDim * 0.485f

                    val auroraBrush = Brush.radialGradient(
                        colors = listOf(
                            champagne.copy(alpha = 0.34f),
                            accentColor.copy(alpha = 0.26f),
                            emeraldGlow.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRippleRadius
                    )
                    val primarySweepBrush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.15f to emeraldGlow.copy(alpha = 0.48f),
                        0.34f to goldWarm.copy(alpha = 0.94f),
                        0.45f to Color.White,
                        0.47f to Color.Transparent,
                        center = center
                    )
                    val crescentSweepBrush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.11f to goldWarm.copy(alpha = 0.72f),
                        0.22f to champagne,
                        0.24f to Color.Transparent,
                        center = center
                    )

                    val trackStroke = Stroke(width = 1.4.dp.toPx())
                    val cometStroke = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                    val crescentStroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                    val tickStrokeWidth = 1.6.dp.toPx()
                    val baseTickLen = 3.dp.toPx()
                    val deltaTickLen = 5.dp.toPx()

                    val primaryTopLeft = Offset(center.x - mainRingRadius, center.y - mainRingRadius)
                    val primaryArcSize = Size(mainRingRadius * 2f, mainRingRadius * 2f)
                    val outerTopLeft = Offset(center.x - outerArcRadius, center.y - outerArcRadius)
                    val outerArcSize = Size(outerArcRadius * 2f, outerArcRadius * 2f)

                    val headAngleRad = 162.0 * (PI / 180.0)
                    val headPos = Offset(
                        center.x + mainRingRadius * cos(headAngleRad).toFloat(),
                        center.y + mainRingRadius * sin(headAngleRad).toFloat()
                    )
                    val tipRad = 80.0 * (PI / 180.0)
                    val tipPos = Offset(
                        center.x + outerArcRadius * cos(tipRad).toFloat(),
                        center.y + outerArcRadius * sin(tipRad).toFloat()
                    )

                    val tickCount = 36
                    val tickAngles = DoubleArray(tickCount) { (it.toDouble() / tickCount) * 2.0 * PI }
                    val tickCos = FloatArray(tickCount) { cos(tickAngles[it]).toFloat() }
                    val tickSin = FloatArray(tickCount) { sin(tickAngles[it]).toFloat() }

                    val haloHaloRadius = 7.dp.toPx()
                    val haloCoreRadius = 3.2.dp.toPx()
                    val tipHaloRadius = 5.5.dp.toPx()
                    val tipCoreRadius = 2.4.dp.toPx()

                    onDrawBehind {
                        // 1. Soft breathing aurora halo behind the caller photo
                        drawCircle(
                            brush = auroraBrush,
                            radius = maxRippleRadius * auroraBreath,
                            alpha = (0.85f + 0.15f * (auroraBreath - 0.90f) / 0.16f).coerceIn(0f, 1f)
                        )

                        // 2. Three staggered acoustic sonar waves radiating smoothly outward
                        for (i in 0 until 3) {
                            val p = (wavePhase + i / 3f) % 1f
                            val rippleRadius = avatarRadius + (maxRippleRadius - avatarRadius) * p
                            val fade = (1f - p) * (1f - p)
                            drawCircle(
                                color = champagne.copy(alpha = 0.34f * fade),
                                radius = rippleRadius,
                                style = Stroke(width = (2.3f - 1.1f * p).dp.toPx())
                            )
                        }

                        // 3. Harmonic bezel ticks
                        val harmonicOffsetRad = (primaryOrbit * 2f) * (PI / 180.0)
                        for (i in 0 until tickCount) {
                            val harmonic = (sin(tickAngles[i] * 3.0 - harmonicOffsetRad) + 1.0).toFloat() * 0.5f
                            val outerR = bezelInnerRadius + baseTickLen + deltaTickLen * harmonic
                            val c = tickCos[i]
                            val s = tickSin[i]
                            drawLine(
                                color = Color.White.copy(alpha = 0.18f + 0.52f * harmonic),
                                start = Offset(center.x + bezelInnerRadius * c, center.y + bezelInnerRadius * s),
                                end = Offset(center.x + outerR * c, center.y + outerR * s),
                                strokeWidth = tickStrokeWidth,
                                cap = StrokeCap.Round
                            )
                        }

                        // 4. Frosted orbital glass track + sweeping luminous champagne/gold comet arc
                        drawCircle(
                            color = Color.White.copy(alpha = 0.14f),
                            radius = mainRingRadius,
                            style = trackStroke
                        )
                        rotate(degrees = primaryOrbit, pivot = center) {
                            drawArc(
                                brush = primarySweepBrush,
                                startAngle = 0f,
                                sweepAngle = 162f,
                                useCenter = false,
                                topLeft = primaryTopLeft,
                                size = primaryArcSize,
                                style = cometStroke
                            )
                            drawCircle(
                                color = champagne.copy(alpha = 0.45f),
                                radius = haloHaloRadius,
                                center = headPos
                            )
                            drawCircle(
                                color = Color.White,
                                radius = haloCoreRadius,
                                center = headPos
                            )
                        }

                        // 5. Counter-rotating dual golden outer crescents
                        for (startDeg in floatArrayOf(15f, 195f)) {
                            rotate(degrees = counterOrbit + startDeg, pivot = center) {
                                drawArc(
                                    brush = crescentSweepBrush,
                                    startAngle = 0f,
                                    sweepAngle = 80f,
                                    useCenter = false,
                                    topLeft = outerTopLeft,
                                    size = outerArcSize,
                                    style = crescentStroke
                                )
                                drawCircle(
                                    color = goldWarm.copy(alpha = 0.45f),
                                    radius = tipHaloRadius,
                                    center = tipPos
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = tipCoreRadius,
                                    center = tipPos
                                )
                            }
                        }
                    }
                }
        )

        // Caller Photo Avatar with frosted double-bezel halo
        Box(
            modifier = Modifier
                .size(size * 0.46f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .border(BorderStroke(2.5.dp, Color.White.copy(alpha = 0.78f)), CircleShape)
                .padding(3.dp)
                .clip(CircleShape)
                .border(BorderStroke(1.dp, champagne.copy(alpha = 0.65f)), CircleShape),
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
                val photoRequest = remember(photoUri) {
                    ImageRequest.Builder(context)
                        .data(photoUri)
                        .size(420)
                        .crossfade(false)
                        .build()
                }
                AsyncImage(
                    model = photoRequest,
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
