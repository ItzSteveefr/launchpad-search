package com.devrinth.launchpad

import com.devrinth.launchpad.utils.StringUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StringUtilsTest {

    @Test
    fun `fuzzyContains returns true for valid fuzzy match`() {
        assertTrue(StringUtils.fuzzyContains("hlw", "hello world"))
        assertTrue(StringUtils.fuzzyContains("h w", "hello world"))
        assertTrue(StringUtils.fuzzyContains("el", "hello world"))
    }

    @Test
    fun `fuzzyContains returns false for invalid fuzzy match`() {
        assertFalse(StringUtils.fuzzyContains("whl", "hello world"))
        assertFalse(StringUtils.fuzzyContains("h z", "hello world"))
    }

    @Test
    fun `fuzzyContains is case insensitive`() {
        assertTrue(StringUtils.fuzzyContains("HLW", "hello world"))
    }

    @Test
    fun `fuzzyContains handles empty query`() {
        assertTrue(StringUtils.fuzzyContains("", "hello world"))
    }

    @Test
    fun `fuzzyContains handles empty target`() {
        assertFalse(StringUtils.fuzzyContains("a", ""))
    }

    @Test
    fun `simpleContains returns true for valid substring`() {
        assertTrue(StringUtils.simpleContains("ell", "hello world"))
    }

    @Test
    fun `simpleContains is case insensitive`() {
        assertTrue(StringUtils.simpleContains("ELL", "hello world"))
    }
}
