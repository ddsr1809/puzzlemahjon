package azulejo.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * [KeyValueStore] sobre las preferencias de Android: un archivo privado de la app, sin
 * permisos ni dependencias. `apply()` actualiza el valor en memoria al momento y lo escribe
 * en disco en segundo plano, así que guardar tras cada jugada no frena la interfaz.
 */
class PrefsStore(context: Context) : KeyValueStore {

    private val prefs: SharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun read(key: String): String? = try {
        prefs.getString(key, null)
    } catch (e: ClassCastException) {
        // La clave existe con otro tipo: se trata como si no estuviera.
        null
    }

    override fun write(key: String, value: String?) {
        val editor = prefs.edit()
        if (value == null) editor.remove(key) else editor.putString(key, value)
        editor.apply()
    }

    private companion object {
        const val FILE_NAME = "azulejo"
    }
}
