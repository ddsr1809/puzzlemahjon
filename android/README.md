# Solitario de azulejos — hito 4: niveles, reto diario y murales

Dos módulos:

- **`core`**: la lógica del juego en Kotlin puro, sin Android. Tablero por capas, regla
  de «ficha libre», 17 plantillas verticales (9 normales y 8 compactas), generador de
  niveles con solución garantizada (tres dificultades), solucionador, sesión de juego
  (tocar, deshacer, pista, barajar, reiniciar), guardado de la partida como texto y, desde
  el hito 4, el progreso: los 200 niveles, el reto diario, las rachas y los murales.
- **`app`**: la app de Android en Jetpack Compose. Pantalla de inicio, selector de
  niveles, calendario del reto, murales, la partida, las 36 fichas de azulejo, los ajustes
  (tamaño de ficha, alto contraste, idioma), el guardado en el teléfono y el icono.

## Novedades del hito 4

- **Pantalla de inicio.** Continuar la partida a medias, jugar el siguiente nivel, el reto
  diario, la lista de niveles, los murales y la partida rápida (la de antes, con tablero
  al azar). El botón «Atrás» del teléfono vuelve siempre al inicio.
- **200 niveles fijos.** El nivel 37 es el mismo tablero para todos los jugadores. Se
  desbloquean en orden y los ganados se pueden repetir. Los 20 primeros son fáciles y la
  dificultad sube por tramos (tabla en «Cómo funciona»).
- **Murales.** Cada nivel ganado coloca un azulejo en uno de los 8 murales de 25 piezas.
- **Reto diario.** Un tablero por día, el mismo para todos ese día. Se puede jugar
  cualquier día pasado desde el calendario, pero solo el de hoy suma racha. Racha actual
  y mejor racha.
- **Fin de nivel.** Al ganar, un aviso dice qué azulejo se ha colocado o cómo va la racha
  y ofrece el siguiente nivel. Ahí irá el anuncio intersticial del hito 5.
- **Tamaño Grande.** Cada nivel y cada reto tienen también su versión con plantilla
  compacta: el mismo número de nivel da un tablero en Normal y otro en Grande, los dos
  siempre iguales.

Tu partida y tus ajustes del hito 3 se conservan: la partida a medias aparece como
«Partida rápida» en «Continuar».

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

Son 117 pruebas en `core` y 50 en `app`, y tardan unos segundos. En cada ejecución se
generan y resuelven más de 3.000 niveles (entre ellos los 200 niveles en los dos tamaños de
ficha y un año de retos diarios), se juegan miles de acciones al azar y la partida se
guarda y se recupera después de cada una.

## Estructura

```
core/src/main/kotlin/azulejo/core/
  board/       Position, Tile, Move, Layout, BoardGeometry, Board
  layouts/     LayoutCatalog: 9 plantillas normales y 8 compactas
  generator/   Difficulty, LevelGenerator, Dismantler
  solver/      Solver (búsqueda), RandomPlayer (medida de dificultad)
  session/     GameSession (partida y ayudas), Shuffler (barajar con solución),
               GameSnapshot y GameSnapshotCodec (guardar y recuperar)
  progress/    LevelCatalog (200 niveles), DailyChallenge (reto diario), Murals,
               PlayerProgress y PlayerProgressCodec (niveles ganados, retos, rachas),
               CalendarDay (fechas sin java.time)
  random/      SeededRandom: aleatoriedad reproducible
app/src/main/kotlin/azulejo/app/
  MainActivity.kt, AppLocale.kt
  tiles/       TileArt (los 36 dibujos), TilePalette (colores), TilePainter (pintarlos)
  data/        Settings, GameMode (nivel, reto o rápida), WinOutcome (qué se apunta al
               ganar), AppStorage (qué se guarda), PrefsStore (dónde), LocalDate (hoy)
  ui/          AzulejoApp: elige la pantalla y muestra los diálogos comunes
  ui/home/     HomeScreen (inicio), LevelSelectScreen (los 200 niveles)
  ui/daily/    DailyScreen (racha y calendario), CalendarModel
  ui/collection/ MuralsScreen
  ui/game/     GameViewModel, GameUiState, GameScreen, BoardView, BoardMetrics, GameNotice
  ui/common/   cabecera, botones y textos compartidos
  ui/settings/ SettingsDialog
  ui/theme/    colores y tema, normal y de alto contraste
app/src/main/res/values/      textos en inglés
app/src/main/res/values-es/   textos en español
app/src/main/res/mipmap-*/, drawable/   icono de la app
store/         icono de 512 px para la ficha de Google Play
tools/         make_icon.py: regenera el icono si cambias el diseño
```

`TileArt`, `TilePalette`, `Settings`, `GameMode`, `WinOutcome`, `AppStorage`, `BoardMetrics`,
`GameNotice`, `GameUiState` y `CalendarModel` son Kotlin puro: se prueban en la JVM, sin
emulador.

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

val text = GameSnapshotCodec.encode(session.snapshot())             // guardar
val restored = GameSnapshotCodec.decode(text)?.let { GameSession.restore(it) }  // null si no vale

val recipe = LevelCatalog.recipe(37)               // o DailyChallenge.recipe(CalendarDay.of(2026, 11, 16))
val level37 = generator.generate(recipe.layout(compact = false), recipe.difficulty, recipe.seed)

var progress = PlayerProgress()
val win = progress.completeLevel(1)                // win.mural, win.muralPieces, win.muralCompleted
progress = win.progress
progress.nextLevel                                 // 2
progress = progress.completeDaily(today, today).progress
progress.currentStreak(today)                      // 1
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

**Fichas.** Cada cara es una de 4 familias con uno de 9 motivos:

| Familia | Color | Marco |
| --- | --- | --- |
| Esquinas | azul cobalto | un cuarto de círculo en cada esquina |
| Medallón | terracota | disco de color con el motivo en claro |
| Rombo | verde | rombo |
| Recuadro | morado | recuadro |

Los motivos son estrella, flor, cruz, triángulo, luna, arco, olas, diana y hoja. Dos fichas
nunca se distinguen solo por el color: una prueba compara los 36 dibujos sin colores y
exige que sigan siendo distintos. `TileArt` describe cada dibujo con cuatro figuras simples
(disco, aro, polígono y contorno) y `TilePainter` las pinta; para cambiar un motivo basta
tocar `TileArt`.

**Alto contraste.** Fondo blanco, colores más oscuros, borde negro y grueso y fichas
bloqueadas más apagadas. Todos los colores de ficha pasan de 7:1 sobre cualquier fondo
(normal, seleccionada o sugerida); en el modo normal, de 4,5:1. Lo comprueban las pruebas.

**Tamaño de ficha.** Las fichas se dibujan tan grandes como permita la plantilla. Normal
usa plantillas de hasta 6 × 8 fichas; Grande, las compactas de 5 × 6 como máximo, con un
número de fichas parecido (Fácil 36 a 44, Media 56 a 60, Difícil 72) repartido en más
capas. En un teléfono de 360 × 800 dp:

| | Ancho de ficha |
| --- | --- |
| Normal | unos 54 dp |
| Grande | unos 67 dp |

El cambio vale para la próxima partida; si la actual está sin empezar, se cambia al momento.

**Guardado.** `GameSession.snapshot()` da una foto de la partida en datos simples y
`GameSnapshotCodec` la convierte en texto de líneas `clave=valor`. Se guardan los dibujos de
cada ficha, no la semilla: una partida a medias se recupera igual aunque cambie el
generador. Al leer, todo se valida reproduciendo los pares retirados con las reglas del
tablero; un guardado dañado o de otro formato se descarta y empieza una partida nueva. La
app lo guarda en las preferencias de Android (`PrefsStore`), sin permisos ni dependencias.

**Idioma.** Por defecto, el del teléfono. Si se elige otro en Ajustes, `MainActivity` lo
aplica antes de mostrar nada. En Android 13 o posterior también aparece en los ajustes del
sistema, en el idioma de la app.

**Niveles.** Cada nivel es una receta fija: dificultad, semilla y qué plantilla de esa
dificultad usar. Con fichas grandes se usa la plantilla compacta del mismo puesto. Las
plantillas de una dificultad se turnan, así que dos niveles seguidos de la misma
dificultad nunca repiten plantilla.

| Niveles | Dificultad |
| --- | --- |
| 1–20 | Fácil |
| 21–50 | Media; los múltiplos de 5, Fácil (un respiro) |
| 51–100 | Media; los múltiplos de 5, Difícil |
| 101–150 | Media y Difícil alternadas |
| 151–200 | Difícil; los múltiplos de 5, Media |

Se desbloquean en orden: el 1, los ganados y el que sigue a uno ganado.

**Murales.** El nivel *n* coloca la pieza `(n − 1) mod 25` del mural `(n − 1) / 25`, por
filas. Cada mural es un panel de 5 × 5 con simetría de cuadrado (centro, anillo interior y
borde repiten dibujo) hecho con los mismos 36 dibujos de las fichas.

**Reto diario.** La semilla sale de la fecha local del teléfono, así que el reto no cambia
en todo el día y el de cualquier día pasado se puede volver a generar. Lunes y domingo,
Fácil; sábado, Difícil; el resto, Media. El calendario empieza el 1 de noviembre de 2026.
Completar el reto el mismo día suma racha; completarlo otro día lo marca, pero no suma.
Si hoy aún no has jugado, la racha de ayer sigue viva hasta que acabe el día. La fecha se
lee con `java.util.GregorianCalendar` (no `java.time`, que no existe en Android 7) y se
actualiza al volver a la app. En el calendario la semana empieza según la región del
teléfono, no según el idioma: en EE. UU. empieza en domingo también en español.

**Progreso guardado.** `PlayerProgressCodec` lo guarda como texto, con los números seguidos
en intervalos (`levels=1-37`). Si el texto no se puede leer, el juego empieza de cero,
pero lo aparta en otra clave (`progress.unreadable`) para poder recuperarlo. Junto a la
partida se guarda qué era (`game.mode`: `level:37`, `daily:20773` o `quick`); las partidas
del hito 3, que no lo tienen, se recuperan como partida rápida.

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
5. **No cambies el formato de guardado ni las claves de los ajustes** sin subir
   `GameSnapshotCodec.FORMAT`. Dos pruebas fijan el texto exacto: si fallan, una
   actualización dejaría de leer lo que ya hay guardado en los teléfonos.
6. **No cambies la receta de un nivel o de un reto publicado.** La prueba `los niveles y
   retos publicados no cambian entre versiones` fija varios. Cambiar la curva de
   dificultad, el orden de las plantillas o las sales de las semillas cambia niveles que
   los jugadores ya han ganado. Si es intencionado, sube `LevelCatalog.VERSION` o
   `DailyChallenge.VERSION`.
7. **No cambies el formato del progreso** sin subir `PlayerProgressCodec.FORMAT`, ni el
   diseño de un mural publicado. Una prueba fija el texto exacto.
8. **Los 36 dibujos no cambian de número.** El generador reparte dibujos del 0 al 35; si
   reordenas las familias o los motivos, las partidas guardadas cambian de aspecto.

## Rendimiento medido (JVM de escritorio)

- Generar un nivel: menos de 0,1 ms en Fácil, unos 3 ms en Media, unos 6 ms en Difícil.
- Resolver un nivel con `Solver`: de media unos 2 ms o menos; el peor caso visto, unos 150 ms.

En un teléfono será varias veces más lento: por eso `GameViewModel` genera niveles y
busca pistas fuera del hilo principal.

## Subir a la pista cerrada

Antes del primer build, dos cosas que **no se pueden cambiar después de subirlo**:

- El `applicationId` (`com.ddsr1809.azulejo`, en `app/build.gradle.kts`).
- La clave de subida: si la pierdes, hay que pedir a Google que la restablezca.

Y una que sí se puede cambiar, pero conviene decidir: el nombre (`app_name` en los dos
`strings.xml`).

1. **Crea el bundle firmado.** En Android Studio: *Build → Generate Signed Bundle/APK →
   Android App Bundle*. La primera vez, *Create new* para crear el almacén de claves:
   guárdalo **fuera del repositorio** y apunta las contraseñas. Elige la variante
   *release*. Si no cambias la carpeta de destino, el archivo queda en
   `app/release/app-release.aab`.
2. **Prueba el build de release en el teléfono.** Está reducido con R8 y hay que comprobar
   que se comporta igual que el de debug. Repite el asistente eligiendo *APK* y, desde
   `android/`:

   ```
   adb uninstall com.ddsr1809.azulejo
   adb install app/release/app-release.apk
   ```

   Desinstalar borra la partida guardada de la versión de debug. Juega una partida, cambia
   los tres ajustes, cierra la app desde recientes y vuelve a abrirla.
3. **Sube el bundle.** En Play Console: *Test and release → Testing → Closed testing →
   Manage track → Create new release*, y sube el `.aab`. En la pestaña *Testers*, crea la
   lista de correos de tus testers.
4. **Completa lo que pida el panel** antes de enviar a revisión: ficha de la tienda (el
   icono de 512 px está en `store/play-icon-512.png`; faltan el gráfico de 1024 × 500 y
   al menos dos capturas), política de privacidad, clasificación de contenido, público
   objetivo y Seguridad de los datos.

En cada build nuevo, sube `versionCode` en `app/build.gradle.kts`: Google Play rechaza un
número repetido.

## Qué probar en el teléfono

1. **Actualizar sin perder nada.** Instala encima de la versión del hito 3 (sin
   desinstalar). Tu partida a medias aparece en «Continuar» como partida rápida, y los
   ajustes siguen igual.
2. **Niveles.** Juega el nivel 1 hasta ganarlo: el aviso dice «Un azulejo nuevo para el
   mural 1: 1 de 25» y ofrece el nivel 2. En «Murales» aparece el azulejo colocado. En
   «Niveles», el 1 lleva ✓, el 2 está resaltado y el 3 bloqueado.
3. **Continuar.** Empieza un nivel, retira un par, vuelve al inicio con «Inicio» o con
   «Atrás» y pulsa «Continuar». Pide luego otro nivel o el reto: pregunta antes de
   abandonar la partida.
4. **Reto diario.** Juega el de hoy: la racha pasa a 1 día y el día queda relleno en el
   calendario. Juega un día pasado: queda con un aro y no suma racha.
5. **Tamaño Grande.** Con un nivel sin empezar, cambia a Grande en Ajustes: el mismo nivel
   pasa a su tablero compacto.
6. **Calendario.** La semana empieza según la región del teléfono (domingo en EE. UU. y
   México, lunes en España), en inglés y en español. El día de hoy es un cuadro redondeado;
   los días hechos, un círculo relleno (ese mismo día) o un aro (otro día).

## Pendiente para el hito 5 y después

- Anuncios (UMP, intersticial al acabar un nivel, recompensado), compra para quitarlos,
  política de privacidad y Seguridad de los datos.
- El tablero no se puede jugar con lector de pantalla (TalkBack): anuncia cuántas fichas
  quedan, pero no cada ficha. Las pantallas nuevas (inicio, niveles, calendario, murales)
  sí se pueden usar con TalkBack.
- El código de `app` que usa Compose se compila por primera vez en tu máquina. Si algo
  falla al sincronizar o compilar, el mensaje de error dice el archivo y la línea.
