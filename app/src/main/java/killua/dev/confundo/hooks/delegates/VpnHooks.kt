package killua.dev.confundo.hooks.delegates

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.ui.pages.home.FieldKeys
import java.net.NetworkInterface
import java.util.Collections

/**
 * Hides the common Java-layer VPN detection paths inside a target app.
 *
 * Capability checks are answered at the query methods rather than by mutating
 * the object, so results delivered through NetworkCallback are covered too.
 */
object VpnHooks : HookDelegate {

    override fun PackageParam.apply(fields: Map<String, String>) {
        if (fields[FieldKeys.HIDE_VPN]?.toBooleanStrictOrNull() != true) return
        hookCapabilities()
        hookNetworkInfo()
        hookInterfaces()
    }

    private fun PackageParam.hookCapabilities() {
        NetworkCapabilities::class.java.hook {
            try {
                injectMember {
                    method { name = "hasTransport"; param(Int::class.java) }
                    beforeHook {
                        if (args[0] == NetworkCapabilities.TRANSPORT_VPN) result = false
                    }
                }
            } catch (_: Throwable) {
            }

            try {
                injectMember {
                    method { name = "hasCapability"; param(Int::class.java) }
                    beforeHook {
                        if (args[0] == NetworkCapabilities.NET_CAPABILITY_NOT_VPN) result = true
                    }
                }
            } catch (_: Throwable) {
            }

            try {
                injectMember {
                    method { name = "getTransportTypes"; emptyParam() }
                    afterHook {
                        val types = result as? IntArray ?: return@afterHook
                        result = types.filter { it != NetworkCapabilities.TRANSPORT_VPN }.toIntArray()
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun PackageParam.hookNetworkInfo() {
        ConnectivityManager::class.java.hook {
            try {
                injectMember {
                    method { name = "getNetworkInfo"; param(Int::class.java) }
                    beforeHook {
                        @Suppress("DEPRECATION")
                        if (args[0] == ConnectivityManager.TYPE_VPN) result = null
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun PackageParam.hookInterfaces() {
        NetworkInterface::class.java.hook {
            try {
                injectMember {
                    method { name = "getNetworkInterfaces"; emptyParam() }
                    afterHook {
                        @Suppress("UNCHECKED_CAST")
                        val interfaces = result as? java.util.Enumeration<NetworkInterface>
                            ?: return@afterHook
                        val visible = Collections.list(interfaces).filterNot { isVpnInterface(it.name) }
                        result = Collections.enumeration(visible)
                    }
                }
            } catch (_: Throwable) {
            }

            try {
                injectMember {
                    method { name = "getByName"; param(String::class.java) }
                    beforeHook {
                        if (isVpnInterface(args[0] as? String)) result = null
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun isVpnInterface(name: String?): Boolean {
        val normalized = name?.lowercase() ?: return false
        return normalized.startsWith("tun") ||
            normalized.startsWith("tap") ||
            normalized.startsWith("ppp") ||
            normalized.startsWith("wg") ||
            normalized.startsWith("ipsec")
    }
}
