package killua.dev.confundo.data

import android.content.Context
import android.content.pm.PackageManager
import com.highcapable.yukihookapi.hook.factory.prefs
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.confundo.ui.pages.home.FieldKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {

    companion object {
        private const val TEMPLATES_PREFS = "_templates"
        private const val IDS_KEY = "ids"
        private const val TEMPLATE_PREFIX = "_template_"
        private const val TEMPLATE_NAME_KEY = "name"
        private const val PKG_GMS = "com.google.android.gms"
        private const val PKG_VENDING = "com.android.vending"

        private fun templatePrefs(id: String) = "$TEMPLATE_PREFIX$id"
    }

    private fun installedVersionName(pkg: String): String? = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(pkg, 0).versionName
    }.getOrNull()

    private fun randomFields(includeActivationTime: Boolean, includeBootTime: Boolean): Map<String, String> =
        RandomEngine.generate(
            includeActivationTime = includeActivationTime,
            includeBootTime = includeBootTime,
            installedGmsVersion = installedVersionName(PKG_GMS),
            installedPlayVersion = installedVersionName(PKG_VENDING),
        )

    /** 失效总线。 */
    private val invalidations = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val writeMutex = Mutex()

    private suspend fun notifyChanged(name: String) {
        invalidations.emit(name)
    }

    // 每个 App 的配置
    data class AppConfig(
        val enabled: Boolean = false,
        val autoReset: Boolean = false,
        val nativeHookEnabled: Boolean = false,
        val fields: Map<String, String> = emptyMap(),
    )

    private fun readAppConfig(pkg: String): AppConfig {
        val p = context.prefs(pkg)
        val fields = FieldKeys.fieldEntries.associate { (key, _) ->
            key to runCatching { p.getString(key, "") }.getOrDefault("")
        }
        return AppConfig(
            enabled = runCatching { p.getBoolean(FieldKeys.ENABLED, false) }.getOrDefault(false),
            autoReset = runCatching { p.getBoolean(FieldKeys.AUTO_RESET, false) }.getOrDefault(false),
            nativeHookEnabled = runCatching { p.getBoolean(FieldKeys.NATIVE_HOOK_ENABLED, false) }.getOrDefault(false),
            fields = fields,
        )
    }

    fun appConfigFlow(pkg: String): Flow<AppConfig> =
        invalidations
            .onStart { emit(pkg) }
            .mapNotNull { changed ->
                if (changed == pkg || changed == FieldKeys.GLOBAL_PREFS) readAppConfig(pkg) else null
            }
            .flowOn(Dispatchers.IO)

    suspend fun getAppConfig(pkg: String): AppConfig =
        withContext(Dispatchers.IO) { readAppConfig(pkg) }

    suspend fun isEnabled(pkg: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { context.prefs(pkg).getBoolean(FieldKeys.ENABLED, false) }.getOrDefault(false)
    }

    fun enabledChanges(): Flow<Pair<String, Boolean>> =
        flow {
            invalidations.collect { changed ->
                if (changed != TEMPLATES_PREFS &&
                    changed != FieldKeys.GLOBAL_PREFS &&
                    !changed.startsWith(TEMPLATE_PREFIX)
                ) {
                    val enabled = runCatching {
                        context.prefs(changed).getBoolean(FieldKeys.ENABLED, false)
                    }.getOrDefault(false)
                    emit(changed to enabled)
                }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun enabledStates(pkgs: List<String>): Map<String, Boolean> =
        withContext(Dispatchers.IO) {
            pkgs.associateWith { pkg ->
                runCatching { context.prefs(pkg).getBoolean(FieldKeys.ENABLED, false) }
                    .getOrDefault(false)
            }
        }

    suspend fun setEnabled(pkg: String, enabled: Boolean) = write(pkg) {
        context.prefs(pkg).edit {
            putBoolean(FieldKeys.ENABLED, enabled)
            // 关闭 Java Hook 总开关时，自动关闭该 App 的 Native 开关。
            if (!enabled) putBoolean(FieldKeys.NATIVE_HOOK_ENABLED, false)
        }
    }

    suspend fun setAppNativeHook(pkg: String, enabled: Boolean) = write(pkg) {
        context.prefs(pkg).edit { putBoolean(FieldKeys.NATIVE_HOOK_ENABLED, enabled) }
    }

    fun isNativeHookEnabled(): Boolean = runCatching {
        context.prefs(FieldKeys.GLOBAL_PREFS).getBoolean(FieldKeys.NATIVE_HOOK_ENABLED, false)
    }.getOrDefault(false)

    /** 写入全局 Native Hook 总开关。被 Hook 进程在下次加载目标 App 时读取，实时生效。 */
    suspend fun setNativeHookEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            context.prefs(FieldKeys.GLOBAL_PREFS)
                .edit { putBoolean(FieldKeys.NATIVE_HOOK_ENABLED, enabled) }
        }
        notifyChanged(FieldKeys.GLOBAL_PREFS)
    }

    suspend fun setAutoReset(pkg: String, autoReset: Boolean) = write(pkg) {
        context.prefs(pkg).edit { putBoolean(FieldKeys.AUTO_RESET, autoReset) }
    }

    suspend fun updateField(pkg: String, key: String, value: String) = write(pkg) {
        context.prefs(pkg).edit { putString(key, value) }
    }

    /** 用一套随机值覆盖该 App 的所有字段，并强制启用。 */
    suspend fun randomFill(pkg: String) {
        val s = settingsRepository.current()
        write(pkg) {
            val values = randomFields(s.randomizeActivationTime, s.randomizeBootTime)
            context.prefs(pkg).edit {
                putBoolean(FieldKeys.ENABLED, true)
                values.forEach { (k, v) -> putString(k, v) }
            }
        }
    }

    /** 一键应用：对目标 App 启用并随机填充。 */
    suspend fun applyRandom(pkg: String, autoReset: Boolean) {
        val s = settingsRepository.current()
        write(pkg) {
            val values = randomFields(s.randomizeActivationTime, s.randomizeBootTime)
            context.prefs(pkg).edit {
                putBoolean(FieldKeys.ENABLED, true)
                putBoolean(FieldKeys.AUTO_RESET, autoReset)
                values.forEach { (k, v) -> putString(k, v) }
            }
        }
    }

    /** 将某模板的字段应用到指定 App。 */
    suspend fun applyTemplate(pkg: String, templateId: String) = write(pkg) {
        val tplFields = readTemplateFields(templateId)
        context.prefs(pkg).edit {
            putBoolean(FieldKeys.ENABLED, true)
            putBoolean(FieldKeys.AUTO_RESET, false)
            tplFields.forEach { (k, v) -> putString(k, v) }
        }
    }

    suspend fun reshuffleFilledFields(pkg: String) {
        val s = settingsRepository.current()
        write(pkg) {
            val current = readAppConfig(pkg)
            val fresh = randomFields(s.randomizeActivationTime, s.randomizeBootTime)
            context.prefs(pkg).edit {
                current.fields.forEach { (key, oldValue) ->
                    if (oldValue.isNotBlank()) {
                        // 仅用非空的新值刷新；空值（如关闭随机化的时间字段）不覆盖，避免误清除。
                        fresh[key]?.takeIf { it.isNotBlank() }?.let { putString(key, it) }
                    }
                }
            }
        }
    }

    /** 清空所有 App 的某个字段（仅处理当前存在非空值的 App）。 */
    suspend fun clearFieldForAll(key: String) = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val pkgs = runCatching {
            pm.getInstalledApplications(PackageManager.GET_META_DATA).map { it.packageName }
        }.getOrDefault(emptyList()).filter { it != context.packageName }

        pkgs.forEach { pkg ->
            val current = runCatching { context.prefs(pkg).getString(key, "") }.getOrDefault("")
            if (current.isNotBlank()) {
                writeMutex.withLock {
                    context.prefs(pkg).edit { putString(key, "") }
                }
                notifyChanged(pkg)
            }
        }
    }

    /** 当前设备已安装的包名集合（用于恢复时判断某个应用是否仍然存在）。 */
    suspend fun installedPackages(): Set<String> = withContext(Dispatchers.IO) {
        runCatching {
            context.packageManager
                .getInstalledApplications(PackageManager.GET_META_DATA)
                .map { it.packageName }
                .toSet()
        }.getOrDefault(emptySet())
    }

    /**
     * 导出所有「有意义」的 App 配置：即已启用、开启自动重置，或存在任意非空字段的应用。
     * 返回包名到配置的映射，供备份使用。
     */
    suspend fun exportConfigs(): Map<String, AppConfig> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val pkgs = runCatching {
            pm.getInstalledApplications(PackageManager.GET_META_DATA).map { it.packageName }
        }.getOrDefault(emptyList()).filter { it != context.packageName }

        pkgs.mapNotNull { pkg ->
            val cfg = runCatching { readAppConfig(pkg) }.getOrNull() ?: return@mapNotNull null
            val hasData = cfg.enabled || cfg.autoReset || cfg.fields.values.any { it.isNotBlank() }
            if (hasData) pkg to cfg else null
        }.toMap()
    }

    /** 将一整份配置写入指定 App（恢复时使用）。 */
    suspend fun writeAppConfig(pkg: String, config: AppConfig) = write(pkg) {
        context.prefs(pkg).edit {
            putBoolean(FieldKeys.ENABLED, config.enabled)
            putBoolean(FieldKeys.AUTO_RESET, config.autoReset)
            putBoolean(FieldKeys.NATIVE_HOOK_ENABLED, config.nativeHookEnabled)
            config.fields.forEach { (k, v) -> putString(k, v) }
        }
    }

    /** 导出全部模版详情（供备份使用）。 */
    suspend fun exportTemplates(): List<TemplateDetail> = withContext(Dispatchers.IO) {
        readTemplateIds().map { id ->
            val name = runCatching {
                context.prefs(templatePrefs(id)).getString(TEMPLATE_NAME_KEY, "")
            }.getOrDefault("")
            TemplateDetail(id, name, readTemplateFields(id))
        }
    }

    /** 导入单个模版（按 id 覆盖，若不存在则追加到列表）。 */
    suspend fun importTemplate(detail: TemplateDetail) {
        writeTemplates(listOf(TEMPLATES_PREFS, templatePrefs(detail.id))) {
            context.prefs(templatePrefs(detail.id)).edit {
                putString(TEMPLATE_NAME_KEY, detail.name)
                detail.fields.forEach { (k, v) -> putString(k, v) }
            }
            val ids = readTemplateIds()
            if (detail.id !in ids) writeTemplateIds(ids + detail.id)
        }
    }

    private suspend fun write(pkg: String, block: () -> Unit) {
        withContext(Dispatchers.IO) {
            writeMutex.withLock { block() }
        }
        notifyChanged(pkg)
    }

    private suspend fun writeTemplates(
        changedPrefs: List<String>,
        block: () -> Unit,
    ) {
        withContext(Dispatchers.IO) {
            writeMutex.withLock { block() }
        }
        changedPrefs.forEach { notifyChanged(it) }
    }

    // 模板
    data class Template(val id: String, val name: String)

    data class TemplateDetail(
        val id: String,
        val name: String,
        val fields: Map<String, String>,
    )

    private fun readTemplateIds(): List<String> {
        val json = runCatching {
            context.prefs(TEMPLATES_PREFS).getString(IDS_KEY, "[]")
        }.getOrDefault("[]")
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            throw IllegalStateException("Template ids corrupted", e)
        }
    }

    private fun writeTemplateIds(ids: List<String>) {
        val arr = JSONArray()
        ids.forEach { arr.put(it) }
        context.prefs(TEMPLATES_PREFS).edit { putString(IDS_KEY, arr.toString()) }
    }

    private fun readTemplateFields(id: String): Map<String, String> {
        val p = context.prefs(templatePrefs(id))
        return FieldKeys.fieldEntries.associate { (key, _) ->
            key to runCatching { p.getString(key, "") }.getOrDefault("")
        }
    }

    private fun readTemplates(): List<Template> = readTemplateIds().map { id ->
        val name = runCatching {
            context.prefs(templatePrefs(id)).getString(TEMPLATE_NAME_KEY, "")
        }.getOrDefault("")
        Template(id, name)
    }

    fun templatesFlow(): Flow<List<Template>> =
        invalidations
            .onStart { emit(TEMPLATES_PREFS) }
            .mapNotNull { changed -> if (changed == TEMPLATES_PREFS) readTemplates() else null }
            .flowOn(Dispatchers.IO)

    suspend fun getTemplates(): List<Template> = withContext(Dispatchers.IO) { readTemplates() }

    suspend fun getTemplateDetail(id: String): TemplateDetail = withContext(Dispatchers.IO) {
        val name = runCatching {
            context.prefs(templatePrefs(id)).getString(TEMPLATE_NAME_KEY, "")
        }.getOrDefault("")
        TemplateDetail(id, name, readTemplateFields(id))
    }


    suspend fun createTemplate(name: String): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        writeTemplates(listOf(TEMPLATES_PREFS)) {
            context.prefs(templatePrefs(id)).edit { putString(TEMPLATE_NAME_KEY, name) }
            writeTemplateIds(readTemplateIds() + id)
        }
        id
    }

    suspend fun setTemplateName(id: String, name: String) {
        writeTemplates(listOf(TEMPLATES_PREFS, templatePrefs(id))) {
            context.prefs(templatePrefs(id)).edit { putString(TEMPLATE_NAME_KEY, name) }
        }
    }

    suspend fun updateTemplateField(id: String, key: String, value: String) {
        writeTemplates(listOf(templatePrefs(id))) {
            context.prefs(templatePrefs(id)).edit { putString(key, value) }
        }
    }

    suspend fun randomFillTemplate(id: String) {
        val s = settingsRepository.current()
        writeTemplates(listOf(templatePrefs(id))) {
            val values = randomFields(s.randomizeActivationTime, s.randomizeBootTime)
            context.prefs(templatePrefs(id)).edit {
                values.forEach { (k, v) -> putString(k, v) }
            }
        }
    }

    fun templateDetailFlow(id: String): Flow<TemplateDetail> {
        val prefsName = templatePrefs(id)
        return invalidations
            .onStart { emit(prefsName) }
            .mapNotNull { changed ->
                if (changed == prefsName) {
                    val name = runCatching {
                        context.prefs(prefsName).getString(TEMPLATE_NAME_KEY, "")
                    }.getOrDefault("")
                    TemplateDetail(id, name, readTemplateFields(id))
                } else null
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun deleteTemplates(ids: List<String>) {
        writeTemplates(listOf(TEMPLATES_PREFS)) {
            val remaining = readTemplateIds().filter { it !in ids }
            writeTemplateIds(remaining)
            ids.forEach { context.prefs(templatePrefs(it)).edit { clear() } }
        }
    }
}
