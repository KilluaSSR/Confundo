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
    private const val FLAGS_CLASS = "android.content.pm.PackageManager\$PackageInfoFlags"

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
                        if (shouldSpoofLastModified(path)) {
                            result = baseDayTime
                        }
                    }
                }
            } catch (_: NoSuchMethodError) {}
        }

        "android.app.ApplicationPackageManager".toClassOrNull()?.hook {
            val hasFlagsClass = FLAGS_CLASS.toClassOrNull() != null
            try {
                injectMember {
                    method { name = "getPackageInfo"; param(String::class.java, Int::class.java) }
                    afterHook { patchPackageInfoResult(result, baseDayTime) }
                }
            } catch (_: NoSuchMethodError) {}

            if (hasFlagsClass) {
                try {
                    injectMember {
                        method { name = "getPackageInfo"; param(String::class.java, FLAGS_CLASS) }
                        afterHook { patchPackageInfoResult(result, baseDayTime) }
                    }
                } catch (_: NoSuchMethodError) {}
            }

            try {
                injectMember {
                    method { name = "getInstalledPackages"; param(Int::class.java) }
                    afterHook { patchInstalledPackagesResult(result, baseDayTime) }
                }
            } catch (_: NoSuchMethodError) {}

            if (hasFlagsClass) {
                try {
                    injectMember {
                        method { name = "getInstalledPackages"; param(FLAGS_CLASS) }
                        afterHook { patchInstalledPackagesResult(result, baseDayTime) }
                    }
                } catch (_: NoSuchMethodError) {}
            }
        }
    }

    private fun shouldSpoofLastModified(path: String): Boolean {
        val normalized = path.trimEnd('/')
        return normalized == "/storage/emulated/0/Android" ||
                normalized.startsWith("/storage/emulated/0/Android/")
    }

    private fun patchPackageInfoResult(result: Any?, targetTime: Long) {
        val pi = result as? PackageInfo ?: return
        modifyInstallTime(pi, targetTime)
    }

    private fun patchInstalledPackagesResult(result: Any?, targetTime: Long) {
        (result as? List<*>)?.forEach { item ->
            (item as? PackageInfo)?.let { modifyInstallTime(it, targetTime) }
        }
    }

    private fun modifyInstallTime(pi: PackageInfo, targetTime: Long) {
        pi.firstInstallTime = targetTime
        pi.lastUpdateTime = targetTime
    }
}
