package com.rihlahidali.advdisplay.analytics

import android.content.Context
import androidx.core.content.edit

object GrowthTracker {

    private const val PREF_NAME = "growthTelemetry"
    private const val KEY_PREFIX_COUNT = "count_"
    private const val KEY_PREFIX_LAST_SEEN = "last_seen_"

    fun trackEvent(context: Context, eventName: String) {
        val safeName = eventName.trim().lowercase()
        if (safeName.isBlank()) {
            return
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val countKey = KEY_PREFIX_COUNT + safeName
        val lastSeenKey = KEY_PREFIX_LAST_SEEN + safeName
        val currentCount = prefs.getInt(countKey, 0)

        prefs.edit {
            putInt(countKey, currentCount + 1)
            putLong(lastSeenKey, System.currentTimeMillis())
        }
    }
}
