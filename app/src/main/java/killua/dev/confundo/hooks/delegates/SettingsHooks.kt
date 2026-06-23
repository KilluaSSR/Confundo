package killua.dev.confundo.hooks.delegates

import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.data.SettingsKey
import killua.dev.confundo.data.SettingsNamespace
import killua.dev.confundo.hooks.HookDelegate

/**
 * Settings.Secure/System/Global.getString Hook。
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
                        fields[fieldKey]?.takeIf { it.isNotEmpty() }?.let { result = it }
                    }
                }
            } catch (_: NoSuchMethodError) {}
        }
    }
}
