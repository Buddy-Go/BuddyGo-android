package com.example.myapplication.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.Gender
import com.example.myapplication.ui.common.ActionButton
import com.example.myapplication.ui.common.EyeIcon
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.MyApplicationTheme

// 2a 회원가입 · 정보 입력 (1 / 3)
@Composable
fun SignUpInfoScreen(
    onBack: () -> Unit = {},
    onNext: () -> Unit = {},
    viewModel: SignUpInfoViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    SignUpInfoContent(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onNicknameChange = viewModel::onNicknameChange,
        onNicknameCheck = viewModel::onNicknameCheck,
        onEmailChange = viewModel::onEmailChange,
        onEmailFocusLost = viewModel::onEmailFocusLost,
        onPasswordChange = viewModel::onPasswordChange,
        onPasswordFocusLost = viewModel::onPasswordFocusLost,
        onPasswordConfirmChange = viewModel::onPasswordConfirmChange,
        onTogglePasswordVisible = viewModel::onTogglePasswordVisible,
        onToggleConfirmVisible = viewModel::onToggleConfirmVisible,
        onGenderSelect = viewModel::onGenderSelect,
        onNext = { if (viewModel.onNext()) onNext() },
    )
}

@Composable
private fun SignUpInfoContent(
    state: SignUpInfoState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onNicknameCheck: () -> Unit,
    onEmailChange: (String) -> Unit,
    onEmailFocusLost: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordFocusLost: () -> Unit,
    onPasswordConfirmChange: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onToggleConfirmVisible: () -> Unit,
    onGenderSelect: (Gender) -> Unit,
    onNext: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(AppColor.Surface).statusBarsPadding().imePadding()) {
        SignUpHeader(step = 1, total = 3, onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpace.x4),
        ) {
            Spacer(Modifier.height(AppSpace.xl))
            Text("기본 정보를 입력해주세요", color = AppColor.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(AppSpace.xxl))
            Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xxl)) {
                FormTextField("이름", state.name, onNameChange)
                FormTextField(
                    label = "닉네임 · 보여질 이름",
                    value = state.nickname,
                    onValueChange = onNicknameChange,
                    trailing = {
                        FieldChipButton(
                            text = "중복확인",
                            enabled = state.nickname.isNotBlank() && state.nicknameCheck != NicknameCheck.Checking,
                            onClick = onNicknameCheck,
                        )
                    },
                    message = state.nicknameMessage,
                )
                FormTextField(
                    label = "이메일",
                    value = state.email,
                    onValueChange = onEmailChange,
                    keyboardType = KeyboardType.Email,
                    leading = { MailIcon() },
                    message = state.emailMessage,
                    onFocusLost = onEmailFocusLost,
                )
                FormTextField(
                    label = "비밀번호",
                    value = state.password,
                    onValueChange = onPasswordChange,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing = { EyeIcon(crossed = state.passwordVisible, modifier = Modifier.clickable(onClick = onTogglePasswordVisible)) },
                    hint = "영문·숫자·특수문자 조합 8자 이상",
                    hintIsError = state.passwordRuleError,
                    onFocusLost = onPasswordFocusLost,
                )
                FormTextField(
                    label = "비밀번호 확인",
                    value = state.passwordConfirm,
                    onValueChange = onPasswordConfirmChange,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (state.confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing = {
                        if (state.passwordMatches) CheckMark(AppColor.AccentStrong)
                        else EyeIcon(crossed = state.confirmVisible, modifier = Modifier.clickable(onClick = onToggleConfirmVisible))
                    },
                    message = state.confirmMessage,
                )
                Column {
                    FormLabel("성별", required = true)
                    Spacer(Modifier.height(AppSpace.md))
                    ChoiceRow(
                        options = listOf(Gender.MALE to "남성", Gender.FEMALE to "여성"),
                        selected = state.gender,
                        onSelect = onGenderSelect,
                    )
                }
            }
            Spacer(Modifier.height(AppSpace.x4))
        }
        HorizontalDivider(color = AppColor.Border)
        ActionButton(
            text = "다음",
            onClick = onNext,
            enabled = state.canProceed,
            modifier = Modifier.padding(AppSpace.x4),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignUpInfoPreview() {
    MyApplicationTheme(dynamicColor = false) {
        SignUpInfoContent(
            state = SignUpInfoState(
                name = "김민준", nickname = "민준", email = "minjun.kim@gmail.com",
                password = "abcd1234!", passwordConfirm = "abcd1234!", gender = Gender.MALE,
                nicknameCheck = NicknameCheck.Available, emailCheck = EmailCheck.Available,
            ),
            onBack = {}, onNameChange = {}, onNicknameChange = {}, onNicknameCheck = {}, onEmailChange = {},
            onEmailFocusLost = {}, onPasswordChange = {}, onPasswordFocusLost = {}, onPasswordConfirmChange = {},
            onTogglePasswordVisible = {}, onToggleConfirmVisible = {}, onGenderSelect = {}, onNext = {},
        )
    }
}
