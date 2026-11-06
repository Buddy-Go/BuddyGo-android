package com.example.myapplication.ui.meeting.detail

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.ui.explore.formatDistance
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 5e 장소 시트 (모임 상세 위에 뜬다): 지도에 장소를 보여 주고, 주소 복사와 길찾기를 할 수 있다.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceBottomSheet(
    meeting: Meeting,
    distanceMeters: Int?,
    onCopyAddress: () -> Unit,
    onFindRoute: () -> Unit,
    onDismiss: () -> Unit,
) {
    val place = meeting.place
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColor.Surface,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = AppSpace.x4).padding(bottom = AppSpace.x4),
            verticalArrangement = Arrangement.spacedBy(AppSpace.xl),
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
                Text("장소", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                formatDistance(distanceMeters)?.let {
                    Text("내 위치에서 $it", fontSize = 15.sp, color = AppColor.TextSecondary, modifier = Modifier.padding(bottom = 3.dp))
                }
            }
            PlaceMiniMap(
                latitude = place.latitude, longitude = place.longitude, sport = meeting.sport,
                modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(AppRadius.lg))
                    .border(1.dp, AppColor.Border, RoundedCornerShape(AppRadius.lg)),
            )
            Text(place.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
            Text(fullAddress(meeting), fontSize = 15.sp, color = AppColor.TextSecondary, lineHeight = 22.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
                SheetButton("주소 복사", outlined = true, onClick = onCopyAddress, modifier = Modifier.weight(1f))
                SheetButton("길찾기", outlined = false, onClick = onFindRoute, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SheetButton(text: String, outlined: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(AppRadius.md)
    Surface(
        modifier = modifier.height(56.dp).clip(shape).clickable(role = Role.Button, onClick = onClick),
        shape = shape,
        color = if (outlined) AppColor.Surface else AppColor.Accent,
        border = if (outlined) androidx.compose.foundation.BorderStroke(1.dp, AppColor.Border) else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        }
    }
}
