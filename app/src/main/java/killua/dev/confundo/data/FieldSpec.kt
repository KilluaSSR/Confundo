package killua.dev.confundo.data

import androidx.annotation.StringRes
import killua.dev.confundo.R
import killua.dev.confundo.ui.pages.home.FieldKeys

enum class FieldCategory(@StringRes val titleRes: Int) {
    IDENTITY(R.string.section_identity),
    TELEPHONY(R.string.section_telephony),
    SYSTEM(R.string.section_system),
    HARDWARE(R.string.section_hardware),
    BATTERY(R.string.section_battery),
    TIME(R.string.section_time),
    GOOGLE(R.string.section_google),
    ACCESSIBILITY(R.string.section_accessibility),
    DRM(R.string.section_drm),
    RENDER(R.string.section_render),
}

sealed interface FieldInputType {

    data object Text : FieldInputType

    data class Number(
        val decimal: Boolean = false,
        val units: List<String> = emptyList(),
    ) : FieldInputType

    data class Enum(val options: List<String>) : FieldInputType

    data object Bool : FieldInputType
}


data class FieldSpec(
    val key: String,
    @StringRes val labelRes: Int,
    val category: FieldCategory,
    val inputType: FieldInputType,
)

object FieldCatalog {

    object Units {
        val STORAGE = listOf("GB", "MB", "TB")
        val MEMORY = listOf("GB", "MB")
        val CAPACITY = listOf("mAh")
        val VOLTAGE = listOf("mV")
        val CURRENT = listOf("mA")
        val REFRESH = listOf("Hz")
    }

    private val boolInput: FieldInputType get() = FieldInputType.Bool

    val specs: List<FieldSpec> = listOf(
        // 身份标识
        FieldSpec(FieldKeys.DEVICE_ID, R.string.detail_item_device_id, FieldCategory.IDENTITY, FieldInputType.Text),
        FieldSpec(FieldKeys.ANDROID_ID, R.string.detail_item_android_id, FieldCategory.IDENTITY, FieldInputType.Text),
        FieldSpec(FieldKeys.SERIAL, R.string.detail_item_serial, FieldCategory.IDENTITY, FieldInputType.Text),
        FieldSpec(FieldKeys.IMEI, R.string.detail_item_imei, FieldCategory.IDENTITY, FieldInputType.Text),
        FieldSpec(FieldKeys.MEID, R.string.detail_item_meid, FieldCategory.IDENTITY, FieldInputType.Text),
        FieldSpec(FieldKeys.IMSI, R.string.detail_item_imsi, FieldCategory.IDENTITY, FieldInputType.Text),

        // 运营商与网络
        FieldSpec(FieldKeys.PHONE_NUMBER, R.string.detail_item_phone_number, FieldCategory.TELEPHONY, FieldInputType.Text),
        FieldSpec(FieldKeys.NETWORK_COUNTRY, R.string.detail_item_network_country, FieldCategory.TELEPHONY, FieldInputType.Text),
        FieldSpec(FieldKeys.NETWORK_OPERATOR, R.string.detail_item_network_operator, FieldCategory.TELEPHONY, FieldInputType.Text),
        FieldSpec(FieldKeys.NETWORK_OPERATOR_NAME, R.string.detail_item_network_operator_name, FieldCategory.TELEPHONY, FieldInputType.Text),
        FieldSpec(FieldKeys.ICCID, R.string.detail_item_network_iccid, FieldCategory.TELEPHONY, FieldInputType.Text),
        FieldSpec(FieldKeys.SIM_COUNTRY, R.string.detail_item_sim_country, FieldCategory.TELEPHONY, FieldInputType.Text),
        FieldSpec(FieldKeys.WIFI_BSSID, R.string.detail_item_wifi_bssid, FieldCategory.TELEPHONY, FieldInputType.Text),
        FieldSpec(FieldKeys.WIFI_SSID, R.string.detail_item_wifi_ssid, FieldCategory.TELEPHONY, FieldInputType.Text),
        FieldSpec(FieldKeys.WIFI_MAC, R.string.detail_item_wifi_mac, FieldCategory.TELEPHONY, FieldInputType.Text),

        // 系统版本
        FieldSpec(FieldKeys.ANDROID_VERSION, R.string.detail_item_android_version, FieldCategory.SYSTEM, FieldInputType.Text),
        FieldSpec(FieldKeys.SDK_INT, R.string.detail_item_sdk_int, FieldCategory.SYSTEM, FieldInputType.Number()),
        FieldSpec(FieldKeys.LOCALE, R.string.detail_item_locale, FieldCategory.SYSTEM, FieldInputType.Text),
        FieldSpec(FieldKeys.TIMEZONE, R.string.detail_item_timezone, FieldCategory.SYSTEM, FieldInputType.Text),
        FieldSpec(FieldKeys.MODEL, R.string.detail_item_model, FieldCategory.SYSTEM, FieldInputType.Text),
        FieldSpec(FieldKeys.DEVICE, R.string.detail_item_device, FieldCategory.SYSTEM, FieldInputType.Text),
        FieldSpec(FieldKeys.BRAND, R.string.detail_item_brand, FieldCategory.SYSTEM, FieldInputType.Text),
        FieldSpec(FieldKeys.MANUFACTURER, R.string.detail_item_manufacturer, FieldCategory.SYSTEM, FieldInputType.Text),
        FieldSpec(FieldKeys.PRODUCT, R.string.detail_item_product, FieldCategory.SYSTEM, FieldInputType.Text),
        FieldSpec(FieldKeys.HARDWARE, R.string.detail_item_hardware, FieldCategory.SYSTEM, FieldInputType.Text),
        FieldSpec(FieldKeys.FINGERPRINT, R.string.detail_item_fingerprint, FieldCategory.SYSTEM, FieldInputType.Text),
        FieldSpec(FieldKeys.INCREMENTAL, R.string.detail_item_incremental, FieldCategory.SYSTEM, FieldInputType.Text),
        FieldSpec(FieldKeys.KERNEL, R.string.detail_item_kernel, FieldCategory.SYSTEM, FieldInputType.Text),

        // 硬件规格
        FieldSpec(FieldKeys.RAM, R.string.detail_item_ram, FieldCategory.HARDWARE, FieldInputType.Number(decimal = true, units = Units.MEMORY)),
        FieldSpec(FieldKeys.CPU_CORES, R.string.detail_item_cpu_cores, FieldCategory.HARDWARE, FieldInputType.Number()),
        FieldSpec(FieldKeys.STORAGE, R.string.detail_item_storage, FieldCategory.HARDWARE, FieldInputType.Number(decimal = true, units = Units.STORAGE)),
        FieldSpec(FieldKeys.STORAGE_USED_PERCENT, R.string.detail_item_storage_used_percent, FieldCategory.HARDWARE, FieldInputType.Number()),
        FieldSpec(FieldKeys.MAX_REFRESH_RATE, R.string.detail_item_max_refresh_rate, FieldCategory.HARDWARE, FieldInputType.Number(decimal = true, units = Units.REFRESH)),

        // 电池
        FieldSpec(FieldKeys.BATTERY, R.string.detail_item_battery, FieldCategory.BATTERY, FieldInputType.Number(units = Units.CAPACITY)),
        FieldSpec(FieldKeys.BATTERY_STATUS, R.string.detail_item_battery_status, FieldCategory.BATTERY, FieldInputType.Enum(BatteryStatus.options())),
        FieldSpec(FieldKeys.BATTERY_PLUGGED, R.string.detail_item_battery_plugged, FieldCategory.BATTERY, FieldInputType.Enum(BatteryPlugged.options())),
        FieldSpec(FieldKeys.BATTERY_VOLTAGE, R.string.detail_item_battery_voltage, FieldCategory.BATTERY, FieldInputType.Number(units = Units.VOLTAGE)),
        FieldSpec(FieldKeys.BATTERY_TEMPERATURE, R.string.detail_item_battery_temp, FieldCategory.BATTERY, FieldInputType.Number()),
        FieldSpec(FieldKeys.BATTERY_HEALTH, R.string.detail_item_battery_health, FieldCategory.BATTERY, FieldInputType.Enum(BatteryHealth.options())),
        FieldSpec(FieldKeys.BATTERY_LEVEL, R.string.detail_item_battery_level, FieldCategory.BATTERY, FieldInputType.Number()),
        FieldSpec(FieldKeys.BATTERY_CURRENT, R.string.detail_item_battery_current, FieldCategory.BATTERY, FieldInputType.Number(units = Units.CURRENT)),
        FieldSpec(FieldKeys.BATTERY_TECHNOLOGY, R.string.detail_item_battery_tech, FieldCategory.BATTERY, FieldInputType.Text),

        // 时间
        FieldSpec(FieldKeys.ACTIVATION_TIME, R.string.detail_item_activation_time, FieldCategory.TIME, FieldInputType.Number()),
        FieldSpec(FieldKeys.BOOT_TIME, R.string.detail_item_boot_time, FieldCategory.TIME, FieldInputType.Number()),
        FieldSpec(FieldKeys.IS_24H, R.string.detail_item_is_24h, FieldCategory.TIME, FieldInputType.Bool),

        // Google 服务
        FieldSpec(FieldKeys.GOOGLE_AD_ID, R.string.detail_item_google_ad_id, FieldCategory.GOOGLE, FieldInputType.Text),
        FieldSpec(FieldKeys.GMS_VERSION, R.string.detail_item_gms_version, FieldCategory.GOOGLE, FieldInputType.Text),
        FieldSpec(FieldKeys.PLAY_STORE_VERSION, R.string.detail_item_play_store_version, FieldCategory.GOOGLE, FieldInputType.Text),

        // DRM
        FieldSpec(FieldKeys.DRM_ID, R.string.detail_item_drm_id, FieldCategory.DRM, FieldInputType.Text),
        FieldSpec(FieldKeys.DRM_SECURITY_LEVEL, R.string.detail_item_drm_security_level, FieldCategory.DRM, FieldInputType.Enum(DrmSecurityLevel.options())),

        // 渲染与传感器
        FieldSpec(FieldKeys.OPENGL_VERSION, R.string.detail_item_opengl_version, FieldCategory.RENDER, FieldInputType.Text),
        FieldSpec(FieldKeys.GL_RENDERER, R.string.detail_item_gl_renderer, FieldCategory.RENDER, FieldInputType.Text),
        FieldSpec(FieldKeys.GL_VENDOR, R.string.detail_item_gl_vendor, FieldCategory.RENDER, FieldInputType.Text),
        FieldSpec(FieldKeys.SENSOR_VENDOR, R.string.detail_item_sensor_vendor, FieldCategory.RENDER, FieldInputType.Text),

        // 无障碍
        FieldSpec(FieldKeys.ACCESSIBILITY_SERVICE_COUNT, R.string.detail_item_accessibility_service_count, FieldCategory.ACCESSIBILITY, FieldInputType.Number()),
        FieldSpec(FieldKeys.ACCESSIBILITY_ENABLED, R.string.detail_item_accessibility_enabled, FieldCategory.ACCESSIBILITY, boolInput),
        FieldSpec(FieldKeys.TALKBACK_ENABLED, R.string.detail_item_talkback_enabled, FieldCategory.ACCESSIBILITY, boolInput),
        FieldSpec(FieldKeys.HIGH_CONTRAST_TEXT, R.string.detail_item_high_contrast_text, FieldCategory.ACCESSIBILITY, boolInput),
        FieldSpec(FieldKeys.COLOR_INVERSION, R.string.detail_item_color_inversion, FieldCategory.ACCESSIBILITY, boolInput),
        FieldSpec(FieldKeys.DALTONIZER_ENABLED, R.string.detail_item_daltonizer_enabled, FieldCategory.ACCESSIBILITY, boolInput),
        FieldSpec(FieldKeys.CAPTION_ENABLED, R.string.detail_item_caption_enabled, FieldCategory.ACCESSIBILITY, boolInput),
        FieldSpec(FieldKeys.MONO_AUDIO, R.string.detail_item_mono_audio, FieldCategory.ACCESSIBILITY, boolInput),
        FieldSpec(FieldKeys.POWER_ENDS_CALL, R.string.detail_item_power_ends_call, FieldCategory.ACCESSIBILITY, boolInput),
    )

    val byKey: Map<String, FieldSpec> = specs.associateBy { it.key }

    val grouped: List<Pair<FieldCategory, List<FieldSpec>>> =
        specs.groupBy { it.category }
            .toList()
            .sortedBy { (cat, _) -> cat.ordinal }
}
