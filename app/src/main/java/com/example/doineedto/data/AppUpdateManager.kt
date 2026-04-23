package com.example.doineedto.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(
    val versionName: String,
    val tagName: String,
    val apkName: String,
    val apkDownloadUrl: String,
    val htmlUrl: String,
)

sealed interface UpdateCheckResult {
    data class Available(val update: AppUpdate) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
}

class AppUpdateManager(private val context: Context) {
    fun checkForUpdate(): UpdateCheckResult {
        val release = fetchLatestRelease()
        val releaseVersion = release.tagName.removePrefix("v")

        return if (isNewerVersion(releaseVersion, currentVersionName())) {
            UpdateCheckResult.Available(release.copy(versionName = releaseVersion))
        } else {
            UpdateCheckResult.UpToDate
        }
    }

    fun downloadUpdateApk(update: AppUpdate): File = downloadApk(update)

    fun promptInstall(apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )

        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }

    fun openUnknownAppSourcesSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    fun canRequestPackageInstalls(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
    }

    private fun fetchLatestRelease(): AppUpdate {
        val json = httpGet("$GITHUB_API_BASE/releases/latest")
        val release = JSONObject(json)
        val assets = release.getJSONArray("assets")

        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            val name = asset.getString("name")
            if (name.endsWith(".apk", ignoreCase = true)) {
                return AppUpdate(
                    versionName = release.getString("tag_name").removePrefix("v"),
                    tagName = release.getString("tag_name"),
                    apkName = name,
                    apkDownloadUrl = asset.getString("browser_download_url"),
                    htmlUrl = release.getString("html_url"),
                )
            }
        }

        error("Latest GitHub release has no APK asset.")
    }

    private fun downloadApk(update: AppUpdate): File {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(updatesDir, update.apkName)

        val connection = openConnection(update.apkDownloadUrl)
        connection.inputStream.use { input ->
            apkFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        connection.disconnect()

        return apkFile
    }

    private fun httpGet(url: String): String {
        val connection = openConnection(url)
        return connection.inputStream.bufferedReader().use { it.readText() }.also {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "Dint/${currentVersionName()}")
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val message = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()
            error("GitHub request failed: $responseCode $message")
        }

        return connection
    }

    private fun currentVersionName(): String {
        return context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
    }

    companion object {
        private const val GITHUB_API_BASE = "https://api.github.com/repos/OncePunchedMan/DINT"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}

private fun isNewerVersion(candidate: String, current: String): Boolean {
    val candidateParts = candidate.versionParts()
    val currentParts = current.versionParts()
    val maxSize = maxOf(candidateParts.size, currentParts.size)

    for (index in 0 until maxSize) {
        val candidatePart = candidateParts.getOrElse(index) { 0 }
        val currentPart = currentParts.getOrElse(index) { 0 }

        if (candidatePart > currentPart) return true
        if (candidatePart < currentPart) return false
    }

    return false
}

private fun String.versionParts(): List<Int> =
    split(".", "-", "_")
        .mapNotNull { part -> part.takeWhile(Char::isDigit).toIntOrNull() }
