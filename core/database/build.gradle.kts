plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.seirei.core.database"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Room（ksp・room プラグイン、room-runtime・sqlite-bundled）は E4-01 で追加する
    implementation(project(":core:model"))
    testImplementation(libs.junit)
}
