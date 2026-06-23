package killua.dev.confundo.data

import killua.dev.confundo.ui.pages.home.FieldKeys
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID


object RandomEngine {

    private val rng = SecureRandom()

    private val operators = listOf(
        Operator("46000", "cn", "中国移动"),
        Operator("46001", "cn", "中国联通"),
        Operator("46011", "cn", "中国电信"),
        Operator("310260", "us", "T-Mobile"),
        Operator("310410", "us", "AT&T"),
        Operator("44010", "jp", "NTT DOCOMO"),
        Operator("44020", "jp", "SoftBank"),
        Operator("45005", "kr", "SK Telecom"),
        Operator("46692", "tw", "Chunghwa"),
        Operator("52501", "sg", "Singtel"),
        Operator("23410", "gb", "O2"),
        Operator("26201", "de", "Telekom"),
    )

    private data class Operator(val numeric: String, val country: String, val name: String)

    private val timezonesByCountry = mapOf(
        "cn" to "Asia/Shanghai", "us" to "America/New_York", "jp" to "Asia/Tokyo",
        "kr" to "Asia/Seoul", "tw" to "Asia/Taipei", "sg" to "Asia/Singapore",
        "gb" to "Europe/London", "de" to "Europe/Berlin",
    )
    private val localeByCountry = mapOf(
        "cn" to "zh_CN", "us" to "en_US", "jp" to "ja_JP", "kr" to "ko_KR",
        "tw" to "zh_TW", "sg" to "en_SG", "gb" to "en_GB", "de" to "de_DE",
    )

    private val batteryCapacities = listOf(4000, 4500, 4700, 5000, 5100, 5500)
    private val batteryTechs = listOf("Li-ion", "Li-poly")

    /**
     * 生成一套完整、自洽的随机字段值。
     *
     * @return key -> 规范化字符串值 的不可变 Map（包含 [FieldCatalog] 中全部字段）。
     */
    fun generate(): Map<String, String> {
        val p = DeviceProfiles.random()
        val op = operators.random()
        val country = op.country
        val ramGb = p.ramOptionsGb.random()
        val romGb = p.romOptionsGb.random()
        val refresh = p.refreshRates.random()
        val kernel = p.kernelOptions.random()
        val buildId = randomBuildId()
        val incremental = (10_000_000..99_999_999).random().toString()

        return buildMap {
            put(FieldKeys.DEVICE_ID, randomHex(16))
            put(FieldKeys.ANDROID_ID, randomHex(16))
            put(FieldKeys.SERIAL, randomAlphaNum(12))
            put(FieldKeys.IMEI, generateImei())
            put(FieldKeys.MEID, randomHexUpper(14))
            put(FieldKeys.IMSI, generateImsi(op.numeric))
            put(FieldKeys.DRM_ID, randomHex(32))
            put(FieldKeys.DRM_SECURITY_LEVEL, DrmSecurityLevel.entries.random().storage)

            put(FieldKeys.PHONE_NUMBER, generatePhone(country))
            put(FieldKeys.NETWORK_COUNTRY, country)
            put(FieldKeys.NETWORK_OPERATOR, op.numeric)
            put(FieldKeys.NETWORK_OPERATOR_NAME, op.name)
            put(FieldKeys.ICCID, generateIccid(op.numeric))
            put(FieldKeys.SIM_COUNTRY, country)
            put(FieldKeys.WIFI_BSSID, generateMac())
            put(FieldKeys.WIFI_SSID, "WIFI-${randomAlphaNum(4).uppercase()}")
            put(FieldKeys.WIFI_MAC, generateMac())

            put(FieldKeys.TIMEZONE, timezonesByCountry[country] ?: "Asia/Shanghai")
            put(FieldKeys.ANDROID_VERSION, p.androidVersion)
            put(FieldKeys.SDK_INT, p.sdkInt.toString())
            put(FieldKeys.LOCALE, localeByCountry[country] ?: "en_US")
            put(FieldKeys.MODEL, p.model)
            put(FieldKeys.DEVICE, p.device)
            put(FieldKeys.BRAND, p.brand)
            put(FieldKeys.MANUFACTURER, p.manufacturer)
            put(FieldKeys.PRODUCT, p.product)
            put(FieldKeys.HARDWARE, p.hardware)
            put(FieldKeys.FINGERPRINT, p.buildFingerprint(buildId, incremental))
            put(FieldKeys.INCREMENTAL, incremental)
            put(FieldKeys.KERNEL, kernel)

            put(FieldKeys.RAM, "$ramGb GB")
            put(FieldKeys.CPU_CORES, p.cpuCores.toString())
            put(FieldKeys.STORAGE, "$romGb GB")
            put(FieldKeys.STORAGE_USED_PERCENT, (25..85).random().toString())
            put(FieldKeys.MAX_REFRESH_RATE, "$refresh Hz")

            put(FieldKeys.BATTERY, "${batteryCapacities.random()} mAh")
            val status = BatteryStatus.entries.random()
            put(FieldKeys.BATTERY_STATUS, status.storage)
            put(
                FieldKeys.BATTERY_PLUGGED,
                if (status == BatteryStatus.CHARGING || status == BatteryStatus.FULL)
                    BatteryPlugged.entries.random().storage else BatteryPlugged.AC.storage
            )
            put(FieldKeys.BATTERY_VOLTAGE, (3700..4400).random().toString())
            put(FieldKeys.BATTERY_TEMPERATURE, (230..360).random().toString()) // 23.0~36.0°C
            put(FieldKeys.BATTERY_HEALTH, BatteryHealth.GOOD.storage)
            put(FieldKeys.BATTERY_LEVEL, (15..100).random().toString())
            put(FieldKeys.BATTERY_CURRENT, (300..3500).random().toString())
            put(FieldKeys.BATTERY_TECHNOLOGY, batteryTechs.random())

            val now = System.currentTimeMillis()
            put(FieldKeys.ACTIVATION_TIME, (now - daysMs((30..720).random())).toString())
            put(FieldKeys.BOOT_TIME, (now - hoursMs((1..2400).random())).toString())
            put(FieldKeys.IS_24H, listOf("true", "false").random())

            // Google 服务
            put(FieldKeys.GOOGLE_AD_ID, UUID.randomUUID().toString())
            put(FieldKeys.GMS_VERSION, "${(23..25).random()}.${(10..50).random()}.${(10..70).random()}")
            put(FieldKeys.PLAY_STORE_VERSION, "${(40..44).random()}.${(1..9).random()}.${(10..40).random()}")

            put(FieldKeys.OPENGL_VERSION, p.openglVersion)
            put(FieldKeys.GL_RENDERER, p.glRenderer)
            put(FieldKeys.GL_VENDOR, p.glVendor)
            put(FieldKeys.SENSOR_VENDOR, p.sensorVendor)

            put(FieldKeys.ACCESSIBILITY_SERVICE_COUNT, (0..5).random().toString())
            put(FieldKeys.ACCESSIBILITY_ENABLED, tf())
            put(FieldKeys.TALKBACK_ENABLED, tf())
            put(FieldKeys.HIGH_CONTRAST_TEXT, tf())
            put(FieldKeys.COLOR_INVERSION, tf())
            put(FieldKeys.DALTONIZER_ENABLED, tf())
            put(FieldKeys.CAPTION_ENABLED, tf())
            put(FieldKeys.MONO_AUDIO, tf())
            put(FieldKeys.POWER_ENDS_CALL, tf())
        }
    }


    fun randomHex(length: Int): String =
        (1..length).joinToString("") { HEX[rng.nextInt(16)].toString() }

    private fun randomHexUpper(length: Int): String = randomHex(length).uppercase()

    private fun randomAlphaNum(length: Int): String =
        (1..length).joinToString("") { ALNUM[rng.nextInt(ALNUM.length)].toString() }

    private fun randomBuildId(): String =
        "${('A'..'Z').random()}${('A'..'Z').random()}${(1..2).random()}A.${(100000..999999).random()}.${(1..30).random().toString().padStart(3, '0')}"

    /** 生成符合 Luhn 校验的 15 位 IMEI。 */
    private fun generateImei(): String {
        val digits = IntArray(14) { rng.nextInt(10) }
        var sum = 0
        digits.forEachIndexed { i, d ->
            // 从右往左第 2、4… 位（此处偶数索引）翻倍
            val v = if (i % 2 == 1) {
                val x = d * 2
                if (x > 9) x - 9 else x
            } else d
            sum += v
        }
        val check = (10 - sum % 10) % 10
        return digits.joinToString("") + check
    }

    private fun generateImsi(mccMnc: String): String {
        // MCC+MNC (5~6位) + 随机 MSIN 补足 15 位
        val rest = 15 - mccMnc.length
        return mccMnc + (1..rest).joinToString("") { rng.nextInt(10).toString() }
    }

    private fun generatePhone(country: String): String = when (country) {
        "cn" -> "1" + listOf("3", "5", "7", "8", "9").random() +
                (1..9).joinToString("") { rng.nextInt(10).toString() }
        "us" -> "+1" + (200..999).random() + (1000000..9999999).random()
        else -> "+" + (1..9).joinToString("") { rng.nextInt(10).toString() }.let { "${(10..99).random()}$it" }
    }

    private fun generateIccid(mccMnc: String): String = "89" + mccMnc + (1..12).joinToString("") { rng.nextInt(10).toString() }

    private fun generateMac(): String =
        (1..6).joinToString(":") {
            String.format(Locale.US, "%02x", rng.nextInt(256))
        }

    private fun daysMs(days: Int) = days.toLong() * 24L * 3600L * 1000L
    private fun hoursMs(hours: Int) = hours.toLong() * 3600L * 1000L

    private const val HEX = "0123456789abcdef"
    private const val ALNUM = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ"

    private fun tf() = listOf("true", "false").random()
}
