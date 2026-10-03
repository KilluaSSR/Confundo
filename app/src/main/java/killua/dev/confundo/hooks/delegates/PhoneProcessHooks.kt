package killua.dev.confundo.hooks.delegates

import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.hooks.spoof
import killua.dev.confundo.ui.pages.home.FieldKeys

object PhoneProcessHooks : HookDelegate {

    private val serviceClasses = listOf(
        "com.android.phone.PhoneInterfaceManager",
        "com.android.internal.telephony.PhoneSubInfoController",
    )

    private val methodFields = listOf(
        "getDeviceId" to FieldKeys.DEVICE_ID,
        "getDeviceIdWithFeature" to FieldKeys.DEVICE_ID,
        "getDeviceIdForPhone" to FieldKeys.DEVICE_ID,
        "getImeiForSlot" to FieldKeys.IMEI,
        "getImeiForSubscriber" to FieldKeys.IMEI,
        "getMeidForSlot" to FieldKeys.MEID,
        "getLine1Number" to FieldKeys.PHONE_NUMBER,
        "getLine1NumberForDisplay" to FieldKeys.PHONE_NUMBER,
        "getLine1NumberForSubscriber" to FieldKeys.PHONE_NUMBER,
        "getIccSerialNumber" to FieldKeys.ICCID,
        "getIccSerialNumberWithFeature" to FieldKeys.ICCID,
        "getIccSerialNumberForSubscriber" to FieldKeys.ICCID,
        "getNetworkCountryIsoForPhone" to FieldKeys.NETWORK_COUNTRY,
    )

    override fun PackageParam.apply(fields: Map<String, String>) {
        val param = this
        serviceClasses.forEach { className ->
            val clazz = className.toClassOrNull() ?: return@forEach
            clazz.hook {
                methodFields.forEach { (methodName, fieldKey) ->
                    runCatching {
                        injectMember {
                            method { name = methodName }.all()
                            afterHook {
                                if (hasThrowable == true || result !is String) return@afterHook
                                val caller = with(SystemCaller) { param.callerFields() } ?: return@afterHook
                                resolve(caller, fieldKey)?.let { result = it }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun resolve(fields: Map<String, String>, key: String): String? = when (key) {
        FieldKeys.DEVICE_ID -> fields.spoof(FieldKeys.DEVICE_ID) ?: fields.spoof(FieldKeys.IMEI)
        FieldKeys.IMEI -> fields.spoof(FieldKeys.IMEI) ?: fields.spoof(FieldKeys.DEVICE_ID)
        else -> fields.spoof(key)
    }
}
