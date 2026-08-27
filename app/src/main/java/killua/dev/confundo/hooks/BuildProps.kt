package killua.dev.confundo.hooks

import android.os.Build
import killua.dev.confundo.ui.pages.home.FieldKeys

/**
 * 设备标识类字段 -> `ro.*` 系统属性的**唯一映射源**。
 *
 * Java 层（[killua.dev.confundo.hooks.delegates.SystemPropertiesHooks]、
 * [killua.dev.confundo.hooks.delegates.BuildHooks]）与 native 层
 * （[killua.dev.confundo.hooks.delegates.NativeHooks]）都从这里取值，
 * 保证 `Build.*` 静态字段、`SystemProperties.get()`、native `__system_property_get`、
 * `build.prop` 直读四条路径读到的值完全一致。
 */
object BuildProps {

    fun propMap(fields: Map<String, String>): Map<String, String> {
        val map = LinkedHashMap<String, String>()

        fun put(value: String?, vararg names: String) {
            val v = value?.takeIf { it.isNotBlank() } ?: return
            names.forEach { map[it] = v }
        }

        fun partitioned(attr: String, value: String?) = put(
            value,
            "ro.product.$attr",
            "ro.product.system.$attr",
            "ro.product.vendor.$attr",
            "ro.product.odm.$attr",
            "ro.product.product.$attr",
            "ro.product.system_ext.$attr",
        )

        partitioned("model", fields.spoof(FieldKeys.MODEL))
        partitioned("brand", fields.spoof(FieldKeys.BRAND))
        partitioned("manufacturer", fields.spoof(FieldKeys.MANUFACTURER))
        partitioned("device", fields.spoof(FieldKeys.DEVICE))
        partitioned("name", fields.spoof(FieldKeys.PRODUCT))

        val hardware = fields.spoof(FieldKeys.HARDWARE)
        put(hardware, "ro.hardware", "ro.board.platform", "ro.product.board")

        put(
            fields.spoof(FieldKeys.FINGERPRINT),
            "ro.build.fingerprint",
            "ro.bootimage.build.fingerprint",
            "ro.system.build.fingerprint",
            "ro.vendor.build.fingerprint",
            "ro.odm.build.fingerprint",
            "ro.product.build.fingerprint",
        )

        put(fields.spoof(FieldKeys.SERIAL), "ro.serialno", "ro.boot.serialno")
        put(fields.spoof(FieldKeys.INCREMENTAL), "ro.build.version.incremental")
        put(
            fields.spoof(FieldKeys.ANDROID_VERSION),
            "ro.build.version.release",
            "ro.build.version.release_or_codename",
        )

        fields.spoof(FieldKeys.SDK_INT)?.toIntOrNull()?.let { requested ->
            put(requested.coerceAtLeast(Build.VERSION.SDK_INT).toString(), "ro.build.version.sdk")
        }

        fields.spoof(FieldKeys.FINGERPRINT)?.let { fp ->
            val segments = fp.split(':')
            segments.getOrNull(1)?.split('/')?.getOrNull(1)?.takeIf { it.isNotBlank() }?.let {
                put(it, "ro.build.id", "ro.build.display.id")
            }
            val tail = segments.getOrNull(2)?.split('/')
            put(tail?.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "user", "ro.build.type")
            put(tail?.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "release-keys", "ro.build.tags")
            put("unknown", "ro.bootloader", "ro.boot.bootloader")
            put("localhost", "ro.build.host")
            put("android-build", "ro.build.user")
        }

        put(fields.spoof(FieldKeys.MANUFACTURER), "ro.soc.manufacturer")
        put(hardware, "ro.soc.model")

        return map
    }
}
