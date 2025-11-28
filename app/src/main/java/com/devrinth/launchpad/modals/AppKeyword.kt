package com.devrinth.launchpad.modals

import android.graphics.drawable.Drawable

data class AppKeyword(
    val packageName: String,
    val label: CharSequence,
    val icon: Drawable,
    var keyword: String?
)
