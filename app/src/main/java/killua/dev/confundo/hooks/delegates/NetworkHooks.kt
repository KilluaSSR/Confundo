package killua.dev.confundo.hooks.delegates

import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.hooks.spoof
import killua.dev.confundo.ui.pages.home.FieldKeys


object NetworkHooks : HookDelegate {

    override fun PackageParam.apply(fields: Map<String, String>) {
        val bssid = fields.spoof(FieldKeys.WIFI_BSSID)
        val ssid = fields.spoof(FieldKeys.WIFI_SSID)
        val mac = fields.spoof(FieldKeys.WIFI_MAC)
        if (bssid == null && ssid == null && mac == null) return

        "android.net.wifi.WifiInfo".toClassOrNull()?.hook {
            bssid?.let { v ->
                try {
                    injectMember {
                        method { name = "getBSSID" }
                        afterHook { result = v }
                    }
                } catch (_: NoSuchMethodError) {}
            }
            ssid?.let { v ->
                val quoted = if (v.startsWith("\"")) v else "\"$v\""
                try {
                    injectMember {
                        method { name = "getSSID" }
                        afterHook { result = quoted }
                    }
                } catch (_: NoSuchMethodError) {}
            }
            mac?.let { v ->
                try {
                    injectMember {
                        method { name = "getMacAddress" }
                        afterHook { result = v }
                    }
                } catch (_: NoSuchMethodError) {}
            }
        }
    }
}
