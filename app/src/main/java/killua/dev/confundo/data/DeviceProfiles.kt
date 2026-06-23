package killua.dev.confundo.data

/**
 * 与设备无关、纯随机的字段（IMEI、序列号、Android ID 等唯一标识）不在此处，
 */
data class DeviceProfile(
    val brand: String,
    val manufacturer: String,
    val model: String,
    val device: String,
    val product: String,
    val hardware: String,
    /** 兼容的 RAM 选项（GB），用于在合理范围内抖动。 */
    val ramOptionsGb: List<Int>,
    /** 兼容的 ROM 选项（GB）。 */
    val romOptionsGb: List<Int>,
    val cpuCores: Int,
    /** Android 大版本号字符串，如 "14"。 */
    val androidVersion: String,
    /** 对应的 SDK_INT。必须与 [androidVersion] 匹配。 */
    val sdkInt: Int,
    /** 与 SDK 年代相符的内核版本池。 */
    val kernelOptions: List<String>,
    val refreshRates: List<Int>,
    val glRenderer: String,
    val glVendor: String,
    val openglVersion: String,
    val sensorVendor: String,
) {
    fun buildFingerprint(buildId: String, incremental: String): String =
        "$brand/$product/$device:$androidVersion/$buildId/$incremental:user/release-keys"
}

object DeviceProfiles {

    val all: List<DeviceProfile> = listOf(
        DeviceProfile(
            brand = "google", manufacturer = "Google",
            model = "Pixel 9 Pro", device = "caiman", product = "caiman", hardware = "zuma",
            ramOptionsGb = listOf(16), romOptionsGb = listOf(128, 256, 512),
            cpuCores = 8, androidVersion = "15", sdkInt = 35,
            kernelOptions = listOf("6.1.75", "6.1.90"),
            refreshRates = listOf(120), glRenderer = "Mali-G715-Immortalis MC12",
            glVendor = "ARM", openglVersion = "OpenGL ES 3.2", sensorVendor = "Google",
        ),
        DeviceProfile(
            brand = "google", manufacturer = "Google",
            model = "Pixel 8", device = "shiba", product = "shiba", hardware = "zuma",
            ramOptionsGb = listOf(8), romOptionsGb = listOf(128, 256),
            cpuCores = 9, androidVersion = "14", sdkInt = 34,
            kernelOptions = listOf("5.15.137", "5.15.148"),
            refreshRates = listOf(120), glRenderer = "Mali-G715-Immortalis MC10",
            glVendor = "ARM", openglVersion = "OpenGL ES 3.2", sensorVendor = "Google",
        ),
        DeviceProfile(
            brand = "samsung", manufacturer = "samsung",
            model = "SM-S928B", device = "e3q", product = "e3qxeea", hardware = "qcom",
            ramOptionsGb = listOf(12), romOptionsGb = listOf(256, 512, 1024),
            cpuCores = 8, androidVersion = "14", sdkInt = 34,
            kernelOptions = listOf("5.15.137", "5.15.149"),
            refreshRates = listOf(120), glRenderer = "Adreno (TM) 750",
            glVendor = "Qualcomm", openglVersion = "OpenGL ES 3.2", sensorVendor = "Samsung",
        ),
        DeviceProfile(
            brand = "samsung", manufacturer = "samsung",
            model = "SM-S918B", device = "dm3q", product = "dm3qxeea", hardware = "qcom",
            ramOptionsGb = listOf(8, 12), romOptionsGb = listOf(256, 512),
            cpuCores = 8, androidVersion = "14", sdkInt = 34,
            kernelOptions = listOf("5.15.94", "5.15.137"),
            refreshRates = listOf(120), glRenderer = "Adreno (TM) 740",
            glVendor = "Qualcomm", openglVersion = "OpenGL ES 3.2", sensorVendor = "Samsung",
        ),
        DeviceProfile(
            brand = "Xiaomi", manufacturer = "Xiaomi",
            model = "24031PN0DC", device = "shennong", product = "shennong", hardware = "qcom",
            ramOptionsGb = listOf(12, 16), romOptionsGb = listOf(256, 512),
            cpuCores = 8, androidVersion = "14", sdkInt = 34,
            kernelOptions = listOf("5.15.123", "5.15.149"),
            refreshRates = listOf(120), glRenderer = "Adreno (TM) 750",
            glVendor = "Qualcomm", openglVersion = "OpenGL ES 3.2", sensorVendor = "Xiaomi",
        ),
        DeviceProfile(
            brand = "OnePlus", manufacturer = "OnePlus",
            model = "PJD110", device = "OP5929L1", product = "PJD110", hardware = "qcom",
            ramOptionsGb = listOf(12, 16), romOptionsGb = listOf(256, 512),
            cpuCores = 8, androidVersion = "14", sdkInt = 34,
            kernelOptions = listOf("5.15.137", "6.1.57"),
            refreshRates = listOf(120), glRenderer = "Adreno (TM) 750",
            glVendor = "Qualcomm", openglVersion = "OpenGL ES 3.2", sensorVendor = "OnePlus",
        ),
        DeviceProfile(
            brand = "vivo", manufacturer = "vivo",
            model = "V2324A", device = "PD2324", product = "PD2324", hardware = "mt6989",
            ramOptionsGb = listOf(12, 16), romOptionsGb = listOf(256, 512),
            cpuCores = 8, androidVersion = "14", sdkInt = 34,
            kernelOptions = listOf("5.15.137", "6.1.57"),
            refreshRates = listOf(120), glRenderer = "Mali-G720-Immortalis MC12",
            glVendor = "ARM", openglVersion = "OpenGL ES 3.2", sensorVendor = "MTK",
        ),
        DeviceProfile(
            brand = "OPPO", manufacturer = "OPPO",
            model = "PHZ110", device = "OP5D2F", product = "PHZ110", hardware = "mt6989",
            ramOptionsGb = listOf(12, 16), romOptionsGb = listOf(256, 512),
            cpuCores = 8, androidVersion = "14", sdkInt = 34,
            kernelOptions = listOf("5.15.137", "6.1.57"),
            refreshRates = listOf(120), glRenderer = "Mali-G720-Immortalis MC12",
            glVendor = "ARM", openglVersion = "OpenGL ES 3.2", sensorVendor = "MTK",
        ),
        DeviceProfile(
            brand = "samsung", manufacturer = "samsung",
            model = "SM-S711B", device = "r11q", product = "r11qxeea", hardware = "qcom",
            ramOptionsGb = listOf(8), romOptionsGb = listOf(128, 256),
            cpuCores = 8, androidVersion = "13", sdkInt = 33,
            kernelOptions = listOf("5.10.177", "5.10.198"),
            refreshRates = listOf(120), glRenderer = "Adreno (TM) 730",
            glVendor = "Qualcomm", openglVersion = "OpenGL ES 3.2", sensorVendor = "Samsung",
        ),
        DeviceProfile(
            brand = "Xiaomi", manufacturer = "Xiaomi",
            model = "2210132C", device = "fuxi", product = "fuxi", hardware = "qcom",
            ramOptionsGb = listOf(8, 12), romOptionsGb = listOf(128, 256),
            cpuCores = 8, androidVersion = "13", sdkInt = 33,
            kernelOptions = listOf("5.15.78", "5.15.94"),
            refreshRates = listOf(120), glRenderer = "Adreno (TM) 740",
            glVendor = "Qualcomm", openglVersion = "OpenGL ES 3.2", sensorVendor = "Xiaomi",
        ),
        DeviceProfile(
            brand = "motorola", manufacturer = "motorola",
            model = "moto g84 5G", device = "bangkk", product = "bangkk_retail", hardware = "qcom",
            ramOptionsGb = listOf(8, 12), romOptionsGb = listOf(128, 256),
            cpuCores = 8, androidVersion = "14", sdkInt = 34,
            kernelOptions = listOf("5.4.210", "5.10.149"),
            refreshRates = listOf(120), glRenderer = "Adreno (TM) 619",
            glVendor = "Qualcomm", openglVersion = "OpenGL ES 3.2", sensorVendor = "Motorola",
        ),
        DeviceProfile(
            brand = "google", manufacturer = "Google",
            model = "Pixel 7", device = "panther", product = "panther", hardware = "gs201",
            ramOptionsGb = listOf(8), romOptionsGb = listOf(128, 256),
            cpuCores = 8, androidVersion = "13", sdkInt = 33,
            kernelOptions = listOf("5.10.157", "5.10.177"),
            refreshRates = listOf(90), glRenderer = "Mali-G710 MC10",
            glVendor = "ARM", openglVersion = "OpenGL ES 3.2", sensorVendor = "Google",
        ),
    )

    fun random(): DeviceProfile = all.random()
}
