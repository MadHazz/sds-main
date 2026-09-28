package com.rihlahidali.advdisplay.model

data class MediaItem(val url: String, val slot: String? = null)
data class MediaPage(val templateId: String, val media: List<MediaItem>)
data class Playlist(val pages: List<MediaPage>, val refreshSeconds: Int = 5)

enum class PlaybackMode(val sessionValue: String) {
    WEB("0"), SLIDER("1"), VIDEO("4");

    companion object {
        fun fromSession(value: String?): PlaybackMode? = values().find { it.sessionValue == value }
    }
}
