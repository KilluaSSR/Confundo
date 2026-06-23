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
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

data class AppSettings(
    val autoRefreshEnabled: Boolean = false,
    val intervalDays: Int = DEFAULT_INTERVAL_DAYS,
    val lastRunMillis: Long = 0L,
) {
    companion object {
        const val DEFAULT_INTERVAL_DAYS = 1
        const val MIN_INTERVAL_DAYS = 1
        const val MAX_INTERVAL_DAYS = 7
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
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            autoRefreshEnabled = p[Keys.AUTO_REFRESH] ?: false,
            intervalDays = (p[Keys.INTERVAL_DAYS] ?: AppSettings.DEFAULT_INTERVAL_DAYS)
                .coerceIn(AppSettings.MIN_INTERVAL_DAYS, AppSettings.MAX_INTERVAL_DAYS),
            lastRunMillis = p[Keys.LAST_RUN] ?: 0L,
        )
    }

    suspend fun setAutoRefreshEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_REFRESH] = enabled }
    }

    suspend fun setIntervalDays(days: Int) {
        context.settingsDataStore.edit {
            it[Keys.INTERVAL_DAYS] =
                days.coerceIn(AppSettings.MIN_INTERVAL_DAYS, AppSettings.MAX_INTERVAL_DAYS)
        }
    }

    suspend fun setLastRun(millis: Long) {
        context.settingsDataStore.edit { it[Keys.LAST_RUN] = millis }
    }
}
