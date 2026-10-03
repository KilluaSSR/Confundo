package killua.dev.confundo.hooks.delegates

import android.os.Binder
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.data.InjectMode
import killua.dev.confundo.ui.pages.home.FieldKeys
import java.util.concurrent.ConcurrentHashMap

internal object SystemCaller {

    private val uidPackages = ConcurrentHashMap<Int, Array<String>>()

    fun PackageParam.callerFields(): Map<String, String>? {
        val uid = Binder.getCallingUid()
        if (uid < 10_000) return null

        val packages = uidPackages[uid] ?: packagesForUid(uid).also {
            if (it.isNotEmpty()) uidPackages[uid] = it
        }
        for (pkg in packages) {
            val p = prefs(pkg)
            val enabled = runCatching { p.getBoolean(FieldKeys.ENABLED, false) }.getOrDefault(false)
            val mode = InjectMode.fromStorage(
                runCatching { p.getString(FieldKeys.INJECT_MODE, "") }.getOrDefault("")
            )
            if (!enabled || mode != InjectMode.SYSTEM_SERVER) continue

            return FieldKeys.fieldEntries.associate { (key, _) ->
                key to runCatching { p.getString(key, "") }.getOrDefault("")
            }
        }
        return null
    }

    private fun packagesForUid(uid: Int): Array<String> = runCatching<Array<String>> {
        val appGlobals = Class.forName("android.app.AppGlobals")
        val pm = appGlobals.getDeclaredMethod("getPackageManager").invoke(null)
        val resolved = pm.javaClass.methods.first {
            it.name == "getPackagesForUid" && it.parameterTypes.contentEquals(arrayOf(Int::class.javaPrimitiveType))
        }.invoke(pm, uid) as? Array<*>
        resolved?.mapNotNull { it as? String }?.toTypedArray() ?: emptyArray<String>()
    }.getOrElse {
        YLog.warn("Cannot resolve packages for uid=$uid: ${it.message}")
        emptyArray<String>()
    }
}
