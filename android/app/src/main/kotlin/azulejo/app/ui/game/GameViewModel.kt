package azulejo.app.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import azulejo.app.AzulejoApplication
import azulejo.app.ads.ExtraHelpOffer
import azulejo.app.billing.Ownership
import azulejo.app.billing.PurchaseMessage
import azulejo.app.billing.StoreState
import azulejo.app.billing.adsRemoved
import azulejo.app.data.AppLanguage
import azulejo.app.data.AppStorage
import azulejo.app.data.GameMode
import azulejo.app.data.PrefsStore
import azulejo.app.data.SavedGame
import azulejo.app.data.Settings
import azulejo.app.data.TileSize
import azulejo.app.data.WinOutcome
import azulejo.app.data.difficulty
import azulejo.app.data.localToday
import azulejo.app.data.recipe
import azulejo.app.data.recordWin
import azulejo.core.generator.Difficulty
import azulejo.core.generator.LevelGenerator
import azulejo.core.layouts.LayoutCatalog
import azulejo.core.monetization.AdPacing
import azulejo.core.monetization.HelpKind
import azulejo.core.monetization.HelpQuota
import azulejo.core.progress.CalendarDay
import azulejo.core.progress.PlayerProgress
import azulejo.core.session.GameEvent
import azulejo.core.session.GameSession
import azulejo.core.session.GameStatus
import azulejo.core.session.Step
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Une la interfaz con [GameSession], con el progreso y con lo que se guarda en el teléfono.
 *
 * - Todo lo que puede tardar (leer del disco, generar un nivel, buscar una pista) se hace
 *   fuera del hilo principal.
 * - La partida se guarda después de cada acción: si Android cierra la app, al volver a
 *   abrirla el jugador la encuentra tal como la dejó.
 * - Al ganar, el progreso se guarda antes de borrar la partida: si la app se cierra justo
 *   entonces, el azulejo ya está colocado.
 * - Al ganar también se decide si toca intersticial ([AdPacing]); el aviso de victoria espera
 *   a que se cierre. Mostrarlo necesita la actividad: lo hace la interfaz, que avisa con
 *   [beginInterstitial] y [onInterstitialFinished].
 * - Sin pistas o sin barajadas, se ofrece ayuda extra ([ExtraHelpOffer]): un vídeo
 *   recompensado o, sin vídeo, la cuota gratuita del día.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = AppStorage(PrefsStore(application))
    private val services = application as AzulejoApplication
    private val generator = LevelGenerator()
    private val _state = MutableStateFlow(GameUiState(today = localToday()))
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var work: Job? = null

    /** Tableros ganados desde el último intersticial. Solo lo usa este ViewModel. */
    private var adPacing = AdPacing()

    /** Lo último que dijo Google Play de la compra, para cuando no contesta. */
    private var cachedRemoveAds = false

    /** El intersticial pendiente ya se ha pedido a la interfaz: no se pide dos veces. */
    private var interstitialInFlight = false

    /** Lo que se lee del disco al abrir la app. */
    private class Stored(
        val settings: Settings,
        val quickDifficulty: Difficulty,
        val progress: PlayerProgress,
        val game: SavedGame?,
        val adPacing: AdPacing,
        val helpQuota: HelpQuota,
        val removeAdsOwned: Boolean,
    )

    init {
        work = viewModelScope.launch {
            val stored = withContext(Dispatchers.IO) {
                Stored(
                    storage.loadSettings(), storage.loadDifficulty(), storage.loadProgress(), storage.loadGame(),
                    storage.loadAdPacing(), storage.loadHelpQuota(), storage.loadRemoveAdsOwned(),
                )
            }
            val saved = stored.game
            adPacing = stored.adPacing
            cachedRemoveAds = stored.removeAdsOwned
            _state.update {
                it.copy(
                    loaded = true,
                    settings = stored.settings,
                    quickDifficulty = stored.quickDifficulty,
                    progress = stored.progress,
                    // Si había una partida a medias, queda lista para «Continuar».
                    mode = saved?.mode ?: GameMode.Quick,
                    difficulty = saved?.difficulty ?: stored.quickDifficulty,
                    session = saved?.session,
                    helpQuota = stored.helpQuota,
                    adsRemoved = adsRemoved(cachedRemoveAds, it.store),
                )
            }
            // Desde aquí, lo que digan los anuncios y Google Play llega al estado. Van en
            // viewModelScope y no como hijos de este trabajo: empezar una partida cancela `work`.
            viewModelScope.launch {
                services.ads.rewardedReady.collect { ready -> _state.update { it.copy(rewardedReady = ready) } }
            }
            viewModelScope.launch { services.removeAds.state.collect { store -> onStore(store) } }
        }
    }

    // ---------- Navegación ----------

    fun show(screen: Screen) {
        refreshToday()
        // Si se sale de la partida antes de que salga el intersticial pendiente, ya no sale.
        val keepAd = screen == Screen.GAME || interstitialInFlight
        _state.update {
            it.copy(
                screen = screen,
                notice = null,
                win = if (screen == Screen.GAME) it.win else null,
                interstitialPending = it.interstitialPending && keepAd,
                extraHelp = if (screen == Screen.GAME) it.extraHelp else null,
            )
        }
    }

    /** El calendario enseña el mes de [month]. */
    fun showMonth(month: CalendarDay) {
        _state.update { it.copy(calendarMonth = month.firstOfMonth()) }
    }

    /**
     * Pone al día la fecha. Se llama al volver a la app: si se dejó abierta de un día para
     * otro, el reto de hoy y la racha tienen que cambiar.
     */
    fun refreshToday() {
        val today = localToday()
        val current = _state.value
        if (today == current.today) return
        _state.update {
            // Si el calendario enseñaba el mes de «hoy», pasa al mes nuevo.
            val month = if (it.calendarMonth == current.today.firstOfMonth()) today.firstOfMonth() else it.calendarMonth
            it.copy(today = today, calendarMonth = month)
        }
    }

    // ---------- Empezar partidas ----------

    /** Vuelve a la partida en memoria. */
    fun continueGame() {
        if (_state.value.canContinue) show(Screen.GAME)
    }

    /**
     * Juega un nivel o un reto. Si es la partida en memoria, la continúa; si no, empieza otra.
     * Antes, la interfaz pregunta si se perdería una partida ([GameUiState.wouldAbandon]).
     */
    fun play(mode: GameMode) {
        if (_state.value.continues(mode)) {
            show(Screen.GAME)
        } else {
            startGame(mode, _state.value.quickDifficulty, showGame = true)
        }
    }

    /** Partida rápida nueva de la dificultad dada. */
    fun playQuick(difficulty: Difficulty) = startGame(GameMode.Quick, difficulty, showGame = true)

    private fun startGame(mode: GameMode, quickDifficulty: Difficulty, showGame: Boolean) {
        val current = _state.value
        if (!current.loaded) return
        work?.cancel()
        val difficulty = mode.difficulty(quickDifficulty)
        val compact = current.settings.tileSize == TileSize.LARGE
        _state.update {
            it.copy(
                screen = if (showGame) Screen.GAME else it.screen,
                mode = mode,
                difficulty = difficulty,
                quickDifficulty = if (mode == GameMode.Quick) difficulty else it.quickDifficulty,
                session = null,
                busy = true,
                notice = null,
                win = null,
                interstitialPending = false,
                extraHelp = null,
            )
        }
        // El jugador ya ha aceptado dejar la partida anterior: no debe reaparecer si la app se
        // cierra mientras se genera la nueva.
        storage.clearGame()
        work = viewModelScope.launch {
            val session = withContext(Dispatchers.Default) {
                GameSession.start(generateBoard(mode, difficulty, compact))
            }
            _state.update { it.copy(session = session, busy = false) }
            storage.saveGame(difficulty, session, mode)
        }
    }

    private fun generateBoard(mode: GameMode, difficulty: Difficulty, compact: Boolean) =
        when (val recipe = mode.recipe()) {
            // Niveles y retos: siempre el mismo tablero para el mismo tamaño de ficha.
            null -> {
                val layouts = LayoutCatalog.forDifficulty(difficulty, compact)
                generator.generate(layouts[Random.nextInt(layouts.size)], difficulty, Random.nextLong()).board
            }
            else -> generator.generate(recipe.layout(compact), recipe.difficulty, recipe.seed).board
        }

    // ---------- Jugar ----------

    fun onTileTap(tileId: Int) {
        act { it.tap(tileId) }
    }

    fun undo() {
        act { it.undo() }
    }

    fun shuffle() {
        val step = act { it.shuffle(Random.nextLong()) } ?: return
        if (step.event == GameEvent.NoShufflesLeft) offerExtraHelp(HelpKind.SHUFFLE)
    }

    fun restart() {
        act { it.restart() }
    }

    fun hint() {
        val current = _state.value
        val session = current.session ?: return
        if (current.busy) return
        _state.update { it.copy(busy = true) }
        work = viewModelScope.launch {
            val step = withContext(Dispatchers.Default) { session.requestHint() }
            apply(step)
            if (step.event == GameEvent.NoHintsLeft) offerExtraHelp(HelpKind.HINT)
        }
    }

    /** Acciones rápidas: se ejecutan en el momento, en el hilo principal. */
    private fun act(action: (GameSession) -> Step): Step? {
        val current = _state.value
        val session = current.session ?: return null
        if (current.busy) return null
        val step = action(session)
        apply(step)
        return step
    }

    private fun apply(step: Step) {
        val before = _state.value
        val session = step.session

        // Recién ganada: se apunta en el progreso, y se guarda antes que la partida.
        var progress = before.progress
        var win: WinOutcome? = before.win
        var interstitial = before.interstitialPending
        if (session.status == GameStatus.WON && before.session?.status != GameStatus.WON) {
            refreshToday()
            val recorded = recordWin(before.progress, before.mode, before.difficulty, _state.value.today)
            progress = recorded.progress
            win = recorded.outcome
            if (progress != before.progress) storage.saveProgress(progress)

            // ¿Toca intersticial? Solo si además hay uno cargado: si no, se intenta en la siguiente.
            adPacing = adPacing.recordWin()
            storage.saveAdPacing(adPacing)
            interstitial = adPacing.isInterstitialDue(System.currentTimeMillis(), before.adsRemoved) &&
                services.ads.isInterstitialReady()
        }

        _state.update {
            it.copy(
                session = session,
                busy = false,
                notice = GameNotice.after(step.event, session),
                progress = progress,
                win = win,
                interstitialPending = interstitial,
            )
        }
        // Solo se escribe si algo ha cambiado: tocar una ficha bloqueada devuelve la misma
        // sesión y no toca el disco.
        if (session !== before.session) storage.saveGame(before.difficulty, session, before.mode)
    }

    // ---------- Intersticial ----------

    /**
     * La interfaz va a mostrar el intersticial pendiente. Devuelve `false` si no hay ninguno
     * pendiente o ya se pidió (por ejemplo, al recrearse la actividad mientras se mostraba).
     */
    fun beginInterstitial(): Boolean {
        if (!_state.value.interstitialPending || interstitialInFlight) return false
        interstitialInFlight = true
        return true
    }

    /** El intersticial se ha cerrado ([shown]) o no se pudo mostrar: ahora, el aviso de victoria. */
    fun onInterstitialFinished(shown: Boolean) {
        interstitialInFlight = false
        if (shown) {
            adPacing = adPacing.interstitialShown(System.currentTimeMillis())
            storage.saveAdPacing(adPacing)
        }
        _state.update { it.copy(interstitialPending = false) }
    }

    // ---------- Ayuda extra ----------

    private fun offerExtraHelp(kind: HelpKind, rewardedAvailable: Boolean = _state.value.rewardedReady) {
        refreshToday()
        _state.update { it.copy(extraHelp = ExtraHelpOffer.of(kind, rewardedAvailable, it.helpQuota, it.today)) }
    }

    fun dismissExtraHelp() {
        _state.update { it.copy(extraHelp = null) }
    }

    /** El jugador ha pulsado «Ver vídeo»: el diálogo se cierra mientras se muestra. */
    fun onRewardedStarted() {
        _state.update { it.copy(extraHelp = null) }
    }

    /** El jugador ha visto el vídeo entero: se entrega la ayuda y se usa al momento. */
    fun onRewardEarned(kind: HelpKind) = grantAndUse(kind)

    /** El vídeo ha terminado. Si no se pudo mostrar, se ofrece lo que quede sin vídeo. */
    fun onRewardedClosed(kind: HelpKind, shown: Boolean) {
        if (!shown) offerExtraHelp(kind, rewardedAvailable = false)
    }

    /** Usa una ayuda de la cuota gratuita del día. */
    fun useFreeHelp() {
        val offer = _state.value.extraHelp as? ExtraHelpOffer.Free ?: return
        refreshToday()
        val quota = _state.value.helpQuota.use(offer.kind, _state.value.today)
        if (quota == null) {
            offerExtraHelp(offer.kind, rewardedAvailable = false)
            return
        }
        storage.saveHelpQuota(quota)
        _state.update { it.copy(helpQuota = quota, extraHelp = null) }
        grantAndUse(offer.kind)
    }

    private fun grantAndUse(kind: HelpKind) {
        val current = _state.value
        val session = current.session ?: return
        if (session.status == GameStatus.WON) return
        val granted = when (kind) {
            HelpKind.HINT -> session.grant(hints = 1)
            HelpKind.SHUFFLE -> session.grant(shuffles = 1)
        }
        _state.update { it.copy(session = granted) }
        storage.saveGame(current.difficulty, granted, current.mode)
        when (kind) {
            HelpKind.HINT -> hint()
            HelpKind.SHUFFLE -> shuffle()
        }
    }

    // ---------- Compra y privacidad ----------

    private fun onStore(store: StoreState) {
        val owned = when (store.ownership) {
            Ownership.OWNED -> true
            Ownership.NOT_OWNED -> false
            Ownership.PENDING, Ownership.UNKNOWN -> cachedRemoveAds
        }
        if (owned != cachedRemoveAds) {
            cachedRemoveAds = owned
            storage.saveRemoveAdsOwned(owned)
        }
        _state.update { it.copy(store = store, adsRemoved = adsRemoved(cachedRemoveAds, store)) }
    }

    /** «Restaurar compra»: vuelve a preguntar a Google Play y cuenta qué ha encontrado. */
    fun restorePurchase() {
        _state.update { it.copy(purchaseMessage = null) }
        services.removeAds.refresh { found ->
            val message = when (found) {
                true -> PurchaseMessage.RESTORED
                false -> PurchaseMessage.NOT_FOUND
                null -> PurchaseMessage.STORE_UNAVAILABLE
            }
            _state.update { it.copy(purchaseMessage = message) }
        }
    }

    /** No se pudo abrir la hoja de pago de Google Play. */
    fun onPurchaseUnavailable() {
        _state.update { it.copy(purchaseMessage = PurchaseMessage.STORE_UNAVAILABLE) }
        services.removeAds.refresh(null)
    }

    fun clearPurchaseMessage() {
        _state.update { it.copy(purchaseMessage = null) }
    }

    fun setPrivacyOptionsRequired(required: Boolean) {
        _state.update { it.copy(privacyOptionsRequired = required) }
    }

    // ---------- Ajustes ----------

    /**
     * Cambia el tamaño de ficha. Vale para la próxima partida, porque depende de la plantilla;
     * si la actual está sin empezar, se cambia ya por la misma partida en el tamaño nuevo (el
     * mismo nivel o reto; en la rápida, otro tablero de la misma dificultad).
     */
    fun setTileSize(tileSize: TileSize) {
        val current = _state.value
        if (current.settings.tileSize == tileSize) return
        updateSettings { it.copy(tileSize = tileSize) }
        val session = current.session
        if (session != null && !current.busy && isUntouched(session)) {
            startGame(current.mode, current.difficulty, showGame = false)
        }
    }

    fun setHighContrast(highContrast: Boolean) = updateSettings { it.copy(highContrast = highContrast) }

    /** Guarda el idioma. Para que se vea hay que recrear la actividad: lo hace `MainActivity`. */
    fun setLanguage(language: AppLanguage) = updateSettings { it.copy(language = language) }

    private fun updateSettings(change: (Settings) -> Settings) {
        _state.update { it.copy(settings = change(it.settings)) }
        storage.saveSettings(_state.value.settings)
    }

    private fun isUntouched(session: GameSession): Boolean =
        session.status != GameStatus.WON && session.moveCount == 0 &&
            session.board.remainingCount == session.board.tiles.size
}
