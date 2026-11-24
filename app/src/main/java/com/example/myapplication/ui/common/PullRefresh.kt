package com.example.myapplication.ui.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 위로 당겨서 새로고침. 안에는 세로로 스크롤되는 내용(verticalScroll·LazyColumn)을 넣는다.
// ViewModel의 새로고침 함수는 바로 돌아오므로, 당긴 것이 보이도록 표시줄을 잠깐 유지한다.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRefresh(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            onRefresh()
            scope.launch {
                delay(REFRESH_INDICATOR_MILLIS)
                refreshing = false
            }
        },
        modifier = modifier,
    ) {
        content()
    }
}

private const val REFRESH_INDICATOR_MILLIS = 900L

// 비어 있는 화면(내용이 가운데에 한 줄 놓이는 화면)도 당겨서 새로고침이 되도록 스크롤 가능하게 감싼다.
// 내용은 화면 높이를 꽉 채운 칸 안에서 가운데 정렬된다.
@Composable
fun ScrollableEmpty(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxSize()) {
        val height = maxHeight
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                content = content,
            )
        }
    }
}
