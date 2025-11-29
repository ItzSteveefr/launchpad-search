package com.devrinth.launchpad

import android.content.Intent
import android.content.pm.PackageManager
import com.devrinth.launchpad.utils.IntentUtils
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class IntentUtilsTest {

    private lateinit var mockPackageManager: PackageManager

    @Before
    fun setup() {
        mockPackageManager = mock(PackageManager::class.java)
    }

    @Test
    fun `getAppIntent returns correct intent`() {
        val packageName = "com.test.app"
        val mockIntent = mock(Intent::class.java)
        `when`(mockPackageManager.getLaunchIntentForPackage(packageName)).thenReturn(mockIntent)

        val intent = IntentUtils.getAppIntent(mockPackageManager, packageName)
        assertEquals(mockIntent, intent)
    }

    @Test
    fun `getLinkIntent returns correct intent`() {
        val link = "https://www.google.com"
        val intent = IntentUtils.getLinkIntent(link)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(link, intent.data?.toString())
    }

    @Test
    fun `getCallIntent returns correct intent`() {
        val number = "1234567890"
        val intent = IntentUtils.getCallIntent(number)
        assertEquals(Intent.ACTION_DIAL, intent.action)
        assertEquals("tel:$number", intent.data?.toString())
    }

    @Test
    fun `getEmailIntent returns correct intent`() {
        val email = "test@test.com"
        val intent = IntentUtils.getEmailIntent(email)
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("mailto:$email", intent.data?.toString())
    }
}
