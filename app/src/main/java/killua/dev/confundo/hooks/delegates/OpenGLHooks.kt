package killua.dev.confundo.hooks.delegates

import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.hooks.spoof
import killua.dev.confundo.ui.pages.home.FieldKeys


object OpenGLHooks : HookDelegate {

    private const val GL_VENDOR = 0x1F00
    private const val GL_RENDERER = 0x1F01
    private const val GL_VERSION = 0x1F02

    private const val EGL_VENDOR = 0x3053

    override fun PackageParam.apply(fields: Map<String, String>) {
        val renderer = fields.spoof(FieldKeys.GL_RENDERER)
        val vendor = fields.spoof(FieldKeys.GL_VENDOR)
        val version = fields.spoof(FieldKeys.OPENGL_VERSION)
        if (renderer == null && vendor == null && version == null) return

        listOf(
            "android.opengl.GLES10",
            "android.opengl.GLES20",
        ).forEach { clazz ->
            val glClass = clazz.toClassOrNull() ?: return@forEach
            val declaresGetString = runCatching {
                glClass.getDeclaredMethod("glGetString", Int::class.javaPrimitiveType)
            }.isSuccess
            if (!declaresGetString) return@forEach

            glClass.hook {
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
            }
        }

        if (vendor != null) eglVendorHook(vendor)
    }

    private fun PackageParam.eglVendorHook(vendor: String) {
        "android.opengl.EGL14".toClassOrNull()?.hook {
            try {
                injectMember {
                    method { name = "eglQueryString" }
                    afterHook {
                        if ((args().last().int()) == EGL_VENDOR) result = vendor
                    }
                }
            } catch (_: NoSuchMethodError) {}
        }

        "com.google.android.gles_jni.EGLImpl".toClassOrNull()?.hook {
            try {
                injectMember {
                    method { name = "eglQueryString" }
                    afterHook {
                        if ((args().last().int()) == EGL_VENDOR) result = vendor
                    }
                }
            } catch (_: NoSuchMethodError) {}
        }
    }
}
