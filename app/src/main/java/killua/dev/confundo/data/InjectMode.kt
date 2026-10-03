package killua.dev.confundo.data

import androidx.annotation.StringRes
import killua.dev.confundo.R
import killua.dev.confundo.ui.pages.home.FieldKeys

enum class InjectMode(
    val storage: String,
    @StringRes val labelRes: Int,
    @StringRes val summaryRes: Int,
) {
    IN_APP("in_app", R.string.inject_mode_in_app, R.string.inject_mode_in_app_summary),
    SYSTEM_SERVER(
        "system_server",
        R.string.inject_mode_system_server,
        R.string.inject_mode_system_server_summary,
    );

    companion object {
        fun fromStorage(value: String?): InjectMode =
            entries.firstOrNull { it.storage == value } ?: IN_APP
    }
}

/**
 * system_server can only provide per-calling-app values for APIs whose Binder
 * endpoint lives in system_server. Local framework APIs (Build, DRM, OpenGL,
 * sensors, libc, etc.) must remain in [InjectMode.IN_APP].
 */
object SystemServerFields {
    val keys: Set<String> = setOf(
        FieldKeys.DEVICE_ID,
        FieldKeys.ANDROID_ID,
        FieldKeys.IMEI,
        FieldKeys.MEID,
        FieldKeys.PHONE_NUMBER,
        FieldKeys.ICCID,
        FieldKeys.NETWORK_COUNTRY,
        FieldKeys.WIFI_BSSID,
        FieldKeys.WIFI_SSID,
        FieldKeys.WIFI_MAC,
        FieldKeys.HIDE_VPN,
        FieldKeys.ACTIVATION_TIME,
        FieldKeys.GMS_VERSION,
        FieldKeys.PLAY_STORE_VERSION,
    )

    fun supports(key: String): Boolean = key in keys
}
