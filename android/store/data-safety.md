# Seguridad de los datos y otras declaraciones de Play Console

Respuestas propuestas para *Policy → App content* en Play Console, para la versión 0.5.0
(hito 5). Comprobadas el 18 de noviembre de 2026 con la página de Google sobre los datos que
recoge el SDK GMA Next-Gen («Google Play data disclosure») y la guía de Android sobre tipos de
datos. **La responsabilidad de lo que se declara es del desarrollador**: si cambias los SDK o la
configuración, revisa estas respuestas. No soy abogado; tómalo como un borrador razonado.

## Qué recoge la app

| Origen | Qué | Sale del teléfono |
| --- | --- | --- |
| La app | Ajustes, progreso, partida, ritmo de anuncios, cuota gratuita, si se compró «Quitar anuncios» | No (solo en el teléfono) |
| SDK de anuncios (GMA Next-Gen) | Dirección IP (ubicación aproximada), ID de publicidad y App set ID, interacciones (aperturas, toques, vídeos vistos), diagnóstico (rendimiento, cierres) | Sí, a Google, cifrado (TLS) |
| UMP (consentimiento) | La respuesta del jugador al mensaje de privacidad | Se guarda en el teléfono; los anuncios la envían con cada petición |
| Play Billing | Si la cuenta tiene «Quitar anuncios» | Lo gestiona Google Play |

## Formulario «Data safety»

**Data collection and security**

- *Does your app collect or share any of the required user data types?* **Yes**.
- *Is all of the user data collected by your app encrypted in transit?* **Yes**.
- *Do you provide a way for users to request that their data is deleted?* **No**. La app no
  tiene cuentas ni servidores: lo que guarda está en el teléfono y se borra al desinstalarla. Lo
  que recoge el SDK de anuncios lo controla Google. (Si prefieres ofrecer borrado por correo,
  puedes responder *Yes* y dar `damian@d2600.com`, pero entonces tienes que atender esas
  peticiones.)

**Data types** (marca solo estos):

| Categoría → tipo | Collected | Shared | Ephemeral | Required | Purposes |
| --- | --- | --- | --- | --- | --- |
| Location → **Approximate location** | Sí | Sí | No | Required | Advertising or marketing · Analytics · Fraud prevention, security, and compliance |
| App activity → **App interactions** | Sí | Sí | No | Required | Advertising or marketing · Analytics |
| App info and performance → **Diagnostics** | Sí | Sí | No | Required | Analytics · Fraud prevention, security, and compliance |
| Device or other IDs → **Device or other IDs** | Sí | Sí | No | Required | Advertising or marketing · Analytics · Fraud prevention, security, and compliance |
| Financial info → **Purchase history** | Sí | No | No | Optional | App functionality |

Notas:

- La ubicación aproximada sale de la dirección IP: Google indica que puede usarse para estimar la
  ubicación general del dispositivo.
- *Purchase history*: la guía de Android pone el uso de la Play Billing Library como ejemplo de
  acceso a datos financieros. Es conservador declararlo; es «Optional» porque solo existe si el
  jugador compra.
- No marques *Personal info*, *Contacts*, *Photos*, *Precise location*, *Crash logs* ni *Other
  app performance data* salvo que añadas otro SDK (por ejemplo, analítica o Crashlytics).

## Otras declaraciones

| Declaración | Respuesta |
| --- | --- |
| **Ads** | *Yes, my app contains ads*. |
| **Advertising ID** | *Yes*. Usos: *Advertising or marketing*, *Analytics*, *Fraud prevention, security, and compliance*. El SDK de anuncios añade el permiso `com.google.android.gms.permission.AD_ID` al manifiesto final; compruébalo en Android Studio con *APK Analyzer → AndroidManifest.xml* sobre el bundle de release. |
| **Target audience and content** | 18 y más. La app no está dirigida a niños. Capturas y textos sobrios, sin personajes ni lenguaje infantil. |
| **Privacy policy** | `https://ddsr1809.github.io/puzzlemahjon/privacy/` (README, «Publicar la política de privacidad»). |
| **App access** | *All functionality is available without special access* (no hay inicio de sesión). |
| **Content rating** | Cuestionario IARC: juego de puzle, sin violencia, sin apuestas, sin interacción entre usuarios, sin compartir ubicación. Responde *Sí* a «contiene anuncios» y a «compras dentro de la app» si te lo pregunta. |
| **Financial features** | *My app doesn't provide any financial features*. |
| **Government apps / Health / News** | No. |

## Cuándo revisar estas respuestas

- Al añadir analítica, Crashlytics, mediación de otras redes de anuncios o un servidor propio.
- Si Google cambia su página de datos del SDK GMA Next-Gen.
- Si la app pasa a estar dirigida también a niños (eso cambiaría los anuncios: haría falta
  marcar las peticiones para menores y usar solo redes certificadas para familias).
