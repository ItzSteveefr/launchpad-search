package com.devrinth.launchpad

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.devrinth.launchpad.utils.ThemeManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ThemeManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        mockSharedPreferences = mock(SharedPreferences::class.java)
        `when`(PreferenceManager.getDefaultSharedPreferences(mockContext)).thenReturn(mockSharedPreferences)
    }

    @Test
    fun `applyThemeFromPreferences applies system theme by default`() {
        `when`(mockSharedPreferences.getString("setting_theme", "system")).thenReturn("system")
        ThemeManager.applyThemeFromPreferences(mockContext)
        verify(AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))
    }

    @Test
    fun `applyThemeFromPreferences applies light theme`() {
        `when`(mockSharedPreferences.getString("setting_theme", "system")).thenReturn("light")
        ThemeManager.applyThemeFromPreferences(mockContext)
        verify(AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO))
    }

    @Test
    fun `applyThemeFromPreferences applies dark theme`() {
        `when`(mockSharedPreferences.getString("setting_theme", "system")).thenReturn("dark")
        ThemeManager.applyThemeFromPreferences(mockContext)
        verify(AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES))
    }
}
