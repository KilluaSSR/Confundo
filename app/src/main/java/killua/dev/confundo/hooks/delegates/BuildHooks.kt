package killua.dev.confundo.hooks.delegates

import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.data.SystemPropKey
import killua.dev.confundo.hooks.BuildProps
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.hooks.spoof
import killua.dev.confundo.ui.pages.home.FieldKeys

/**
 * android.os.Build 静态字段 + Build.getString / getSerial Hook。
 */
object BuildHooks : HookDelegate {

    override fun PackageParam.apply(fields: Map<String, String>) {
        val buildClass = "android.os.Build".toClassOrNull() ?: return
        val versionClass = "android.os.Build\$VERSION".toClassOrNull()

        spoofStaticField(buildClass, "MODEL", fields.spoof(FieldKeys.MODEL))
        spoofStaticField(buildClass, "DEVICE", fields.spoof(FieldKeys.DEVICE))
        spoofStaticField(buildClass, "BRAND", fields.spoof(FieldKeys.BRAND))
        spoofStaticField(buildClass, "MANUFACTURER", fields.spoof(FieldKeys.MANUFACTURER))
        spoofStaticField(buildClass, "PRODUCT", fields.spoof(FieldKeys.PRODUCT))
        spoofStaticField(buildClass, "HARDWARE", fields.spoof(FieldKeys.HARDWARE))
        spoofStaticField(buildClass, "FINGERPRINT", fields.spoof(FieldKeys.FINGERPRINT))
        spoofStaticField(buildClass, "SERIAL", fields.spoof(FieldKeys.SERIAL))

        spoofRelatedBuildFields(buildClass, fields)

        versionClass?.let { vClass ->
            spoofStaticField(vClass, "INCREMENTAL", fields.spoof(FieldKeys.INCREMENTAL))
            val realSdk = android.os.Build.VERSION.SDK_INT
            fields.spoof(FieldKeys.SDK_INT)?.toIntOrNull()?.let { sdk ->
                val effectiveSdk = sdk.coerceAtLeast(realSdk)
                runCatching { vClass.field { name = "SDK_INT" }.ignored().get().set(effectiveSdk) }
            }
            fields.spoof(FieldKeys.ANDROID_VERSION)?.let { release ->
                runCatching { vClass.field { name = "RELEASE" }.ignored().get().set(release) }
            }
        }

        buildClass.method {
            name = "getString"
            param(String::class.java)
        }.ignoredError().hook {
            before {
                val key = args[0] as? String ?: return@before
                val fieldKey = SystemPropKey.fieldKeyFor(key) ?: return@before
                fields.spoof(fieldKey)?.let { result = it }
            }
        }

        buildClass.method { name = "getSerial" }.ignoredError().hook {
            before { fields.spoof(FieldKeys.SERIAL)?.let { result = it } }
        }
    }

    private fun PackageParam.spoofRelatedBuildFields(buildClass: Class<*>, fields: Map<String, String>) {
        val props = BuildProps.propMap(fields)

        spoofStaticField(buildClass, "ID", props["ro.build.id"])
        spoofStaticField(buildClass, "DISPLAY", props["ro.build.display.id"])
        spoofStaticField(buildClass, "TYPE", props["ro.build.type"])
        spoofStaticField(buildClass, "TAGS", props["ro.build.tags"])
        spoofStaticField(buildClass, "BOARD", props["ro.product.board"])
        spoofStaticField(buildClass, "BOOTLOADER", props["ro.bootloader"])
        spoofStaticField(buildClass, "HOST", props["ro.build.host"])
        spoofStaticField(buildClass, "USER", props["ro.build.user"])

        // SOC_MANUFACTURER / SOC_MODEL 为 API 31+ 字段，缺失时忽略。
        spoofStaticField(buildClass, "SOC_MANUFACTURER", props["ro.soc.manufacturer"])
        spoofStaticField(buildClass, "SOC_MODEL", props["ro.soc.model"])
    }

    private fun PackageParam.spoofStaticField(clazz: Class<*>, fieldName: String, value: String?) {
        if (value == null) return
        runCatching { clazz.field { name = fieldName }.ignored().get().set(value) }
    }
}
