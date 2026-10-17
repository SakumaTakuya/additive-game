// Pure Kotlin test support; never add Android dependencies here.
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
