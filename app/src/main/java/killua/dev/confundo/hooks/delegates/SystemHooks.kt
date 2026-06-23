package killua.dev.confundo.hooks.delegates

import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.hooks.spoof
import killua.dev.confundo.ui.pages.home.FieldKeys

/**
 * 内核版本 (os.version) / 开机时间 (elapsedRealtime) / 24 小时制 Hook。
 */
object SystemHooks : HookDelegate {

    override fun PackageParam.apply(fields: Map<String, String>) {
        hookKernel(fields)
        hookBootTime(fields)
        hook24Hour(fields)
    }

    private fun PackageParam.hookKernel(fields: Map<String, String>) {
        val kernel = fields.spoof(FieldKeys.KERNEL) ?: return
        "java.lang.System".toClassOrNull()?.hook {
            try {
                injectMember {
                    method { name = "getProperty"; paramCount = 1 }
                    beforeHook {
                        if (args().first().string() == "os.version") result = kernel
                    }
                }
            } catch (_: NoSuchMethodError) {}
        }
    }

    private fun PackageParam.hookBootTime(fields: Map<String, String>) {
        val bootTime = fields.spoof(FieldKeys.BOOT_TIME)?.toLongOrNull() ?: return
        "android.os.SystemClock".toClassOrNull()?.hook {
            try {
                injectMember {
                    method { name = "elapsedRealtime" }
                    afterHook { result = System.currentTimeMillis() - bootTime }
                }
            } catch (_: NoSuchMethodError) {}
        }
    }

    private fun PackageParam.hook24Hour(fields: Map<String, String>) {
        val is24h = fields.spoof(FieldKeys.IS_24H)?.toBooleanStrictOrNull() ?: return
        "android.text.format.DateFormat".toClassOrNull()?.hook {
            try {
                injectMember {
                    method { name = "is24HourFormat" }
                    afterHook { result = is24h }
                }
            } catch (_: NoSuchMethodError) {}
        }
    }
}
