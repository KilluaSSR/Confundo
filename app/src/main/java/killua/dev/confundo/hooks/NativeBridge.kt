package killua.dev.confundo.hooks

/**
 * 与 native 层通信的桥接。
 *
 * native 侧对 libc 的 `__system_property_get` / `__system_property_read_callback`、
 * `openat`（重定向 build.prop）以及 libvulkan 的 `vkGetPhysicalDeviceProperties`
 * 做 inline hook，从而覆盖 Java 层 hook 无法触及的「native 直读」泄露路径。
 */
object NativeBridge {

    /**
     * @param keys 系统属性名数组（如 `ro.product.model`）。
     * @param values 与 [keys] 一一对应的伪装值。
     * @param cacheDir 目标 App 可写缓存目录；为空则跳过基于文件重定向的 hook（build.prop / proc）。
     * @param kernel 内核版本，用于 `uname` 与 `/proc/version` 伪装；为空则跳过。
     * @param vulkanDeviceName Vulkan `deviceName` 伪装值；为空则跳过 Vulkan hook。
     * @return 是否安装成功（含幂等返回）。
     */
    @JvmStatic
    external fun nativeInstall(
        keys: Array<String>,
        values: Array<String>,
        cacheDir: String,
        kernel: String,
        vulkanDeviceName: String,
    ): Boolean
}
