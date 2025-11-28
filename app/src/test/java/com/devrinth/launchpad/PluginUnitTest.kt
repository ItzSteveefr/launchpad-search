package com.devrinth.launchpad

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
import com.devrinth.launchpad.utils.StringUtils

class PluginUnitTest {
    @Test
    fun fuzzyContains_isCorrect() {
        assertTrue(StringUtils.fuzzyContains("hlwd", "hello world"))
        assertTrue(StringUtils.fuzzyContains("wrld", "hello world"))
        assertFalse(StringUtils.fuzzyContains("dlrow", "hello world"))
    }
}