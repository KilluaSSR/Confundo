package killua.dev.confundo.hooks.delegates

import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.confundo.hooks.HookDelegate
import killua.dev.confundo.hooks.spoof
import killua.dev.confundo.ui.pages.home.FieldKeys

object LocaleHooks : HookDelegate {

    private val validLocales = listOf(
        "zh_CN", "zh_TW", "zh_HK",
        "en_US", "en_GB", "en_AU", "en_CA",
        "ja_JP", "ko_KR",
        "de_DE", "fr_FR", "es_ES", "it_IT", "pt_BR", "ru_RU", "nl_NL",
        "ar_SA", "th_TH", "vi_VN", "id_ID", "tr_TR", "pl_PL",
    )

    override fun PackageParam.apply(fields: Map<String, String>) {
        val locale = fields.spoof(FieldKeys.LOCALE)
        val timezone = fields.spoof(FieldKeys.TIMEZONE)

        if (locale != null && locale in validLocales) {
            val parts = locale.split("_")
            val fake = java.util.Locale(parts[0], parts.getOrElse(1) { "" })

            java.util.Locale::class.java.hook {
                try {
                    injectMember {
                        method { name = "getDefault" }
                        afterHook { result = fake }
                    }
                } catch (_: NoSuchMethodError) {}
            }
        }

        if (timezone != null) {
            java.util.TimeZone::class.java.hook {
                try {
                    injectMember {
                        method { name = "getDefault" }
                        afterHook { result = java.util.TimeZone.getTimeZone(timezone) }
                    }
                } catch (_: NoSuchMethodError) {}
            }

            java.time.ZoneId::class.java.hook {
                try {
                    injectMember {
                        method { name = "systemDefault" }
                        afterHook { result = java.time.ZoneId.of(timezone) }
                    }
                } catch (e: Exception) {
                    YLog.error("Hook ZoneId.systemDefault failed", e)
                }
            }
        }
    }
}
