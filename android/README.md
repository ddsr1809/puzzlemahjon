# Solitario de azulejos — hito 5: anuncios, compra y privacidad

Dos módulos:

- **`core`**: la lógica del juego en Kotlin puro, sin Android. Tablero por capas, regla
  de «ficha libre», 17 plantillas verticales (9 normales y 8 compactas), generador de
  niveles con solución garantizada (tres dificultades), solucionador, sesión de juego
  (tocar, deshacer, pista, barajar, reiniciar), guardado de la partida como texto y, desde
  el hito 4, el progreso: los 200 niveles, el reto diario, las rachas y los murales. Desde
  el hito 5, también cuándo toca un anuncio y la cuota diaria de ayudas gratuitas.
- **`app`**: la app de Android en Jetpack Compose. Pantalla de inicio, selector de
  niveles, calendario del reto, murales, la partida, las 36 fichas de azulejo, los ajustes
  (tamaño de ficha, alto contraste, idioma), el guardado en el teléfono y el icono. Desde el
  hito 5, los anuncios (AdMob), el consentimiento (UMP) y la compra «Quitar anuncios»
  (Google Play Billing).

La política de privacidad está en `docs/privacy/` (en la raíz del repositorio), lista para
publicarla con GitHub Pages.

## Novedades del hito 5

- **Anuncio al acabar algunos tableros.** Un intersticial justo al vaciar el tablero, antes del
  aviso de victoria (nunca al empezar, durante la partida ni entre «Siguiente» y el tablero
  nuevo). Los 5 primeros tableros ganados no llevan ninguno; después, como máximo uno cada 2
  tableros y con 3 minutos como mínimo entre dos.
- **Ayuda extra.** Sin pistas o sin barajadas, el juego ofrece ver un vídeo corto a cambio de
  una más. Si no hay vídeo (sin conexión, por ejemplo), cada día hay 3 pistas y 1 barajada
  gratis. Decir que no no cambia nada: deshacer y reiniciar siguen siendo gratis.
- **Quitar anuncios.** Compra única en Ajustes que quita los intersticiales; los vídeos siguen
  siendo opcionales. «Restaurar compra» la recupera en otro teléfono con la misma cuenta.
- **Privacidad.** Mensaje de consentimiento donde la ley lo pide (Europa y estados de EE. UU.
  con ley de privacidad) y, en esos lugares, «Opciones de privacidad» en Ajustes. «Política de
  privacidad» en Ajustes abre la página pública.
- **Política de privacidad y Seguridad de los datos.** La política en inglés y español
  (`docs/privacy/`) y las respuestas para Play Console en `store/data-safety.md`.

Todo lo guardado antes (partida, ajustes, progreso) se conserva.

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

Son 128 pruebas en `core` y 58 en `app`, y tardan unos segundos. En cada ejecución se
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
  monetization/ AdPacing (cuándo toca intersticial), HelpQuota (ayudas gratuitas del día)
app/src/main/kotlin/azulejo/app/
  MainActivity.kt, AppLocale.kt, AzulejoApplication.kt (servicios que viven todo el proceso)
  ads/         AdsConfig (identificadores), ConsentManager (UMP), AdsController (intersticial y
               recompensado), ExtraHelpOffer (qué se ofrece sin pistas)
  billing/     RemoveAdsStore (Google Play Billing), StoreState (estado de la compra)
  tiles/       TileArt (los 36 dibujos), TilePalette (colores), TilePainter (pintarlos)
  data/        Settings, GameMode (nivel, reto o rápida), WinOutcome (qué se apunta al
               ganar), AppStorage (qué se guarda), PrefsStore (dónde), LocalDate (hoy)
  ui/          AzulejoApp: elige la pantalla, muestra los diálogos comunes y los anuncios;
               PlatformActions: lo que necesita la actividad
  ui/home/     HomeScreen (inicio), LevelSelectScreen (los 200 niveles)
  ui/daily/    DailyScreen (racha y calendario), CalendarModel
  ui/collection/ MuralsScreen
  ui/game/     GameViewModel, GameUiState, GameScreen, BoardView, BoardMetrics, GameNotice,
               ExtraHelpDialog
  ui/common/   cabecera, botones y textos compartidos
  ui/settings/ SettingsDialog
  ui/theme/    colores y tema, normal y de alto contraste
app/src/main/res/values/      textos en inglés
app/src/main/res/values-es/   textos en español
app/src/main/res/mipmap-*/, drawable/   icono de la app
store/         icono de 512 px para la ficha de Google Play; data-safety.md (Play Console)
tools/         make_icon.py: regenera el icono si cambias el diseño
../docs/       web pública (GitHub Pages): política de privacidad en inglés y español
```

`TileArt`, `TilePalette`, `Settings`, `GameMode`, `WinOutcome`, `AppStorage`, `BoardMetrics`,
`GameNotice`, `GameUiState`, `CalendarModel`, `ExtraHelpOffer` y `StoreState` son Kotlin puro:
se prueban en la JVM, sin emulador.

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

**Anuncios.** Se usa el SDK GMA Next-Gen de Google, el recomendado desde julio de 2026 (el
clásico, `play-services-ads`, quedó como «heredado»). Al abrir la app, `MainActivity` pide al
SDK de consentimiento (UMP) que ponga al día la respuesta del jugador y, si hace falta, le
muestre el mensaje; los anuncios no se piden hasta que UMP lo permite. El intersticial y el
vídeo se cargan por adelantado y se renuevan antes de caducar (a la hora); sin conexión se
reintenta cada vez más tarde (de 30 s a 15 min).

- **Intersticial.** `AdPacing` (en `core`) cuenta los tableros ganados de cualquier modo. Al
  ganar, si toca y hay uno cargado, el aviso de victoria espera: el tablero vacío dice
  «¡Tablero completado!», a los 1,2 s sale el anuncio y al cerrarlo, el aviso. La pausa evita
  que un toque rápido acabe en el anuncio (AdMob lo trata como clic no válido). Si no había
  ninguno cargado, se intenta en la siguiente victoria.
- **Ayuda extra.** Pedir una pista o barajar sin quedar ninguna abre un diálogo: «Ver vídeo»
  si hay uno cargado; si no, la cuota gratuita del día (`HelpQuota`: 3 pistas y 1 barajada); si
  tampoco, una explicación. La ayuda conseguida se usa al momento (la pista aparece, el tablero
  se baraja). El vídeo nunca sale solo.
- **Identificadores.** El build de debug usa siempre los de prueba de Google. El de release
  usa los de `gradle.properties` o, si faltan, también los de prueba (Gradle lo avisa).

**Compra «Quitar anuncios».** Producto único `remove_ads`. `RemoveAdsStore` conecta con Google
Play al abrir la app y al volver a ella, consulta el precio y si la cuenta ya la tiene, y
confirma cada compra (sin confirmar, Google la reembolsa a los 3 días). El resultado se guarda
en el teléfono (`purchase.removeAds`) para quitar los anuncios aunque Google Play aún no haya
contestado; cuando contesta, manda Google Play (por ejemplo, tras un reembolso). No hay
servidor que verifique las compras: para un producto que solo quita anuncios no compensa.

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
9. **Los anuncios, nunca durante la partida.** El intersticial solo se decide en
   `GameViewModel.apply` al ganar, y el vídeo solo sale al pulsar «Ver vídeo». No cambies el
   id del producto `remove_ads`: las compras hechas se perderían.

## Rendimiento medido (JVM de escritorio)

- Generar un nivel: menos de 0,1 ms en Fácil, unos 3 ms en Media, unos 6 ms en Difícil.
- Resolver un nivel con `Solver`: de media unos 2 ms o menos; el peor caso visto, unos 150 ms.

En un teléfono será varias veces más lento: por eso `GameViewModel` genera niveles y
busca pistas fuera del hilo principal.

## Anuncios y compra: lo que tienes que configurar

Sin hacer nada, la app funciona con anuncios de prueba de Google y la compra aparece como «no
disponible». Para los reales:

**1. Publicar la política de privacidad** (hace falta para AdMob y para Play Console).

1. Sube a GitHub la carpeta `docs/` (está en la raíz del repositorio, fuera de `android/`).
2. En GitHub: *Settings → Pages → Build and deployment → Deploy from a branch*, rama `main`
   (o `development` mientras pruebas) y carpeta `/docs`. Guarda.
3. A los pocos minutos estará en `https://ddsr1809.github.io/puzzlemahjon/privacy/` (inglés) y
   `…/privacy/es.html` (español). Son las direcciones que abre la app desde Ajustes. Si usas
   otra, cámbiala en `privacy_policy_url` de los dos `strings.xml`.
4. Revisa el texto: lleva tu nombre y el correo `damian@d2600.com` como contacto.

**2. AdMob.**

1. En [admob.google.com](https://admob.google.com), *Apps → Add app → Android*. A «¿Está
   publicada en una tienda?» contesta que no (se enlaza con Google Play más adelante). Apunta el
   **ID de la app** (`ca-app-pub-…~…`).
2. En la app, *Ad units → Add ad unit*: uno **Interstitial** («Fin de tablero») y uno
   **Rewarded** («Ayuda extra»; la recompensa que pongas en AdMob no importa: el juego siempre
   da 1 pista o 1 barajada). Apunta los dos **ID de bloque** (`ca-app-pub-…/…`).
3. *Privacy & messaging*: crea el mensaje **European regulations** (RGPD) y el de **US state
   regulations**, con la URL de la política. Publícalos. Sin ellos, UMP no muestra nada.
4. Opcional: *Blocking controls → Content* para limitar la categoría de los anuncios (por
   ejemplo, «PG»), sin tocar el código.
5. Pon los tres identificadores en `android/gradle.properties` (hay líneas de ejemplo
   comentadas al final): quita el `#` y sustituye los valores.
6. Registra tus teléfonos como **dispositivos de prueba** antes de instalar un build de release:
   *AdMob → Settings → Test devices → Add test device* con el ID de publicidad del teléfono
   (*Ajustes → Google → Anuncios*). También puedes poner en `azulejo.admob.testDevices` el ID
   que el SDK escribe en Logcat. Pide lo mismo a tus testers o diles que no pulsen anuncios:
   los clics no válidos pueden hacer que AdMob cierre la cuenta.

**3. Producto en Play Console** (necesita un build ya subido a una pista).

1. *Monetize with Play → Products → One-time products → Create*. ID del producto:
   **`remove_ads`** (exactamente así). Nombre: «Remove ads» / «Quitar anuncios». Precio: el que
   elijas (por ejemplo, 2,99 USD). Actívalo.
2. *Settings → License testing*: añade tu cuenta y las de los testers. Sus compras son de
   prueba y no se cobran.
3. La compra solo funciona en la app instalada desde Google Play (el enlace de la pista
   cerrada), no en la que instala Android Studio.

**4. Seguridad de los datos.** Las respuestas para Play Console están en
`store/data-safety.md`.

## Probar el consentimiento

En un build de debug puedes ver el mensaje europeo aunque estés en EE. UU.: en
`gradle.properties` activa `azulejo.ump.forceEea=true` y pon el ID de tu teléfono en
`azulejo.admob.testDevices` (los emuladores ya cuentan como de prueba). Para volver a ver el
mensaje, borra los datos de la app (*Ajustes del teléfono → Apps → Azulejo Mahjong →
Almacenamiento → Borrar datos*): también se borra tu progreso.

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
   objetivo, anuncios, ID de publicidad y Seguridad de los datos (respuestas en
   `store/data-safety.md`).

En cada build nuevo, sube `versionCode` en `app/build.gradle.kts`: Google Play rechaza un
número repetido.

## Qué probar en el teléfono

Con el build de debug (anuncios de prueba, marcados «Test ad»):

1. **Actualizar sin perder nada.** Instala encima de la versión del hito 4 (sin desinstalar):
   partida, ajustes y progreso siguen igual.
2. **Sin anuncios al principio.** Gana 5 tableros (Fácil en partida rápida es lo más rápido):
   ningún anuncio. El tablero vacío dice «¡Tablero completado!» y sale el aviso de victoria.
3. **Intersticial.** Gana el 6.º: «¡Tablero completado!», un segundo después el anuncio y, al
   cerrarlo, el aviso de victoria. El 7.º no lleva anuncio. Si ganas el 8.º antes de 3 minutos
   desde el anterior, tampoco.
4. **Ayuda extra con vídeo.** Gasta las 3 pistas y pide otra: «Sin pistas» ofrece «Ver vídeo».
   Míralo entero: al cerrarlo aparece la pista marcada. Repite cerrando el vídeo antes de
   tiempo: no hay pista. Igual con «Barajar».
5. **Ayuda extra sin conexión.** Con el modo avión, pide una pista sin quedar ninguna: ofrece
   «Usar pista gratis» (3 al día). Gasta las 3: la siguiente vez explica que no quedan.
6. **Ajustes.** Aparecen «Anuncios» y «Privacidad». «Política de privacidad» abre la página
   (cuando esté publicada). Sin el producto creado en Play Console, la compra dice «no está
   disponible» y «Restaurar compra» contesta.
7. **Atrás durante la pausa.** Gana un tablero que lleve anuncio y pulsa «Atrás» enseguida: no
   sale el anuncio en el inicio.

Con el build de la pista cerrada (instalado desde Google Play) y tu cuenta como tester de
licencias:

8. **Compra.** *Ajustes → Quitar anuncios · precio*: paga con la tarjeta de prueba. Ajustes da
   las gracias y ya no salen intersticiales; los vídeos siguen ofreciéndose.
9. **Restaurar.** Desinstala, instala de nuevo desde Google Play y abre Ajustes: la compra se
   recupera sola (o con «Restaurar compra»).

## Pendiente para el hito 6 y después

- `app-ads.txt`: AdMob lo recomienda para no perder ingresos. Tiene que estar en la raíz del
  sitio web de desarrollador que pongas en Google Play (por ejemplo, un repositorio
  `ddsr1809.github.io`).
- Enlazar la app de AdMob con su ficha de Google Play cuando esté publicada.
- El tablero no se puede jugar con lector de pantalla (TalkBack): anuncia cuántas fichas
  quedan, pero no cada ficha. Las pantallas nuevas (inicio, niveles, calendario, murales)
  sí se pueden usar con TalkBack.
- El código de `app` que usa Compose y los SDK de anuncios, consentimiento y compra se
  compila por primera vez en tu máquina. Si algo falla al sincronizar o compilar, el mensaje
  de error dice el archivo y la línea.
