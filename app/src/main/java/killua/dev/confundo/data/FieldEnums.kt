package killua.dev.confundo.data

import android.os.BatteryManager

/**
 * 电池运行状态。
 */
enum class BatteryStatus(val storage: String, val androidValue: Int) {
    CHARGING("charging", BatteryManager.BATTERY_STATUS_CHARGING),
    DISCHARGING("discharging", BatteryManager.BATTERY_STATUS_DISCHARGING),
    FULL("full", BatteryManager.BATTERY_STATUS_FULL),
    NOT_CHARGING("not_charging", BatteryManager.BATTERY_STATUS_NOT_CHARGING);

    companion object {
        fun options() = entries.map { it.storage }
        fun from(value: String?): BatteryStatus? =
            entries.firstOrNull { it.storage.equals(value, ignoreCase = true) }
    }
}

/** 电源连接类型。 */
enum class BatteryPlugged(val storage: String, val androidValue: Int) {
    AC("ac", BatteryManager.BATTERY_PLUGGED_AC),
    USB("usb", BatteryManager.BATTERY_PLUGGED_USB),
    WIRELESS("wireless", BatteryManager.BATTERY_PLUGGED_WIRELESS);

    companion object {
        fun options() = entries.map { it.storage }
        fun from(value: String?): BatteryPlugged? =
            entries.firstOrNull { it.storage.equals(value, ignoreCase = true) }
    }
}

/** 电池健康状态。 */
enum class BatteryHealth(val storage: String, val androidValue: Int) {
    GOOD("good", BatteryManager.BATTERY_HEALTH_GOOD),
    OVERHEAT("overheat", BatteryManager.BATTERY_HEALTH_OVERHEAT),
    DEAD("dead", BatteryManager.BATTERY_HEALTH_DEAD),
    OVER_VOLTAGE("over_voltage", BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE),
    COLD("cold", BatteryManager.BATTERY_HEALTH_COLD);

    companion object {
        fun options() = entries.map { it.storage }
        fun from(value: String?): BatteryHealth? =
            entries.firstOrNull { it.storage.equals(value, ignoreCase = true) }
    }
}

/**
 * Widevine DRM 安全级别。L1 = 硬件级安全（可播放高清 DRM 内容），L3 = 软件级。
 */
enum class DrmSecurityLevel(val storage: String) {
    L1("L1"),
    L3("L3");

    companion object {
        fun options() = entries.map { it.storage }
        fun from(value: String?): DrmSecurityLevel? =
            entries.firstOrNull { it.storage.equals(value, ignoreCase = true) }
    }
}

/**
 * 系统属性键。
 */
enum class SystemPropKey(val prop: String, val fieldKey: String) {
    TIMEZONE("persist.sys.timezone", killua.dev.confundo.ui.pages.home.FieldKeys.TIMEZONE),
    OPERATOR_NUMERIC("gsm.operator.numeric", killua.dev.confundo.ui.pages.home.FieldKeys.NETWORK_OPERATOR),
    OPERATOR_ISO("gsm.operator.iso-country", killua.dev.confundo.ui.pages.home.FieldKeys.NETWORK_COUNTRY),
    SIM_OPERATOR_ISO("gsm.sim.operator.iso-country", killua.dev.confundo.ui.pages.home.FieldKeys.SIM_COUNTRY),
    PRODUCT_MODEL("ro.product.model", killua.dev.confundo.ui.pages.home.FieldKeys.MODEL),
    PRODUCT_DEVICE("ro.product.device", killua.dev.confundo.ui.pages.home.FieldKeys.DEVICE),
    PRODUCT_BRAND("ro.product.brand", killua.dev.confundo.ui.pages.home.FieldKeys.BRAND),
    PRODUCT_MANUFACTURER("ro.product.manufacturer", killua.dev.confundo.ui.pages.home.FieldKeys.MANUFACTURER),
    PRODUCT_NAME("ro.product.name", killua.dev.confundo.ui.pages.home.FieldKeys.PRODUCT),
    HARDWARE("ro.hardware", killua.dev.confundo.ui.pages.home.FieldKeys.HARDWARE),
    FINGERPRINT("ro.build.fingerprint", killua.dev.confundo.ui.pages.home.FieldKeys.FINGERPRINT),
    VERSION_RELEASE("ro.build.version.release", killua.dev.confundo.ui.pages.home.FieldKeys.ANDROID_VERSION),
    VERSION_SDK("ro.build.version.sdk", killua.dev.confundo.ui.pages.home.FieldKeys.SDK_INT),
    SERIAL("ro.serialno", killua.dev.confundo.ui.pages.home.FieldKeys.SERIAL);

    companion object {
        private val byProp = entries.associateBy { it.prop }
        fun fieldKeyFor(prop: String): String? = byProp[prop]?.fieldKey
    }
}

/**
 * Settings 提供器键映射。namespace 区分 Secure/System/Global。
 */
enum class SettingsKey(
    val namespace: SettingsNamespace,
    val settingName: String,
    val fieldKey: String,
) {
    ANDROID_ID(SettingsNamespace.SECURE, "android_id", killua.dev.confundo.ui.pages.home.FieldKeys.ANDROID_ID),
    TOUCH_EXPLORATION(SettingsNamespace.SECURE, "touch_exploration_enabled", killua.dev.confundo.ui.pages.home.FieldKeys.TALKBACK_ENABLED),
    HIGH_CONTRAST(SettingsNamespace.SECURE, "high_text_contrast_enabled", killua.dev.confundo.ui.pages.home.FieldKeys.HIGH_CONTRAST_TEXT),
    COLOR_INVERSION(SettingsNamespace.SECURE, "accessibility_display_inversion_enabled", killua.dev.confundo.ui.pages.home.FieldKeys.COLOR_INVERSION),
    DALTONIZER(SettingsNamespace.SECURE, "accessibility_display_daltonizer_enabled", killua.dev.confundo.ui.pages.home.FieldKeys.DALTONIZER_ENABLED),
    ACCESSIBILITY_ENABLED(SettingsNamespace.SYSTEM, "accessibility_enabled", killua.dev.confundo.ui.pages.home.FieldKeys.ACCESSIBILITY_ENABLED),
    CAPTIONS(SettingsNamespace.GLOBAL, "captions_enabled", killua.dev.confundo.ui.pages.home.FieldKeys.CAPTION_ENABLED);

    companion object {
        fun forNamespace(ns: SettingsNamespace): Map<String, String> =
            entries.filter { it.namespace == ns }.associate { it.settingName to it.fieldKey }
    }
}

enum class SettingsNamespace(val className: String) {
    SECURE("android.provider.Settings\$Secure"),
    SYSTEM("android.provider.Settings\$System"),
    GLOBAL("android.provider.Settings\$Global"),
}
