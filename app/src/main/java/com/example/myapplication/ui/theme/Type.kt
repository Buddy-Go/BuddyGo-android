package com.example.myapplication.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.myapplication.R

// 앱 전체 글꼴: SUIT (SUNN, SIL Open Font License 1.1). 라이선스는 docs/licenses/SUIT-OFL.txt 와 앱 assets/licenses 에 있다.
// 앱이 쓰는 굵기(Normal, Medium, SemiBold, Bold, ExtraBold)만 넣었다.
val SuitFontFamily = FontFamily(
    Font(R.font.suit_regular, FontWeight.Normal),
    Font(R.font.suit_medium, FontWeight.Medium),
    Font(R.font.suit_semibold, FontWeight.SemiBold),
    Font(R.font.suit_bold, FontWeight.Bold),
    Font(R.font.suit_extrabold, FontWeight.ExtraBold),
)

private val DefaultTypography = Typography()

// Material 글자 스타일 전부에 SUIT를 입힌다. 화면의 Text는 이 스타일을 이어받는다.
// (BasicTextField처럼 Material 스타일을 이어받지 않는 곳은 TextStyle에 SuitFontFamily를 직접 넣는다)
val Typography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = SuitFontFamily),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = SuitFontFamily),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = SuitFontFamily),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = SuitFontFamily),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = SuitFontFamily),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = SuitFontFamily),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = SuitFontFamily),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = SuitFontFamily),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = SuitFontFamily),
    bodyLarge = TextStyle(
        fontFamily = SuitFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = SuitFontFamily),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = SuitFontFamily),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = SuitFontFamily),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = SuitFontFamily),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = SuitFontFamily),
)
