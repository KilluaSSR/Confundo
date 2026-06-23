package killua.dev.confundo.hooks.delegates

import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.hooks.spoof
import killua.dev.confundo.ui.pages.home.FieldKeys

object TelephonyHooks : HookDelegate {

    override fun PackageParam.apply(fields: Map<String, String>) {
        val tmClass = "android.telephony.TelephonyManager".toClassOrNull() ?: return
        val subInfoClass = "android.telephony.SubscriptionInfo".toClassOrNull()

        tmClass.hook {
            fields.spoof(FieldKeys.IMEI)?.let { imei ->
                listOf("getDeviceId", "getImei").forEach { methodName ->
                    try { injectMember { method { name = methodName }; afterHook { result = imei } } } catch (_: Throwable) {}
                    try { injectMember { method { name = methodName; param(Int::class.java) }; afterHook { result = imei } } } catch (_: Throwable) {}
                }
            }

            // MEID
            fields.spoof(FieldKeys.MEID)?.let { meid ->
                try { injectMember { method { name = "getMeid" }; afterHook { result = meid } } } catch (_: Throwable) {}
                try { injectMember { method { name = "getMeid"; param(Int::class.java) }; afterHook { result = meid } } } catch (_: Throwable) {}
            }

            // IMSI
            fields.spoof(FieldKeys.IMSI)?.let { imsi ->
                try { injectMember { method { name = "getSubscriberId" }; afterHook { result = imsi } } } catch (_: Throwable) {}
                try { injectMember { method { name = "getSubscriberId"; param(Int::class.java) }; afterHook { result = imsi } } } catch (_: Throwable) {}
            }

            // Phone Number
            fields.spoof(FieldKeys.PHONE_NUMBER)?.let { phone ->
                try { injectMember { method { name = "getLine1Number" }; afterHook { result = phone } } } catch (_: Throwable) {}
                try { injectMember { method { name = "getLine1Number"; param(Int::class.java) }; afterHook { result = phone } } } catch (_: Throwable) {}
            }

            // ICCID
            fields.spoof(FieldKeys.ICCID)?.let { iccid ->
                try { injectMember { method { name = "getSimSerialNumber" }; afterHook { result = iccid } } } catch (_: Throwable) {}
                try { injectMember { method { name = "getSimSerialNumber"; param(Int::class.java) }; afterHook { result = iccid } } } catch (_: Throwable) {}
            }

            // Network Country ISO
            fields.spoof(FieldKeys.NETWORK_COUNTRY)?.let { country ->
                try { injectMember { method { name = "getNetworkCountryIso" }; afterHook { result = country } } } catch (_: Throwable) {}
                try { injectMember { method { name = "getNetworkCountryIso"; param(Int::class.java) }; afterHook { result = country } } } catch (_: Throwable) {}
            }

            // Network Operator
            fields.spoof(FieldKeys.NETWORK_OPERATOR)?.let { operator ->
                try { injectMember { method { name = "getNetworkOperator" }; afterHook { result = operator } } } catch (_: Throwable) {}
                try { injectMember { method { name = "getNetworkOperator"; param(Int::class.java) }; afterHook { result = operator } } } catch (_: Throwable) {}
            }

            // Network Operator Name
            fields.spoof(FieldKeys.NETWORK_OPERATOR_NAME)?.let { nw ->
                try { injectMember { method { name = "getNetworkOperatorName" }; afterHook { result = nw } } } catch (_: Throwable) {}
                try { injectMember { method { name = "getNetworkOperatorName"; param(Int::class.java) }; afterHook { result = nw } } } catch (_: Throwable) {}
            }

            // Sim Country ISO
            fields.spoof(FieldKeys.SIM_COUNTRY)?.let { simCountry ->
                try { injectMember { method { name = "getSimCountryIso" }; afterHook { result = simCountry } } } catch (_: Throwable) {}
                try { injectMember { method { name = "getSimCountryIso"; param(Int::class.java) }; afterHook { result = simCountry } } } catch (_: Throwable) {}
            }
        }

        subInfoClass?.hook {
            // 手机号
            fields.spoof(FieldKeys.PHONE_NUMBER)?.let { phone ->
                try { injectMember { method { name = "getNumber" }; afterHook { result = phone } } } catch (_: Throwable) {}
            }

            // ICCID
            fields.spoof(FieldKeys.ICCID)?.let { iccid ->
                try { injectMember { method { name = "getIccId" }; afterHook { result = iccid } } } catch (_: Throwable) {}
            }

            // 运营商名称
            fields.spoof(FieldKeys.NETWORK_OPERATOR_NAME)?.let { iccid ->
                try { injectMember { method { name = "getCarrierName" }; afterHook { result = iccid } } } catch (_: Throwable) {}
            }
        }
    }
}
