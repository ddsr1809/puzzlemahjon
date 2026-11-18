package azulejo.app

import android.content.Context
import android.content.res.Configuration
import azulejo.app.data.AppLanguage
import java.util.Locale

/** Aplica el idioma elegido en Ajustes. */
object AppLocale {

    /**
     * Devuelve [base] con los textos en el idioma [language]. Con [AppLanguage.SYSTEM] lo
     * devuelve tal cual: manda el idioma del teléfono.
     */
    fun wrap(base: Context, language: AppLanguage): Context {
        val tag = language.tag ?: return base
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(tag))
        return base.createConfigurationContext(configuration)
    }
}
