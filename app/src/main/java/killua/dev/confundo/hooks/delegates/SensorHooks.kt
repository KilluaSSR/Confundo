package killua.dev.confundo.hooks.delegates

import android.hardware.Sensor
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.hooks.spoof
import killua.dev.confundo.ui.pages.home.FieldKeys

/**
 * 传感器 Hook：仅篡改 [Sensor] 的厂商字符串（私有字段 `mVendor`），
 */
object SensorHooks : HookDelegate {

    @Volatile
    private var vendorFieldMissingLogged = false

    override fun PackageParam.apply(fields: Map<String, String>) {
        val vendor = fields.spoof(FieldKeys.SENSOR_VENDOR) ?: return

        "android.hardware.SensorManager".toClassOrNull()?.hook {
            try {
                injectMember {
                    method { name = "getSensorList"; param(Int::class.java) }
                    afterHook {
                        (result as? List<*>)?.forEach { s ->
                            (s as? Sensor)?.let { patchVendor(it, vendor) }
                        }
                    }
                }
            } catch (_: NoSuchMethodError) {}

            try {
                injectMember {
                    method { name = "getDefaultSensor"; param(Int::class.java) }
                    afterHook { (result as? Sensor)?.let { patchVendor(it, vendor) } }
                }
            } catch (_: NoSuchMethodError) {}

            try {
                injectMember {
                    method { name = "getDynamicSensorList"; param(Int::class.java) }
                    afterHook {
                        (result as? List<*>)?.forEach { s ->
                            (s as? Sensor)?.let { patchVendor(it, vendor) }
                        }
                    }
                }
            } catch (_: NoSuchMethodError) {}
        }
    }

    /** 反射改写 Sensor.mVendor；字段缺失安全跳过。 */
    private fun patchVendor(sensor: Sensor, vendor: String) {
        try {
            Sensor::class.java.getDeclaredField("mVendor").apply {
                isAccessible = true
                set(sensor, vendor)
            }
        } catch (_: NoSuchFieldException) {
            if (!vendorFieldMissingLogged) {
                vendorFieldMissingLogged = true
                YLog.warn("Sensor.mVendor not found, skip sensor vendor spoof")
            }
        } catch (_: Exception) {
        }
    }
}
