package killua.dev.confundo.hooks.delegates

import android.content.pm.PackageInfo
import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.hooks.spoof
import killua.dev.confundo.ui.pages.home.FieldKeys

/**
 * 激活时间：Build.TIME 静态字段 + File.lastModified + PackageInfo 安装时间。
 */
object ActivationTimeHooks : HookDelegate {
    override fun PackageParam.apply(fields: Map<String, String>) {
        val baseDayTime = fields.spoof(FieldKeys.ACTIVATION_TIME)?.toLongOrNull() ?: return

        runCatching {
            android.os.Build::class.java.getDeclaredField("TIME").apply {
                isAccessible = true
                set(null, baseDayTime)
            }
        }

        java.io.File::class.java.hook {
            try {
                injectMember {
                    method { name = "lastModified" }
                    afterHook {
                        val path = (instance as java.io.File).absolutePath
                        if (path.endsWith("/Android") || path.endsWith("/Android/") || path.contains("emulated/0")) {
                            result = baseDayTime
                        }
                    }
                }
            } catch (_: NoSuchMethodError) {}
        }

        "android.app.ApplicationPackageManager".toClassOrNull()?.hook {
            try {
                injectMember {
                    method { name = "getPackageInfo" }
                    afterHook {
                        val pi = result as? PackageInfo ?: return@afterHook
                        modifyInstallTime(pi, baseDayTime)
                    }
                }
            } catch (_: NoSuchMethodError) {}

            try {
                injectMember {
                    method { name = "getInstalledPackages" }
                    afterHook {
                        (result as? List<*>)?.forEach { item ->
                            val pi = item as? PackageInfo
                            if (pi != null) {
                                modifyInstallTime(pi, baseDayTime)
                            }
                        }
                    }
                }
            } catch (_: NoSuchMethodError) {}
        }
    }

    private fun modifyInstallTime(pi: PackageInfo, targetTime: Long) {
        pi.firstInstallTime = targetTime
        pi.lastUpdateTime = targetTime
    }
}
