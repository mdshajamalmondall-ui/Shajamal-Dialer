package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CallerIdentityReport
import com.example.data.model.CallerTrustStatus
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings

@Composable
fun IncomingCallerIdCard(
    report: CallerIdentityReport,
    simLabel: String?,
    onDecline: () -> Unit,
    onMessage: () -> Unit,
    onAnswer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    val (badgeBg, badgeTextColor, accentColor, avatarIcon) = when (report.status) {
        CallerTrustStatus.VERIFIED_BUSINESS -> QuadStyle(
            Color(0xFF0A331C),
            Color(0xFF34D399),
            Color(0xFF22C55E),
            Icons.Rounded.Business
        )
        CallerTrustStatus.FRAUD_WARNING, CallerTrustStatus.BLOCKED -> QuadStyle(
            Color(0xFF450A0A),
            Color(0xFFFCA5A5),
            Color(0xFFEF4444),
            Icons.Rounded.ReportProblem
        )
        CallerTrustStatus.SPAM_SUSPECTED -> QuadStyle(
            Color(0xFF451A03),
            Color(0xFFFCD34D),
            Color(0xFFF59E0B),
            Icons.Rounded.ReportProblem
        )
        CallerTrustStatus.SENSITIVE_PROTECTED -> QuadStyle(
            Color(0xFF1E1B4B),
            Color(0xFFA5B4FC),
            Color(0xFF818CF8),
            Icons.Rounded.Lock
        )
        CallerTrustStatus.VERIFIED_CONTACT -> QuadStyle(
            Color(0xFF0A331C),
            Color(0xFF6EE7B7),
            Color(0xFF10B981),
            Icons.Rounded.Person
        )
        CallerTrustStatus.UNKNOWN_CLEAN -> QuadStyle(
            Color(0xFF1E293B),
            Color(0xFFCBD5E1),
            Color(0xFF38BDF8),
            Icons.Rounded.Person
        )
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF0A0C0E).copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        shadowElevation = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("incoming_caller_id_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top Header: "Incoming Call" + Trust / Business / Spam / Fraud Pill Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = strings.incomingCallHeader,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (!simLabel.isNullOrBlank()) {
                        Text(
                            text = simLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeTextColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = report.statusBadge,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Middle Row: Circular Business/Photo/Alert Icon + Caller Title, Number, Verification Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF272A2E),
                    border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.45f)),
                    modifier = Modifier.size(66.dp)
                ) {
                    if (!report.photoUri.isNullOrBlank() && !report.isSensitiveMasked) {
                        AsyncImage(
                            model = report.photoUri,
                            contentDescription = report.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = avatarIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = report.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = report.formattedNumber,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = report.subStatusLine,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

            // Reputation / Spam Report Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = when (report.status) {
                        CallerTrustStatus.FRAUD_WARNING, CallerTrustStatus.SPAM_SUSPECTED -> Icons.Rounded.ReportProblem
                        CallerTrustStatus.BLOCKED -> Icons.Rounded.Block
                        CallerTrustStatus.VERIFIED_BUSINESS, CallerTrustStatus.VERIFIED_CONTACT -> Icons.Rounded.VerifiedUser
                        else -> Icons.Rounded.Shield
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = report.reputationLine,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Three Circular Action Buttons matching screenshot: Red Decline, Grey Message, Green Answer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF2C2E30),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f)),
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDecline)
                        .testTag("incoming_card_decline_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CallEnd,
                            contentDescription = strings.declineCall,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF2C2E30),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onMessage)
                        .testTag("incoming_card_message_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = strings.messageLabel,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0A331C),
                    border = BorderStroke(1.2.dp, Color(0xFF22C55E).copy(alpha = 0.6f)),
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onAnswer)
                        .testTag("incoming_card_answer_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = strings.slideToAnswer,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class QuadStyle(
    val badgeBg: Color,
    val badgeText: Color,
    val accent: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
