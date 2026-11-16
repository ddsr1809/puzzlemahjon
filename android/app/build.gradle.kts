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
        // OJO: el applicationId no se puede cambiar después de subir el primer build a
        // Google Play. Confírmalo antes de subirlo (README, «Subir a la pista cerrada»).
        applicationId = "com.ddsr1809.azulejo"
        minSdk = 24
        targetSdk = 36
        // Google Play exige un versionCode mayor en cada build que subas.
        versionCode = 3
        versionName = "0.3.0"
    }

    buildTypes {
        release {
            // R8 quita el código y los recursos que no se usan, y la descarga queda mucho
            // más pequeña. El build de debug no cambia.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
