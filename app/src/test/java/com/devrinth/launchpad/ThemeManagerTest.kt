package com.devrinth.launchpad

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.devrinth.launchpad.utils.ThemeManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ThemeManagerTest {

    private lateinit var context: Context
    private lateinit var mockSharedPreferences: SharedPreferences

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        mockSharedPreferences = mock(SharedPreferences::class.java)
        `when`(PreferenceManager.getDefaultSharedPreferences(context)).thenReturn(mockSharedPreferences)
    }

    @Test
    fun `applyThemeFromPreferences applies system theme by default`() {
        `when`(mockSharedPreferences.getString("setting_theme", "system")).thenReturn("system")
        ThemeManager.applyThemeFromPreferences(context)
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun `applyThemeFromPreferences applies light theme`() {
        `when`(mockSharedPreferences.getString("setting_theme", "system")).thenReturn("light")
        ThemeManager.applyThemeFromPreferences(context)
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun `applyThemeFromPreferences applies dark theme`() {
        `when`(mockSharedPreferences.getString("setting_theme", "system")).thenReturn("dark")
        ThemeManager.applyThemeFromPreferences(context)
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
    }
}
