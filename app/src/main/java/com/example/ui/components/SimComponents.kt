package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SimSlotInfo
import com.example.data.telephony.PhoneNumberInfoHelper
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalPictureTheme

@Composable
fun DualSimCallBar(
    simSlots: List<SimSlotInfo>,
    onCallSlot: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pictureTheme = LocalPictureTheme.current
    val sim1 = simSlots.getOrNull(0)
    val sim2 = simSlots.getOrNull(1)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SimCallButton(
            slotNumber = 1,
            carrierName = sim1?.carrierName ?: "SIM 1",
            isAvailable = sim1?.isActive ?: true,
            accentColor = pictureTheme.sim1Accent,
            onClick = { onCallSlot(0) },
            modifier = Modifier
                .weight(1f)
                .testTag("call_sim_1_button")
        )

        SimCallButton(
            slotNumber = 2,
            carrierName = sim2?.carrierName ?: "SIM 2",
            isAvailable = sim2?.isActive ?: true,
            accentColor = Color(0xFF2563EB),
            onClick = { onCallSlot(1) },
            modifier = Modifier
                .weight(1f)
                .testTag("call_sim_2_button")
        )
    }
}

@Composable
private fun SimCallButton(
    slotNumber: Int,
    carrierName: String,
    isAvailable: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isAvailable) accentColor else accentColor.copy(alpha = 0.25f)
    val contentColor = if (isAvailable) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        shadowElevation = if (isAvailable) 6.dp else 0.dp,
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = "Call using SIM $slotNumber",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "SIM $slotNumber",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1
                )
                Text(
                    text = carrierName,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SimBadgeChip(
    simSlotIndex: Int?,
    modifier: Modifier = Modifier
) {
    if (simSlotIndex == null) return
    val badgeColor = if (simSlotIndex == 0) Color(0xFF10B981) else Color(0xFF2563EB)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = badgeColor.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Text(
            text = "SIM ${simSlotIndex + 1}",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = badgeColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimPickerBottomSheet(
    phoneNumber: String,
    simSlots: List<SimSlotInfo>,
    onSelectSlot: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val sim1Color = Color(0xFF10B981)
    val sim2Color = Color(0xFF2563EB)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.callWith,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = PhoneNumberInfoHelper.formatDisplayNumber(phoneNumber),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = sim1Color
            )
            Spacer(Modifier.height(4.dp))

            val slot1 = simSlots.getOrNull(0)
            val slot2 = simSlots.getOrNull(1)

            SimPickerOptionCard(
                slotNumber = 1,
                carrierName = slot1?.carrierName ?: "SIM 1",
                isActive = slot1?.isActive ?: true,
                accentColor = sim1Color,
                onClick = { onSelectSlot(0) },
                modifier = Modifier.testTag("sheet_sim_1_option")
            )
            SimPickerOptionCard(
                slotNumber = 2,
                carrierName = slot2?.carrierName ?: "SIM 2",
                isActive = slot2?.isActive ?: true,
                accentColor = sim2Color,
                onClick = { onSelectSlot(1) },
                modifier = Modifier.testTag("sheet_sim_2_option")
            )
        }
    }
}

@Composable
private fun SimPickerOptionCard(
    slotNumber: Int,
    carrierName: String,
    isActive: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = accentColor.copy(alpha = 0.09f),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.18f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.SimCard,
                        contentDescription = "SIM $slotNumber",
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SIM $slotNumber",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isActive) carrierName else strings.noSimDetected,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
