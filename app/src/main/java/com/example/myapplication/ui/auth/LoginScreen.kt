package com.example.myapplication.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.common.EyeIcon
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.MyApplicationTheme

// 1 로그인 기본
// 입력란 라벨은 디자인의 '아이디' 대신 서비스 규칙에 따라 '이메일'로 쓴다.
@Composable
fun LoginScreen(
    onLoginClick: (email: String, password: String, keepLogin: Boolean) -> Unit = { _, _, _ -> },
    onSignUpClick: () -> Unit = {},
    onFindPasswordClick: () -> Unit = {},
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var keepLogin by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.Surface)
            .padding(horizontal = AppSpace.x5),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LogoMark()
            Spacer(Modifier.width(AppSpace.xxl))
            Text(
                text = "BuddyGo",
                color = AppColor.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(AppSpace.x4))
        Text(
            text = "Find your buddy, and go.",
            color = AppColor.TextSecondary,
            fontSize = 15.sp,
        )

        Spacer(Modifier.height(48.dp))

        LoginField(
            label = "이메일",
            value = email,
            onValueChange = { email = it },
            keyboardType = KeyboardType.Email,
            leading = {
                PersonIcon()
            },
        )
        Spacer(Modifier.height(AppSpace.xl))
        LoginField(
            label = "비밀번호",
            value = password,
            onValueChange = { password = it },
            keyboardType = KeyboardType.Password,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            leading = {
                LockIcon()
            },
            trailing = {
                EyeIcon(
                    crossed = !passwordVisible,
                    modifier = Modifier.clickable { passwordVisible = !passwordVisible },
                )
            },
        )

        Spacer(Modifier.height(AppSpace.xxl))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(AppRadius.lg))
                .background(AppColor.Accent)
                .clickable { onLoginClick(email, password, keepLogin) },
            contentAlignment = Alignment.Center,
        ) {
            Text("로그인", color = AppColor.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(AppSpace.xl))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.clickable { keepLogin = !keepLogin },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (keepLogin) AppColor.Accent else AppColor.Surface)
                        .border(
                            BorderStroke(1.5.dp, if (keepLogin) AppColor.Accent else AppColor.Border),
                            RoundedCornerShape(6.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (keepLogin) {
                        CheckIcon()
                    }
                }
                Spacer(Modifier.width(AppSpace.md))
                Text("로그인 상태 유지", color = AppColor.TextPrimary, fontSize = 14.sp)
            }
            // 비밀번호 찾기는 메일 발송이 필요해 추후 구현 (service-rules 1장)
            Text(
                text = "비밀번호 찾기",
                color = AppColor.TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onFindPasswordClick() },
            )
        }

        Spacer(Modifier.height(AppSpace.x5))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("처음이신가요? ", color = AppColor.TextSecondary, fontSize = 14.sp)
            Text(
                text = "회원가입",
                color = AppColor.AccentStrong,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSignUpClick() },
            )
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
internal fun LoginField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(modifier) {
        Text(
            text = label,
            color = if (focused) AppColor.AccentStrong else AppColor.TextSecondary,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(AppSpace.md))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            interactionSource = interaction,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(color = AppColor.TextPrimary, fontSize = 17.sp),
            cursorBrush = SolidColor(AppColor.AccentStrong),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(AppRadius.lg))
                        .border(
                            BorderStroke(
                                if (focused) 2.dp else 1.dp,
                                if (focused) AppColor.Accent else AppColor.Border,
                            ),
                            RoundedCornerShape(AppRadius.lg),
                        )
                        .padding(horizontal = AppSpace.x4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leading()
                    Spacer(Modifier.width(AppSpace.xxl))
                    Box(Modifier.weight(1f)) { inner() }
                    if (trailing != null) trailing()
                }
            },
        )
    }
}

// BuddyGo 심볼(B+G). 바탕 없이 심볼만 보여 준다.
@Composable
private fun LogoMark() {
    Image(
        painter = painterResource(R.drawable.ic_buddygo_logo),
        contentDescription = "BuddyGo",
        modifier = Modifier.size(64.dp),
    )
}

@Composable
private fun PersonIcon() {
    Canvas(Modifier.size(22.dp)) {
        val sw = 1.8.dp.toPx()
        drawCircle(AppColor.IconMuted, radius = size.width * 0.2f, center = Offset(size.width / 2, size.height * 0.32f), style = Stroke(sw))
        drawArc(
            AppColor.IconMuted, 180f, 180f, false,
            Offset(size.width * 0.15f, size.height * 0.6f), Size(size.width * 0.7f, size.height * 0.7f),
            style = Stroke(sw),
        )
    }
}

@Composable
private fun LockIcon() {
    Canvas(Modifier.size(22.dp)) {
        val c = AppColor.AccentStrong
        val sw = 1.8.dp.toPx()
        drawArc(c, 180f, 180f, false, Offset(size.width * 0.28f, size.height * 0.1f), Size(size.width * 0.44f, size.height * 0.5f), style = Stroke(sw))
        drawRoundRect(c, Offset(size.width * 0.15f, size.height * 0.42f), Size(size.width * 0.7f, size.height * 0.48f), CornerRadius(size.width * 0.1f), style = Stroke(sw))
    }
}

@Composable
private fun CheckIcon() {
    Canvas(Modifier.size(16.dp)) {
        val sw = 2.dp.toPx()
        drawLine(Color.White, Offset(size.width * 0.2f, size.height * 0.52f), Offset(size.width * 0.42f, size.height * 0.74f), sw, StrokeCap.Round)
        drawLine(Color.White, Offset(size.width * 0.42f, size.height * 0.74f), Offset(size.width * 0.8f, size.height * 0.28f), sw, StrokeCap.Round)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginScreenPreview() {
    MyApplicationTheme(dynamicColor = false) {
        LoginScreen()
    }
}
