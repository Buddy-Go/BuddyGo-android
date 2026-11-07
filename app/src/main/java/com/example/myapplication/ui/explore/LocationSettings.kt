package com.example.myapplication.ui.explore

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.example.myapplication.ui.common.ConfirmDialog

// 위치 권한을 '다시 묻지 않음'으로 거절한 경우. 앱 안에서는 더 물을 수 없어서 설정 화면으로 안내한다.
// 권한을 요청한 결과가 모두 거절일 때만 부른다. (요청 전에는 shouldShowRequestPermissionRationale이 false라 구분되지 않는다)
fun isLocationPermanentlyDenied(context: Context): Boolean {
    val activity = context.findActivity() ?: return false
    return !hasLocationPermission(context) &&
        LocationPermissions.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
}

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun LocationSettingsDialog(onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = "위치 권한이 꺼져 있어요",
        message = "설정에서 위치 권한을 허용하면 내 주변 장소를 찾을 수 있어요.",
        confirmText = "설정 열기",
        dismissText = "닫기",
        onConfirm = onOpenSettings,
        onDismiss = onDismiss,
    )
}
