package com.example.myapplication.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// TODO(임시) 아직 만들지 않은 화면 자리. 화면을 만들면 AppNavHost에서 이 호출을 실제 화면으로 바꾼다.
@Composable
fun TempScreen(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title)
    }
}
