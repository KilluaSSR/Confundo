package killua.dev.confundo.hooks.delegates

import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.data.DrmSecurityLevel
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.hooks.spoof
import killua.dev.confundo.ui.pages.home.FieldKeys

/**
 * Widevine DRM Hook：
 * - `getPropertyByteArray("deviceUniqueId")` -> 唯一设备 ID
 * - `getPropertyString("securityLevel")` -> L1 / L3 安全级别
 *
 */
object MediaDrmHooks : HookDelegate {

    override fun PackageParam.apply(fields: Map<String, String>) {
        val drmId = fields.spoof(FieldKeys.DRM_ID)
        val securityLevel = DrmSecurityLevel.from(fields.spoof(FieldKeys.DRM_SECURITY_LEVEL))?.storage
        if (drmId == null && securityLevel == null) return

        "android.media.MediaDrm".toClassOrNull()?.hook {
            drmId?.let { id ->
                try {
                    injectMember {
                        method { name = "getPropertyByteArray"; paramCount = 1 }
                        beforeHook {
                            if (args().first().string() == "deviceUniqueId") {
                                result = id.toByteArray()
                            }
                        }
                    }
                } catch (_: NoSuchMethodError) {}
            }
            securityLevel?.let { level ->
                try {
                    injectMember {
                        method { name = "getPropertyString"; paramCount = 1 }
                        beforeHook {
                            if (args().first().string() == "securityLevel") {
                                result = level
                            }
                        }
                    }
                } catch (_: NoSuchMethodError) {}
            }
        }
    }
}
