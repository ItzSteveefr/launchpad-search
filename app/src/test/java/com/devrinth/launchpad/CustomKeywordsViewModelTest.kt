package com.devrinth.launchpad

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.devrinth.launchpad.viewmodels.CustomKeywordsViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class CustomKeywordsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: CustomKeywordsViewModel
    private lateinit var mockApplication: Application
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        mockApplication = mock(Application::class.java)
        mockSharedPreferences = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        `when`(mockApplication.getSharedPreferences("com.devrinth.launchpad_preferences", 0)).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(androidx.preference.PreferenceManager.getDefaultSharedPreferences(mockApplication)).thenReturn(mockSharedPreferences)

        viewModel = CustomKeywordsViewModel(mockApplication)
    }

    @Test
    fun `test save keyword`() {
        viewModel.saveKeyword("com.test.app", "test")
        verify(mockEditor).putString("com.test.app", "test")
        verify(mockEditor).apply()
    }
}
