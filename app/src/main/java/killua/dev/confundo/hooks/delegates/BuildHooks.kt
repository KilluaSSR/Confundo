package killua.dev.confundo.hooks.delegates

import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.data.SystemPropKey
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

        versionClass?.let { vClass ->
            spoofStaticField(vClass, "INCREMENTAL", fields.spoof(FieldKeys.INCREMENTAL))
            fields.spoof(FieldKeys.SDK_INT)?.toIntOrNull()?.let { sdk ->
                runCatching { vClass.field { name = "SDK_INT" }.ignored().get().set(sdk) }
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

    private fun PackageParam.spoofStaticField(clazz: Class<*>, fieldName: String, value: String?) {
        if (value == null) return
        runCatching { clazz.field { name = fieldName }.ignored().get().set(value) }
    }
}
