package com.example.swartlandathletics.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object WebDashboardConfig {
    private const val PREFS_NAME = "swartland_web_config"
    private const val KEY_CUSTOM_URL = "custom_web_url"
    
    // Default GitHub Pages URL for the imported repository
    const val DEFAULT_WEB_URL = "https://konradtoerien.github.io/swartland-athletics-app/"
    const val GITHUB_SETTINGS_PAGES_URL = "https://github.com/konradtoerien/swartland-athletics-app/settings/pages"

    fun getWebUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_URL, DEFAULT_WEB_URL) ?: DEFAULT_WEB_URL
    }

    fun setWebUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_URL, url.trim()).apply()
    }

    fun getShareableMessage(context: Context, language: AppLanguage): String {
        val isEnglish = language == AppLanguage.ENGLISH
        val url = getWebUrl(context)
        return if (isEnglish) {
            """
            🏃 *LAERSKOOL SWARTLAND - LIVE ATHLETICS MEET* 🏃
            
            Follow all the action, medals, team points and live results in real time from your phone or PC (No app installation needed):
            
            🌐 *Live Dashboard Link:*
            $url
            
            ⏱️ Results are updated live by the track & field officials!
            """.trimIndent()
        } else {
            """
            🏃 *LAERSKOOL SWARTLAND - INTYDSE ATLETIEK BYEENKOMS* 🏃
            
            Volg al die aksie, medaljes, skole punte en regstreekse uitslae intyds vanaf jou foon of rekenaar (Geen toepassing nodig om te installeer nie):
            
            🌐 *Lewendige Skakel vir Ouers & Toeskouers:*
            $url
            
            ⏱️ Uitslae word intyds bygewerk soos die baan- en veld-beoordelaars dit voltooi!
            """.trimIndent()
        }
    }

    fun shareToWhatsApp(context: Context, language: AppLanguage) {
        val message = getShareableMessage(context, language)
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                },
                if (language == AppLanguage.ENGLISH) "Share Live Web Link" else "Deel Lewendige Web-Skakel"
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    fun openInBrowser(context: Context) {
        try {
            val url = getWebUrl(context)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun openGitHubPagesSettings(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_SETTINGS_PAGES_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }
}
