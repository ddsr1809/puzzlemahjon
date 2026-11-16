# Reglas de R8 propias de la app. Las de Compose, AndroidX y Kotlin vienen dentro de cada
# biblioteca, y el juego no usa reflexión propia.
#
# Android crea GameViewModel buscando por reflexión su constructor (Application). AndroidX
# ya trae esta regla; se repite aquí para no depender de ello.
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# Si un build de release falla donde el de debug funciona, añade aquí la regla -keep que
# indique el error y anótalo en el README.
