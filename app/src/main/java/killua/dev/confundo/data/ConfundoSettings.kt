package killua.dev.confundo.data

import killua.dev.arsenal.data.settings.boolSetting
import killua.dev.arsenal.data.settings.enumSetting
import killua.dev.arsenal.data.settings.intSetting
import killua.dev.arsenal.data.settings.longSetting

enum class DarkMode {
    System, Light, Dark;

    fun toBackupInt(): Int = when (this) {
        System -> 0
        Light -> 1
        Dark -> 2
    }

    companion object {
        fun fromBackupInt(value: Int): DarkMode = when (value) {
            1 -> Light
            2 -> Dark
            else -> System
        }
    }
}

object ConfundoSettings {

    val autoRefreshEnabled = boolSetting("auto_refresh_enabled", false)
    val refreshIntervalDays = intSetting("auto_refresh_interval_days", DEFAULT_INTERVAL_DAYS)
    val lastRunMillis = longSetting("auto_refresh_last_run", 0L)

    val randomizeActivationTime = boolSetting("randomize_activation_time", false)
    val randomizeBootTime = boolSetting("randomize_boot_time", false)

    val nativeHookEnabled = boolSetting("native_hook_enabled", false)

    val darkMode = enumSetting("dark_mode", DarkMode.System)
    val dynamicColor = boolSetting("dynamic_color", true)

    val legacyMigrated = boolSetting("legacy_settings_migrated_v1", false)

    const val DEFAULT_INTERVAL_DAYS = 1
    const val MIN_INTERVAL_DAYS = 1
    const val MAX_INTERVAL_DAYS = 7
}
