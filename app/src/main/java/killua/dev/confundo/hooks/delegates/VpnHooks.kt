package killua.dev.confundo.hooks.delegates

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.NetworkRequest
import android.net.RouteInfo
import android.net.TransportInfo
import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.ui.pages.home.FieldKeys
import java.net.NetworkInterface
import java.util.Collections

object VpnHooks : HookDelegate {

    private val VPN_TRANSPORT_BIT = 1L shl NetworkCapabilities.TRANSPORT_VPN

    override fun PackageParam.apply(fields: Map<String, String>) {
        if (fields[FieldKeys.HIDE_VPN]?.toBooleanStrictOrNull() != true) return
        hookCapabilities()
        hookNetworkInfo()
        hookConnectivity()
        hookInterfaces()
        hookLinkProperties()
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

            try {
                injectMember {
                    method { name = "getCapabilities"; emptyParam() }
                    afterHook {
                        val caps = result as? IntArray ?: return@afterHook
                        if (!caps.contains(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) {
                            result = caps + NetworkCapabilities.NET_CAPABILITY_NOT_VPN
                        }
                    }
                }
            } catch (_: Throwable) {
            }

            try {
                injectMember {
                    method { name = "getTransportInfo"; emptyParam() }
                    afterHook {
                        val info = result as? TransportInfo ?: return@afterHook
                        if (info.javaClass.name.endsWith("VpnTransportInfo")) result = null
                    }
                }
            } catch (_: Throwable) {
            }

            try {
                injectMember {
                    method { name = "toString"; emptyParam() }
                    afterHook {
                        val text = result as? String ?: return@afterHook
                        result = scrubCapabilitiesText(text)
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun PackageParam.hookNetworkInfo() {
        NetworkInfo::class.java.hook {
            listOf("getType", "getSubtype").forEach { target ->
                try {
                    injectMember {
                        method { name = target; emptyParam() }
                        afterHook {
                            @Suppress("DEPRECATION")
                            if (result == ConnectivityManager.TYPE_VPN) {
                                @Suppress("DEPRECATION")
                                result = ConnectivityManager.TYPE_WIFI
                            }
                        }
                    }
                } catch (_: Throwable) {
                }
            }

            listOf("getTypeName", "getSubtypeName").forEach { target ->
                try {
                    injectMember {
                        method { name = target; emptyParam() }
                        afterHook {
                            val value = result as? String ?: return@afterHook
                            if (value.contains("VPN", ignoreCase = true)) result = "WIFI"
                        }
                    }
                } catch (_: Throwable) {
                }
            }

            listOf("isConnected", "isConnectedOrConnecting").forEach { target ->
                try {
                    injectMember {
                        method { name = target; emptyParam() }
                        afterHook {
                            val rawType = readField(instance, "mNetworkType") as? Int ?: return@afterHook
                            @Suppress("DEPRECATION")
                            if (rawType == ConnectivityManager.TYPE_VPN) result = false
                        }
                    }
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun PackageParam.hookConnectivity() {
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

            try {
                injectMember {
                    method { name = "getNetworkInfo"; param(Network::class.java) }
                    afterHook {
                        val info = result as? NetworkInfo ?: return@afterHook
                        val rawType = readField(info, "mNetworkType") as? Int ?: return@afterHook
                        @Suppress("DEPRECATION")
                        if (rawType == ConnectivityManager.TYPE_VPN) result = null
                    }
                }
            } catch (_: Throwable) {
            }

            listOf("registerNetworkCallback", "requestNetwork").forEach { target ->
                try {
                    injectMember {
                        method { name = target }.all()
                        beforeHook {
                            val request = args.firstOrNull { it is NetworkRequest } as? NetworkRequest
                                ?: return@beforeHook
                            if (!requestTargetsVpn(request)) return@beforeHook
                            args.forEachIndexed { index, arg ->
                                if (arg is ConnectivityManager.NetworkCallback) {
                                    args(index).set(silentCallback())
                                }
                            }
                        }
                    }
                } catch (_: Throwable) {
                }
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

            try {
                injectMember {
                    method { name = "getByInetAddress" }.all()
                    afterHook {
                        val nif = result as? NetworkInterface ?: return@afterHook
                        if (isVpnInterface(rawInterfaceName(nif))) result = null
                    }
                }
            } catch (_: Throwable) {
            }

            try {
                injectMember {
                    method { name = "isVirtual"; emptyParam() }
                    afterHook {
                        if (isVpnInterface(rawInterfaceName(instance))) result = false
                    }
                }
            } catch (_: Throwable) {
            }

            try {
                injectMember {
                    method { name = "isUp"; emptyParam() }
                    afterHook {
                        if (isVpnInterface(rawInterfaceName(instance))) result = false
                    }
                }
            } catch (_: Throwable) {
            }

            try {
                injectMember {
                    method { name = "getMTU"; emptyParam() }
                    afterHook {
                        if (isVpnInterface(rawInterfaceName(instance))) result = 1500
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun PackageParam.hookLinkProperties() {
        LinkProperties::class.java.hook {
            try {
                injectMember {
                    method { name = "getInterfaceName"; emptyParam() }
                    afterHook {
                        if (isVpnInterface(result as? String)) result = null
                    }
                }
            } catch (_: Throwable) {
            }

            try {
                injectMember {
                    method { name = "getRoutes"; emptyParam() }
                    afterHook {
                        @Suppress("UNCHECKED_CAST")
                        val routes = result as? List<RouteInfo> ?: return@afterHook
                        result = routes.filterNot { isVpnInterface(it.`interface`) }
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun requestTargetsVpn(request: NetworkRequest): Boolean {
        val caps = readField(request, "networkCapabilities") ?: return false
        val mask = readField(caps, "mTransportTypes") as? Long ?: return false
        return mask and VPN_TRANSPORT_BIT != 0L
    }

    private fun silentCallback() = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {}
        override fun onLost(network: Network) {}
        override fun onUnavailable() {}
        override fun onLosing(network: Network, maxMsToLive: Int) {}
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {}
        override fun onLinkPropertiesChanged(network: Network, props: LinkProperties) {}
    }

    private fun scrubCapabilitiesText(text: String): String {
        var out = text
        if (out.contains("VpnTransportInfo")) {
            out = out.replace(Regex("TransportInfo: <[^>]*>"), "TransportInfo: <>")
        }
        out = out.replace(Regex("\\bVPN\\b"), "")
            .replace("||", "|")
            .replace("| ", " ")
            .replace("|]", "]")
            .replace(": |", ": ")
        if (out.contains("Capabilities:") && !out.contains("NOT_VPN")) {
            out = out.replaceFirst("Capabilities: ", "Capabilities: NOT_VPN&")
        }
        return out
    }

    private fun readField(target: Any, name: String): Any? = runCatching {
        var current: Class<*>? = target.javaClass
        while (current != null) {
            val field = current.declaredFields.firstOrNull { it.name == name }
            if (field != null) {
                field.isAccessible = true
                return@runCatching field.get(target)
            }
            current = current.superclass
        }
        null
    }.getOrNull()

    private fun rawInterfaceName(target: Any): String? = readField(target, "name") as? String

    private fun isVpnInterface(name: String?): Boolean {
        val normalized = name?.lowercase() ?: return false
        return normalized.startsWith("tun") ||
            normalized.startsWith("tap") ||
            normalized.startsWith("ppp") ||
            normalized.startsWith("pptp") ||
            normalized.startsWith("wg") ||
            normalized.startsWith("ipsec")
    }
}
