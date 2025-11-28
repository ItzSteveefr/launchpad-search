package com.devrinth.launchpad

import android.content.Context
import com.devrinth.launchpad.search.plugins.AppsPlugin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class AppsPluginTest {

    private lateinit var appsPlugin: AppsPlugin

    @Before
    fun setup() {
        val mockContext = mock(Context::class.java)
        appsPlugin = AppsPlugin(mockContext)
        appsPlugin.appList = listOf(
            AppsPlugin.AppInfo("Google Chrome", "com.android.chrome"),
            AppsPlugin.AppInfo("Gmail", "com.google.android.gm"),
            AppsPlugin.AppInfo("Google Maps", "com.google.android.apps.maps"),
            AppsPlugin.AppInfo("Discord", "com.discord")
        )
        appsPlugin.customKeywords["com.discord"] = "chat"
    }

    @Test
    fun `test custom keyword search`() = runBlocking {
        val results = appsPlugin.filterApps("chat")
        assertEquals(1, results.size)
        assertEquals("Discord", results[0].value)
    }

    @Test
    fun `test prefix search`() = runBlocking {
        val results = appsPlugin.filterApps("goo")
        assertEquals(2, results.size)
        assertEquals("Google Chrome", results[0].value)
        assertEquals("Google Maps", results[1].value)
    }

    @Test
    fun `test word prefix search`() = runBlocking {
        val results = appsPlugin.filterApps("chr")
        assertEquals(1, results.size)
        assertEquals("Google Chrome", results[0].value)
    }

    @Test
    fun `test fuzzy search`() = runBlocking {
        val results = appsPlugin.filterApps("gmaps")
        assertEquals(1, results.size)
        assertEquals("Google Maps", results[0].value)
    }

    @Test
    fun `test search with no results`() = runBlocking {
        val results = appsPlugin.filterApps("nonexistent")
        assertEquals(0, results.size)
    }
}
