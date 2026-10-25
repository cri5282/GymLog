package it.gymlog.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class OneRmFormula(val label: String) { EPLEY("Epley"), BRZYCKI("Brzycki") }

enum class ThemeMode(val label: String) { SYSTEM("Sistema"), LIGHT("Chiaro"), DARK("Scuro") }

enum class Sex(val label: String) { MALE("Uomo"), FEMALE("Donna") }

/** Icona dell'app (le varianti sono activity-alias nel manifest). */
enum class AppIcon(val label: String, val alias: String) {
    CLASSIC("Classica", ".MainActivityClassic"),
    DARK("Scura", ".MainActivityDark"),
    ACCENT("Accento", ".MainActivityAccent"),
}

/** Colore d'accento: valori ARGB per tema scuro e chiaro (spec §2). */
enum class Accent(val label: String, val dark: Long, val light: Long) {
    GREEN("Verde", 0xFF32D74B, 0xFF1FA33A),
    BLUE("Blu", 0xFF4C8DFF, 0xFF1F63E0),
    PURPLE("Viola", 0xFFA06BFF, 0xFF7A3FE0),
    ORANGE("Arancione", 0xFFFF9F1C, 0xFFD97A00),
    RED("Rosso", 0xFFFF5A5F, 0xFFD9363C),
    CYAN("Ciano", 0xFF22D3EE, 0xFF0596B5),
}

data class Settings(
    val defaultLoadStepKg: Double = 2.5,
    val oneRmFormula: OneRmFormula = OneRmFormula.EPLEY,
    val timerVibration: Boolean = true,
    val timerSound: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accent: Accent = Accent.GREEN,
    val keepScreenOn: Boolean = true,
    /** Serie di riscaldamento calcolate dai carichi dell'ultima volta. */
    val autoWarmup: Boolean = true,
    /** Dati facoltativi per composizione corporea e FFMI. */
    val heightCm: Double? = null,
    val birthYear: Int? = null,
    val sex: Sex = Sex.MALE,
    /** Backup automatico settimanale nella cartella scelta. */
    val autoBackup: Boolean = false,
    val backupTreeUri: String? = null,
    val lastBackupAt: Long = 0,
    val appIcon: AppIcon = AppIcon.CLASSIC,
)

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    /** Valori usati quando una chiave non è ancora stata scritta (i test disattivano il riscaldamento). */
    private val defaults: Settings = Settings(),
) {
    private object Keys {
        val step = doublePreferencesKey("default_load_step_kg")
        val formula = stringPreferencesKey("one_rm_formula")
        val vibration = booleanPreferencesKey("timer_vibration")
        val sound = booleanPreferencesKey("timer_sound")
        val themeMode = stringPreferencesKey("theme_mode")
        val accent = stringPreferencesKey("accent")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val autoWarmup = booleanPreferencesKey("auto_warmup")
        val height = doublePreferencesKey("height_cm")
        val birthYear = longPreferencesKey("birth_year")
        val sex = stringPreferencesKey("sex")
        val autoBackup = booleanPreferencesKey("auto_backup")
        val backupTree = stringPreferencesKey("backup_tree_uri")
        val lastBackup = longPreferencesKey("last_backup_at")
        val appIcon = stringPreferencesKey("app_icon")
    }

    private inline fun <reified E : Enum<E>> Preferences.enumOr(key: Preferences.Key<String>, default: E): E =
        this[key]?.let { v -> enumValues<E>().firstOrNull { it.name == v } } ?: default

    private fun read(p: Preferences): Settings {
        val d = defaults
        return Settings(
            defaultLoadStepKg = p[Keys.step] ?: d.defaultLoadStepKg,
            oneRmFormula = p.enumOr(Keys.formula, d.oneRmFormula),
            timerVibration = p[Keys.vibration] ?: d.timerVibration,
            timerSound = p[Keys.sound] ?: d.timerSound,
            themeMode = p.enumOr(Keys.themeMode, d.themeMode),
            accent = p.enumOr(Keys.accent, d.accent),
            keepScreenOn = p[Keys.keepScreenOn] ?: d.keepScreenOn,
            autoWarmup = p[Keys.autoWarmup] ?: d.autoWarmup,
            heightCm = p[Keys.height]?.takeIf { it > 0 },
            birthYear = p[Keys.birthYear]?.takeIf { it > 0 }?.toInt(),
            sex = p.enumOr(Keys.sex, d.sex),
            autoBackup = p[Keys.autoBackup] ?: d.autoBackup,
            backupTreeUri = p[Keys.backupTree]?.takeIf { it.isNotBlank() },
            lastBackupAt = p[Keys.lastBackup] ?: 0,
            appIcon = p.enumOr(Keys.appIcon, d.appIcon),
        )
    }

    val settings: Flow<Settings> = dataStore.data.map(::read)

    suspend fun current(): Settings = settings.first()

    suspend fun update(transform: (Settings) -> Settings) {
        dataStore.edit { p ->
            val next = transform(read(p))
            p[Keys.step] = next.defaultLoadStepKg
            p[Keys.formula] = next.oneRmFormula.name
            p[Keys.vibration] = next.timerVibration
            p[Keys.sound] = next.timerSound
            p[Keys.themeMode] = next.themeMode.name
            p[Keys.accent] = next.accent.name
            p[Keys.keepScreenOn] = next.keepScreenOn
            p[Keys.autoWarmup] = next.autoWarmup
            p[Keys.height] = next.heightCm ?: 0.0
            p[Keys.birthYear] = next.birthYear?.toLong() ?: 0L
            p[Keys.sex] = next.sex.name
            p[Keys.autoBackup] = next.autoBackup
            p[Keys.backupTree] = next.backupTreeUri.orEmpty()
            p[Keys.lastBackup] = next.lastBackupAt
            p[Keys.appIcon] = next.appIcon.name
        }
    }
}
