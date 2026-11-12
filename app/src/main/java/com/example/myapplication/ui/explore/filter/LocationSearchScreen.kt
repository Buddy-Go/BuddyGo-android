package com.example.myapplication.ui.explore.filter

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.ui.common.ActionButton
import com.example.myapplication.ui.common.ToastHost
import com.example.myapplication.ui.common.ToastState
import com.example.myapplication.ui.common.ToastType
import kotlinx.coroutines.launch
import com.example.myapplication.ui.explore.SearchIcon
import com.example.myapplication.ui.explore.iconButton
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.SuitFontFamily

// 4g 위치 검색, 4g-2 검색 결과. 구·동을 하나 골라 '적용'하면 조건 설정의 위치가 된다.
// 검색어를 쓰고 고르지 않았으면 결과가 하나이거나 이름이 똑같을 때만 그 지역이 되고, 아니면 안내한다. 검색어도 없이 적용하면 위치 조건이 풀린다.
// (디자인의 "현재 위치에서 1·3·5km"는 정의서가 구·동 하나만 쓰기로 해서 만들지 않았다.)
@Composable
fun LocationSearchScreen(
    initialDistrict: String?,
    onBack: () -> Unit,
    onApplied: (String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationSearchViewModel = viewModel(factory = viewModelFactory { initializer { LocationSearchViewModel(initialDistrict) } }),
) {
    val state by viewModel.state.collectAsState()
    val toast = remember { ToastState() }
    val scope = rememberCoroutineScope()
    Box(modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(AppColor.Surface).statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = AppSpace.x4, vertical = AppSpace.xxl), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).iconButton("뒤로", onBack), contentAlignment = Alignment.Center) {
                ChevronLeftIcon(AppColor.TextPrimary)
            }
            Spacer(Modifier.width(AppSpace.xl))
            Text("위치", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary, modifier = Modifier.weight(1f))
            Text(
                "초기화", color = AppColor.TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.clip(RoundedCornerShape(AppRadius.sm)).clickable(onClick = viewModel::onReset).padding(AppSpace.md),
            )
        }

        SearchField(state.query, viewModel::onQueryChange, viewModel::onClearQuery, Modifier.padding(horizontal = AppSpace.x4))
        Spacer(Modifier.height(AppSpace.xxl))

        LazyColumn(Modifier.weight(1f).padding(horizontal = AppSpace.x4)) {
            if (state.isSearching) {
                // 4g-2: 검색 결과. 검색어와 겹치는 글자는 굵게 보인다.
                items(state.results, key = { it.fullName }) { district ->
                    DistrictRow(
                        text = highlighted(district.fullName, state.query),
                        trailing = { if (state.selected == district.fullName) CheckMarkIcon(AppColor.AccentStrong) },
                        onClick = { viewModel.onSelect(district.fullName) },
                    )
                }
                if (state.results.isEmpty() && !state.loading) item { EmptyText(if (state.failed) "검색하지 못했어요. 잠시 후 다시 시도해 주세요" else "검색 결과가 없어요") }
            } else {
                // 4g: 최근 검색. 고른 곳은 굵게 + 체크, 나머지는 회색 + 지우기(X)
                item { Text("최근 검색", fontSize = 14.sp, color = AppColor.TextSecondary, modifier = Modifier.padding(bottom = AppSpace.md)) }
                items(state.recent, key = { it }) { name ->
                    val selected = state.selected == name
                    DistrictRow(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) AppColor.TextPrimary else AppColor.TextSecondary)) { append(name) }
                        },
                        trailing = {
                            if (selected) CheckMarkIcon(AppColor.AccentStrong)
                            else Box(Modifier.size(32.dp).clip(CircleShape).iconButton("$name 최근 검색 삭제") { viewModel.onRemoveRecent(name) }, contentAlignment = Alignment.Center) {
                                CloseIcon(AppColor.TextDisabled, size = 18.dp)
                            }
                        },
                        onClick = { viewModel.onSelect(name) },
                        bordered = false,
                    )
                }
                if (state.recent.isEmpty()) item { EmptyText("최근에 검색한 지역이 없어요") }
            }
        }

        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.Border))
            ActionButton(
                text = "적용",
                onClick = {
                    // 검색어만 쓰고 아무것도 안 골랐으면 조건을 풀지 말고 고르라고 알려 준다.
                    if (viewModel.needsChoice) scope.launch { toast.show("검색 결과에서 지역을 선택해 주세요", ToastType.Warning) }
                    else onApplied(viewModel.onApply())
                },
                modifier = Modifier.padding(horizontal = AppSpace.x4, vertical = AppSpace.xxl),
            )
        }
    }
    ToastHost(toast, Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(AppRadius.pill)).background(AppColor.ChipNeutralBg).padding(horizontal = AppSpace.xxl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchIcon(AppColor.TextSecondary, size = 20.dp)
        Spacer(Modifier.width(AppSpace.xl))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("구 · 동 이름으로 검색", fontSize = 16.sp, color = AppColor.TextDisabled)
            BasicTextField(
                value = query, onValueChange = onChange, singleLine = true,
                textStyle = TextStyle(fontFamily = SuitFontFamily, fontSize = 16.sp, color = AppColor.TextPrimary),
                cursorBrush = SolidColor(AppColor.AccentStrong),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(AppColor.TextDisabled).iconButton("검색어 지우기", onClear), contentAlignment = Alignment.Center) {
                CloseIcon(AppColor.Surface, size = 14.dp)
            }
        }
    }
}

@Composable
private fun DistrictRow(text: androidx.compose.ui.text.AnnotatedString, trailing: @Composable () -> Unit, onClick: () -> Unit, bordered: Boolean = true) {
    Column {
        Row(
            Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text, fontSize = 17.sp, color = AppColor.TextSecondary)
            trailing()
        }
        if (bordered) Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.Border))
    }
}

@Composable
private fun EmptyText(text: String) =
    Box(Modifier.fillMaxWidth().padding(vertical = AppSpace.x5 * 2), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 15.sp, color = AppColor.TextSecondary)
    }

// 검색어와 겹치는 글자는 굵고 진하게, 나머지는 회색으로
private fun highlighted(text: String, query: String) = buildAnnotatedString {
    highlightPieces(text, query).forEach { piece ->
        withStyle(
            if (piece.matched) SpanStyle(fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
            else SpanStyle(color = AppColor.TextSecondary),
        ) { append(piece.text) }
    }
}
