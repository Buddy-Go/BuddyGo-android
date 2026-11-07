package com.example.myapplication.ui.auth

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.R
import com.example.myapplication.data.repository.Repositories
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.ui.common.ActionButton
import com.example.myapplication.ui.common.InlineErrorBanner
import com.example.myapplication.ui.record.SignUpInfoState
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppSpace
import kotlinx.coroutines.launch

// 비밀번호 찾기: 이메일을 적으면 재설정 메일을 보낸다. 디자인이 없어 로그인 화면의 입력칸과 공용 버튼으로 만든 임시 화면이다.
// 가입된 이메일인지는 알려 주지 않고 같은 안내를 보여 준다. (실제 발송은 서버 몫)
data class FindPasswordState(
    val email: String = "",
    val isSending: Boolean = false,
    val sent: Boolean = false,
    val failed: Boolean = false,
) {
    val canSend: Boolean get() = SignUpInfoState.isValidEmail(email) && !isSending
}

class FindPasswordViewModel(private val repository: UserRepository = Repositories.users) : ViewModel() {
    var state by mutableStateOf(FindPasswordState())
        private set

    fun onEmailChange(email: String) {
        state = state.copy(email = email, failed = false)
    }

    fun onSend() {
        if (!state.canSend) return
        state = state.copy(isSending = true, failed = false)
        viewModelScope.launch {
            val result = repository.requestPasswordReset(state.email.trim())
            state = state.copy(isSending = false, sent = result.isSuccess, failed = result.isFailure)
        }
    }
}

@Composable
fun FindPasswordScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FindPasswordViewModel = viewModel(factory = viewModelFactory { initializer { FindPasswordViewModel() } }),
) {
    val state = viewModel.state
    Column(
        modifier
            .fillMaxSize()
            .background(AppColor.Surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = AppSpace.xxl), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "뒤로 가기",
                tint = AppColor.TextPrimary,
                modifier = Modifier.size(24.dp).clickable(onClick = onBack),
            )
            Spacer(Modifier.size(AppSpace.lg))
            Text("비밀번호 찾기", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        }
        Column(Modifier.weight(1f).padding(horizontal = AppSpace.x4), verticalArrangement = Arrangement.spacedBy(AppSpace.xxl)) {
            Spacer(Modifier.height(AppSpace.xl))
            if (state.sent) {
                Text("메일을 보냈어요", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                Text(
                    "가입된 이메일이라면 비밀번호를 다시 정하는 메일이 도착해요.\n메일이 안 보이면 스팸함도 확인해 주세요.",
                    fontSize = 15.sp, lineHeight = 22.sp, color = AppColor.TextSecondary,
                )
            } else {
                Text("가입한 이메일을 알려 주세요", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                Text("비밀번호를 다시 정하는 메일을 보내 드려요.", fontSize = 15.sp, color = AppColor.TextSecondary)
                if (state.failed) InlineErrorBanner("메일을 보내지 못했어요. 다시 시도해주세요.")
                LoginField(
                    label = "이메일",
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    keyboardType = KeyboardType.Email,
                    leading = {},
                )
            }
        }
        Box(Modifier.padding(horizontal = AppSpace.x4, vertical = AppSpace.xxl)) {
            if (state.sent) {
                ActionButton(text = "로그인으로 돌아가기", onClick = onBack)
            } else {
                ActionButton(
                    text = if (state.failed) "다시 시도" else "재설정 메일 보내기",
                    loadingText = "보내는 중",
                    loading = state.isSending,
                    enabled = state.canSend || state.isSending,
                    onClick = viewModel::onSend,
                )
            }
        }
    }
}
