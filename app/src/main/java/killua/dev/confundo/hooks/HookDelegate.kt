package killua.dev.confundo.hooks

import com.highcapable.yukihookapi.hook.param.PackageParam

interface HookDelegate {
    fun PackageParam.apply(fields: Map<String, String>)
}

/**
 * 空字段熔断器：返回非空的字段值，否则返回 null。
 *
 * "为空则放行真实系统值" 的统一实现——配合 `?.let { ... }` 使用，
 * 任何被用户清空（或默认 ""）的字段都不会触发 Hook，从而暴露真实值。
 */
fun Map<String, String>.spoof(key: String): String? =
    this[key]?.takeIf { it.isNotEmpty() }
