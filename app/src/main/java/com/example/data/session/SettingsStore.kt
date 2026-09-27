package com.example.data.session

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppAppearanceSettings
import com.example.data.model.AppThemeMode
import com.example.data.model.VoucherFontScale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    private val _appearance = MutableStateFlow(loadAppearance())
    val appearance: StateFlow<AppAppearanceSettings> = _appearance.asStateFlow()

    private fun loadAppearance(): AppAppearanceSettings {
        val themeModeName = prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        val fontScaleName = prefs.getString("font_scale", VoucherFontScale.STANDARD.name) ?: VoucherFontScale.STANDARD.name
        val neumorphism = prefs.getBoolean("neumorphism", false)
        val colorHex = prefs.getString("primary_color", "#0066FF") ?: "#0066FF"

        val themeMode = try { AppThemeMode.valueOf(themeModeName) } catch (_: Exception) { AppThemeMode.SYSTEM }
        val fontScale = try { VoucherFontScale.valueOf(fontScaleName) } catch (_: Exception) { VoucherFontScale.STANDARD }

        return AppAppearanceSettings(
            themeMode = themeMode,
            fontScale = fontScale,
            enableNeumorphism = neumorphism,
            primaryColorHex = colorHex
        )
    }

    fun updateAppearance(newSettings: AppAppearanceSettings) {
        prefs.edit()
            .putString("theme_mode", newSettings.themeMode.name)
            .putString("font_scale", newSettings.fontScale.name)
            .putBoolean("neumorphism", newSettings.enableNeumorphism)
            .putString("primary_color", newSettings.primaryColorHex)
            .apply()
        _appearance.value = newSettings
    }
}
