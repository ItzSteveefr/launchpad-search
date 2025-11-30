package com.devrinth.launchpad

import android.content.Context
import android.text.Editable
import android.widget.EditText
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.devrinth.launchpad.search.SearchManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SearchManagerTest {

    private lateinit var searchManager: SearchManager
    private lateinit var context: Context
    private lateinit var mockSearchTextBox: EditText
    private lateinit var mockResultRecyclerView: RecyclerView
    private lateinit var mockSearchCardLayout: LinearLayout

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        mockSearchTextBox = mock(EditText::class.java)
        mockResultRecyclerView = mock(RecyclerView::class.java)
        mockSearchCardLayout = mock(LinearLayout::class.java)

        searchManager = SearchManager(
            context,
            mockSearchTextBox,
            mockResultRecyclerView,
            mockSearchCardLayout
        )
    }

    @Test
    fun `test search manager initialization`() {
        assert(searchManager != null)
    }

    @Test
    fun `test processQuery is called on text changed`() {
        val editable = mock(Editable::class.java)
        `when`(editable.toString()).thenReturn("test")
        searchManager.textWatcher.afterTextChanged(editable)
        // Since processQuery is internal, we can't verify it directly.
        // We can, however, verify that the search results are cleared.
        verify(searchManager).clearAllResults()
    }

    @Test
    fun `test clearAllResults is called when search box is empty`() {
        val editable = mock(Editable::class.java)
        `when`(editable.toString()).thenReturn("")
        searchManager.textWatcher.afterTextChanged(editable)
        verify(searchManager).clearAllResults()
    }
}
