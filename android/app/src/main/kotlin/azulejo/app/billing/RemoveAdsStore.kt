package azulejo.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * La compra única «Quitar anuncios» con Google Play Billing.
 *
 * - Es un producto único no consumible ([PRODUCT_ID]), creado en Play Console.
 * - La compra la guarda Google Play en la cuenta del jugador: al reinstalar o en otro teléfono
 *   con la misma cuenta, [refresh] la encuentra («restaurar»).
 * - Toda compra se confirma (acknowledge); si no, Google Play la reembolsa a los 3 días.
 * - Un pago pendiente (efectivo, transferencia) no quita los anuncios hasta completarse.
 *
 * No hay servidor propio que verifique las compras: para un producto que solo quita
 * anuncios, el riesgo de una compra falsificada no compensa mantener uno.
 */
class RemoveAdsStore(context: Context) : PurchasesUpdatedListener {

    private val _state = MutableStateFlow(StoreState())
    val state: StateFlow<StoreState> = _state.asStateFlow()

    @Volatile private var productDetails: ProductDetails? = null
    @Volatile private var connecting = false

    /** Quien espera el resultado de «Restaurar compra» mientras se conecta. */
    @Volatile private var pendingRefresh: ((Boolean?) -> Unit)? = null

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        // Si Google Play se desconecta, la biblioteca vuelve a conectar sola en la siguiente llamada.
        .enableAutoServiceReconnection()
        .build()

    /** Conecta con Google Play y consulta el precio y si la compra ya está hecha. */
    fun connect() {
        refresh(null)
    }

    /**
     * Vuelve a preguntar a Google Play. [onResult] recibe `true` si la compra está hecha,
     * `false` si no, y `null` si Google Play no contesta (sin conexión, sin cuenta…).
     */
    fun refresh(onResult: ((Boolean?) -> Unit)?) {
        if (client.isReady) {
            if (productDetails == null) queryProduct()
            queryOwned(onResult)
            return
        }
        if (onResult != null) pendingRefresh = onResult
        if (connecting) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                connecting = false
                val waiting = pendingRefresh
                pendingRefresh = null
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    queryProduct()
                    queryOwned(waiting)
                } else {
                    Log.i(TAG, "Google Play Billing no disponible: ${billingResult.debugMessage}")
                    waiting?.invoke(null)
                }
            }

            override fun onBillingServiceDisconnected() {
                // Con la reconexión automática no hay que hacer nada aquí.
                connecting = false
            }
        })
    }

    /**
     * Abre la hoja de pago de Google Play. `false` si no se puede (aún no hay precio, Google
     * Play no responde…); el resultado de la compra llega a [onPurchasesUpdated].
     */
    fun launchPurchase(activity: Activity): Boolean {
        val details = productDetails ?: return false
        val product = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details)
        // Desde Billing 8 un producto único puede tener varias opciones de compra; se usa la primera.
        details.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken?.let { product.setOfferToken(it) }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(product.build()))
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode == BillingResponseCode.ITEM_ALREADY_OWNED) queryOwned(null)
        return result.responseCode == BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> apply(purchases.orEmpty().firstOrNull { PRODUCT_ID in it.products }, confirmedAbsent = false)
            // Ya la tenía (por ejemplo, comprada en otro teléfono): se busca para aplicarla.
            BillingResponseCode.ITEM_ALREADY_OWNED -> queryOwned(null)
            BillingResponseCode.USER_CANCELED -> Unit
            else -> Log.i(TAG, "Compra sin completar: ${billingResult.responseCode} ${billingResult.debugMessage}")
        }
    }

    private fun queryProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        client.queryProductDetailsAsync(params) { billingResult, result ->
            if (billingResult.responseCode != BillingResponseCode.OK) return@queryProductDetailsAsync
            val details = result.productDetailsList.firstOrNull { it.productId == PRODUCT_ID }
            productDetails = details
            val price = details?.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
                ?: details?.oneTimePurchaseOfferDetails?.formattedPrice
            _state.update { it.copy(price = price) }
        }
    }

    private fun queryOwned(onResult: ((Boolean?) -> Unit)?) {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingResponseCode.OK) {
                onResult?.invoke(null)
                return@queryPurchasesAsync
            }
            val purchase = purchases.firstOrNull { PRODUCT_ID in it.products }
            apply(purchase, confirmedAbsent = true)
            onResult?.invoke(purchase?.purchaseState == Purchase.PurchaseState.PURCHASED)
        }
    }

    /**
     * Apunta lo que dice Google Play. [confirmedAbsent]: la respuesta es la lista completa de
     * compras, así que si no está, no la tiene (por ejemplo, tras un reembolso).
     */
    private fun apply(purchase: Purchase?, confirmedAbsent: Boolean) {
        val ownership = when {
            purchase == null -> if (confirmedAbsent) Ownership.NOT_OWNED else return
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED -> Ownership.OWNED
            purchase.purchaseState == Purchase.PurchaseState.PENDING -> Ownership.PENDING
            else -> Ownership.NOT_OWNED
        }
        _state.update { it.copy(ownership = ownership) }
        if (purchase != null && ownership == Ownership.OWNED && !purchase.isAcknowledged) acknowledge(purchase)
    }

    private fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        client.acknowledgePurchase(params) { billingResult ->
            // Si falla, la próxima consulta (al abrir la app) lo vuelve a intentar.
            if (billingResult.responseCode != BillingResponseCode.OK) {
                Log.w(TAG, "Confirmación pendiente: ${billingResult.debugMessage}")
            }
        }
    }

    companion object {
        /** Id del producto en Play Console (*Monetize → Products → One-time products*). No cambiarlo. */
        const val PRODUCT_ID = "remove_ads"

        private const val TAG = "AzulejoBilling"
    }
}
