package azulejo.app.billing

/** Situación de la compra «Quitar anuncios» según Google Play. */
enum class Ownership {
    /** Aún no se sabe: Google Play no ha contestado (arrancando, sin conexión…). */
    UNKNOWN,

    /** Google Play confirma que esta cuenta no la tiene. */
    NOT_OWNED,

    /** Pagada con un método que tarda (efectivo, transferencia): aún no cuenta. */
    PENDING,

    OWNED,
}

/**
 * Lo que la interfaz necesita saber de la tienda. Kotlin puro.
 *
 * @property price precio con su moneda tal como lo da Google Play («$2.99», «2,99 €»), o
 *   `null` si no se ha podido consultar el producto.
 */
data class StoreState(
    val ownership: Ownership = Ownership.UNKNOWN,
    val price: String? = null,
) {
    /** Se puede ofrecer el botón de compra. */
    val canBuy: Boolean get() = price != null && (ownership == Ownership.NOT_OWNED || ownership == Ownership.UNKNOWN)
}

/**
 * Si los intersticiales están quitados.
 *
 * Mientras Google Play no contesta se usa lo último que se supo ([cachedOwned], guardado en
 * el teléfono), para que quien pagó no vea un anuncio por abrir la app sin conexión. Cuando
 * contesta, manda Google Play (por ejemplo, tras un reembolso).
 */
fun adsRemoved(cachedOwned: Boolean, store: StoreState): Boolean = when (store.ownership) {
    Ownership.OWNED -> true
    Ownership.NOT_OWNED -> false
    Ownership.PENDING, Ownership.UNKNOWN -> cachedOwned
}

/** Respuesta a «Restaurar compra», para enseñarla en Ajustes. */
enum class PurchaseMessage {
    RESTORED,
    NOT_FOUND,
    STORE_UNAVAILABLE,
}
