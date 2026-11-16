package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.myapplication.ui.navigation.AppNavHost
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 앱 화면은 다크 모드에서도 밝은 바탕이라, 상태바·내비게이션 바 아이콘을 항상 어둡게 둔다.
        // (기본값은 폰이 다크 모드면 흰 아이콘이 돼서 흰 화면 위에서 시간·배터리가 안 보인다)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            MyApplicationTheme {
                AppNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
