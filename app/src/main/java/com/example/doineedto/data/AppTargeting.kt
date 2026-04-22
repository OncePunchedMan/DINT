package com.example.doineedto.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class LaunchableApp(
    val label: String,
    val packageName: String,
)

data class AppTargetSelection(
    val packageName: String,
    val label: String,
)

enum class PresetTargetCategory(
    val key: String,
    val title: String,
    val presetLabel: String,
) {
    MEMES("memes", "Memes", "Look at memes"),
    SOCIAL("social", "Social catch-up", "Catch up on social media"),
    NEWS("news", "News", "Read the news"),
    VIDEO("video", "Video", "Watch a video"),
    MUSIC("music", "Music and podcasts", "Play music or a podcast"),
    SHOPPING("shopping", "Shopping", "Shop for something"),
    GAMES("games", "Games", "Play a game"),
}

fun queryLaunchableApps(context: Context): List<LaunchableApp> {
    val packageManager = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    return packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        .mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
            val packageName = activityInfo.packageName ?: return@mapNotNull null
            val label = resolveInfo.loadLabel(packageManager)?.toString()?.trim().orEmpty()
            if (label.isBlank()) return@mapNotNull null

            LaunchableApp(
                label = label,
                packageName = packageName,
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

fun launchIntentForReason(
    context: Context,
    reason: String,
    appTargetSelection: (PresetTargetCategory) -> AppTargetSelection?,
): Intent? {
    val normalizedReason = reason.trim().lowercase()
    if (normalizedReason.isBlank()) return null

    val explicitPackages = mapOf(
        "open instagram" to "com.instagram.android",
        "i want to check instagram" to "com.instagram.android",
        "open tiktok" to "com.zhiliaoapp.musically",
        "i want to check tiktok" to "com.zhiliaoapp.musically",
        "open youtube" to "com.google.android.youtube",
        "check reddit" to "com.reddit.frontpage",
        "open reddit" to "com.reddit.frontpage",
    )

    explicitPackages[normalizedReason]?.let { packageName ->
        return context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    val category = PresetTargetCategory.entries.firstOrNull { preset ->
        normalizedReason == preset.presetLabel.lowercase()
    } ?: when {
        "meme" in normalizedReason -> PresetTargetCategory.MEMES
        "social" in normalizedReason -> PresetTargetCategory.SOCIAL
        "news" in normalizedReason -> PresetTargetCategory.NEWS
        "video" in normalizedReason -> PresetTargetCategory.VIDEO
        "music" in normalizedReason || "podcast" in normalizedReason -> PresetTargetCategory.MUSIC
        "shop" in normalizedReason -> PresetTargetCategory.SHOPPING
        "game" in normalizedReason -> PresetTargetCategory.GAMES
        else -> null
    }

    val mappedPackage = category?.let(appTargetSelection)?.packageName ?: return null
    return context.packageManager.getLaunchIntentForPackage(mappedPackage)?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

// TODO: Showing truly most-frequent apps as presets would need usage-based ranking or prediction logic,
// which is heavier than the current curated-preset plus manual-mapping approach.
