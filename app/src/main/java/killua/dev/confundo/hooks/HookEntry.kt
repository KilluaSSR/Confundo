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

            // 全空则整体放行真实值。
            if (fields.values.all { it.isEmpty() }) return@loadApp

            delegates.forEach { delegate ->
                runCatching {
                    with(delegate) { apply(fields) }
                }.onFailure { YLog.error("Delegate ${delegate.javaClass.simpleName} failed", it) }
            }
        }
    }

    private val delegates = listOf(
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
