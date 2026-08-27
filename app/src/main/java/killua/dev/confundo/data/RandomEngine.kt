package killua.dev.confundo.data

import android.os.Build
import killua.dev.confundo.ui.pages.home.FieldKeys
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID

object RandomEngine {

    private val rng = SecureRandom()

    /** Android 大版本号上限（Android 16）。 */
    private const val MAX_ANDROID_VERSION = 16

    /** SDK_INT 上限（API 36 = Android 16）。 */
    private const val MAX_SDK_INT = 36

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

    private val dialCodeByCountry = mapOf(
        "cn" to "86", "us" to "1", "jp" to "81", "kr" to "82",
        "tw" to "886", "sg" to "65", "gb" to "44", "de" to "49",
    )

    private val batteryCapacities = listOf(4000, 4500, 4700, 5000, 5100, 5500)
    private val batteryTechs = listOf("Li-ion", "Li-poly")
    private val meidPrefixes = listOf("A00000", "A10000", "990000", "860000")

    /**
     * @param includeActivationTime 为 false 时「开机激活时间」保持为空。
     * @param includeBootTime 为 false 时「开机时间」保持为空。
     * @param installedGmsVersion 本机已安装的 Google Play 服务 versionName，用于生成「不低于本机」的版本。
     * @param installedPlayVersion 本机已安装的 Play Store versionName，用途同上。
     */
    fun generate(
        includeActivationTime: Boolean = true,
        includeBootTime: Boolean = true,
        installedGmsVersion: String? = null,
        installedPlayVersion: String? = null,
    ): Map<String, String> {
        val p = DeviceProfiles.random()
        val op = operators.securePick()
        val country = op.country
        val ramGb = p.ramOptionsGb.securePick()
        val romGb = p.romOptionsGb.securePick()
        val refresh = p.refreshRates.securePick()
        val kernel = p.kernelOptions.securePick()
        val buildId = randomBuildId()
        val incremental = randomInt(10_000_000, 99_999_999).toString()
        val batteryStatus = BatteryStatus.entries.securePick()
        val batteryPlugged = when (batteryStatus) {
            BatteryStatus.CHARGING, BatteryStatus.FULL -> BatteryPlugged.entries.securePick()
            else -> null
        }

        // 版本随机化：严格落在 [本机真实值, 上限] 区间内，绝不低于本机真实值，也不超过上限。
        val realSdk = Build.VERSION.SDK_INT
        val realAndroidVersion = Build.VERSION.RELEASE?.substringBefore('.')?.toIntOrNull()
            ?: sdkToAndroidVersion(realSdk)
        val sdkInt = randomInt(realSdk.coerceAtMost(MAX_SDK_INT), MAX_SDK_INT)
        val androidVersion = randomInt(
            realAndroidVersion.coerceAtMost(MAX_ANDROID_VERSION),
            MAX_ANDROID_VERSION,
        )

        return buildMap {
            put(FieldKeys.DEVICE_ID, randomHex(16))
            put(FieldKeys.ANDROID_ID, randomHex(16))
            put(FieldKeys.SERIAL, randomAlphaNum(12))
            put(FieldKeys.IMEI, generateImei())
            put(FieldKeys.MEID, generateMeid())
            put(FieldKeys.IMSI, generateImsi(op.numeric))
            put(FieldKeys.DRM_ID, randomHex(32))
            put(FieldKeys.DRM_SECURITY_LEVEL, DrmSecurityLevel.entries.securePick().storage)

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
            put(FieldKeys.ANDROID_VERSION, androidVersion.toString())
            put(FieldKeys.SDK_INT, sdkInt.toString())
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
            put(FieldKeys.STORAGE_USED_PERCENT, randomInt(25, 85).toString())
            put(FieldKeys.MAX_REFRESH_RATE, "$refresh Hz")

            put(FieldKeys.BATTERY, "${batteryCapacities.securePick()} mAh")
            put(FieldKeys.BATTERY_STATUS, batteryStatus.storage)
            put(FieldKeys.BATTERY_PLUGGED, batteryPlugged?.storage.orEmpty())
            put(FieldKeys.BATTERY_VOLTAGE, randomInt(3700, 4400).toString())
            put(FieldKeys.BATTERY_TEMPERATURE, randomInt(230, 360).toString()) // 23.0~36.0°C
            put(FieldKeys.BATTERY_HEALTH, BatteryHealth.GOOD.storage)
            put(FieldKeys.BATTERY_LEVEL, randomInt(15, 100).toString())
            put(FieldKeys.BATTERY_CURRENT, randomInt(300, 3500).toString())
            put(FieldKeys.BATTERY_TECHNOLOGY, batteryTechs.securePick())

            val now = System.currentTimeMillis()
            put(
                FieldKeys.ACTIVATION_TIME,
                if (includeActivationTime) (now - daysMs(randomInt(30, 720))).toString() else "",
            )
            put(
                FieldKeys.BOOT_TIME,
                if (includeBootTime) (now - hoursMs(randomInt(1, 2400))).toString() else "",
            )
            put(FieldKeys.IS_24H, tf())

            // Google 服务
            put(FieldKeys.GOOGLE_AD_ID, UUID.randomUUID().toString())
            put(FieldKeys.GMS_VERSION, randomVersionNotBelow(installedGmsVersion, FALLBACK_GMS_VERSION))
            put(FieldKeys.PLAY_STORE_VERSION, randomVersionNotBelow(installedPlayVersion, FALLBACK_PLAY_VERSION))

            put(FieldKeys.OPENGL_VERSION, p.openglVersion)
            put(FieldKeys.GL_RENDERER, p.glRenderer)
            put(FieldKeys.GL_VENDOR, p.glVendor)
            put(FieldKeys.SENSOR_VENDOR, p.sensorVendor)

            put(FieldKeys.ACCESSIBILITY_SERVICE_COUNT, randomInt(0, 5).toString())
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
        "${randomUpper()}${randomUpper()}${randomInt(1, 2)}A.${randomInt(100000, 999999)}.${randomInt(1, 30).toString().padStart(3, '0')}"

    /** 生成符合 Luhn 校验的 15 位 IMEI。 */
    private fun generateImei(): String {
        val body = randomDigits(14)
        return body + luhnCheckDigit(body)
    }

    private fun generateMeid(): String = meidPrefixes.securePick() + randomHexUpper(8)

    private fun generateImsi(mccMnc: String): String {
        val prefix = mccMnc.take(6)
        val rest = (15 - prefix.length).coerceAtLeast(0)
        return prefix + randomDigits(rest)
    }

    private fun generatePhone(country: String): String {
        val dial = dialCodeByCountry[country] ?: "1"
        return when (country) {
            "cn" -> "+86" + "1" + listOf("3", "5", "7", "8", "9").securePick() + randomDigits(9)
            "us" -> {
                val area = "${randomInt(2, 9)}${randomInt(0, 8)}${randomInt(0, 9)}"
                val exchange = "${randomInt(2, 9)}${randomInt(0, 9)}${randomInt(0, 9)}"
                "+1$area$exchange${randomDigits(4)}"
            }
            "jp" -> "+81" + listOf("70", "80", "90").securePick() + randomDigits(8)
            "kr" -> "+82" + "10" + randomDigits(8)
            "tw" -> "+886" + "9" + randomDigits(8)
            "sg" -> "+65" + listOf("8", "9").securePick() + randomDigits(7)
            "gb" -> "+44" + "7" + randomDigits(9)
            "de" -> "+49" + "15" + randomDigits(9)
            else -> "+$dial${randomDigits(9)}"
        }
    }

    private fun generateIccid(mccMnc: String): String {
        val prefix = "89" + mccMnc.take(6)
        val body = prefix + randomDigits((19 - prefix.length).coerceAtLeast(1))
        return body + luhnCheckDigit(body)
    }

    private fun generateMac(): String {
        val first = (rng.nextInt(256) or 0x02) and 0xFE
        val tail = IntArray(5) { rng.nextInt(256) }
        return (listOf(first) + tail.toList())
            .joinToString(":") { String.format(Locale.US, "%02x", it) }
    }

    private fun luhnCheckDigit(body: String): Int {
        var sum = 0
        body.reversed().forEachIndexed { index, c ->
            var d = c.digitToInt()
            if (index % 2 == 0) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
        }
        return (10 - (sum % 10)) % 10
    }

    private fun randomDigits(length: Int): String =
        (1..length).joinToString("") { rng.nextInt(10).toString() }

    private fun randomUpper(): Char = ('A'.code + rng.nextInt(26)).toChar()

    private fun randomInt(min: Int, max: Int): Int = min + rng.nextInt(max - min + 1)

    private fun randomVersionNotBelow(installed: String?, fallback: String): String {
        val base = installed?.let { VERSION_PATTERN.find(it) } ?: VERSION_PATTERN.find(fallback)
        ?: return fallback
        val (major, minor, patch) = base.destructured
        val bumpedPatch = (patch.toIntOrNull() ?: 0) + randomInt(0, 5)
        return "$major.$minor.$bumpedPatch"
    }

    private val VERSION_PATTERN = Regex("""(\d+)\.(\d+)\.(\d+)""")

    /** 当 Build.VERSION.RELEASE 非数字（如预览版代号）时，由 SDK_INT 推断大版本号。 */
    private fun sdkToAndroidVersion(sdk: Int): Int = when (sdk) {
        in Int.MIN_VALUE..29 -> 10
        30 -> 11
        31, 32 -> 12
        33 -> 13
        34 -> 14
        35 -> 15
        else -> MAX_ANDROID_VERSION
    }

    private fun <T> List<T>.securePick(): T = this[rng.nextInt(size)]

    private fun daysMs(days: Int) = days.toLong() * 24L * 3600L * 1000L
    private fun hoursMs(hours: Int) = hours.toLong() * 3600L * 1000L

    private const val HEX = "0123456789abcdef"
    private const val ALNUM = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ"

    private const val FALLBACK_GMS_VERSION = "26.32.34"
    private const val FALLBACK_PLAY_VERSION = "52.4.41"

    private fun tf() = if (rng.nextBoolean()) "true" else "false"
}
