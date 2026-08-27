# ============================================================================
# Confundo R8 规则
# 目标：在保证 Xposed 模块可被加载、JNI / 反射不被破坏的前提下，尽量压缩包体积。
# 通过 proguard-android-optimize.txt 启用优化；下面追加体积相关与必需的 keep。
# ============================================================================

# ---- 体积优化：扁平化包名 + 允许修改访问修饰符，便于内联与合并 ----
-repackageclasses ''
-allowaccessmodification
# 更激进的优化轮次。
-optimizationpasses 5

# 不保留调试信息（更小体积）。崩溃栈将被混淆——如需排查可临时改为 include。
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
# kavaref（YukiHookAPI 反射内核）在低版本 SDK 上对该反射类的可选引用。
-dontwarn java.lang.reflect.AnnotatedType

# ============================================================================
# Xposed / YukiHookAPI —— 入口类由 KSP 生成并写入 assets/xposed_init，
# Xposed 框架按「字符串类名」反射加载，绝不能被重命名或裁剪。
# ============================================================================
# 原生 Xposed 入口接口 / 注解入口实现。
-keep class * implements com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit { *; }
-keep @com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed class * { *; }
# KSP 生成的 Xposed 初始化类（位于 app 包下，名字含 _YukiHookXposedInit）。
-keep class **_YukiHookXposedInit { *; }
# YukiHookAPI 自身大量使用反射与 Xposed 桥接，整体保留以确保 hook 生效。
-keep class com.highcapable.yukihookapi.** { *; }
-dontwarn com.highcapable.yukihookapi.**
# 兼容直接引用 Xposed API 的情形（本模块 compileOnly，不打包，但保险起见忽略告警）。
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**

# 本模块 Xposed 入口对象，确保被生成类可达。
-keep class killua.dev.confundo.hooks.HookEntry { *; }

# ============================================================================
# JNI —— native 代码通过精确符号名 Java_killua_dev_confundo_hooks_NativeBridge_nativeInstall
# 反查该类与方法；类名 / 方法名 / 包名一旦被混淆即崩溃。
# ============================================================================
-keep class killua.dev.confundo.hooks.NativeBridge { *; }
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# ============================================================================
# WorkManager —— 周期任务 Worker 由框架按类名实例化。
# ============================================================================
-keep class killua.dev.confundo.work.** { *; }

# ============================================================================
# Shizuku —— AIDL / Binder 交互，保留其接口与 Provider。
# ============================================================================
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-dontwarn rikka.shizuku.**
-dontwarn moe.shizuku.**

# ============================================================================
# Kotlin 元数据 / 协程：容忍缺失，避免优化告警中断构建。
# ============================================================================
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlin.Metadata { *; }
