package killua.dev.confundo.hooks.delegates

import android.content.pm.PackageInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.os.Bundle
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.hooks.spoof
import killua.dev.confundo.ui.pages.home.FieldKeys
import java.util.concurrent.ConcurrentHashMap

/**
 * Hooks Binder endpoints that execute inside system_server. Unlike an in-app
 * hook, every replacement must be selected from Binder.getCallingUid().
 *
 * Connectivity and Wi-Fi services live in APEX jars on newer releases and are
 * defined by a child classloader, so their implementation classes are taken
 * from the registered binder instead of being resolved by name.
 */
object SystemServerHooks : HookDelegate {

    private const val SERVICE_CONNECTIVITY = "connectivity"
    private const val SERVICE_WIFI = "wifi"
    private const val REDACTED_MAC = "02:00:00:00:00:00"
    private const val UNKNOWN_SSID = "<unknown ssid>"

    private const val SETTINGS_PROVIDER = "com.android.providers.settings.SettingsProvider"
    private const val ANDROID_ID_SETTING = "android_id"

    private val pendingServices = ConcurrentHashMap<String, (Class<*>) -> Unit>()
    private val hookedServices: MutableSet<String> = ConcurrentHashMap.newKeySet()

    @Volatile
    private var settingsProviderHooked = false

    override fun PackageParam.apply(fields: Map<String, String>) {
        hookPackageInfo()
        hookAndroidId()
        val param = this
        whenServiceReady(SERVICE_CONNECTIVITY) { clazz -> with(param) { hookConnectivity(clazz) } }
        whenServiceReady(SERVICE_WIFI) { clazz -> with(param) { hookWifi(clazz) } }
    }

    private fun PackageParam.configuredCaller(): Map<String, String>? =
        with(SystemCaller) { this@configuredCaller.callerFields() }

    private fun PackageParam.hookAndroidId() {
        val param = this
        "android.content.ContentProvider".toClassOrNull()?.hook {
            runCatching {
                injectMember {
                    method { name = "attachInfo" }.all()
                    afterHook {
                        val provider = instanceOrNull ?: return@afterHook
                        if (provider.javaClass.name != SETTINGS_PROVIDER) return@afterHook
                        with(param) { hookSettingsProviderCall(provider.javaClass) }
                    }
                }
            }
        }
    }

    private fun PackageParam.hookSettingsProviderCall(clazz: Class<*>) {
        if (settingsProviderHooked) return
        settingsProviderHooked = true
        clazz.hook {
            runCatching {
                injectMember {
                    method { name = "call" }.all()
                    afterHook {
                        if (args.none { it == ANDROID_ID_SETTING }) return@afterHook
                        val value = configuredCaller()?.spoof(FieldKeys.ANDROID_ID) ?: return@afterHook
                        val bundle = result as? Bundle ?: return@afterHook
                        bundle.putString("value", value)
                    }
                }
            }
        }
    }

    // ---- service class resolution ---------------------------------------

    private fun PackageParam.whenServiceReady(name: String, install: (Class<*>) -> Unit) {
        pendingServices[name] = install
        hookServiceRegistration()
        registeredBinder(name)?.let { attach(name, it.javaClass) }
    }

    private fun registeredBinder(name: String): Any? = runCatching {
        Class.forName("android.os.ServiceManager")
            .getDeclaredMethod("checkService", String::class.java)
            .invoke(null, name)
    }.getOrNull()

    private fun attach(name: String, clazz: Class<*>) {
        val install = pendingServices[name] ?: return
        if (!hookedServices.add(name)) return
        runCatching { install(clazz) }
            .onSuccess { YLog.info("Confundo system_server hooked $name via ${clazz.name}") }
            .onFailure { YLog.error("Confundo failed to hook $name", it) }
    }

    @Volatile
    private var registrationHooked = false

    private fun PackageParam.hookServiceRegistration() {
        if (registrationHooked) return
        registrationHooked = true
        "android.os.ServiceManager".toClassOrNull()?.hook {
            (2..4).forEach { count ->
                try {
                    injectMember {
                        method { name = "addService"; paramCount = count }
                        afterHook {
                            val name = args[0] as? String ?: return@afterHook
                            val binder = args[1] ?: return@afterHook
                            if (pendingServices.containsKey(name)) attach(name, binder.javaClass)
                        }
                    }
                } catch (_: Throwable) {
                }
            }
        }
    }

    // ---- PackageManagerService -------------------------------------------

    private fun PackageParam.hookPackageInfo() {
        val classes = listOf(
            "com.android.server.pm.PackageManagerService",
            "com.android.server.pm.PackageManagerService\$IPackageManagerImpl",
            "com.android.server.pm.ComputerEngine",
        )
        classes.forEach { className ->
            val clazz = className.toClassOrNull() ?: return@forEach
            clazz.hook {
                (2..4).forEach { count ->
                    try {
                        injectMember {
                            method { name = "getPackageInfo"; paramCount = count }
                            afterHook {
                                val fields = configuredCaller() ?: return@afterHook
                                val info = result as? PackageInfo ?: return@afterHook
                                patchPackageInfo(info, fields)
                            }
                        }
                    } catch (_: Throwable) {
                    }
                }
            }
        }
    }

    private fun patchPackageInfo(info: PackageInfo, fields: Map<String, String>) {
        fields[FieldKeys.ACTIVATION_TIME]?.toLongOrNull()?.let {
            info.firstInstallTime = it
            info.lastUpdateTime = it
        }

        val version = when (info.packageName) {
            "com.google.android.gms" -> fields[FieldKeys.GMS_VERSION]
            "com.android.vending" -> fields[FieldKeys.PLAY_STORE_VERSION]
            else -> null
        }?.takeIf { it.isNotBlank() } ?: return

        val derived = version.filter(Char::isDigit).take(9).toLongOrNull() ?: 0L
        val code = maxOf(info.longVersionCode, derived)
        info.versionName = version
        info.longVersionCode = code
        @Suppress("DEPRECATION")
        info.versionCode = code.toInt()
    }

    // ---- ConnectivityService ---------------------------------------------

    private fun PackageParam.hookConnectivity(clazz: Class<*>) {
        clazz.hook {
            (1..4).forEach { count ->
                try {
                    injectMember {
                        method { name = "getNetworkCapabilities"; paramCount = count }
                        afterHook {
                            val fields = configuredCaller() ?: return@afterHook
                            val caps = result as? NetworkCapabilities ?: return@afterHook
                            if (fields.isTrue(FieldKeys.HIDE_VPN)) sanitizeVpn(caps)
                            (caps.transportInfo as? WifiInfo)?.let { patchWifiInfo(it, fields) }
                        }
                    }
                } catch (_: Throwable) {
                }
            }

            try {
                injectMember {
                    method { name = "getNetworkInfo"; paramCount = 1 }
                    afterHook {
                        val fields = configuredCaller() ?: return@afterHook
                        @Suppress("DEPRECATION")
                        if (fields.isTrue(FieldKeys.HIDE_VPN) && args[0] == ConnectivityManager.TYPE_VPN) {
                            result = null
                        }
                    }
                }
            } catch (_: Throwable) {
            }

            try {
                injectMember {
                    method { name = "getAllNetworkInfo"; emptyParam() }
                    afterHook {
                        val fields = configuredCaller() ?: return@afterHook
                        if (!fields.isTrue(FieldKeys.HIDE_VPN)) return@afterHook
                        @Suppress("DEPRECATION")
                        val infos = result as? Array<*> ?: return@afterHook
                        @Suppress("DEPRECATION")
                        result = infos.filterIsInstance<NetworkInfo>()
                            .filter { it.type != ConnectivityManager.TYPE_VPN }
                            .toTypedArray()
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    /** A VPN network both carries TRANSPORT_VPN and lacks NET_CAPABILITY_NOT_VPN. */
    private fun sanitizeVpn(caps: NetworkCapabilities) {
        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return
        invokeHidden(caps, "removeTransportType", NetworkCapabilities.TRANSPORT_VPN)
        invokeHidden(caps, "addCapability", NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
    }

    // ---- WifiService -----------------------------------------------------

    private fun PackageParam.hookWifi(clazz: Class<*>) {
        clazz.hook {
            (0..3).forEach { count ->
                try {
                    injectMember {
                        method { name = "getConnectionInfo"; paramCount = count }
                        afterHook {
                            val fields = configuredCaller() ?: return@afterHook
                            val info = result as? WifiInfo ?: return@afterHook
                            patchWifiInfo(info, fields)
                        }
                    }
                } catch (_: Throwable) {
                }
            }
        }
    }

    /**
     * Values redacted by the platform (no location / local-MAC permission) are
     * kept redacted: returning a real-looking value there would be anomalous.
     */
    private fun patchWifiInfo(info: WifiInfo, fields: Map<String, String>) {
        normalizeMac(fields[FieldKeys.WIFI_BSSID])?.let { bssid ->
            if (info.bssid != null && info.bssid != REDACTED_MAC) {
                invokeHidden(info, "setBSSID", bssid) || setField(info, "mBSSID", bssid)
            }
        }
        normalizeMac(fields[FieldKeys.WIFI_MAC])?.let { mac ->
            @Suppress("DEPRECATION")
            if (info.macAddress != null && info.macAddress != REDACTED_MAC) {
                invokeHidden(info, "setMacAddress", mac) || setField(info, "mMacAddress", mac)
            }
        }
        fields[FieldKeys.WIFI_SSID]?.takeIf { it.isNotBlank() }?.let { ssid ->
            if (info.ssid != null && info.ssid != UNKNOWN_SSID) {
                wifiSsidOf(ssid.removeSurrounding("\""))?.let { invokeHidden(info, "setSSID", it) }
            }
        }
    }

    private fun wifiSsidOf(ssid: String): Any? {
        val clazz = runCatching { Class.forName("android.net.wifi.WifiSsid") }.getOrNull() ?: return null
        return runCatching {
            clazz.getDeclaredMethod("fromBytes", ByteArray::class.java).invoke(null, ssid.toByteArray())
        }.recoverCatching {
            clazz.getDeclaredMethod("createFromAsciiEncoded", String::class.java).invoke(null, ssid)
        }.getOrNull()
    }

    // ---- helpers -----------------------------------------------------------

    private fun Map<String, String>.isTrue(key: String) = this[key]?.toBooleanStrictOrNull() == true

    private fun normalizeMac(value: String?): String? =
        value?.replace('-', ':')?.lowercase()?.takeIf { MAC_REGEX.matches(it) }

    private val MAC_REGEX = Regex("^[0-9a-f]{2}(:[0-9a-f]{2}){5}$")

    private fun invokeHidden(target: Any, name: String, arg: Any): Boolean = runCatching {
        target.javaClass.methods.first { it.name == name && it.parameterCount == 1 }
            .apply { isAccessible = true }
            .invoke(target, arg)
        true
    }.getOrDefault(false)

    private fun setField(target: Any, name: String, value: Any): Boolean = runCatching {
        target.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(target, value)
        true
    }.getOrDefault(false)
}
