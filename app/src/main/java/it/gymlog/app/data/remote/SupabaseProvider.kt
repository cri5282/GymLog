package it.gymlog.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import it.gymlog.app.BuildConfig

/**
 * Client Supabase, creato solo se il progetto è configurato.
 * Senza `SUPABASE_URL`/`SUPABASE_ANON_KEY` (build di test o repo appena clonato) vale `null` e la
 * condivisione resta spenta: l'app funziona come la 1.5.0.
 */
object SupabaseProvider {
    val client: SupabaseClient? by lazy {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY
        if (url.isBlank() || key.isBlank()) return@lazy null
        createSupabaseClient(url, key) {
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }
}
