package killua.dev.confundo.ui.pages.home


object FieldKeys {
    const val ENABLED = "_enabled"
    const val AUTO_RESET = "_auto_reset"

    const val DEVICE_ID = "device_id"
    const val ANDROID_ID = "android_id"
    const val SERIAL = "serial"
    const val IMEI = "imei"
    const val MEID = "meid"
    const val IMSI = "imsi"
    const val DRM_ID = "drm_id"
    const val DRM_SECURITY_LEVEL = "drm_security_level"

    // 运营商与网络
    const val PHONE_NUMBER = "phone_number"
    const val NETWORK_COUNTRY = "network_country"
    const val NETWORK_OPERATOR = "network_operator"
    const val NETWORK_OPERATOR_NAME = "network_operator_name"
    const val ICCID = "iccid"
    const val SIM_COUNTRY = "sim_country"
    const val WIFI_BSSID = "wifi_bssid"
    const val WIFI_SSID = "wifi_ssid"
    const val WIFI_MAC = "wifi_mac"

    // 系统版本
    const val TIMEZONE = "timezone"
    const val ANDROID_VERSION = "android_version"
    const val SDK_INT = "sdk_int"
    const val LOCALE = "locale"
    const val MODEL = "model"
    const val DEVICE = "device"
    const val BRAND = "brand"
    const val MANUFACTURER = "manufacturer"
    const val PRODUCT = "product"
    const val HARDWARE = "hardware"
    const val FINGERPRINT = "fingerprint"
    const val INCREMENTAL = "incremental"
    const val KERNEL = "kernel"

    // 硬件规格
    const val RAM = "ram"
    const val CPU_CORES = "cpu_cores"
    const val STORAGE = "storage"
    const val STORAGE_USED_PERCENT = "storage_used_percent"
    const val MAX_REFRESH_RATE = "max_refresh_rate"

    // 电池
    const val BATTERY = "battery"
    const val BATTERY_STATUS = "battery_status"
    const val BATTERY_PLUGGED = "battery_plugged"
    const val BATTERY_VOLTAGE = "battery_voltage"
    const val BATTERY_TEMPERATURE = "battery_temp"
    const val BATTERY_HEALTH = "battery_health"
    const val BATTERY_LEVEL = "battery_level"
    const val BATTERY_CURRENT = "battery_current"
    const val BATTERY_TECHNOLOGY = "battery_tech"

    // 时间
    const val ACTIVATION_TIME = "activation_time"
    const val BOOT_TIME = "boot_time"
    const val IS_24H = "is_24h"

    // Google 服务
    const val GOOGLE_AD_ID = "google_ad_id"
    const val GMS_VERSION = "gms_version"
    const val PLAY_STORE_VERSION = "play_store_version"

    // 渲染与传感器
    const val OPENGL_VERSION = "opengl_version"
    const val GL_RENDERER = "gl_renderer"
    const val GL_VENDOR = "gl_vendor"
    const val SENSOR_VENDOR = "sensor_vendor"

    // 无障碍
    const val ACCESSIBILITY_SERVICE_COUNT = "accessibility_service_count"
    const val ACCESSIBILITY_ENABLED = "accessibility_enabled"
    const val TALKBACK_ENABLED = "talkback_enabled"
    const val HIGH_CONTRAST_TEXT = "high_contrast_text"
    const val COLOR_INVERSION = "color_inversion"
    const val DALTONIZER_ENABLED = "daltonizer_enabled"
    const val CAPTION_ENABLED = "caption_enabled"
    const val MONO_AUDIO = "mono_audio"
    const val POWER_ENDS_CALL = "power_ends_call"

    val fieldEntries: List<Pair<String, Int>> by lazy {
        killua.dev.confundo.data.FieldCatalog.specs.map { it.key to it.labelRes }
    }
}
