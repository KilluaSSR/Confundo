package killua.dev.confundo.hooks

import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import killua.dev.confundo.hooks.delegates.ActivationTimeHooks
import killua.dev.confundo.hooks.delegates.BatteryHooks
import killua.dev.confundo.hooks.delegates.BuildHooks
import killua.dev.confundo.hooks.delegates.DisplayHooks
import killua.dev.confundo.hooks.delegates.GoogleHooks
import killua.dev.confundo.hooks.delegates.HardwareHooks
import killua.dev.confundo.hooks.delegates.LocaleHooks
import killua.dev.confundo.hooks.delegates.MediaDrmHooks
import killua.dev.confundo.hooks.delegates.NativeHooks
import killua.dev.confundo.hooks.delegates.NetworkHooks
import killua.dev.confundo.hooks.delegates.OpenGLHooks
import killua.dev.confundo.hooks.delegates.SensorHooks
import killua.dev.confundo.hooks.delegates.SettingsHooks
import killua.dev.confundo.hooks.delegates.SystemHooks
import killua.dev.confundo.hooks.delegates.SystemPropertiesHooks
import killua.dev.confundo.hooks.delegates.TelephonyHooks
import killua.dev.confundo.ui.pages.home.FieldKeys

@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {
    override fun onHook() = encase {
        loadApp(isExcludeSelf = true) {
            val pkg = packageName

            val enabled = runCatching {
                prefs(pkg).getBoolean(FieldKeys.ENABLED, false)
            }.getOrDefault(false)
            if (!enabled) return@loadApp

            val fields = FieldKeys.fieldEntries.associate { (key, _) ->
                key to runCatching { prefs(pkg).getString(key, "") }.getOrDefault("")
            }
            val normalizedFields = normalizeSdkFloor(fields)

            // 全空则整体放行真实值。
            if (normalizedFields.values.all { it.isEmpty() }) return@loadApp

            val nativeGlobal = runCatching {
                prefs(FieldKeys.GLOBAL_PREFS).getBoolean(FieldKeys.NATIVE_HOOK_ENABLED, false)
            }.getOrDefault(false)
            val nativePerApp = runCatching {
                prefs(pkg).getBoolean(FieldKeys.NATIVE_HOOK_ENABLED, false)
            }.getOrDefault(false)
            val nativeEnabled = nativeGlobal && nativePerApp

            val activeDelegates =
                if (nativeEnabled) delegates else delegates.filterNot { it === NativeHooks }

            activeDelegates.forEach { delegate ->
                runCatching {
                    with(delegate) { apply(normalizedFields) }
                }.onFailure { YLog.error("Delegate ${delegate.javaClass.simpleName} failed", it) }
            }
        }
    }

    private fun normalizeSdkFloor(fields: Map<String, String>): Map<String, String> {
        val requestedRaw = fields[FieldKeys.SDK_INT]?.trim().orEmpty()
        if (requestedRaw.isEmpty()) return fields

        val effectiveSdk = requestedRaw
            .toIntOrNull()
            ?.coerceAtLeast(android.os.Build.VERSION.SDK_INT)
        val normalizedSdk = effectiveSdk?.toString().orEmpty()
        if (normalizedSdk == requestedRaw) return fields

        return fields.toMutableMap().apply {
            put(FieldKeys.SDK_INT, normalizedSdk)
        }
    }

    private val delegates = listOf(
        NativeHooks,
        BuildHooks,
        SystemPropertiesHooks,
        SystemHooks,
        TelephonyHooks,
        SettingsHooks,
        GoogleHooks,
        MediaDrmHooks,
        NetworkHooks,
        LocaleHooks,
        DisplayHooks,
        BatteryHooks,
        HardwareHooks,
        ActivationTimeHooks,
        OpenGLHooks,
        SensorHooks,
    )
}
