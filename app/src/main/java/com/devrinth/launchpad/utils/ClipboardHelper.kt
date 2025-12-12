package com.devrinth.launchpad.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import java.security.MessageDigest

/**
 * Utility class for clipboard operations.
 * Handles reading/writing clipboard content and sensitivity detection.
 */
object ClipboardHelper {

    /**
     * Get the current clipboard text content.
     * Returns null if clipboard is empty or contains non-text content.
     */
    fun getCurrentClipboardContent(context: Context): String? {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        
        if (!clipboardManager.hasPrimaryClip()) {
            return null
        }
        
        val clip = clipboardManager.primaryClip ?: return null
        
        // Check if content is sensitive (API 33+)
        if (isSensitiveContent(clip)) {
            return null
        }
        
        if (clip.itemCount == 0) {
            return null
        }
        
        val item = clip.getItemAt(0)
        val text = item.text?.toString()
        
        // Return null for empty or very short content
        return if (text.isNullOrBlank() || text.length < 2) null else text
    }

    /**
     * Check if the clipboard content is marked as sensitive.
     * Uses ClipDescription.EXTRA_IS_SENSITIVE on API 33+.
     * Also uses heuristic checks for password-like content.
     */
    fun isSensitiveContent(clip: ClipData): Boolean {
        // Check Android 13+ sensitive flag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val description = clip.description
            if (description.extras?.getBoolean("android.content.extra.IS_SENSITIVE", false) == true) {
                return true
            }
        }
        
        // Heuristic check for password-like content
        if (clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: return false
            
            // Skip if content contains "password" MIME type hint
            val description = clip.description
            for (i in 0 until description.mimeTypeCount) {
                val mimeType = description.getMimeType(i)
                if (mimeType.contains("password", ignoreCase = true)) {
                    return true
                }
            }
            
            // Heuristic: Skip content that looks like a password
            // (single line, no spaces, mixed case/numbers/symbols)
            if (looksLikePassword(text)) {
                return true
            }
        }
        
        return false
    }

    /**
     * Heuristic check if text looks like a password.
     * Passwords typically: single line, no spaces, 8-64 chars, mixed character types.
     */
    private fun looksLikePassword(text: String): Boolean {
        if (text.contains('\n') || text.contains(' ')) return false
        if (text.length < 8 || text.length > 64) return false
        
        val hasUppercase = text.any { it.isUpperCase() }
        val hasLowercase = text.any { it.isLowerCase() }
        val hasDigit = text.any { it.isDigit() }
        val hasSpecial = text.any { !it.isLetterOrDigit() }
        
        // If it has 3+ character types and is reasonable length, might be a password
        val typeCount = listOf(hasUppercase, hasLowercase, hasDigit, hasSpecial).count { it }
        return typeCount >= 3
    }

    /**
     * Compute MD5 hash of content for duplicate detection.
     */
    fun computeContentHash(content: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(content.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Copy text to the system clipboard.
     */
    fun copyToClipboard(context: Context, text: String, label: String = "Launchpad") {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
    }

    /**
     * Get a human-readable relative time string.
     */
    fun getRelativeTimeString(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            else -> "${days / 7}w ago"
        }
    }

    /**
     * Truncate text for preview display.
     */
    fun truncateForPreview(content: String, maxLength: Int = 100): String {
        return if (content.length <= maxLength) {
            content.replace('\n', ' ')
        } else {
            content.take(maxLength).replace('\n', ' ') + "…"
        }
    }
}
