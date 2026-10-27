package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import com.example.model.GameProfile
import com.example.model.InstalledGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameScanner(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    suspend fun getInstalledGames(existingProfiles: List<GameProfile>): List<InstalledGame> = withContext(Dispatchers.IO) {
        val profileMap = existingProfiles.associateBy { it.packageName }
        val gamesList = mutableListOf<InstalledGame>()

        try {
            val installedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            }

            for (appInfo in installedApps) {
                // Filter out non-launchable or system apps unless they have an existing game profile
                val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                val isLaunchable = launchIntent != null
                val hasProfile = profileMap.containsKey(appInfo.packageName)

                if (!isLaunchable && !hasProfile) continue
                if (appInfo.packageName == context.packageName) continue // Don't list ourselves

                val isGameCategory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appInfo.category == ApplicationInfo.CATEGORY_GAME
                } else {
                    @Suppress("DEPRECATION")
                    (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                }

                // Known popular game package name identifiers or user configured
                val isLikelyGame = isGameCategory || hasProfile || isRecognizedGame(appInfo.packageName)

                val label = try {
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    appInfo.packageName
                }

                gamesList.add(
                    InstalledGame(
                        packageName = appInfo.packageName,
                        appName = label,
                        isSystemGameCategory = isLikelyGame,
                        hasCustomProfile = hasProfile,
                        configuredProfile = profileMap[appInfo.packageName]
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("StretchX_Scanner", "Error scanning installed packages", e)
        }

        // Sort: games first, then alphabetically
        gamesList.sortedWith(
            compareByDescending<InstalledGame> { it.hasCustomProfile }
                .thenByDescending { it.isSystemGameCategory }
                .thenBy { it.appName.lowercase() }
        )
    }

    fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            null
        }
    }

    fun launchGame(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("StretchX_Scanner", "Failed to launch $packageName", e)
            false
        }
    }

    private fun isRecognizedGame(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower.contains("pubg") ||
                lower.contains("codm") ||
                lower.contains("callofduty") ||
                lower.contains("freefire") ||
                lower.contains("fortnite") ||
                lower.contains("roblox") ||
                lower.contains("minecraft") ||
                lower.contains("genshin") ||
                lower.contains("brawlstars") ||
                lower.contains("clash") ||
                lower.contains("apex") ||
                lower.contains("arena") ||
                lower.contains("valorant") ||
                lower.contains("riot") ||
                lower.contains("game")
    }
}
