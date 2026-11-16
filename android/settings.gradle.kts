pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "azulejo-mahjong"

// Lógica del juego en Kotlin puro. No depende de Android.
include(":core")

// La app de Android: interfaz, guardado y monetización.
include(":app")
