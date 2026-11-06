package com.example.myapplication.ui.record

import com.example.myapplication.data.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignUpInfoStateTest {
    private val ok = SignUpInfoState(
        name = "김민준", nickname = "민준", email = "a@b.com",
        password = "abcd1234!", passwordConfirm = "abcd1234!", gender = Gender.MALE,
        nicknameCheck = NicknameCheck.Available, emailCheck = EmailCheck.Available,
    )

    @Test
    fun 비밀번호는_영문_숫자_특수문자를_모두_포함한_8자_이상() {
        assertTrue(SignUpInfoState.isValidPassword("abcd1234!"))
        assertFalse("7자는 안 됨", SignUpInfoState.isValidPassword("abc12!x"))
        assertFalse("특수문자 없음", SignUpInfoState.isValidPassword("abcd12345"))
        assertFalse("숫자 없음", SignUpInfoState.isValidPassword("abcdefgh!"))
        assertFalse("영문 없음", SignUpInfoState.isValidPassword("12345678!"))
        assertFalse("공백은 특수문자가 아님", SignUpInfoState.isValidPassword("abcd1234 "))
    }

    @Test
    fun 이메일_형식() {
        assertTrue(SignUpInfoState.isValidEmail("minjun.kim@gmail.com"))
        assertTrue(SignUpInfoState.isValidEmail("  a@b.co  "))
        assertFalse(SignUpInfoState.isValidEmail("minjun"))
        assertFalse(SignUpInfoState.isValidEmail("minjun@gmail"))
        assertFalse(SignUpInfoState.isValidEmail("a b@c.com"))
    }

    @Test
    fun 모든_조건이_맞아야_다음으로_갈_수_있다() {
        assertTrue(ok.canProceed)
        assertFalse(ok.copy(name = " ").canProceed)
        assertFalse(ok.copy(gender = null).canProceed)
        assertFalse("닉네임 중복확인 전", ok.copy(nicknameCheck = NicknameCheck.Idle).canProceed)
        assertFalse("닉네임 중복", ok.copy(nicknameCheck = NicknameCheck.Taken).canProceed)
        assertFalse("이메일 확인 전", ok.copy(emailCheck = EmailCheck.Idle).canProceed)
        assertFalse("이메일 중복", ok.copy(emailCheck = EmailCheck.Taken).canProceed)
        assertFalse("비밀번호 불일치", ok.copy(passwordConfirm = "different1!").canProceed)
        assertFalse("비밀번호 규칙 위반", ok.copy(password = "short1!", passwordConfirm = "short1!").canProceed)
    }

    @Test
    fun 비밀번호_안내는_입력란을_벗어난_뒤_규칙을_어겼을_때만_빨갛다() {
        val weak = SignUpInfoState(password = "abc")
        assertFalse("아직 입력 중", weak.passwordRuleError)
        assertTrue(weak.copy(passwordTouched = true).passwordRuleError)
        assertFalse("비어 있으면 안내만", SignUpInfoState(passwordTouched = true).passwordRuleError)
        assertFalse(ok.copy(passwordTouched = true).passwordRuleError)
    }

    @Test
    fun 오류_문구() {
        assertEquals("이미 사용 중인 닉네임이에요", ok.copy(nicknameCheck = NicknameCheck.Taken).nicknameMessage?.text)
        assertFalse(ok.nicknameMessage!!.isError)
        assertEquals("이메일 형식이 맞지 않아요", ok.copy(emailCheck = EmailCheck.InvalidFormat).emailMessage?.text)
        assertNull(ok.emailMessage)
        assertEquals("비밀번호가 일치하지 않아요", ok.copy(passwordConfirm = "x").confirmMessage?.text)
        assertNull(ok.confirmMessage)
    }
}
