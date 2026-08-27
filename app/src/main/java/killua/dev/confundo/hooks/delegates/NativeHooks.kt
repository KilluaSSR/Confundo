package killua.dev.confundo.hooks.delegates

import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.hooks.BuildProps
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.hooks.NativeBridge
import killua.dev.confundo.hooks.spoof
import killua.dev.confundo.ui.pages.home.FieldKeys
import java.io.File


object NativeHooks : HookDelegate {

    @Volatile
    private var installed = false

    @Volatile
    private var librariesLoaded = false

    // 加载顺序：ShadowHook -> 本模块。libconfundo 依赖 libshadowhook。
    //
    // 注意：**不要**手动加载 libshadowhook_nothing.so。ShadowHook 在 shadowhook_init 时会用 soname
    // `dlopen("libshadowhook_nothing.so")` 现场加载该辅助库，并借其构造函数触发 soinfo 内存扫描
    // （hook linker call_constructors 的探针）。若我们抢先加载它，dlopen 会命中已加载的缓存句柄、
    // 不再触发构造函数，扫描失败 -> shadowhook_init 报 SHADOWHOOK_ERRNO_INIT_LINKER(12)，所有 hook 失效。
    //
    // 同时，libshadowhook_nothing.so 必须能被上面的 soname dlopen 找到：它走的是「libshadowhook.so 所在
    // linker 命名空间」的库搜索路径。LSPosed 把模块命名空间的搜索路径设为 base.apk!/lib/<abi>（APK 内嵌），
    // 因此 .so 必须以未压缩形式留在 APK 内（extractNativeLibs=false），且优先用 System.loadLibrary 经
    // classloader 从该内嵌路径加载，从而使 libshadowhook.so 落入正确命名空间、其内部 dlopen 才能命中 nothing.so。
    private val loadOrder = listOf(
        "shadowhook" to true,
        "confundo" to true,
    )

    override fun PackageParam.apply(fields: Map<String, String>) {
        if (installed) return

        val props = LinkedHashMap(BuildProps.propMap(fields)).apply {
            fields.spoof(FieldKeys.TIMEZONE)?.let { put("persist.sys.timezone", it) }
        }
        if (props.isEmpty()) return

        if (!ensureLibrariesLoaded()) {
            YLog.warn("Confundo native libs not loaded, skip native spoof")
            return
        }

        val cacheDir = writableCacheDir(packageName)
        val vulkanName = fields.spoof(FieldKeys.GL_RENDERER).orEmpty()
        val kernel = fields.spoof(FieldKeys.KERNEL).orEmpty()

        val ok = runCatching {
            NativeBridge.nativeInstall(
                keys = props.keys.toTypedArray(),
                values = props.values.toTypedArray(),
                cacheDir = cacheDir,
                kernel = kernel,
                vulkanDeviceName = vulkanName,
            )
        }.getOrElse {
            YLog.error("nativeInstall failed", it)
            false
        }

        if (ok) {
            installed = true
            YLog.info("Confundo native spoof installed for $packageName (${props.size} props)")
        }
    }

    /** 目标 App 自身的可写缓存目录，仅用于 native 侧生成改写后的 build.prop / proc 文件（只读打开，不执行）。 */
    private fun writableCacheDir(pkg: String): String {
        val dir = currentAppCacheDir() ?: "/data/data/$pkg/cache"
        return runCatching {
            File(dir, "confundo_native").apply { mkdirs() }.absolutePath
        }.getOrDefault("")
    }

    /** 反射取当前进程 Application 的可写缓存目录，避免直接依赖隐藏 API。 */
    private fun currentAppCacheDir(): String? = runCatching {
        val activityThread = Class.forName("android.app.ActivityThread")
        val app = activityThread.getMethod("currentApplication").invoke(null) as? android.app.Application
        app?.let { (it.codeCacheDir ?: it.cacheDir)?.absolutePath }
    }.getOrNull()

    /**
     * 加载 native 库。优先 `System.loadLibrary`：它经模块 classloader 解析到 APK 内嵌路径
     * （base.apk!/lib/<abi>，SELinux 上下文为 apk_data_file，允许被 app 域 execute），使 libshadowhook.so
     * 落入 LSPosed 模块命名空间，其内部 soname dlopen 才能找到 libshadowhook_nothing.so（见 [loadOrder] 说明）。
     *
     * 仅当 loadLibrary 失败时，回退到「按绝对路径加载模块自身 native 库目录里的 .so」——用于个别把
     * .so 解压到 /data/app/.../lib/<abi> 的环境；同时避免把 .so 拷到目标 App 的 app_data_file 目录后
     * 被 SELinux 拒绝执行（Android 10+：neverallow appdomain execute app_data_file）。
     */
    @Synchronized
    private fun ensureLibrariesLoaded(): Boolean {
        if (librariesLoaded) return true

        val loader = NativeBridge::class.java.classLoader
        return runCatching {
            loadOrder.forEach { (name, required) ->
                if (loadViaClassLoader(name)) return@forEach

                val path = findLibraryPath(loader, name)
                if (path == null) {
                    if (required) throw IllegalStateException("cannot locate lib$name.so (loadLibrary + lib dir both failed)")
                    return@forEach
                }
                System.load(path)
                YLog.info("Confundo loaded native lib (abs): $path")
            }
            librariesLoaded = true
            true
        }.getOrElse {
            YLog.error("load native libs failed", it)
            false
        }
    }

    private fun loadViaClassLoader(name: String): Boolean = runCatching {
        System.loadLibrary(name)
        YLog.info("Confundo loaded native lib (loadLibrary): lib$name.so")
        true
    }.getOrElse {
        YLog.warn("Confundo System.loadLibrary($name) failed, will try abs path: ${it.message}")
        false
    }

    private fun findLibraryPath(loader: ClassLoader?, name: String): String? {
        if (loader == null) return null
        val soName = "lib$name.so"

        runCatching {
            loader.javaClass.getMethod("findLibrary", String::class.java)
                .invoke(loader, name) as? String
        }.getOrNull()?.takeIf { it.isNotBlank() && File(it).exists() }?.let { return it }

        (moduleLibDirs(loader).map { it.absolutePath } +
            parseNativeLibDirs(loader.toString()))
            .forEach { dir ->
                if (!dir.contains("!")) {
                    val f = File(dir, soName)
                    if (f.exists()) return f.absolutePath
                }
            }

        moduleApkPaths(loader.toString()).forEach { apk ->
            val libRoot = File(File(apk).parentFile, "lib")
            libRoot.listFiles()?.forEach { abiDir ->
                val f = File(abiDir, soName)
                if (f.exists()) return f.absolutePath
            }
        }

        YLog.warn("Confundo cannot locate $soName; classloader=$loader")
        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun moduleLibDirs(loader: ClassLoader): List<File> {
        var clazz: Class<*>? = loader.javaClass
        while (clazz != null && clazz != Any::class.java) {
            val dirs = runCatching {
                val pathListField = clazz!!.getDeclaredField("pathList").apply { isAccessible = true }
                val pathList = pathListField.get(loader) ?: return@runCatching null
                val ndField = pathList.javaClass
                    .getDeclaredField("nativeLibraryDirectories").apply { isAccessible = true }
                (ndField.get(pathList) as? List<File>)
            }.getOrNull()
            if (dirs != null) return dirs
            clazz = clazz.superclass
        }
        return emptyList()
    }

    private fun parseNativeLibDirs(dump: String): List<String> =
        Regex("""nativeLibraryDirectories=\[([^\]]*)]""").find(dump)
            ?.groupValues?.getOrNull(1)
            ?.split(',')
            ?.map { it.trim() }
            ?: emptyList()

    private fun moduleApkPaths(dump: String): List<String> {
        val paths = LinkedHashSet<String>()
        Regex("""module=(\S+?\.apk)""").find(dump)?.groupValues?.getOrNull(1)?.let { paths.add(it) }
        Regex("""(/[^\s",\[\]]+?\.apk)!/""").findAll(dump).forEach { paths.add(it.groupValues[1]) }
        Regex(""""(/[^"]+\.apk)"""").findAll(dump).forEach { paths.add(it.groupValues[1]) }
        return paths.toList()
    }
}
