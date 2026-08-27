package killua.dev.confundo.hooks.delegates

import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.data.SystemPropKey
import killua.dev.confundo.hooks.BuildProps
import killua.dev.confundo.hooks.HookDelegate

object SystemPropertiesHooks : HookDelegate {

    override fun PackageParam.apply(fields: Map<String, String>) {
        val sysProp = "android.os.SystemProperties".toClassOrNull() ?: return

        val propOverrides = BuildProps.propMap(fields)
        val resolve: (String?) -> String? = { key -> resolveProp(key, propOverrides, fields) }

        sysProp.hook {
            listOf(1, 2).forEach { count ->
                try {
                    injectMember {
                        method { name = "get"; paramCount = count }
                        afterHook {
                            resolve(args().first().string())?.let { result = it }
                        }
                    }
                } catch (_: NoSuchMethodError) {}
            }

            try {
                injectMember {
                    method { name = "getInt"; paramCount = 2 }
                    afterHook {
                        resolve(args().first().string())?.toIntOrNull()?.let { result = it }
                    }
                }
            } catch (_: NoSuchMethodError) {}

            try {
                injectMember {
                    method { name = "getLong"; paramCount = 2 }
                    afterHook {
                        resolve(args().first().string())?.toLongOrNull()?.let { result = it }
                    }
                }
            } catch (_: NoSuchMethodError) {}

            try {
                injectMember {
                    method { name = "getBoolean"; paramCount = 2 }
                    afterHook {
                        resolve(args().first().string())?.let { v ->
                            result = v == "1" || v.equals("true", ignoreCase = true)
                        }
                    }
                }
            } catch (_: NoSuchMethodError) {}
        }
    }

    private fun resolveProp(
        key: String?,
        propOverrides: Map<String, String>,
        fields: Map<String, String>,
    ): String? {
        if (key == null) return null
        propOverrides[key]?.let { return it }

        val fieldKey = SystemPropKey.fieldKeyFor(key) ?: return null
        return fields[fieldKey]?.trim()?.takeIf { it.isNotEmpty() }
    }
}
