package com.devrinth.launchpad.activities

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowInsetsController
import android.view.WindowManager
import com.devrinth.launchpad.modals.OverlayState
import com.devrinth.launchpad.search.SearchWindow

class LaunchpadOverlayActivity : Activity() {

    private lateinit var mSearchWindow: SearchWindow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        window.statusBarColor = Color.TRANSPARENT

        mSearchWindow = SearchWindow(this)
        mSearchWindow.createLaunchpadWindow(window)

        setContentView(mSearchWindow.getLaunchpadWindow())

        mSearchWindow.showWindow()
        mSearchWindow.showKeyboard()

        mSearchWindow.onWindowClose { finish() }
    }

    override fun onStart() {
        super.onStart()
        OverlayState.isOverlayActive = true
    }

    override fun onStop() {
        super.onStop()
        mSearchWindow.unload()
        OverlayState.isOverlayActive = false
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        mSearchWindow.hideWindow()
    }

    override fun onResume() {
        super.onResume()
        mSearchWindow.resume()
    }

    override fun onPause() {
        super.onPause()
        finish()
    }
}
