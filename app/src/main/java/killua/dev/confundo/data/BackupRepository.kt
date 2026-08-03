package killua.dev.confundo.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.confundo.ui.pages.home.FieldKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 负责整个 App 的备份与恢复：
 * - 每个目标 App 的 hook 参数、是否开启、是否自动重置
 * - 全部模版
 * - 应用级设置（外观、时间字段、定时重置等）
 *
 * 备份文件为 JSON，恢复时对已卸载的应用会自动跳过，并对损坏/不兼容的文件给出明确错误。
 */
@Singleton
class BackupRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val configRepository: ConfigRepository,
    private val settingsRepository: SettingsRepository,
) {

    companion object {
        /** 备份格式版本，向后不兼容时递增。 */
        const val BACKUP_VERSION = 1

        private const val KEY_VERSION = "version"
        private const val KEY_CREATED_AT = "createdAt"
        private const val KEY_APP_ID = "appId"
        private const val KEY_SETTINGS = "settings"
        private const val KEY_APPS = "apps"
        private const val KEY_TEMPLATES = "templates"

        private const val KEY_PACKAGE = "packageName"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_AUTO_RESET = "autoReset"
        private const val KEY_FIELDS = "fields"

        private const val KEY_TEMPLATE_ID = "id"
        private const val KEY_TEMPLATE_NAME = "name"

        private const val KEY_AUTO_REFRESH = "autoRefreshEnabled"
        private const val KEY_INTERVAL_DAYS = "intervalDays"
        private const val KEY_DARK_MODE = "darkMode"
        private const val KEY_DYNAMIC_COLOR = "dynamicColor"
        private const val KEY_RANDOMIZE_ACTIVATION = "randomizeActivationTime"
        private const val KEY_RANDOMIZE_BOOT = "randomizeBootTime"
    }

    /** 恢复结果，用于向用户反馈。 */
    data class RestoreSummary(
        val restoredApps: Int,
        val skippedApps: Int,
        val restoredTemplates: Int,
        val settingsRestored: Boolean,
    )

    // ---------------------------------------------------------------------
    // 导出
    // ---------------------------------------------------------------------

    /** 将备份写入指定 Uri（由调用方通过 SAF 选择）。 */
    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        val json = buildBackupJson()
        val resolver = context.contentResolver
        val out = resolver.openOutputStream(uri, "wt")
            ?: throw IOException("无法打开导出文件")
        out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
    }

    private suspend fun buildBackupJson(): String {
        val root = JSONObject()
        root.put(KEY_VERSION, BACKUP_VERSION)
        root.put(KEY_CREATED_AT, System.currentTimeMillis())
        root.put(KEY_APP_ID, context.packageName)

        // 应用级设置
        val s = settingsRepository.current()
        val settingsObj = JSONObject().apply {
            put(KEY_AUTO_REFRESH, s.autoRefreshEnabled)
            put(KEY_INTERVAL_DAYS, s.intervalDays)
            put(KEY_DARK_MODE, s.darkMode)
            put(KEY_DYNAMIC_COLOR, s.dynamicColor)
            put(KEY_RANDOMIZE_ACTIVATION, s.randomizeActivationTime)
            put(KEY_RANDOMIZE_BOOT, s.randomizeBootTime)
        }
        root.put(KEY_SETTINGS, settingsObj)

        // 每个 App 的配置
        val appsArr = org.json.JSONArray()
        configRepository.exportConfigs().forEach { (pkg, cfg) ->
            val obj = JSONObject()
            obj.put(KEY_PACKAGE, pkg)
            obj.put(KEY_ENABLED, cfg.enabled)
            obj.put(KEY_AUTO_RESET, cfg.autoReset)
            obj.put(KEY_FIELDS, fieldsToJson(cfg.fields))
            appsArr.put(obj)
        }
        root.put(KEY_APPS, appsArr)

        // 模版
        val templatesArr = org.json.JSONArray()
        configRepository.exportTemplates().forEach { tpl ->
            val obj = JSONObject()
            obj.put(KEY_TEMPLATE_ID, tpl.id)
            obj.put(KEY_TEMPLATE_NAME, tpl.name)
            obj.put(KEY_FIELDS, fieldsToJson(tpl.fields))
            templatesArr.put(obj)
        }
        root.put(KEY_TEMPLATES, templatesArr)

        return root.toString(2)
    }

    private fun fieldsToJson(fields: Map<String, String>): JSONObject {
        val obj = JSONObject()
        // 仅导出非空字段，减小体积。
        fields.forEach { (k, v) -> if (v.isNotBlank()) obj.put(k, v) }
        return obj
    }

    // ---------------------------------------------------------------------
    // 恢复
    // ---------------------------------------------------------------------

    /** 从指定 Uri 读取并恢复备份。已卸载的应用会被跳过。 */
    suspend fun restoreFrom(uri: Uri): RestoreSummary = withContext(Dispatchers.IO) {
        val raw = context.contentResolver.openInputStream(uri)
            ?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: throw IOException("无法读取备份文件")
        restoreFromJson(raw)
    }

    private suspend fun restoreFromJson(raw: String): RestoreSummary {
        val root = try {
            JSONObject(raw)
        } catch (e: JSONException) {
            throw IllegalArgumentException("备份文件格式无效，无法解析", e)
        }

        val version = root.optInt(KEY_VERSION, -1)
        if (version <= 0) {
            throw IllegalArgumentException("这不是有效的 Confundo 备份文件")
        }
        if (version > BACKUP_VERSION) {
            throw IllegalArgumentException("备份文件版本 ($version) 高于当前应用支持的版本，请升级应用后重试")
        }

        // 恢复应用级设置
        var settingsRestored = false
        root.optJSONObject(KEY_SETTINGS)?.let { obj ->
            val restored = AppSettings(
                autoRefreshEnabled = obj.optBoolean(KEY_AUTO_REFRESH, false),
                intervalDays = obj.optInt(KEY_INTERVAL_DAYS, AppSettings.DEFAULT_INTERVAL_DAYS),
                darkMode = obj.optInt(KEY_DARK_MODE, AppSettings.DARK_MODE_SYSTEM),
                dynamicColor = obj.optBoolean(KEY_DYNAMIC_COLOR, true),
                randomizeActivationTime = obj.optBoolean(KEY_RANDOMIZE_ACTIVATION, false),
                randomizeBootTime = obj.optBoolean(KEY_RANDOMIZE_BOOT, false),
            )
            settingsRepository.importSettings(restored)
            settingsRestored = true
        }

        // 恢复每个 App 的配置，跳过已卸载的应用
        val installed = configRepository.installedPackages()
        var restoredApps = 0
        var skippedApps = 0
        root.optJSONArray(KEY_APPS)?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val pkg = obj.optString(KEY_PACKAGE, "").takeIf { it.isNotBlank() } ?: continue
                if (pkg !in installed) {
                    skippedApps++
                    continue
                }
                val config = ConfigRepository.AppConfig(
                    enabled = obj.optBoolean(KEY_ENABLED, false),
                    autoReset = obj.optBoolean(KEY_AUTO_RESET, false),
                    fields = jsonToFields(obj.optJSONObject(KEY_FIELDS)),
                )
                runCatching { configRepository.writeAppConfig(pkg, config) }
                    .onSuccess { restoredApps++ }
                    .onFailure { skippedApps++ }
            }
        }

        // 恢复模版
        var restoredTemplates = 0
        root.optJSONArray(KEY_TEMPLATES)?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString(KEY_TEMPLATE_ID, "").takeIf { it.isNotBlank() } ?: continue
                val detail = ConfigRepository.TemplateDetail(
                    id = id,
                    name = obj.optString(KEY_TEMPLATE_NAME, ""),
                    fields = jsonToFields(obj.optJSONObject(KEY_FIELDS)),
                )
                runCatching { configRepository.importTemplate(detail) }
                    .onSuccess { restoredTemplates++ }
            }
        }

        return RestoreSummary(
            restoredApps = restoredApps,
            skippedApps = skippedApps,
            restoredTemplates = restoredTemplates,
            settingsRestored = settingsRestored,
        )
    }

    /** 只保留已知字段键，忽略未知/无效项，保证向后兼容。 */
    private fun jsonToFields(obj: JSONObject?): Map<String, String> {
        if (obj == null) return emptyMap()
        val validKeys = FieldKeys.fieldEntries.map { it.first }.toSet()
        val result = LinkedHashMap<String, String>()
        obj.keys().forEach { key ->
            if (key in validKeys) {
                val value = obj.optString(key, "")
                if (value.isNotBlank()) result[key] = value
            }
        }
        return result
    }
}
