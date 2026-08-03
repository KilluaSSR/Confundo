package killua.dev.confundo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

data class AppSettings(
    val autoRefreshEnabled: Boolean = false,
    val intervalDays: Int = DEFAULT_INTERVAL_DAYS,
    val lastRunMillis: Long = 0L,
    /** 0 = 跟随系统, 1 = 浅色, 2 = 深色 */
    val darkMode: Int = DARK_MODE_SYSTEM,
    val dynamicColor: Boolean = true,
    /** 是否随机化「开机激活时间」。关闭时该字段保持为空。 */
    val randomizeActivationTime: Boolean = false,
    /** 是否随机化「开机时间」。关闭时该字段保持为空。 */
    val randomizeBootTime: Boolean = false,
) {
    companion object {
        const val DEFAULT_INTERVAL_DAYS = 1
        const val MIN_INTERVAL_DAYS = 1
        const val MAX_INTERVAL_DAYS = 7

        const val DARK_MODE_SYSTEM = 0
        const val DARK_MODE_LIGHT = 1
        const val DARK_MODE_DARK = 2
    }
}

@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val AUTO_REFRESH = booleanPreferencesKey("auto_refresh_enabled")
        val INTERVAL_DAYS = intPreferencesKey("auto_refresh_interval_days")
        val LAST_RUN = longPreferencesKey("auto_refresh_last_run")
        val DARK_MODE = intPreferencesKey("dark_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val RANDOMIZE_ACTIVATION_TIME = booleanPreferencesKey("randomize_activation_time")
        val RANDOMIZE_BOOT_TIME = booleanPreferencesKey("randomize_boot_time")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            autoRefreshEnabled = p[Keys.AUTO_REFRESH] ?: false,
            intervalDays = (p[Keys.INTERVAL_DAYS] ?: AppSettings.DEFAULT_INTERVAL_DAYS)
                .coerceIn(AppSettings.MIN_INTERVAL_DAYS, AppSettings.MAX_INTERVAL_DAYS),
            lastRunMillis = p[Keys.LAST_RUN] ?: 0L,
            darkMode = p[Keys.DARK_MODE] ?: AppSettings.DARK_MODE_SYSTEM,
            dynamicColor = p[Keys.DYNAMIC_COLOR] ?: true,
            randomizeActivationTime = p[Keys.RANDOMIZE_ACTIVATION_TIME] ?: false,
            randomizeBootTime = p[Keys.RANDOMIZE_BOOT_TIME] ?: false,
        )
    }

    /** 同步读取当前设置快照（供仓库层在生成随机值时判断开关）。 */
    suspend fun current(): AppSettings = settings.first()

    suspend fun setRandomizeActivationTime(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.RANDOMIZE_ACTIVATION_TIME] = enabled }
    }

    suspend fun setRandomizeBootTime(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.RANDOMIZE_BOOT_TIME] = enabled }
    }

    suspend fun setDarkMode(mode: Int) {
        context.settingsDataStore.edit { it[Keys.DARK_MODE] = mode }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setAutoRefreshEnabled(enabled: Boolean) {
        context.settingsDataStore.edit {
            if (it[Keys.AUTO_REFRESH] != enabled) {
                it[Keys.AUTO_REFRESH] = enabled
            }
        }
    }

    suspend fun setIntervalDays(days: Int) {
        val normalized = days.coerceIn(AppSettings.MIN_INTERVAL_DAYS, AppSettings.MAX_INTERVAL_DAYS)
        context.settingsDataStore.edit {
            if (it[Keys.INTERVAL_DAYS] != normalized) {
                it[Keys.INTERVAL_DAYS] = normalized
            }
        }
    }

    suspend fun setLastRun(millis: Long) {
        context.settingsDataStore.edit {
            if (it[Keys.LAST_RUN] != millis) {
                it[Keys.LAST_RUN] = millis
            }
        }
    }

    /** 用备份中的设置覆盖当前设置（不还原 lastRunMillis）。 */
    suspend fun importSettings(s: AppSettings) {
        context.settingsDataStore.edit {
            it[Keys.AUTO_REFRESH] = s.autoRefreshEnabled
            it[Keys.INTERVAL_DAYS] = s.intervalDays
                .coerceIn(AppSettings.MIN_INTERVAL_DAYS, AppSettings.MAX_INTERVAL_DAYS)
            it[Keys.DARK_MODE] = s.darkMode
            it[Keys.DYNAMIC_COLOR] = s.dynamicColor
            it[Keys.RANDOMIZE_ACTIVATION_TIME] = s.randomizeActivationTime
            it[Keys.RANDOMIZE_BOOT_TIME] = s.randomizeBootTime
        }
    }
}
