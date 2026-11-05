package it.gymlog.app.data.remote

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsCodeVerifierCache
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import it.gymlog.app.BuildConfig

/**
 * Client Supabase, creato solo se il progetto è configurato.
 * Senza `SUPABASE_URL`/`SUPABASE_ANON_KEY` (build di test o repo appena clonato) vale `null` e la
 * condivisione resta spenta: l'app funziona come la 1.5.0.
 *
 * La sessione usa un file SharedPreferences esplicito (`gymlog-auth`), escluso dai backup:
 * dopo un ripristino si rifà il login.
 */
object SupabaseProvider {
    const val SESSION_PREFS = "gymlog-auth"

    fun create(context: Context): SupabaseClient? {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY
        if (url.isBlank() || key.isBlank()) return null
        val prefs = SharedPreferencesSettings(
            context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
        )
        return createSupabaseClient(url, key) {
            install(Auth) {
                sessionManager = SettingsSessionManager(prefs)
                codeVerifierCache = SettingsCodeVerifierCache(prefs)
            }
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }
}
