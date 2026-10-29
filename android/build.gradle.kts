// 小泽助手 Android 独立 Gradle 工程（与仓库根 Maven 后端互不感知）
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
