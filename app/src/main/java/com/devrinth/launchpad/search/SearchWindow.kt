package com.devrinth.launchpad.search

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.service.voice.VoiceInteractionSessionService
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.devrinth.launchpad.R
import com.devrinth.launchpad.receivers.AssistantActionReceiver
import com.devrinth.launchpad.utils.AnimUtils
import com.devrinth.launchpad.utils.GlassBlurHelper
import com.google.android.material.card.MaterialCardView

class SearchWindow(val context: Context) {

    private lateinit var searchInput : EditText
    private lateinit var resultsView : RecyclerView

    private lateinit var launchpadMainLayout: LinearLayout
    private var rootLayout: CoordinatorLayout? = null

    private lateinit var mSearchManager: SearchManager

    private var reloadReceiver : AssistantActionReceiver = AssistantActionReceiver {
        mSearchManager.reloadPlugins()
    }

    // Use new spring animations
    private var animIn = AnimationUtils.loadAnimation(context, R.anim.spring_up)
    private var animOut = AnimationUtils.loadAnimation(context, R.anim.spring_down)

    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private var lastUiMode: Int = context.resources.configuration.uiMode
    private var isAlternateLayout: Boolean

    private lateinit var mContentView : View
    private lateinit var searchCardLayout: LinearLayout

    private var windowCloseAction: (() -> Unit)? = null

    init {
        // Use spring interpolator for smoother animations
        val interpolator = AnimUtils.getSpringInterpolator()
        animIn.interpolator = interpolator
        animOut.interpolator = PathInterpolatorCompat.create(0.4f, 0.0f, 1.0f, 1.0f) // Accelerate out

        isAlternateLayout = sharedPreferences.getBoolean("setting_layout_window_alternate", false)
    }

    private fun initViews(contentView : View) {

        launchpadMainLayout = contentView.findViewById(R.id.launchpad_main_layout)
        searchInput = contentView.findViewById(R.id.search_input)

        searchCardLayout = contentView.findViewById(R.id.search_card_layout)

        resultsView = contentView.findViewById(R.id.results_view)
        
        // Try to get root layout for glass effect
        rootLayout = contentView.findViewById(R.id.launchpad_root_layout)
        
        // Add haptic feedback on scroll
        setupScrollHaptics()
    }
    
    /**
     * Setup haptic feedback when scrolling through results.
     * Provides subtle CLOCK_TICK feedback while scrolling.
     */
    private fun setupScrollHaptics() {
        resultsView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var lastScrollY = 0
            private var scrollAccumulator = 0
            private val hapticThreshold = 100 // Pixels scrolled before haptic
            
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                scrollAccumulator += kotlin.math.abs(dy)
                
                if (scrollAccumulator >= hapticThreshold) {
                    recyclerView.performHapticFeedback(
                        HapticFeedbackConstants.CLOCK_TICK,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    )
                    scrollAccumulator = 0
                }
            }
        })
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
        
        // Apply glass blur effect to root layout
        rootLayout?.let { root ->
            GlassBlurHelper.applyGlassEffect(root, context, 25f)
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
        val searchCardView = mContentView.findViewById<MaterialCardView>(R.id.search_card_view)

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
            searchInput.postDelayed({
                searchInput.requestFocus()

                val lManager =
                    context.getSystemService(VoiceInteractionSessionService.INPUT_METHOD_SERVICE) as InputMethodManager
                lManager.showSoftInput(searchInput, 0)
            }, 160)
        }
    }

    fun unload() {
        mSearchManager.unloadPlugins()
        
        // Clean up glass effect
        rootLayout?.let { GlassBlurHelper.removeGlassEffect(it) }
        
        try {
            context.unregisterReceiver(reloadReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }

    fun reload() {
        mSearchManager.reloadPlugins()
    }

    fun resume() {
        mSearchManager.resumePlugins()
    }

    // -- WINDOW FUNCTION
    fun showWindow() {
        if (!mContentView.isShown) {
            // Provide haptic feedback on window open
            mContentView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            mContentView.startAnimation(animIn)
        }
    }
    
    fun hideWindow() {
        // Provide subtle haptic feedback on dismiss
        mContentView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        mContentView.startAnimation(animOut)
        mContentView.postOnAnimationDelayed({
            mSearchManager.unloadPlugins()
            windowCloseAction?.invoke()
        }, 180)
    }
}