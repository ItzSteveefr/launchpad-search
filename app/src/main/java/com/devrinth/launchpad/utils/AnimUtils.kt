package com.devrinth.launchpad.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/**
 * Animation utilities for premium spring and cascade effects.
 * Provides Material You-style fluid, bouncy animations.
 */
object AnimUtils {

    // Spring animation parameters
    private const val SPRING_STIFFNESS = SpringForce.STIFFNESS_LOW
    private const val SPRING_DAMPING = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
    
    // Cascade animation parameters
    private const val CASCADE_DELAY_MS = 50L
    private const val CASCADE_DURATION_MS = 300L
    private const val CASCADE_TRANSLATION_Y = 40f

    /**
     * Create a spring-up animation for a view (entry animation from bottom).
     * Uses physics-based SpringAnimation for natural, bouncy feel.
     *
     * @param view The view to animate
     * @param initialTranslationY Starting Y translation (positive = below final position)
     * @param onEnd Callback when animation completes
     */
    fun createSpringUpAnimation(
        view: View,
        initialTranslationY: Float = 100f,
        onEnd: (() -> Unit)? = null
    ) {
        // Set initial state
        view.translationY = initialTranslationY
        view.alpha = 0f
        
        // Create spring animation for translation
        val springAnim = SpringAnimation(view, DynamicAnimation.TRANSLATION_Y, 0f).apply {
            spring = SpringForce(0f).apply {
                stiffness = SPRING_STIFFNESS
                dampingRatio = SPRING_DAMPING
            }
            addEndListener { _, _, _, _ -> 
                onEnd?.invoke()
            }
        }
        
        // Fade in animation
        view.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
        
        // Start spring animation
        springAnim.start()
    }

    /**
     * Create a spring-down animation for a view (exit animation to bottom).
     *
     * @param view The view to animate
     * @param targetTranslationY Ending Y translation
     * @param onEnd Callback when animation completes
     */
    fun createSpringDownAnimation(
        view: View,
        targetTranslationY: Float = 100f,
        onEnd: (() -> Unit)? = null
    ) {
        val springAnim = SpringAnimation(view, DynamicAnimation.TRANSLATION_Y, targetTranslationY).apply {
            spring = SpringForce(targetTranslationY).apply {
                stiffness = SpringForce.STIFFNESS_MEDIUM
                dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            }
            addEndListener { _, _, _, _ ->
                onEnd?.invoke()
            }
        }
        
        view.animate()
            .alpha(0f)
            .setDuration(150)
            .start()
        
        springAnim.start()
    }

    /**
     * Get a spring interpolator for XML-based animations.
     * Uses OvershootInterpolator for a subtle bounce effect.
     */
    fun getSpringInterpolator(): TimeInterpolator {
        return OvershootInterpolator(1.2f)
    }

    /**
     * Create a custom ItemAnimator with staggered cascade effect for RecyclerView.
     * Items fade-up with staggered delay for a premium cascading appearance.
     */
    fun cascadeItemAnimator(): RecyclerView.ItemAnimator {
        return CascadeItemAnimator()
    }

    /**
     * Apply cascade fade-up animation to a single item view.
     * Used manually when binding RecyclerView items.
     *
     * @param view The item view to animate
     * @param position The adapter position (used for stagger delay)
     */
    fun animateCascadeItem(view: View, position: Int) {
        // Set initial state
        view.alpha = 0f
        view.translationY = CASCADE_TRANSLATION_Y
        
        // Calculate stagger delay based on position (cap at 5 items)
        val delay = (position.coerceAtMost(5)) * CASCADE_DELAY_MS
        
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(CASCADE_DURATION_MS)
            .setStartDelay(delay)
            .setInterpolator(getSpringInterpolator())
            .start()
    }

    /**
     * Animate elevation change for focus states.
     */
    fun animateElevation(view: View, targetElevation: Float, durationMs: Long = 200) {
        ObjectAnimator.ofFloat(view, "elevation", view.elevation, targetElevation).apply {
            duration = durationMs
            start()
        }
    }

    /**
     * Custom ItemAnimator with cascade effect for RecyclerView additions.
     */
    private class CascadeItemAnimator : DefaultItemAnimator() {
        
        init {
            addDuration = CASCADE_DURATION_MS
            removeDuration = 200
            moveDuration = 300
            changeDuration = 300
        }

        override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
            val view = holder.itemView
            val position = holder.bindingAdapterPosition
            
            // Set initial state
            view.alpha = 0f
            view.translationY = CASCADE_TRANSLATION_Y
            
            // Calculate delay
            val delay = (position.coerceAtMost(5)) * CASCADE_DELAY_MS
            
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(addDuration)
                .setStartDelay(delay)
                .setInterpolator(getSpringInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        dispatchAddFinished(holder)
                        view.animate().setListener(null)
                    }
                })
                .start()
            
            return true
        }

        override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
            val view = holder.itemView
            
            view.animate()
                .alpha(0f)
                .translationY(-20f)
                .setDuration(removeDuration)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        dispatchRemoveFinished(holder)
                        // Reset for recycling
                        view.alpha = 1f
                        view.translationY = 0f
                        view.animate().setListener(null)
                    }
                })
                .start()
            
            return true
        }
    }
}
