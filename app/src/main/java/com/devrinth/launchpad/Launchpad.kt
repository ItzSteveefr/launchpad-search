package com.devrinth.launchpad

import android.app.Application
import com.devrinth.launchpad.utils.ThemeManager
import com.google.android.material.color.DynamicColors

class Launchpad : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.applyThemeFromPreferences(this)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }

}