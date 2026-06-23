package killua.dev.confundo.work

import android.content.Context
import android.content.pm.PackageManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import killua.dev.confundo.data.ConfigRepository
import killua.dev.confundo.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 定时随机刷新引擎。
 *
 * - 仅处理 **已启用** 且 **AUTO_RESET=true** 的 App。
 * - 对每个目标 App，仅重新随机化其 **已填充** 的字段；未填充字段保持为空
 * - 每个 App 独立生成随机集合。
 *
 */
@HiltWorker
class RefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val configRepository: ConfigRepository,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            val pm = applicationContext.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .map { it.packageName }
                .filter { it != applicationContext.packageName }

            packages.forEach { pkg ->
                val cfg = configRepository.getAppConfig(pkg)
                if (cfg.enabled && cfg.autoReset) {
                    configRepository.reshuffleFilledFields(pkg)
                }
            }

            settingsRepository.setLastRun(System.currentTimeMillis())
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }

    companion object {
        private const val UNIQUE_NAME = "auto_refresh_worker"

        /** 根据设置安排或取消周期任务。 */
        fun schedule(context: Context, intervalDays: Int) {
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(
                intervalDays.toLong().coerceAtLeast(1L), TimeUnit.DAYS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }

        /** 立即触发一次刷新。 */
        fun runNow(context: Context) {
            val request = androidx.work.OneTimeWorkRequestBuilder<RefreshWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
