// Versiones de los plugins: gradle/libs.versions.toml. Cada módulo aplica los que necesita.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
