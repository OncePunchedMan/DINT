package com.example.doineedto.data

import android.content.Context
import android.content.Intent
import android.os.Build
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

    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
    } else {
        null
    }

    val resolvedActivities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(launcherIntent, flags!!)
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
    }

    return resolvedActivities
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

private fun launchIntentForPackage(context: Context, packageName: String): Intent? {
    val packageManager = context.packageManager

    packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val fallbackIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        `package` = packageName
    }

    val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.resolveActivity(
            fallbackIntent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.resolveActivity(fallbackIntent, PackageManager.MATCH_DEFAULT_ONLY)
    } ?: return null

    val activityInfo = resolveInfo.activityInfo ?: return null
    return Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        setClassName(activityInfo.packageName, activityInfo.name)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
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
        "open gmail" to "com.google.android.gm",
        "check gmail" to "com.google.android.gm",
        "open outlook" to "com.microsoft.office.outlook",
        "check outlook" to "com.microsoft.office.outlook",
        "outlook" to "com.microsoft.office.outlook",
        "open protonmail" to "ch.protonmail.android",
        "check protonmail" to "ch.protonmail.android",
        "open spotify" to "com.spotify.music",
        "play spotify" to "com.spotify.music",
        "spotify" to "com.spotify.music",
        "listen to music" to "com.spotify.music",
        "play music" to "com.spotify.music",
        "open podcasts" to "com.spotify.music",
        "listen to podcasts" to "com.spotify.music",
        "open amazon" to "com.amazon.mShop.android.shopping",
        "shop on amazon" to "com.amazon.mShop.android.shopping",
        "open amazon shopping" to "com.amazon.mShop.android.shopping",
    )

    explicitPackages[normalizedReason]?.let { packageName ->
        return launchIntentForPackage(context, packageName)
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
    return launchIntentForPackage(context, mappedPackage)
}

// TODO: Showing truly most-frequent apps as presets would need usage-based ranking or prediction logic,
// which is heavier than the current curated-preset plus manual-mapping approach.
