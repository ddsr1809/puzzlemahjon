# Solitario de azulejos — hito 1: motor del tablero

Módulo `core`: la lógica del juego en Kotlin puro, sin Android. Contiene el modelo de
tablero por capas, la regla de «ficha libre», nueve plantillas verticales, el generador
de niveles con solución garantizada (tres dificultades) y las pruebas.

## Cómo ejecutarlo

Requisitos: un JDK entre 17 y 24 para ejecutar Gradle 8.14.3 (por ejemplo, el JDK 21
que trae Android Studio). Con JDK 25, Android Studio muestra «Incompatible Gradle JVM
version»: pulsa *Apply compatible Gradle JDK configuration and sync*. En el hito 2 el
proyecto pasará a Gradle 9, que sí admite JDK 25.

```
./gradlew :core:test
```

Ejecútalo desde la carpeta `android/` (en Windows, `gradlew.bat :core:test`).

En Android Studio: abre la carpeta `android/`, espera a que sincronice y, en el panel
Gradle de la derecha, haz doble clic en **core → Tasks → verification → test**. El informe
queda en `core/build/reports/tests/test/index.html`.

Todavía no hay app de Android (llega en el hito 2), así que el botón verde de ejecutar
da «Module not specified». Si Android Studio creó una configuración «app», bórrala en
*Run → Edit Configurations…*.

Son 50 pruebas y tardan unos segundos. En cada ejecución se generan y resuelven más de
1.500 niveles.

> **Estado de la verificación.** Las 50 pruebas pasan compilando con `kotlinc` 2.2.21 y
> ejecutando con JUnit 4. Los archivos de Gradle no se han podido ejecutar en el entorno
> donde se escribió esto (sin acceso a Maven Central), así que la primera sincronización
> en tu máquina es la comprobación que falta. Si Android Studio propone actualizar Kotlin
> o Gradle, acepta: el código no depende de la versión.

## Estructura

```
core/src/main/kotlin/azulejo/core/
  board/       Position, Tile, Move, Layout, BoardGeometry, Board
  layouts/     LayoutCatalog: las 9 plantillas
  generator/   Difficulty, LevelGenerator, GeneratedLevel
  solver/      Solver (búsqueda), RandomPlayer (medida de dificultad)
  random/      SeededRandom: aleatoriedad reproducible
core/src/test/kotlin/azulejo/core/
  BoardTest, LayoutTest, LevelGeneratorTest, SolverTest, SeededRandomTest
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

En un teléfono será varias veces más lento: genera y resuelve fuera del hilo principal.
