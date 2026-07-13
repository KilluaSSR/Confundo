package killua.dev.confundo.hooks.delegates

import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.hooks.spoof
import killua.dev.confundo.ui.pages.home.FieldKeys

/**
 * 显示刷新率 Hook。
 */
object DisplayHooks : HookDelegate {

    override fun PackageParam.apply(fields: Map<String, String>) {
        val refreshRate = fields.spoof(FieldKeys.MAX_REFRESH_RATE)
            ?.replace(Regex("[^0-9.]"), "")?.toFloatOrNull() ?: return

        "android.view.Display".toClassOrNull()?.hook {
            try {
                injectMember {
                    method { name = "getRefreshRate" }
                    afterHook { result = refreshRate }
                }
            } catch (_: NoSuchMethodError) {}
        }

        "android.view.Display\$Mode".toClassOrNull()?.hook {
            try {
                injectMember {
                    method { name = "getRefreshRate" }
                    afterHook { result = refreshRate }
                }
            } catch (_: NoSuchMethodError) {}

            try {
                injectMember {
                    method { name = "getAlternativeRefreshRates" }
                    afterHook { result = floatArrayOf(refreshRate) }
                }
            } catch (_: NoSuchMethodError) {}
        }
    }
}
