package com.example.myapplication.ui.meeting.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 신청 탭 ← 7a 신청 있음, 7c 신청 없음
@Composable
fun ApplicantsTab(
    applicants: List<Applicant>,
    processingApplicationId: String?,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (applicants.isEmpty()) {
        // 7c
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "아직 들어온 신청이 없어요", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary)
        }
        return
    }
    Column(modifier = modifier) {
        applicants.forEachIndexed { index, applicant ->
            if (index > 0) ListDivider()
            ApplicantRow(
                applicant = applicant,
                buttonsEnabled = processingApplicationId == null,
                onAccept = { onAccept(applicant.applicationId) },
                onReject = { onReject(applicant.applicationId) },
            )
        }
        HintText(
            text = "수락하면 확정 인원에 바로 반영돼요. 정원이 차면 자동으로 마감돼요",
            modifier = Modifier.padding(top = AppSpace.xxl),
        )
    }
}

@Composable
private fun ApplicantRow(
    applicant: Applicant,
    buttonsEnabled: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpace.xxl),
        horizontalArrangement = Arrangement.spacedBy(AppSpace.xl),
    ) {
        InitialAvatar(applicant.nickname)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppSpace.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpace.sm),
            ) {
                NicknameText(applicant.nickname)
                Text(
                    text = "· ${applicant.recordLabel}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColor.TextSecondary,
                )
            }
            MannerText(applicant.mannerLabel)
            if (!applicant.message.isNullOrBlank()) {
                Text(
                    text = applicant.message,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColor.TextBody,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpace.md, Alignment.End),
            ) {
                SmallButton(
                    text = "거절",
                    background = AppColor.Surface,
                    textColor = AppColor.TextPrimary,
                    bordered = true,
                    enabled = buttonsEnabled,
                    onClick = onReject,
                )
                SmallButton(
                    text = "수락",
                    background = AppColor.Accent,
                    textColor = AppColor.OnAccent,
                    bordered = false,
                    enabled = buttonsEnabled,
                    onClick = onAccept,
                )
            }
        }
    }
}

// 7a 거절·수락 (64×36)
@Composable
private fun SmallButton(
    text: String,
    background: Color,
    textColor: Color,
    bordered: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(AppRadius.sm)
    Box(
        modifier = Modifier
            .width(64.dp)
            .height(36.dp)
            .clip(shape)
            .background(background)
            .then(if (bordered) Modifier.border(1.dp, AppColor.Border, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) textColor else AppColor.TextDisabled,
        )
    }
}
