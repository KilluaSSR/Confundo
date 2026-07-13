package killua.dev.confundo.hooks.delegates

import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.data.SettingsKey
import killua.dev.confundo.data.SettingsNamespace
import killua.dev.confundo.hooks.HookDelegate

/**
 * Settings.Secure/System/Global Hook。
 */
object SettingsHooks : HookDelegate {

    override fun PackageParam.apply(fields: Map<String, String>) {
        SettingsNamespace.entries.forEach { ns ->
            hookSettings(ns, SettingsKey.forNamespace(ns), fields)
        }
    }

    private fun PackageParam.hookSettings(
        ns: SettingsNamespace,
        keys: Map<String, String>,
        fields: Map<String, String>,
    ) {
        if (keys.isEmpty()) return
        val clazz = ns.className.toClassOrNull() ?: return

        clazz.hook {
            try {
                injectMember {
                    method { name = "getString"; paramCount = 2 }
                    beforeHook {
                        val settingName = args().last().string()
                        val fieldKey = keys[settingName] ?: return@beforeHook
                        resolveStringValue(settingName, fieldKey, fields)?.let { result = it }
                    }
                }
            } catch (_: NoSuchMethodError) {}

            listOf(2, 3).forEach { count ->
                try {
                    injectMember {
                        method { name = "getInt"; paramCount = count }
                        beforeHook {
                            val settingName = args[1] as? String ?: return@beforeHook
                            val fieldKey = keys[settingName] ?: return@beforeHook
                            resolveIntValue(settingName, fieldKey, fields)?.let { result = it }
                        }
                    }
                } catch (_: NoSuchMethodError) {}
            }
        }
    }

    private fun resolveStringValue(
        settingName: String,
        fieldKey: String,
        fields: Map<String, String>,
    ): String? {
        val raw = fields[fieldKey]?.takeIf { it.isNotEmpty() } ?: return null
        return when (settingName) {
            "enabled_accessibility_services" -> {
                val count = raw.toIntOrNull()?.coerceAtLeast(0) ?: return null
                if (count == 0) "" else List(count) { index -> "confundo/.SpoofService$index" }.joinToString(":")
            }

            "master_mono",
            "touch_exploration_enabled",
            "high_text_contrast_enabled",
            "accessibility_display_inversion_enabled",
            "accessibility_display_daltonizer_enabled",
            "accessibility_enabled",
            "captions_enabled" -> boolToIntString(raw)

            "incall_power_button_behavior" -> powerEndsCallBehavior(raw)?.toString()
            else -> raw
        }
    }

    private fun resolveIntValue(
        settingName: String,
        fieldKey: String,
        fields: Map<String, String>,
    ): Int? {
        val raw = fields[fieldKey]?.takeIf { it.isNotEmpty() } ?: return null
        return when (settingName) {
            "enabled_accessibility_services" -> raw.toIntOrNull()?.coerceAtLeast(0)
            "master_mono",
            "touch_exploration_enabled",
            "high_text_contrast_enabled",
            "accessibility_display_inversion_enabled",
            "accessibility_display_daltonizer_enabled",
            "accessibility_enabled",
            "captions_enabled" -> boolToInt(raw)

            "incall_power_button_behavior" -> powerEndsCallBehavior(raw)
            else -> raw.toIntOrNull()
        }
    }

    private fun boolToInt(raw: String): Int? = raw.toBooleanStrictOrNull()?.let { if (it) 1 else 0 }

    private fun boolToIntString(raw: String): String? = boolToInt(raw)?.toString()

    private fun powerEndsCallBehavior(raw: String): Int? {
        return when {
            raw.toBooleanStrictOrNull() == true -> 2
            raw.toBooleanStrictOrNull() == false -> 1
            else -> raw.toIntOrNull()?.takeIf { it in 1..2 }
        }
    }
}
