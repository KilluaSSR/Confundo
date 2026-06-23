package killua.dev.confundo.hooks.delegates

import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.hooks.spoof
import killua.dev.confundo.ui.pages.home.FieldKeys

/**
 * OpenGL ES 渲染器 Hook：篡改 `glGetString(GL_RENDERER)` / `glGetString(GL_VENDOR)` /
 * `glGetString(GL_VERSION)` 的返回值（如 "Adreno (TM) 750" / "Qualcomm"）。
 */
object OpenGLHooks : HookDelegate {

    private const val GL_VENDOR = 0x1F00
    private const val GL_RENDERER = 0x1F01
    private const val GL_VERSION = 0x1F02

    override fun PackageParam.apply(fields: Map<String, String>) {
        val renderer = fields.spoof(FieldKeys.GL_RENDERER)
        val vendor = fields.spoof(FieldKeys.GL_VENDOR)
        val version = fields.spoof(FieldKeys.OPENGL_VERSION)
        if (renderer == null && vendor == null && version == null) return

        listOf("android.opengl.GLES20", "android.opengl.GLES10").forEach { clazz ->
            clazz.toClassOrNull()?.hook {
                try {
                    injectMember {
                        method { name = "glGetString"; param(Int::class.java) }
                        afterHook {
                            when (args().first().int()) {
                                GL_RENDERER -> renderer?.let { result = it }
                                GL_VENDOR -> vendor?.let { result = it }
                                GL_VERSION -> version?.let { result = it }
                            }
                        }
                    }
                } catch (_: NoSuchMethodError) {}
            }
        }
    }
}
