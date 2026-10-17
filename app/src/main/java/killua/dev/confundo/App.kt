package killua.dev.confundo

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import killua.dev.arsenal.core.di.ApplicationScope
import killua.dev.arsenal.data.settings.SettingsReactorRegistry
import killua.dev.arsenal.data.settings.SettingsStore
import killua.dev.arsenal.error.ArsenalErrorHandler
import killua.dev.arsenal.error.ErrorConfig
import killua.dev.confundo.data.SettingsMigrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settingsStore: SettingsStore

    @Inject
    lateinit var settingsMigrator: SettingsMigrator

    @Inject
    lateinit var reactorRegistryProvider: Provider<SettingsReactorRegistry>

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ArsenalErrorHandler.install(
            application = this,
            config = ErrorConfig(
                restartActivity = MainActivity::class.java,
                showStackTrace = BuildConfig.DEBUG,
            ),
        )
        appScope.launch {
            settingsStore.awaitLoaded()
            settingsMigrator.migrateIfNeeded()
            reactorRegistryProvider.get().ensureStarted()
        }
    }

    private val workConfig: Configuration by lazy {
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    override val workManagerConfiguration: Configuration
        get() = workConfig
}
