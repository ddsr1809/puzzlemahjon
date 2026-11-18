plugins {
    alias(libs.plugins.android.application)
    // AGP 9 compila Kotlin por sí mismo ("built-in Kotlin"): no hace falta el plugin kotlin-android.
    alias(libs.plugins.kotlin.compose)
}

// ---------- Identificadores de AdMob ----------
//
// Los de prueba de Google sirven para desarrollar: muestran anuncios marcados como «Test ad» y
// no generan ingresos. El build de debug usa SIEMPRE estos, para que ni tú ni los testers
// pulséis nunca un anuncio real desde Android Studio (AdMob puede cerrar la cuenta por clics
// no válidos).
//
// Los reales van en gradle.properties (README, «Anuncios y compra»). Si falta alguno, el
// build de release también usa los de prueba y Gradle lo avisa.
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"
val testRewardedId = "ca-app-pub-3940256099942544/5224354917"

fun gradleText(name: String): String? = providers.gradleProperty(name).orNull?.trim()?.takeIf { it.isNotEmpty() }

val realAdmobAppId = gradleText("azulejo.admob.appId")
val realInterstitialId = gradleText("azulejo.admob.interstitialId")
val realRewardedId = gradleText("azulejo.admob.rewardedId")
val hasRealAdIds = realAdmobAppId != null && realInterstitialId != null && realRewardedId != null
if (!hasRealAdIds) {
    logger.warn("Azulejo: faltan los identificadores reales de AdMob en gradle.properties; el build de release usará los de prueba.")
}
// Teléfonos de prueba (ID con hash que el SDK escribe en Logcat), separados por comas. En ellos
// el SDK sirve anuncios de prueba aunque el build use los identificadores reales.
val adTestDevices = gradleText("azulejo.admob.testDevices").orEmpty()
// Solo en debug y en teléfonos de prueba: hace como si el teléfono estuviera en Europa para ver
// el mensaje de consentimiento del RGPD (README, «Probar el consentimiento»).
val umpForceEea = gradleText("azulejo.ump.forceEea") == "true"

/** Texto para `buildConfigField`: entre comillas y sin caracteres que rompan el código generado. */
fun quoted(value: String): String {
    require(value.none { it == '"' || it == '\\' || it == '\n' }) { "Valor no válido en gradle.properties: $value" }
    return "\"$value\""
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
        versionCode = 5
        versionName = "0.5.0"

        buildConfigField("String", "AD_TEST_DEVICES", quoted(adTestDevices))
        buildConfigField("boolean", "UMP_FORCE_EEA", umpForceEea.toString())
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = testAdmobAppId
            buildConfigField("String", "ADMOB_APP_ID", quoted(testAdmobAppId))
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", quoted(testInterstitialId))
            buildConfigField("String", "REWARDED_AD_UNIT_ID", quoted(testRewardedId))
        }
        release {
            val appId = if (hasRealAdIds) realAdmobAppId!! else testAdmobAppId
            manifestPlaceholders["admobAppId"] = appId
            buildConfigField("String", "ADMOB_APP_ID", quoted(appId))
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", quoted(if (hasRealAdIds) realInterstitialId!! else testInterstitialId))
            buildConfigField("String", "REWARDED_AD_UNIT_ID", quoted(if (hasRealAdIds) realRewardedId!! else testRewardedId))

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
        // BuildConfig lleva los identificadores de AdMob de cada tipo de build.
        buildConfig = true
    }
}

// El SDK de anuncios nuevo (GMA Next-Gen) choca con el clásico si alguna dependencia lo trae:
// Google pide excluirlo en todo el proyecto.
configurations.configureEach {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
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

    // Hito 5: anuncios, consentimiento y compra.
    implementation(libs.google.ads.mobile.sdk)
    implementation(libs.google.ump)
    implementation(libs.play.billing)

    testImplementation(libs.kotlin.test.junit)
}
