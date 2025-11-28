package com.devrinth.launchpad.search

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.service.voice.VoiceInteractionSessionService
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.devrinth.launchpad.R
import com.devrinth.launchpad.receivers.AssistantActionReceiver

class SearchWindow(val context: Context) {

    private lateinit var searchInput : EditText
    private lateinit var resultsView : RecyclerView

    private lateinit var launchpadMainLayout: LinearLayout

    private lateinit var mSearchManager: SearchManager

    private var reloadReceiver : AssistantActionReceiver = AssistantActionReceiver {
        mSearchManager.reloadPlugins()
    }


    private var anim = AnimationUtils.loadAnimation(context, R.anim.pop_up)
    private var animOut = AnimationUtils.loadAnimation(context, R.anim.pop_down)

    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private var lastUiMode: Int = context.resources.configuration.uiMode
    private var isAlternateLayout: Boolean

    private lateinit var mContentView : View
    private lateinit var searchCardLayout: LinearLayout

    private var windowCloseAction: (() -> Unit)? = null

    init {
        val interpolator = PathInterpolatorCompat.create(0.425f, 0.130f, 0.130f, 0.975f)
        anim.interpolator = interpolator
        animOut.interpolator = interpolator

        isAlternateLayout = sharedPreferences.getBoolean("setting_layout_window_alternate", false)
    }

    private fun initViews(contentView : View) {

        launchpadMainLayout = contentView.findViewById(R.id.launchpad_main_layout)
        searchInput = contentView.findViewById(R.id.search_input)

        searchCardLayout = contentView.findViewById(R.id.search_card_layout)

        resultsView = contentView.findViewById(R.id.results_view)
//            pinnedResultView = contentView.findViewById(R.id.pinned_apps_view)
//            pinnedResultView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun initListeners() {
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(reloadReceiver, IntentFilter(AssistantActionReceiver.ACTION_OVERLAY_SHOW),
                VoiceInteractionSessionService.RECEIVER_EXPORTED
            )
        } else {
            context.registerReceiver(reloadReceiver, IntentFilter(AssistantActionReceiver.ACTION_OVERLAY_SHOW))
        }

        if (sharedPreferences.getBoolean("setting_close_on_outclick", false)) {
            launchpadMainLayout.setOnClickListener {
                this.hideWindow()
            }
        }

//            pinnedAdapter = PinnedActionListAdapter(pinnedArray, context)
//            pinnedResultView.adapter = pinnedAdapter
//
//            pinnedArray.add(
//                PinnedActionAdapter(
//                    "android.intent.action.VIEW",
//                    "https://google.com",
//                    AppCompatResources.getDrawable(context, R.drawable.web_search_24)
//                `)
//            )
//            pinnedArray.add(
//                PinnedActionAdapter(
//                    "android.intent.action.VIEW",
//                    "https://google.com",
//                    AppCompatResources.getDrawable(context, R.drawable.baseline_settings_24)
//                )
//            )`
//            pinnedAdapter.notifyDataSetChanged()
    }

    // PUBLIC
    fun onWindowClose(closeFn: () -> Unit) {
        windowCloseAction = closeFn
    }

    fun createLaunchpadWindow(window: Window?) {
        val inflater = context.getSystemService(VoiceInteractionSessionService.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val contentView: View = if (isAlternateLayout) {
            inflater.inflate(R.layout.search_layout_reverse, null) }
        else {
            inflater.inflate(R.layout.search_layout, null)
        }

        mContentView = contentView
        initViews(contentView)

        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE) // kb
            if (sharedPreferences.getBoolean("setting_layout_blur_behind", false)) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.attributes?.blurBehindRadius = 25
                }
            }
            window.attributes = window.attributes
        }
        mSearchManager = SearchManager(
            context,
            searchInput,
            resultsView,
            searchCardLayout
        )

        initListeners()

    }

    fun fullscreen() {
        val searchCardView = mContentView.findViewById<CardView>(R.id.search_card_view)

        val params = searchCardView.layoutParams as LinearLayout.LayoutParams
        params.width = LinearLayout.LayoutParams.MATCH_PARENT
        params.height = LinearLayout.LayoutParams.MATCH_PARENT
        searchCardView.layoutParams = params

        searchCardView.requestLayout()
    }


    fun getLaunchpadWindow() : View {
        return mContentView
    }

    fun showKeyboard() {
        if (sharedPreferences.getBoolean("setting_search_show_keyboard", true)) {
            searchInput.isFocusableInTouchMode = true
            searchInput.clearFocus()
            searchInput.requestFocus()
            val lManager =
                context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            lManager.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun unload() {
        mSearchManager.unloadPlugins()
    }

    fun reload() {
        mSearchManager.reloadPlugins()
    }

    fun resume() {
        mSearchManager.resumePlugins()
    }

    // -- WINDOW FUNCTION
    fun showWindow() {
        if (!mContentView.isShown)
            mContentView.startAnimation(anim)

    }
    fun hideWindow() {
        mContentView.startAnimation(animOut)
        mContentView.postOnAnimationDelayed({
            mSearchManager.unloadPlugins()
            windowCloseAction?.invoke()
        }, 120)
    }

}