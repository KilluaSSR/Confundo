package killua.dev.confundo.work

import android.content.Context
import android.content.pm.PackageManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import killua.dev.arsenal.automation.ArsenalTask
import killua.dev.arsenal.automation.ProgressReporter
import killua.dev.arsenal.automation.TaskParams
import killua.dev.arsenal.automation.TaskResult
import killua.dev.arsenal.automation.di.TaskKey
import killua.dev.arsenal.data.settings.SettingsStore
import killua.dev.confundo.data.ConfigRepository
import killua.dev.confundo.data.ConfundoSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefreshTask @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val configRepository: ConfigRepository,
    private val settingsStore: SettingsStore,
) : ArsenalTask {

    override val name: String = NAME

    override suspend fun execute(params: TaskParams, reporter: ProgressReporter): TaskResult =
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .map { it.packageName }
                    .filter { it != context.packageName }

                packages.forEach { pkg ->
                    val cfg = configRepository.getAppConfig(pkg)
                    if (cfg.enabled && cfg.autoReset) {
                        configRepository.reshuffleFilledFields(pkg)
                    }
                }

                settingsStore.set(ConfundoSettings.lastRunMillis, System.currentTimeMillis())
                TaskResult.Success()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                TaskResult.Retry()
            }
        }

    companion object {
        const val NAME = "auto_refresh"
        const val UNIQUE_NAME = "auto_refresh_worker"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RefreshTaskModule {

    @Binds
    @IntoMap
    @TaskKey(RefreshTask.NAME)
    abstract fun bindRefreshTask(impl: RefreshTask): ArsenalTask
}
