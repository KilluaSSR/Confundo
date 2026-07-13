package killua.dev.confundo.hooks.delegates

import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.hooks.spoof
import killua.dev.confundo.ui.pages.home.FieldKeys

object TelephonyHooks : HookDelegate {

    override fun PackageParam.apply(fields: Map<String, String>) {
        val tmClass = "android.telephony.TelephonyManager".toClassOrNull() ?: return
        val subInfoClass = "android.telephony.SubscriptionInfo".toClassOrNull()

        val deviceId = fields.spoof(FieldKeys.DEVICE_ID) ?: fields.spoof(FieldKeys.IMEI)
        val imei = fields.spoof(FieldKeys.IMEI) ?: fields.spoof(FieldKeys.DEVICE_ID)

        tmClass.hook {
            deviceId?.let { value ->
                try {
                    injectMember { method { name = "getDeviceId" }; afterHook { result = value } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
                try {
                    injectMember { method { name = "getDeviceId"; param(Int::class.java) }; afterHook { result = value } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
            }

            imei?.let { value ->
                try {
                    injectMember { method { name = "getImei" }; afterHook { result = value } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
                try {
                    injectMember { method { name = "getImei"; param(Int::class.java) }; afterHook { result = value } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
            }

            fields.spoof(FieldKeys.MEID)?.let { meid ->
                try {
                    injectMember { method { name = "getMeid" }; afterHook { result = meid } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
                try {
                    injectMember { method { name = "getMeid"; param(Int::class.java) }; afterHook { result = meid } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
            }

            fields.spoof(FieldKeys.IMSI)?.let { imsi ->
                try {
                    injectMember { method { name = "getSubscriberId" }; afterHook { result = imsi } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
                try {
                    injectMember { method { name = "getSubscriberId"; param(Int::class.java) }; afterHook { result = imsi } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
            }

            fields.spoof(FieldKeys.PHONE_NUMBER)?.let { phone ->
                try {
                    injectMember { method { name = "getLine1Number" }; afterHook { result = phone } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
                try {
                    injectMember { method { name = "getLine1Number"; param(Int::class.java) }; afterHook { result = phone } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
            }

            fields.spoof(FieldKeys.ICCID)?.let { iccid ->
                try {
                    injectMember { method { name = "getSimSerialNumber" }; afterHook { result = iccid } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
                try {
                    injectMember { method { name = "getSimSerialNumber"; param(Int::class.java) }; afterHook { result = iccid } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
            }

            fields.spoof(FieldKeys.NETWORK_COUNTRY)?.let { country ->
                try {
                    injectMember { method { name = "getNetworkCountryIso" }; afterHook { result = country } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
                try {
                    injectMember { method { name = "getNetworkCountryIso"; param(Int::class.java) }; afterHook { result = country } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
            }

            fields.spoof(FieldKeys.NETWORK_OPERATOR)?.let { operator ->
                try {
                    injectMember { method { name = "getNetworkOperator" }; afterHook { result = operator } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
                try {
                    injectMember { method { name = "getNetworkOperator"; param(Int::class.java) }; afterHook { result = operator } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
            }

            fields.spoof(FieldKeys.NETWORK_OPERATOR_NAME)?.let { operatorName ->
                try {
                    injectMember { method { name = "getNetworkOperatorName" }; afterHook { result = operatorName } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
                try {
                    injectMember {
                        method { name = "getNetworkOperatorName"; param(Int::class.java) }
                        afterHook { result = operatorName }
                    }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
            }

            fields.spoof(FieldKeys.SIM_COUNTRY)?.let { simCountry ->
                try {
                    injectMember { method { name = "getSimCountryIso" }; afterHook { result = simCountry } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
                try {
                    injectMember { method { name = "getSimCountryIso"; param(Int::class.java) }; afterHook { result = simCountry } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
            }
        }

        subInfoClass?.hook {
            fields.spoof(FieldKeys.PHONE_NUMBER)?.let { phone ->
                try {
                    injectMember { method { name = "getNumber" }; afterHook { result = phone } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
            }

            fields.spoof(FieldKeys.ICCID)?.let { iccid ->
                try {
                    injectMember { method { name = "getIccId" }; afterHook { result = iccid } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
            }

            fields.spoof(FieldKeys.NETWORK_OPERATOR_NAME)?.let { carrierName ->
                try {
                    injectMember { method { name = "getCarrierName" }; afterHook { result = carrierName } }
                } catch (_: NoSuchMethodError) {
                } catch (_: Exception) {
                }
            }
        }
    }
}
