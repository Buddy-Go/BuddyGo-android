package com.example.myapplication.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// TODO: 색 값은 로그인(1) 스크린샷에서 추정한 값. 16번 프레임 캡션 기준 값으로 확정되면 교체한다.
object AppColor {
    val Accent = Color(0xFFA3E635)
    val AccentStrong = Color(0xFF4D7C0F)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF7F7F8)
    val TextPrimary = Color(0xFF18181B)
    val TextSecondary = Color(0xFF71717A)
    val TextDisabled = Color(0xFFA1A1AA)
    val Border = Color(0xFFE4E4E7)
    // 16번 프레임 캡션 값
    val SkeletonBase = Color(0xFFEBECEE)
    val SkeletonShimmer = Color(0xFFF4F4F5)
    val IconMuted = Color(0xFF71717A)

    // ---- 아래는 흐름 A 화면(문진곤)에서 추가. 값은 디자인 v4 HTML의 CSS 변수·프레임에서 가져왔다. ----
    // (추정) = 디자인에 이름 없이 쓰인 값
    val AccentSoft = Color(0xFFF1FBE0) // 선택된 칩·카드 바탕
    val OnAccent = Color(0xFF17171A) // Accent 위 글자
    val TextBody = Color(0xFF37383C) // (추정) 값·본문 글자. TextPrimary보다 연하고 TextSecondary보다 진함
    val ChipNeutralBg = Color(0xFFF4F4F5) // 회색 칩·입력칸 바탕
    val DisabledBg = Color(0xFFEBECEE) // 꺼진 큰 버튼 바탕 (16c)
    val Divider = Color(0xFFEEEEEE) // (추정) 섹션 구분선
    val DividerSoft = Color(0x2470737C) // (추정) rgba(112,115,124,.14) 목록·카드 안 구분선

    // 이니셜 원 (프로필 사진 없을 때)
    val AvatarBg = Color(0xFFEBECEE)
    val NeutralSoft = Color(0xFFE6E7E9) // (추정) 댓글 이니셜 원, 꺼진 전송 버튼 (8f)
    val HostAvatarBg = Color(0xFFDDEFD9) // (추정) 호스트 이니셜 원 (8f)
    val HostAvatarText = Color(0xFF00822F)
    val AvatarPurpleBg = Color(0xFFF4E9FF) // (추정) 12a
    val AvatarPurpleText = Color(0xFF7A3FD1)
    val AvatarBlueBg = Color(0xFFE8F3FF) // (추정) 12a
    val AvatarBlueText = Color(0xFF0F70D6)

    // 긍정·평가
    val Positive = Color(0xFF00822F) // '● 신청자 2' (13-H)
    val ReviewCardBg = Color(0xFFF4FDF7) // 평가 대기 카드 바탕 (13-R)

    // 위험·오류
    val Danger = Color(0xFFE13030) // 모임 취소·삭제 버튼, 노쇼
    val DestructiveSoftBg = Color(0xFFFDECEC) // 취소 경고 상자 (5a-X2)
    val DestructiveText = Color(0xFFB3261E)

    // 바텀시트·토글
    val Scrim = Color(0x8017171A) // (추정) 시트 뒤 어둡게
    val Grabber = Color(0xFFDBDCDF) // (추정) 시트 손잡이, 꺼진 토글 바탕
}

object AppRadius {
    val xs = 4.dp // (추정) '내가 호스트' 같은 작은 태그 (7b, 8f) — 흐름 A에서 추가
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val sheet = 24.dp // (추정) 바텀시트 위쪽 모서리 — 흐름 A에서 추가
    val pill = 999.dp
}

object AppSpace {
    val xxs = 2.dp // (추정) 흐름 A에서 추가
    val xs = 4.dp // 흐름 A에서 추가
    val sm = 6.dp // 흐름 A에서 추가
    val md = 8.dp
    val lg = 10.dp // 흐름 A에서 추가
    val xl = 12.dp
    val xxl = 16.dp
    val x4 = 20.dp
    val x5 = 24.dp
}
