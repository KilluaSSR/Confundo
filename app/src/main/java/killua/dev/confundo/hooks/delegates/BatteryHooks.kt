package killua.dev.confundo.hooks.delegates

import android.content.Intent
import android.os.BatteryManager
import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.data.BatteryHealth
import killua.dev.confundo.data.BatteryPlugged
import killua.dev.confundo.data.BatteryStatus
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.hooks.spoof
import killua.dev.confundo.ui.pages.home.FieldKeys
import kotlin.math.abs
import kotlin.math.min

/**
 * 电池 Hook。
 */
object BatteryHooks : HookDelegate {

    override fun PackageParam.apply(fields: Map<String, String>) {
        val capacityMah = fields.spoof(FieldKeys.BATTERY)
            ?.replace(Regex("[^0-9]"), "")?.toIntOrNull()
        val status = BatteryStatus.from(fields.spoof(FieldKeys.BATTERY_STATUS))
        val plugged = BatteryPlugged.from(fields.spoof(FieldKeys.BATTERY_PLUGGED))
        val voltage = fields.spoof(FieldKeys.BATTERY_VOLTAGE)?.toIntOrNull()
        val temperature = fields.spoof(FieldKeys.BATTERY_TEMPERATURE)?.toIntOrNull()
        val health = BatteryHealth.from(fields.spoof(FieldKeys.BATTERY_HEALTH))
        val level = fields.spoof(FieldKeys.BATTERY_LEVEL)?.toIntOrNull()?.coerceIn(0, 100)
        val tech = fields.spoof(FieldKeys.BATTERY_TECHNOLOGY)
        val currentMa = fields.spoof(FieldKeys.BATTERY_CURRENT)?.toIntOrNull()

        // 若没有任何电池字段被填充，整体放行真实值。
        val anyBattery = capacityMah != null || status != null || plugged != null ||
                voltage != null || temperature != null || health != null ||
                level != null || tech != null || currentMa != null
        if (!anyBattery) return

        // 充放电符号：充电/已满为负（流入），否则为正。
        val currentNowUa: Long? = currentMa?.let {
            val magnitude = abs(it).toLong() * 1000L
            if (status == BatteryStatus.CHARGING || status == BatteryStatus.FULL) -magnitude else magnitude
        }
        val chargeCounterUah: Long? = capacityMah?.let { it.toLong() * 1000L }

        // PowerProfile.getAveragePower("battery.capacity") -> 设计容量(mAh)
        capacityMah?.let { cap ->
            "com.android.internal.os.PowerProfile".toClassOrNull()?.hook {
                try {
                    injectMember {
                        method { name = "getAveragePower"; param(String::class.java) }
                        afterHook {
                            if ((args[0] as? String) == "battery.capacity") {
                                result = cap.toDouble()
                            }
                        }
                    }
                } catch (_: NoSuchMethodError) {}
            }
        }

        BatteryManager::class.java.hook {
            // getIntProperty: CHARGE_COUNTER / CURRENT_NOW 返回 Int
            try {
                injectMember {
                    method { name = "getIntProperty"; paramCount = 1 }
                    afterHook {
                        when (args().first().int()) {
                            BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER ->
                                chargeCounterUah?.let { result = min(Int.MAX_VALUE.toLong(), it).toInt() }
                            BatteryManager.BATTERY_PROPERTY_CURRENT_NOW ->
                                currentNowUa?.let { result = it.toInt() }
                            BatteryManager.BATTERY_PROPERTY_CAPACITY ->
                                level?.let { result = it }
                        }
                    }
                }
            } catch (_: NoSuchMethodError) {}

            // getLongProperty: 必须返回 Long
            try {
                injectMember {
                    method { name = "getLongProperty"; paramCount = 1 }
                    afterHook {
                        when (args().first().int()) {
                            BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER ->
                                chargeCounterUah?.let { result = it }
                            BatteryManager.BATTERY_PROPERTY_CURRENT_NOW ->
                                currentNowUa?.let { result = it }
                        }
                    }
                }
            } catch (_: NoSuchMethodError) {}
        }

        // ACTION_BATTERY_CHANGED 广播 extras：固定 scale=100
        Intent::class.java.hook {
            try {
                injectMember {
                    method { name = "getIntExtra"; param(String::class.java, Int::class.java) }
                    afterHook {
                        val intent = instance as? Intent ?: return@afterHook
                        if (intent.action != Intent.ACTION_BATTERY_CHANGED) return@afterHook
                        when (args().first().string()) {
                            BatteryManager.EXTRA_STATUS -> status?.let { result = it.androidValue }
                            BatteryManager.EXTRA_PLUGGED -> plugged?.let { result = it.androidValue }
                            BatteryManager.EXTRA_VOLTAGE -> voltage?.let { result = it }
                            BatteryManager.EXTRA_TEMPERATURE -> temperature?.let { result = it }
                            BatteryManager.EXTRA_HEALTH -> health?.let { result = it.androidValue }
                            BatteryManager.EXTRA_LEVEL -> level?.let { result = it }
                            BatteryManager.EXTRA_SCALE -> if (level != null) result = 100
                        }
                    }
                }
            } catch (_: NoSuchMethodError) {}

            try {
                injectMember {
                    method { name = "getStringExtra"; param(String::class.java) }
                    afterHook {
                        val intent = instance as? Intent ?: return@afterHook
                        if (intent.action != Intent.ACTION_BATTERY_CHANGED) return@afterHook
                        if (args().first().string() == BatteryManager.EXTRA_TECHNOLOGY) {
                            tech?.let { result = it }
                        }
                    }
                }
            } catch (_: NoSuchMethodError) {}
        }
    }
}
