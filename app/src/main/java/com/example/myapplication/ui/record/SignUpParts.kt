package com.example.myapplication.ui.record

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 회원가입 3단계(2a, 2b, 2c~)가 같이 쓰는 위쪽 줄: 뒤로 가기 + 제목 + '단계 / 전체' + 진행 막대
@Composable
fun SignUpHeader(step: Int, total: Int, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = AppSpace.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(width = 10.dp, height = 18.dp)) {
                    val sw = 2.2.dp.toPx()
                    drawLine(AppColor.TextPrimary, Offset(size.width, 0f), Offset(0f, size.height / 2), sw, StrokeCap.Round)
                    drawLine(AppColor.TextPrimary, Offset(0f, size.height / 2), Offset(size.width, size.height), sw, StrokeCap.Round)
                }
            }
            Spacer(Modifier.width(AppSpace.md))
            Text("회원가입", color = AppColor.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text("$step / $total", color = AppColor.TextDisabled, fontSize = 15.sp, modifier = Modifier.padding(end = AppSpace.md))
        }
        Row(Modifier.fillMaxWidth().height(3.dp).background(AppColor.SurfaceMuted)) {
            Box(Modifier.weight(step.toFloat()).height(3.dp).background(AppColor.Accent))
            if (total > step) Spacer(Modifier.weight((total - step).toFloat()))
        }
    }
}

// 입력란 이름. required = true이면 뒤에 초록 별표
@Composable
fun FormLabel(text: String, required: Boolean, focused: Boolean = false) {
    val color = if (focused) AppColor.AccentStrong else AppColor.TextSecondary
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = color)) { append(text) }
            if (required) withStyle(SpanStyle(color = AppColor.AccentStrong, fontWeight = FontWeight.Bold)) { append(" *") }
        },
        fontSize = 14.sp,
    )
}

// 입력란 + 이름 + 아래 한 줄 문구(성공은 초록 체크, 오류는 빨간 글자, 안내는 회색)
@Composable
fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    message: FieldMessage? = null,
    hint: String? = null,
    hintIsError: Boolean = false,
    onFocusLost: () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var hadFocus by remember { mutableStateOf(false) }
    // 포커스를 잃는 순간 한 번 알려 준다.
    LaunchedEffect(focused) {
        if (focused) hadFocus = true
        else if (hadFocus) { hadFocus = false; onFocusLost() }
    }
    Column(modifier) {
        FormLabel(label, required, focused)
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
                        .height(48.dp)
                        .clip(RoundedCornerShape(AppRadius.md))
                        .border(
                            BorderStroke(if (focused) 2.dp else 1.dp, if (focused) AppColor.Accent else AppColor.Border),
                            RoundedCornerShape(AppRadius.md),
                        )
                        .padding(start = AppSpace.xxl, end = AppSpace.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leading != null) { leading(); Spacer(Modifier.width(AppSpace.xl)) }
                    Box(Modifier.weight(1f)) { inner() }
                    if (trailing != null) { Spacer(Modifier.width(AppSpace.md)); trailing() }
                }
            },
        )
        if (message != null) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!message.isError) { CheckMark(AppColor.AccentStrong, 14.dp); Spacer(Modifier.width(6.dp)) }
                Text(message.text, color = if (message.isError) AppColor.DestructiveText else AppColor.AccentStrong, fontSize = 13.sp)
            }
        } else if (hint != null) {
            Spacer(Modifier.height(6.dp))
            Text(hint, color = if (hintIsError) AppColor.DestructiveText else AppColor.TextDisabled, fontSize = 13.sp)
        }
    }
}

// 남성 / 여성 같은 두 칸 선택. 선택된 칸은 연한 라임 바탕 + 초록 테두리·굵은 글자
@Composable
fun <T> ChoiceRow(options: List<Pair<T, String>>, selected: T?, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
        options.forEach { (value, label) ->
            val on = value == selected
            val shape = RoundedCornerShape(AppRadius.md)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(shape)
                    .background(if (on) AppColor.AccentSoft else AppColor.Surface)
                    .border(1.dp, if (on) AppColor.Accent else AppColor.Border, shape)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (on) AppColor.AccentStrong else AppColor.TextSecondary,
                    fontSize = 16.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
fun CheckMark(color: Color, size: androidx.compose.ui.unit.Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val sw = 2.dp.toPx()
        drawLine(color, Offset(this.size.width * .12f, this.size.height * .55f), Offset(this.size.width * .4f, this.size.height * .82f), sw, StrokeCap.Round)
        drawLine(color, Offset(this.size.width * .4f, this.size.height * .82f), Offset(this.size.width * .9f, this.size.height * .22f), sw, StrokeCap.Round)
    }
}

@Composable
fun MailIcon() {
    Canvas(Modifier.size(22.dp)) {
        val c = AppColor.IconMuted
        val sw = 1.8.dp.toPx()
        drawRoundRect(c, Offset(size.width * .1f, size.height * .22f), Size(size.width * .8f, size.height * .56f), CornerRadius(size.width * .1f), style = Stroke(sw))
        drawLine(c, Offset(size.width * .12f, size.height * .28f), Offset(size.width * .5f, size.height * .56f), sw, StrokeCap.Round)
        drawLine(c, Offset(size.width * .88f, size.height * .28f), Offset(size.width * .5f, size.height * .56f), sw, StrokeCap.Round)
    }
}

// 입력란 안쪽 오른쪽의 작은 버튼 (예: 닉네임 '중복확인')
@Composable
fun FieldChipButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(AppRadius.md - 2.dp))
            .background(if (enabled) AppColor.AccentSoft else AppColor.SurfaceMuted)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = AppSpace.xl + 2.dp, vertical = AppSpace.xl),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) AppColor.AccentStrong else AppColor.TextDisabled, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
