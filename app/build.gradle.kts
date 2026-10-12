plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.seirei.ruins"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.seirei.ruins"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0-poc"
    }

    // 定数セットはフレーバで固定する（設計書02 v0.5 §6、ADR D-003）。
    // standard = RollConstants.POC（探索120分）／ demo = RollConstants.DEMO（探索3分）
    // ビルド・インストールのタスク名は必ずフレーバ込みで書く（例：assembleDemoDebug、installStandardDebug）
    flavorDimensions += "constants"
    productFlavors {
        create("standard") {
            dimension = "constants"
        }
        create("demo") {
            dimension = "constants"
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:roll"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:expedition"))
    implementation(project(":feature:collection"))
    implementation(project(":widget"))
    implementation(project(":notification"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime)

    testImplementation(libs.junit)
}
