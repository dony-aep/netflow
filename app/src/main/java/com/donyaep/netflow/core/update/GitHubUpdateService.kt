package com.donyaep.netflow.core.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class UpdateCheckStatus {
    UpToDate,
    UpdateAvailable,
    Error,
}

data class ReleaseInfo(
    val version: String,
    val tagName: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val apkSizeBytes: Long?,
    val publishedAt: Instant?,
) {
    val apkSizeFormatted: String
        get() {
            val bytes = apkSizeBytes ?: return ""
            return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        }

    val publishedAtFormatted: String
        get() {
            val instant = publishedAt ?: return ""
            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("es-ES"))
            return formatter.format(instant.atZone(ZoneId.systemDefault()))
        }
}

data class UpdateResult(
    val status: UpdateCheckStatus,
    val releaseInfo: ReleaseInfo? = null,
    val message: String? = null,
)

object GitHubUpdateService {
    private const val OWNER = "dony-aep"
    private const val REPO = "netflow"
    private const val API_URL = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"
    const val RELEASES_URL = "https://github.com/$OWNER/$REPO/releases/latest"

    suspend fun checkForUpdates(currentVersion: String): UpdateResult = withContext(Dispatchers.IO) {
        val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/vnd.github.v3+json")
            setRequestProperty("User-Agent", "NetFlow-Android")
        }

        try {
            when (val responseCode = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val releaseInfo = parseRelease(body)
                    val hasUpdate = compareVersions(currentVersion, releaseInfo.version) < 0
                    UpdateResult(
                        status = if (hasUpdate) UpdateCheckStatus.UpdateAvailable else UpdateCheckStatus.UpToDate,
                        releaseInfo = releaseInfo,
                    )
                }

                HttpURLConnection.HTTP_NOT_FOUND -> {
                    UpdateResult(
                        status = UpdateCheckStatus.UpToDate,
                        message = "No hay releases publicadas todavía.",
                    )
                }

                else -> {
                    UpdateResult(
                        status = UpdateCheckStatus.Error,
                        message = "Error del servidor: $responseCode",
                    )
                }
            }
        } catch (_: IOException) {
            UpdateResult(
                status = UpdateCheckStatus.Error,
                message = "Sin conexión o error de red al consultar GitHub.",
            )
        } catch (exception: Exception) {
            UpdateResult(
                status = UpdateCheckStatus.Error,
                message = exception.message ?: "No se pudo verificar actualizaciones.",
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRelease(body: String): ReleaseInfo {
        val data = JSONObject(body)
        val assets = data.optJSONArray("assets")
        var apkUrl: String? = null
        var apkSize: Long? = null

        if (assets != null) {
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                val name = asset.optString("name")
                if (name.endsWith(".apk")) {
                    apkUrl = asset.optString("browser_download_url")
                    apkSize = asset.optLong("size")
                    break
                }
            }
        }

        val publishedAt = data.optString("published_at").takeIf { it.isNotBlank() }?.let(Instant::parse)

        return ReleaseInfo(
            version = data.optString("tag_name").removePrefix("v").trim(),
            tagName = data.optString("tag_name"),
            releaseNotes = data.optString("body"),
            htmlUrl = data.optString("html_url"),
            apkDownloadUrl = apkUrl,
            apkSizeBytes = apkSize,
            publishedAt = publishedAt,
        )
    }

    private fun compareVersions(first: String, second: String): Int {
        val firstParts = first.split('.').map { it.toIntOrNull() ?: 0 }.toMutableList()
        val secondParts = second.split('.').map { it.toIntOrNull() ?: 0 }.toMutableList()

        while (firstParts.size < 3) firstParts.add(0)
        while (secondParts.size < 3) secondParts.add(0)

        for (index in 0 until 3) {
            if (firstParts[index] < secondParts[index]) return -1
            if (firstParts[index] > secondParts[index]) return 1
        }
        return 0
    }
}