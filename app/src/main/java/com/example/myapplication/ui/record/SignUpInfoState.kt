package com.example.myapplication.ui.record

import com.example.myapplication.data.model.Gender

enum class NicknameCheck { Idle, Checking, Available, Taken, Failed }
enum class EmailCheck { Idle, Checking, Available, Taken, InvalidFormat, Failed }

// 입력란 아래에 보여 줄 한 줄 문구. isError = true이면 빨간 글자, false이면 초록 글자 + 체크.
data class FieldMessage(val text: String, val isError: Boolean)

// 2a 정보 입력 화면의 값. 값의 정의와 값에서 계산되는 것만 담고, 바꾸는 코드는 ViewModel에 둔다.
data class SignUpInfoState(
    val name: String = "",
    val nickname: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val gender: Gender? = null,
    val nicknameCheck: NicknameCheck = NicknameCheck.Idle,
    val emailCheck: EmailCheck = EmailCheck.Idle,
    val passwordTouched: Boolean = false, // 비밀번호 입력란을 한 번이라도 벗어났는지
    val passwordVisible: Boolean = false,
    val confirmVisible: Boolean = false,
) {
    val passwordRuleOk: Boolean get() = isValidPassword(password)

    val passwordMatches: Boolean get() = passwordConfirm.isNotEmpty() && passwordConfirm == password

    // 비밀번호 규칙을 어겼을 때만 안내 글자를 빨갛게 한다. (입력란을 벗어난 뒤부터)
    val passwordRuleError: Boolean get() = passwordTouched && password.isNotEmpty() && !passwordRuleOk

    val nicknameMessage: FieldMessage?
        get() = when (nicknameCheck) {
            NicknameCheck.Available -> FieldMessage("모임에서 이 이름으로 보여요", isError = false)
            NicknameCheck.Taken -> FieldMessage("이미 사용 중인 닉네임이에요", isError = true)
            NicknameCheck.Failed -> FieldMessage("확인하지 못했어요. 다시 시도해주세요", isError = true)
            else -> null
        }

    val emailMessage: FieldMessage?
        get() = when (emailCheck) {
            EmailCheck.InvalidFormat -> FieldMessage("이메일 형식이 맞지 않아요", isError = true)
            EmailCheck.Taken -> FieldMessage("이미 가입된 이메일이에요", isError = true)
            EmailCheck.Failed -> FieldMessage("확인하지 못했어요. 다시 시도해주세요", isError = true)
            else -> null
        }

    val confirmMessage: FieldMessage?
        get() = if (passwordConfirm.isNotEmpty() && !passwordMatches) {
            FieldMessage("비밀번호가 일치하지 않아요", isError = true)
        } else {
            null
        }

    // 다음 버튼은 모든 조건이 맞을 때만 켠다.
    val canProceed: Boolean
        get() = name.isNotBlank() &&
            nickname.isNotBlank() && nicknameCheck == NicknameCheck.Available &&
            emailCheck == EmailCheck.Available &&
            passwordRuleOk && passwordMatches &&
            gender != null

    companion object {
        private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

        fun isValidEmail(email: String): Boolean = EMAIL.matches(email.trim())

        // 영문·숫자·특수문자를 모두 포함한 8자 이상 (service-rules 1장)
        fun isValidPassword(password: String): Boolean =
            password.length >= 8 &&
                password.any { it.isLetter() } &&
                password.any { it.isDigit() } &&
                password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
    }
}
