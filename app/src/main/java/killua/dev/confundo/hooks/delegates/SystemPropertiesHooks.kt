package killua.dev.confundo.hooks.delegates

import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.data.SystemPropKey
import killua.dev.confundo.hooks.HookDelegate


object SystemPropertiesHooks : HookDelegate {

    override fun PackageParam.apply(fields: Map<String, String>) {
        val sysProp = "android.os.SystemProperties".toClassOrNull() ?: return

        sysProp.hook {
            listOf(1, 2).forEach { count ->
                try {
                    injectMember {
                        method { name = "get"; paramCount = count }
                        afterHook {
                            resolveProp(args().first().string(), fields)?.let { result = it }
                        }
                    }
                } catch (_: NoSuchMethodError) {}
            }

            try {
                injectMember {
                    method { name = "getInt"; paramCount = 2 }
                    afterHook {
                        resolveProp(args().first().string(), fields)?.toIntOrNull()?.let { result = it }
                    }
                }
            } catch (_: NoSuchMethodError) {}

            try {
                injectMember {
                    method { name = "getLong"; paramCount = 2 }
                    afterHook {
                        resolveProp(args().first().string(), fields)?.toLongOrNull()?.let { result = it }
                    }
                }
            } catch (_: NoSuchMethodError) {}

            try {
                injectMember {
                    method { name = "getBoolean"; paramCount = 2 }
                    afterHook {
                        resolveProp(args().first().string(), fields)?.let { v ->
                            result = v == "1" || v.equals("true", ignoreCase = true)
                        }
                    }
                }
            } catch (_: NoSuchMethodError) {}
        }
    }

    private fun resolveProp(key: String?, fields: Map<String, String>): String? {
        if (key == null) return null
        val fieldKey = SystemPropKey.fieldKeyFor(key) ?: return null
        return fields[fieldKey]?.takeIf { it.isNotEmpty() }
    }
}
