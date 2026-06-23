package killua.dev.confundo.hooks.delegates

import android.content.pm.PackageInfo
import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.hooks.spoof
import killua.dev.confundo.ui.pages.home.FieldKeys

/**
 * Google 广告 ID / GMS 版本 / Play Store 版本 Hook。
 */
object GoogleHooks : HookDelegate {

    private const val PKG_GMS = "com.google.android.gms"
    private const val PKG_VENDING = "com.android.vending"
    private const val FLAGS_CLASS = "android.content.pm.PackageManager\$PackageInfoFlags"

    override fun PackageParam.apply(fields: Map<String, String>) {
        adIdHook(fields)
        val gms = fields.spoof(FieldKeys.GMS_VERSION)
        val play = fields.spoof(FieldKeys.PLAY_STORE_VERSION)
        if (gms != null || play != null) packageVersionHook(gms, play)
    }

    private fun PackageParam.adIdHook(fields: Map<String, String>) {
        val adId = fields.spoof(FieldKeys.GOOGLE_AD_ID) ?: return
        "com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info".toClassOrNull()?.hook {
            try {
                injectMember {
                    method { name = "getId" }
                    afterHook { result = adId }
                }
            } catch (_: NoSuchMethodError) {}
        }
    }

    private fun PackageParam.packageVersionHook(gmsVersion: String?, playVersion: String?) {
        val pm = "android.app.ApplicationPackageManager".toClassOrNull() ?: return

        fun applyVersion(pi: PackageInfo, version: String) {
            val code = version.replace(Regex("[^0-9]"), "").take(9).toLongOrNull() ?: 0L
            pi.versionName = version
            pi.longVersionCode = code
            @Suppress("DEPRECATION")
            pi.versionCode = code.toInt()
        }

        fun YukiAfter(pkgName: String?, result: Any?) {
            val pi = result as? PackageInfo ?: return
            when (pkgName) {
                PKG_GMS -> gmsVersion?.let { applyVersion(pi, it) }
                PKG_VENDING -> playVersion?.let { applyVersion(pi, it) }
            }
        }

        pm.hook {
            // getPackageInfo(String, int)
            try {
                injectMember {
                    method { name = "getPackageInfo"; param(String::class.java, Int::class.java) }
                    afterHook { YukiAfter(args[0] as? String, result) }
                }
            } catch (_: NoSuchMethodError) {}
            try {
                injectMember {
                    method { name = "getPackageInfo"; param(String::class.java, FLAGS_CLASS) }
                    afterHook { YukiAfter(args[0] as? String, result) }
                }
            } catch (_: NoSuchMethodError) {}
        }
    }
}
