package killua.dev.confundo.hooks.delegates

import android.os.StatFs
import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.hooks.spoof
import killua.dev.confundo.ui.pages.home.FieldKeys
import java.io.File

object HardwareHooks : HookDelegate {
    private const val BLOCK_SIZE = 4096L
    override fun PackageParam.apply(fields: Map<String, String>) {
        CPUHooks(fields)
        MemoryHooks(fields)
        StorageHooks(fields)
    }

    private fun PackageParam.CPUHooks(fields: Map<String, String>){
        val cpuCores = fields.spoof(FieldKeys.CPU_CORES)?.toIntOrNull()
        if (cpuCores != null) {
            Runtime.getRuntime().javaClass.hook {
                try {
                    injectMember {
                        method { name = "availableProcessors" }
                        afterHook { result = cpuCores }
                    }
                } catch (_: NoSuchMethodError) {}
            }
        }
    }

    private fun PackageParam.MemoryHooks(fields: Map<String, String>){
        val ram = fields.spoof(FieldKeys.RAM) ?: return
        val ramGb = ram.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: return
        val totalMem = (ramGb * 1024 * 1024 * 1024).toLong()

        "android.app.ActivityManager".toClassOrNull()?.hook {
            try {
                injectMember {
                    method {
                        name = "getMemoryInfo"
                        param("android.app.ActivityManager\$MemoryInfo")
                    }
                    afterHook {
                        (args[0] as? android.app.ActivityManager.MemoryInfo)?.totalMem = totalMem
                    }
                }
            } catch (_: NoSuchMethodError) {}
        }
    }

    private fun PackageParam.StorageHooks(fields: Map<String, String>){
        val storage = fields.spoof(FieldKeys.STORAGE) ?: return
        val sizeGb = storage.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: return
        val usedPercentStr = fields.spoof(FieldKeys.STORAGE_USED_PERCENT)
        val usedPercent = usedPercentStr?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
        val totalBytes = (sizeGb * 1000 * 1000 * 1000).toLong()

        val freeBytes = (totalBytes * ((100.0 - usedPercent) / 100.0)).toLong()

        val blockCount = totalBytes / BLOCK_SIZE
        val freeBlocks = freeBytes / BLOCK_SIZE
        StatFs::class.java.hook {
            try {
                injectMember {
                    method { name = "getTotalBytes" }
                    afterHook { result = totalBytes }
                }
            } catch (_: NoSuchMethodError) {}
            try {
                injectMember {
                    method { name = "getFreeBytes" }
                    afterHook { result = freeBytes }
                }
            } catch (_: NoSuchMethodError) {}
            try {
                injectMember {
                    method { name = "getAvailableBytes" }
                    afterHook { result = freeBytes }
                }
            } catch (_: NoSuchMethodError) {}
            try {
                injectMember {
                    method { name = "getBlockCountLong" }
                    afterHook { result = blockCount }
                }
            } catch (_: NoSuchMethodError) {}

            try {
                injectMember {
                    method { name = "getBlockSizeLong" }
                    afterHook { result = BLOCK_SIZE }
                }
            } catch (_: NoSuchMethodError) {}
            try {
                injectMember {
                    method { name = "getFreeBlocksLong" }
                    afterHook { result = freeBlocks }
                }
            } catch (_: NoSuchMethodError) {}
            try {
                injectMember {
                    method { name = "getAvailableBlocksLong" }
                    afterHook { result = freeBlocks }
                }
            } catch (_: NoSuchMethodError) {}
        }

        File::class.java.hook {
            try {
                injectMember {
                    method { name = "getTotalSpace" }
                    afterHook {
                        if (instance<File>().path.contains("emulated") || instance<File>().path.startsWith("/data") || instance<File>().path.startsWith("/storage")) {
                            result = totalBytes
                        }
                    }
                }

            } catch (_: NoSuchMethodError) {}
            try {
                injectMember {
                    method { name = "getFreeSpace" }
                    afterHook {
                        val path = instance<File>().path
                        if (path.contains("emulated") || path.startsWith("/data") || path.startsWith("/storage")) {
                            result = freeBytes
                        }
                    }
                }

            } catch (_: NoSuchMethodError) {}
            try {
                injectMember {
                    method { name = "getUsableSpace" }
                    afterHook {
                        val path = instance<File>().path
                        if (path.contains("emulated") || path.startsWith("/data") || path.startsWith("/storage")) {
                            result = freeBytes
                        }
                    }
                }

            } catch (_: NoSuchMethodError) {}
        }

        "android.app.usage.StorageStatsManager".toClassOrNull()?.hook {
            try {
                injectMember {
                    method { name = "getTotalBytes"; paramCount = 1 }
                    afterHook { result = totalBytes }
                }
                injectMember {
                    method { name = "getFreeBytes"; paramCount = 1 }
                    afterHook { result = freeBytes }
                }
            } catch (_: NoSuchMethodError) {}
        }
    }
}
