// 純Kotlinモジュール。Android依存・I/O・現在時刻の直接取得を持ち込まないこと（AGENTS.md「禁止事項」）
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core:model"))
    testImplementation(libs.junit)
}
