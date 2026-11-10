import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.myapplication"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 카카오 네이티브 앱 키는 local.properties의 KAKAO_NATIVE_APP_KEY에서 읽는다. (git에 올리지 않는다)
        val localProps = Properties().apply {
            val file = rootProject.file("local.properties")
            if (file.exists()) file.inputStream().use { load(it) }
        }
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"${localProps.getProperty("KAKAO_NATIVE_APP_KEY", "")}\"")
        // 위치 검색(카카오 로컬 API)은 네이티브 키가 아니라 REST API 키를 쓴다. 없으면 서울 예시 목록으로 검색한다.
        buildConfigField("String", "KAKAO_REST_API_KEY", "\"${localProps.getProperty("KAKAO_REST_API_KEY", "")}\"")
        // BuddyGo 서버 주소. 기본은 배포 서버다. 내 PC에서 돌리는 서버를 보려면 local.properties에 API_BASE_URL=http://10.0.2.2:8080/ (에뮬레이터)를 넣는다.
        val apiBaseUrl = localProps.getProperty("API_BASE_URL", "https://buddygo.duckdns.org/").let { if (it.endsWith("/")) it else "$it/" }
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kakao.map)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303") // 단위 테스트에서 JSON 파싱 확인용 (Android의 org.json은 JVM 테스트에서 비어 있다)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}