package com.rihlahidali.advdisplay.data

import com.rihlahidali.advdisplay.model.MediaItem
import com.rihlahidali.advdisplay.model.MediaPage
import com.rihlahidali.advdisplay.model.PlaybackMode
import com.rihlahidali.advdisplay.model.Playlist
import com.google.gson.Gson
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import java.security.MessageDigest

fun interface MediaDownloader {
    suspend fun download(url: String, destination: File)
}

class PlaylistCache(val directory: File, private val downloader: MediaDownloader) {
    private val gson = Gson()
    private val manifest = File(directory, "playlist.json")

    fun fileFor(item: MediaItem): File {
        if (item.url.startsWith("legacy:")) {
            val name = item.url.removePrefix("legacy:")
            require(name == File(name).name && name.endsWith(".mp4", ignoreCase = true))
            return File(directory, name)
        }
        val hash = MessageDigest.getInstance("SHA-256").digest(item.url.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$hash.media")
    }

    fun load(mode: PlaybackMode): Playlist? {
        if (manifest.exists()) {
            return runCatching {
                gson.fromJson(manifest.readText(), Playlist::class.java).also { playlist ->
                    require(playlist.pages.isNotEmpty() && playlist.refreshSeconds in 1..86400)
                    playlist.pages.forEach { page ->
                        require(if (mode == PlaybackMode.VIDEO) page.templateId == "4" else page.templateId in setOf("2", "3"))
                        require(page.media.isNotEmpty())
                        page.media.forEach { require(isComplete(fileFor(it))) }
                    }
                }
            }.getOrNull()
        }
        // Preserve videos downloaded by the previous version on the first upgraded launch.
        if (mode != PlaybackMode.VIDEO) return null
        val legacy = directory.listFiles().orEmpty()
            .filter { it.extension.equals("mp4", true) && isComplete(it) }.sortedBy { it.name }
            .map { MediaPage("4", listOf(MediaItem("legacy:${it.name}"))) }
        return legacy.takeIf { it.isNotEmpty() }?.let { Playlist(it) }
    }

    suspend fun replace(playlist: Playlist): Playlist {
        check(playlist.pages.isNotEmpty()) { "No media is assigned to this screen." }
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create media storage.")
        val filesAdded = mutableListOf<File>()
        try {
            for (item in playlist.pages.flatMap { it.media }.distinctBy { it.url }) {
                currentCoroutineContext().ensureActive()
                val destination = fileFor(item)
                if (isComplete(destination)) continue
                val temporary = File(directory, "${destination.name}.part")
                try {
                    downloader.download(item.url, temporary)
                    currentCoroutineContext().ensureActive()
                    check(isComplete(temporary)) { "The server returned an empty media file." }
                    if (!temporary.renameTo(destination)) throw IOException("Cannot save downloaded media.")
                    filesAdded.add(destination)
                } finally {
                    temporary.delete()
                }
            }
            currentCoroutineContext().ensureActive()
            val pending = File(directory, "playlist.json.part")
            try {
                pending.outputStream().use { stream ->
                    stream.write(gson.toJson(playlist).toByteArray())
                    stream.fd.sync()
                }
                if (!pending.renameTo(manifest)) throw IOException("Cannot save the playlist.")
            } finally {
                pending.delete()
            }
        } catch (error: Exception) {
            filesAdded.forEach { it.delete() }
            throw error
        }
        // Only a fully downloaded and committed playlist can authorize removing old content.
        val keep = playlist.pages.flatMap { it.media }.map { fileFor(it).name }.toSet() + manifest.name
        directory.listFiles().orEmpty().filter {
            it.isFile && it.name !in keep && (it.extension == "media" || it.extension.equals("mp4", true))
        }.forEach { it.delete() }
        return playlist
    }

    private fun isComplete(file: File) = file.isFile && file.length() > 0
}
