package com.devrinth.launchpad.utils

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.devrinth.launchpad.R

/**
 * Utility class for applying glass blur effects.
 * Uses RenderEffect on Android 12+ for real-time blur,
 * and falls back to high-alpha translucent background on older versions.
 */
object GlassBlurHelper {

    private const val DEFAULT_BLUR_RADIUS = 25f

    /**
     * Apply glass blur effect to a view.
     * On Android 12+, uses RenderEffect for real-time blur.
     * On older versions, applies a fallback translucent background.
     *
     * @param view The view to apply the effect to
     * @param context The context for accessing resources
     * @param blurRadiusPx The blur radius in pixels (only used on Android 12+)
     */
    fun applyGlassEffect(view: View, context: Context, blurRadiusPx: Float = DEFAULT_BLUR_RADIUS) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            applyRenderEffectBlur(view, blurRadiusPx)
        } else {
            applyFallbackGlassBackground(view, context)
        }
    }

    /**
     * Apply real-time blur using RenderEffect (Android 12+)
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyRenderEffectBlur(view: View, radiusPx: Float) {
        val blurEffect = RenderEffect.createBlurEffect(
            radiusPx,
            radiusPx,
            Shader.TileMode.CLAMP
        )
        view.setRenderEffect(blurEffect)
    }

    /**
     * Apply fallback translucent background for pre-Android 12
     */
    private fun applyFallbackGlassBackground(view: View, context: Context) {
        val glassColor = ContextCompat.getColor(context, R.color.glass_background)
        view.setBackgroundColor(glassColor)
    }

    /**
     * Remove any applied glass effect
     */
    fun removeGlassEffect(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(null)
        }
        view.background = null
    }

    /**
     * Check if real blur effect is supported on this device
     */
    fun isRealBlurSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
}
