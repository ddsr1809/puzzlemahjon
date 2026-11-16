# Solitario de azulejos — hito 2: el juego en el teléfono

Dos módulos:

- **`core`**: la lógica del juego en Kotlin puro, sin Android. Tablero por capas, regla
  de «ficha libre», nueve plantillas verticales, generador de niveles con solución
  garantizada (tres dificultades), solucionador y sesión de juego (tocar, deshacer, pista,
  barajar, reiniciar).
- **`app`**: la app de Android en Jetpack Compose. Elige dificultad, juega, pide ayudas y
  gana. Las fichas son provisionales (número del 1 al 9 y símbolo de familia); el arte de
  azulejos llega en el hito 3.

## Requisitos

- **Android Studio Panda 4 (2025.3.4) o posterior.** El proyecto usa el Android Gradle
  Plugin 9.2.1, Gradle 9.9.1 y Kotlin 2.4.20 (versiones en `gradle/libs.versions.toml`).
- **SDK de Android 17 (API 37)** para compilar: Compose 1.12 lo exige. Si falta, Android
  Studio ofrece instalarlo al sincronizar; si no, en *Tools → SDK Manager*. La app apunta
  a Android 16 (API 36) y funciona desde Android 7 (API 24).
- **JDK 17 o posterior para Gradle.** Gradle 9.9.1 admite también JDK 25.

## Probarlo en el teléfono

1. En el teléfono: *Ajustes → Acerca del teléfono*, toca 7 veces *Número de compilación*.
   Después, en *Opciones de desarrollador*, activa *Depuración USB*.
2. Conecta el teléfono por cable y acepta el aviso de depuración que aparece en él.
3. En Android Studio, elige la configuración **app** y tu teléfono en la barra superior y
   pulsa el botón verde de ejecutar.

Sin teléfono: *Device Manager → Add a new device* crea un emulador.

## Pruebas

Desde la carpeta `android/`:

```
./gradlew :core:test :app:testDebugUnitTest
```

En Windows, `gradlew.bat` en lugar de `./gradlew`. En Android Studio, las mismas tareas
están en el panel Gradle: **core → Tasks → verification → test** y
**app → Tasks → verification → testDebugUnitTest**.

Son 72 pruebas en `core` y 6 en `app`, y tardan unos segundos. En cada ejecución se
generan y resuelven más de 1.500 niveles y se juegan miles de acciones al azar.

## Estructura

```
core/src/main/kotlin/azulejo/core/
  board/       Position, Tile, Move, Layout, BoardGeometry, Board
  layouts/     LayoutCatalog: las 9 plantillas
  generator/   Difficulty, LevelGenerator, Dismantler
  solver/      Solver (búsqueda), RandomPlayer (medida de dificultad)
  session/     GameSession (partida y ayudas), Shuffler (barajar con solución)
  random/      SeededRandom: aleatoriedad reproducible
app/src/main/kotlin/azulejo/app/
  MainActivity.kt
  ui/game/     GameViewModel, GameScreen, BoardView, BoardMetrics, GameNotice
  ui/theme/    colores y tema
app/src/main/res/values/      textos en inglés
app/src/main/res/values-es/   textos en español
```

## Uso

```kotlin
val generator = LevelGenerator()
val level = generator.generate(LayoutCatalog.patio, Difficulty.EASY, seed = 20261116)

var board = level.board
board.freeTiles()            // fichas que se pueden tocar
board.availableMoves()       // pares que se pueden retirar
board.check(3, 17)           // OK, NOT_FREE, DIFFERENT_FACE…
board = board.remove(3, 17)  // devuelve un tablero nuevo; el anterior sirve para deshacer
board.isCleared              // victoria
board.isStuck                // sin pares: ofrecer barajar

level.solution               // una forma de vaciar el tablero
Solver.solve(board)          // Solved(moves), Unsolvable o Unknown, desde cualquier estado

var session = GameSession.start(level.board)       // 3 pistas y 1 barajada por nivel
session = session.tap(3).session                   // Selected, Blocked, NoMatch, Matched…
session = session.undo().session                   // ilimitado
session = session.requestHint().session            // par seguro en session.hint
session = session.shuffle(seed = 42).session       // nuevo reparto, siempre con solución
session.status                                     // PLAYING, WON o STUCK
```

## Cómo funciona

**Coordenadas.** La rejilla va en medias fichas. Una ficha en `(x, y, z)` ocupa las celdas
`x..x+1`, `y..y+1` de la capa `z`. Así una ficha puede apoyarse a caballo entre dos o
cuatro de la capa inferior.

**Ficha libre.** No tiene ninguna encima (ni media) y tiene despejado el lado izquierdo o
el derecho. Las vecinas de arriba y abajo no bloquean.

**Generador.** Juega la partida al revés: con el tablero lleno y sin dibujos, retira dos
fichas libres al azar hasta vaciarlo y apunta el orden; luego da el mismo dibujo a cada
par retirado. El orden apuntado es una solución por construcción.

**Dificultades.**

| | Plantillas | Copias por dibujo | Tasa de victoria jugando al azar |
| --- | --- | --- | --- |
| Fácil | 36 a 48 fichas | 2 | 100 %: no existe el bloqueo |
| Media | 56 a 64 fichas | 4 | objetivo 80 % (medido: 71–81 %) |
| Difícil | 72 fichas | 4 | objetivo 30 % (medido: 39–44 %) |

En Media y Difícil el generador crea 4 o 6 tableros candidatos, mide cada uno con 40
partidas al azar y se queda con el más cercano al objetivo.

**Ayudas.** Deshacer es ilimitado. La pista usa el solucionador para marcar un par que
mantiene el tablero con solución; si la anterior sigue en pantalla, no gasta otra, y si ya
no hay solución pide deshacer sin gastar. Barajar reparte de nuevo los dibujos que quedan
con el mismo método que el generador, así que el resultado siempre tiene solución, y borra
el historial de deshacer.

**Reproducibilidad.** Plantilla + dificultad + semilla dan siempre el mismo nivel. La
aleatoriedad sale de `SeededRandom` (SplitMix64 propio), no de `kotlin.random`, para que
el resultado no dependa de la versión de Kotlin ni de la plataforma.

## Reglas para no romper nada

1. **`core` no importa `java.*` ni `android.*`.** Así se podrá compartir con iOS mediante
   Kotlin Multiplatform sin reescribirlo.
2. **No cambies una plantilla publicada.** Cambiarían los niveles y retos diarios que la
   usan. Añade otra con un id nuevo.
3. **Si cambias el generador o `SeededRandom`,** la prueba `los niveles publicados no
   cambian entre versiones` fallará. Si el cambio es intencionado, sube
   `LevelGenerator.VERSION` y actualiza las huellas.
4. **Toda plantilla nueva va a `LayoutCatalog.all`.** Las pruebas la validan y generan
   niveles con ella automáticamente.

## Rendimiento medido (JVM de escritorio)

- Generar un nivel: menos de 0,1 ms en Fácil, unos 3 ms en Media, unos 6 ms en Difícil.
- Resolver un nivel con `Solver`: de media unos 2 ms o menos; el peor caso visto, unos 150 ms.

En un teléfono será varias veces más lento: por eso `GameViewModel` genera niveles y
busca pistas fuera del hilo principal.

## Pendiente para el hito 3

- La partida vive en memoria: si Android cierra la app, se pierde. Llega el guardado local.
- Arte de azulejos, tamaño de ficha ajustable y modo de alto contraste.
- Icono de la app y nombre definitivo. El `applicationId` (`com.ddsr1809.azulejo`) no se
  puede cambiar después de publicar: hay que confirmarlo antes del primer build a Play.
