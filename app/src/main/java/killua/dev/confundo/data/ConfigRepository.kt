package killua.dev.confundo.data

import android.content.Context
import com.highcapable.yukihookapi.hook.factory.prefs
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.confundo.ui.pages.home.FieldKeys
import kotlinx.coroutines.Dispatchers
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
) {

    companion object {
        private const val TEMPLATES_PREFS = "_templates"
        private const val IDS_KEY = "ids"
        private const val TEMPLATE_PREFIX = "_template_"
        private const val TEMPLATE_NAME_KEY = "name"

        private fun templatePrefs(id: String) = "$TEMPLATE_PREFIX$id"
    }

    /** 失效总线。 */
    private val invalidations = MutableSharedFlow<String>(extraBufferCapacity = 64)

    private val writeMutex = Mutex()

    private suspend fun notifyChanged(name: String) {
        invalidations.emit(name)
    }

    // 每个 App 的配置
    data class AppConfig(
        val enabled: Boolean = false,
        val autoReset: Boolean = false,
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
            fields = fields,
        )
    }

    fun appConfigFlow(pkg: String): Flow<AppConfig> =
        invalidations
            .onStart { emit(pkg) }
            .mapNotNull { changed -> if (changed == pkg) readAppConfig(pkg) else null }
            .flowOn(Dispatchers.IO)

    suspend fun getAppConfig(pkg: String): AppConfig =
        withContext(Dispatchers.IO) { readAppConfig(pkg) }

    suspend fun isEnabled(pkg: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { context.prefs(pkg).getBoolean(FieldKeys.ENABLED, false) }.getOrDefault(false)
    }

    fun enabledChanges(): Flow<Pair<String, Boolean>> =
        flow {
            invalidations.collect { changed ->
                if (changed != TEMPLATES_PREFS && !changed.startsWith(TEMPLATE_PREFIX)) {
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
        context.prefs(pkg).edit { putBoolean(FieldKeys.ENABLED, enabled) }
    }

    suspend fun setAutoReset(pkg: String, autoReset: Boolean) = write(pkg) {
        context.prefs(pkg).edit { putBoolean(FieldKeys.AUTO_RESET, autoReset) }
    }

    suspend fun updateField(pkg: String, key: String, value: String) = write(pkg) {
        context.prefs(pkg).edit { putString(key, value) }
    }

    /** 用一套随机值覆盖该 App 的所有字段，并强制启用。 */
    suspend fun randomFill(pkg: String) = write(pkg) {
        val values = RandomEngine.generate()
        context.prefs(pkg).edit {
            putBoolean(FieldKeys.ENABLED, true)
            values.forEach { (k, v) -> putString(k, v) }
        }
    }

    /** 一键应用：对目标 App 启用并随机填充。 */
    suspend fun applyRandom(pkg: String, autoReset: Boolean) = write(pkg) {
        val values = RandomEngine.generate()
        context.prefs(pkg).edit {
            putBoolean(FieldKeys.ENABLED, true)
            putBoolean(FieldKeys.AUTO_RESET, autoReset)
            values.forEach { (k, v) -> putString(k, v) }
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

    suspend fun reshuffleFilledFields(pkg: String) = write(pkg) {
        val current = readAppConfig(pkg)
        val fresh = RandomEngine.generate()
        context.prefs(pkg).edit {
            current.fields.forEach { (key, oldValue) ->
                if (oldValue.isNotEmpty()) {
                    fresh[key]?.let { putString(key, it) }
                }
            }
        }
    }

    private suspend fun write(pkg: String, block: () -> Unit) {
        withContext(Dispatchers.IO) {
            writeMutex.withLock { runCatching(block) }
        }
        notifyChanged(pkg)
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
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
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
        writeMutex.withLock {
            runCatching {
                context.prefs(templatePrefs(id)).edit { putString(TEMPLATE_NAME_KEY, name) }
                writeTemplateIds(readTemplateIds() + id)
            }
        }
        notifyChanged(TEMPLATES_PREFS)
        id
    }

    suspend fun setTemplateName(id: String, name: String) {
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                runCatching {
                    context.prefs(templatePrefs(id)).edit { putString(TEMPLATE_NAME_KEY, name) }
                }
            }
        }
        notifyChanged(TEMPLATES_PREFS)
        notifyChanged(templatePrefs(id))
    }

    suspend fun updateTemplateField(id: String, key: String, value: String) {
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                runCatching { context.prefs(templatePrefs(id)).edit { putString(key, value) } }
            }
        }
        notifyChanged(templatePrefs(id))
    }

    suspend fun randomFillTemplate(id: String) {
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                runCatching {
                    val values = RandomEngine.generate()
                    context.prefs(templatePrefs(id)).edit {
                        values.forEach { (k, v) -> putString(k, v) }
                    }
                }
            }
        }
        notifyChanged(templatePrefs(id))
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
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                runCatching {
                    val remaining = readTemplateIds().filter { it !in ids }
                    writeTemplateIds(remaining)
                    ids.forEach { context.prefs(templatePrefs(it)).edit { clear() } }
                }
            }
        }
        notifyChanged(TEMPLATES_PREFS)
    }
}
