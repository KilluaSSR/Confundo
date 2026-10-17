package killua.dev.confundo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.ExistingPeriodicWorkPolicy
import com.highcapable.yukihookapi.hook.factory.prefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import killua.dev.arsenal.automation.TaskScheduler
import killua.dev.arsenal.data.settings.SettingBinding
import killua.dev.arsenal.data.settings.SettingsStore
import killua.dev.arsenal.data.settings.bindSetting
import killua.dev.confundo.ui.pages.home.FieldKeys
import killua.dev.confundo.work.RefreshTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsBindings {

    /** Native Hook 全局开关 → SP（Hook 进程读取 confundo_global）。 */
    @Provides
    @IntoSet
    fun nativeHookBridge(@ApplicationContext context: Context): SettingBinding<*> =
        bindSetting(ConfundoSettings.nativeHookEnabled) { enabled ->
            withContext(Dispatchers.IO) {
                context.prefs(FieldKeys.GLOBAL_PREFS).edit {
                    putBoolean(FieldKeys.NATIVE_HOOK_ENABLED, enabled)
                }
            }
        }

    /** 自动重置开关 → 调度/取消周期任务（启动时也会应用一次当前值）。 */
    @Provides
    @IntoSet
    fun autoRefreshScheduler(
        store: SettingsStore,
        taskScheduler: TaskScheduler,
    ): SettingBinding<*> = bindSetting(ConfundoSettings.autoRefreshEnabled) { enabled ->
        if (enabled) {
            scheduleRefresh(taskScheduler, store.get(ConfundoSettings.refreshIntervalDays))
        } else {
            taskScheduler.cancelUnique(RefreshTask.UNIQUE_NAME)
        }
    }

    /** 重置周期变化 → 若已开启则按新周期重调度。 */
    @Provides
    @IntoSet
    fun refreshIntervalScheduler(
        store: SettingsStore,
        taskScheduler: TaskScheduler,
    ): SettingBinding<*> = bindSetting(ConfundoSettings.refreshIntervalDays) { days ->
        if (store.get(ConfundoSettings.autoRefreshEnabled)) {
            scheduleRefresh(taskScheduler, days)
        }
    }

    private fun scheduleRefresh(taskScheduler: TaskScheduler, intervalDays: Int) {
        taskScheduler.enqueuePeriodic(
            taskName = RefreshTask.NAME,
            uniqueName = RefreshTask.UNIQUE_NAME,
            repeatIntervalMs = TimeUnit.DAYS.toMillis(
                intervalDays.coerceIn(
                    ConfundoSettings.MIN_INTERVAL_DAYS,
                    ConfundoSettings.MAX_INTERVAL_DAYS,
                ).toLong(),
            ),
            existingPolicy = ExistingPeriodicWorkPolicy.UPDATE,
        )
    }
}

private val Context.legacySettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
)

@Singleton
class SettingsMigrator @Inject constructor(
    private val store: SettingsStore,
    private val configRepository: ConfigRepository,
    @param:ApplicationContext private val context: Context,
) {
    private object LegacyKeys {
        val AUTO_REFRESH = booleanPreferencesKey("auto_refresh_enabled")
        val INTERVAL_DAYS = intPreferencesKey("auto_refresh_interval_days")
        val LAST_RUN = longPreferencesKey("auto_refresh_last_run")
        val DARK_MODE = intPreferencesKey("dark_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val RANDOMIZE_ACTIVATION_TIME = booleanPreferencesKey("randomize_activation_time")
        val RANDOMIZE_BOOT_TIME = booleanPreferencesKey("randomize_boot_time")
    }

    suspend fun migrateIfNeeded() {
        if (store.get(ConfundoSettings.legacyMigrated)) return

        val legacy = context.legacySettingsDataStore.data.first()
        store.set(
            ConfundoSettings.autoRefreshEnabled,
            legacy[LegacyKeys.AUTO_REFRESH] ?: false,
        )
        store.set(
            ConfundoSettings.refreshIntervalDays,
            (legacy[LegacyKeys.INTERVAL_DAYS] ?: ConfundoSettings.DEFAULT_INTERVAL_DAYS)
                .coerceIn(ConfundoSettings.MIN_INTERVAL_DAYS, ConfundoSettings.MAX_INTERVAL_DAYS),
        )
        store.set(ConfundoSettings.lastRunMillis, legacy[LegacyKeys.LAST_RUN] ?: 0L)
        store.set(
            ConfundoSettings.darkMode,
            when (legacy[LegacyKeys.DARK_MODE] ?: 0) {
                1 -> DarkMode.Light
                2 -> DarkMode.Dark
                else -> DarkMode.System
            },
        )
        store.set(ConfundoSettings.dynamicColor, legacy[LegacyKeys.DYNAMIC_COLOR] ?: true)
        store.set(
            ConfundoSettings.randomizeActivationTime,
            legacy[LegacyKeys.RANDOMIZE_ACTIVATION_TIME] ?: false,
        )
        store.set(
            ConfundoSettings.randomizeBootTime,
            legacy[LegacyKeys.RANDOMIZE_BOOT_TIME] ?: false,
        )
        store.set(ConfundoSettings.nativeHookEnabled, configRepository.isNativeHookEnabled())
        store.set(ConfundoSettings.legacyMigrated, true)
    }
}
