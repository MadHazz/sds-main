package com.DevCiplak.advdisplay.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.DevCiplak.advdisplay.model.PlaybackMode
import com.DevCiplak.advdisplay.util.IdentifierCode
import java.util.UUID

data class SessionSnapshot(
    val code: String?,
    val deviceId: String?,
    val menuType: String?
) {
    fun isValid(): Boolean = !deviceId.isNullOrBlank() && PlaybackMode.fromSession(menuType) != null &&
        runCatching { IdentifierCode.normalize(code.orEmpty()) == code }.getOrDefault(false)
}

class SessionStore private constructor(
    private val sharedPreferences: SharedPreferences
) {

    fun getCode(): String? = sharedPreferences.getString(KEY_CODES, null)

    fun getDeviceId(): String? = sharedPreferences.getString(KEY_DEVICE_ID, null)

    fun getMenuType(): String? = sharedPreferences.getString(KEY_MENU_TYPE, null)

    fun getSession(): SessionSnapshot {
        return SessionSnapshot(
            code = getCode(),
            deviceId = getDeviceId(),
            menuType = getMenuType()
        )
    }

    fun saveSession(code: String, deviceId: String, menuType: String) {
        sharedPreferences.edit().apply {
            putString(KEY_CODES, code)
            putString(KEY_DEVICE_ID, deviceId)
            putString(KEY_MENU_TYPE, menuType)
            apply()
        }
    }

    fun getOrCreateDeviceId(androidId: String?): String {
        val existing = sharedPreferences.getString("installationDeviceId", null) ?: getDeviceId()
        val id = existing?.takeIf { it.isNotBlank() } ?: androidId?.takeIf { it.isNotBlank() }
            ?.let { UUID.nameUUIDFromBytes(it.toByteArray()).toString() } ?: UUID.randomUUID().toString()
        sharedPreferences.edit { putString("installationDeviceId", id) }
        return id
    }

    fun clearSession() {
        sharedPreferences.edit().apply {
            remove(KEY_CODES)
            remove(KEY_DEVICE_ID)
            remove(KEY_MENU_TYPE)
            apply()
        }
    }

    companion object {
        private const val PREF_NAME = "advForward"
        private const val KEY_CODES = "codes"
        private const val KEY_DEVICE_ID = "deviceId"
        private const val KEY_MENU_TYPE = "menuType"

        operator fun invoke(context: Context): SessionStore {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return SessionStore(prefs)
        }
    }
}
