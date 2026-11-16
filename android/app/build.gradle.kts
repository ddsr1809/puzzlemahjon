plugins {
    alias(libs.plugins.android.application)
    // AGP 9 compila Kotlin por sí mismo ("built-in Kotlin"): no hace falta el plugin kotlin-android.
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "azulejo.app"
    // Compose 1.12 exige compilar contra la API 37. La app sigue apuntando a la 36,
    // que es lo que pide Google Play para apps nuevas.
    compileSdk = 37

    defaultConfig {
        // OJO: el applicationId no se puede cambiar después de publicar en Google Play.
        // Decide el definitivo antes de subir el primer build (hito 3).
        applicationId = "com.ddsr1809.azulejo"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlin.test.junit)
}
