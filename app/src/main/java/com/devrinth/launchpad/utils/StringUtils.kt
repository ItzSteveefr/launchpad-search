package com.devrinth.launchpad.utils

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Utility class providing various string manipulation, comparison, and validation methods.
 */
class StringUtils {
    companion object {

        // COMPARISONS

        /**
         * Calculates the Levenshtein distance between two strings.
         *
         * The Levenshtein distance is the minimum number of single-character edits
         * (insertions, deletions, or substitutions) required to change one string into another.
         * This is commonly used in spell checking, autocorrect, and auto-complete functionality.
         *
         * The lower the distance, the more similar the strings are.
         *
         * @param s1 The first string to compare
         * @param s2 The second string to compare
         * @return The Levenshtein distance between the two strings
         *
         * Example:
         * ```
         * levenshteinDistance("kitten", "sitting") // Returns 3
         * // Changes: k→s, e→i, +g = 3 edits
         * ```
         *
         * @see <a href="https://en.wikipedia.org/wiki/Levenshtein_distance">Levenshtein Distance - Wikipedia</a>
         */
        fun levenshteinDistance(s1: String, s2: String): Int {
            val len1 = s1.length
            val len2 = s2.length

            val dp = Array(len1 + 1) { IntArray(len2 + 1) }

            for (i in 0..len1) dp[i][0] = i
            for (j in 0..len2) dp[0][j] = j

            for (i in 1..len1) {
                for (j in 1..len2) {
                    val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                    dp[i][j] = minOf(
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1,
                        dp[i - 1][j - 1] + cost
                    )
                }
            }

            return dp[len1][len2]
        }

        /**
         * Checks if any string in the target list contains the query using fuzzy matching.
         *
         * This method performs fuzzy matching, where characters in the query don't need to be
         * consecutive in the target string, but they must appear in the same order.
         *
         * @param query The search string to look for
         * @param targets List of strings to search within
         * @return `true` if any target string fuzzy-contains the query, `false` otherwise
         *
         * Example:
         * ```
         * val targets = arrayListOf("hello world", "goodbye")
         * anyFuzzyContains("hlwd", targets) // Returns true (matches "hello world")
         * ```
         */
        fun anyFuzzyContains(query: String, targets: ArrayList<String>): Boolean {
            if (query.isEmpty()) return true
            if (targets.isEmpty()) return false

            for (target in targets) {
                if (fuzzyContains(query, target)) {
                    return true
                }
            }

            return false
        }

        /**
         * Performs fuzzy string matching between a query and target string.
         *
         * Fuzzy matching allows characters in the query to be non-consecutive in the target,
         * but they must appear in the same order. Case-insensitive comparison.
         *
         * @param query The search string
         * @param target The string to search within
         * @return `true` if the target fuzzy-contains the query, `false` otherwise
         *
         * Example:
         * ```
         * fuzzyContains("hlwd", "hello world") // Returns true
         * fuzzyContains("wrld", "hello world") // Returns true
         * fuzzyContains("dlrow", "hello world") // Returns false (wrong order)
         * ```
         */
        @Deprecated("Use fuzzySearch for scored matching", ReplaceWith("fuzzySearch(query, target) > 0"))
        fun fuzzyContains(query: String, target: String): Boolean {
            if (query.isEmpty()) return true
            if (target.isEmpty()) return false

            val queryLower = query.lowercase().trim()
            val targetLower = target.lowercase().trim()

            var queryIndex = 0

            for (char in targetLower) {
                if (queryIndex < queryLower.length && char == queryLower[queryIndex]) {
                    queryIndex++
                }
            }

            return queryIndex == queryLower.length
        }

        fun fuzzySearch(query: String, target: String): Int {
            if (query.isEmpty()) {
                return if (target.isEmpty()) 100 else 0
            }

            val queryLower = query.lowercase().trim()
            val targetLower = target.lowercase().trim()
            var score = 0
            var queryIndex = 0
            var targetIndex = 0
            var consecutiveMatches = 0
            var isFirstMatch = true

            while (queryIndex < queryLower.length && targetIndex < targetLower.length) {
                if (queryLower[queryIndex] == targetLower[targetIndex]) {
                    var matchScore = 1
                    if (isFirstMatch) {
                        matchScore += 2
                        isFirstMatch = false
                    }
                    if (targetIndex == 0 || targetLower[targetIndex - 1] == ' ') {
                        matchScore += 5
                    }
                    if (consecutiveMatches > 0) {
                        matchScore += consecutiveMatches * 2
                    }
                    score += matchScore
                    consecutiveMatches++
                    queryIndex++
                } else {
                    consecutiveMatches = 0
                }
                targetIndex++
            }
            if (queryIndex != queryLower.length) {
                return 0
            }

            val targetWords = target.split(" ")
            if (targetWords.size > 1 && query.length > 1) {
                val acronym = targetWords.mapNotNull { it.firstOrNull() }.joinToString("")
                if (acronym.equals(query, ignoreCase = true)) {
                    score += 30
                }
            } else if (query.length > 1 && query.all { it.isUpperCase() }) {
                val acronym = target.split(" ").mapNotNull { it.firstOrNull() }.joinToString("")
                if (acronym.startsWith(query, ignoreCase = true)) {
                    score += 20
                }
            }


            score -= (target.length - query.length)

            return score
        }

        /**
         * Checks if any string in the target list contains the query using exact substring matching.
         *
         * @param query The search string to look for
         * @param targets List of strings to search within
         * @return `true` if any target string contains the query as a substring, `false` otherwise
         *
         * Example:
         * ```
         * val targets = arrayListOf("hello world", "goodbye")
         * anyContains("world", targets) // Returns true
         * anyContains("xyz", targets) // Returns false
         * ```
         */
        fun anyContains(query: String, targets: ArrayList<String>): Boolean {
            for (target in targets) {
                if (simpleContains(query, target)) {
                    return true
                }
            }
            return false
        }

        /**
         * Performs exact substring matching between a query and target string.
         *
         * Case-insensitive comparison that checks if the target contains the query
         * as a consecutive substring.
         *
         * @param query The search string
         * @param target The string to search within
         * @return `true` if the target contains the query as a substring, `false` otherwise
         *
         * Example:
         * ```
         * simpleContains("world", "hello world") // Returns true
         * simpleContains("WORLD", "hello world") // Returns true (case-insensitive)
         * simpleContains("xyz", "hello world") // Returns false
         * ```
         */
        fun simpleContains(query: String, target: String): Boolean {
            if (query.isEmpty()) return true
            if (target.isEmpty()) return false

            val queryLower = query.lowercase().trim()
            val targetLower = target.lowercase().trim()

            return targetLower.contains(queryLower)
        }


        // CHECKS

        /**
         * Validates if a string is a valid URL.
         *
         * Checks if the URL starts with either "http://" or "https://" protocol.
         *
         * @param url The URL string to validate
         * @return `true` if the URL has a valid HTTP/HTTPS protocol, `false` otherwise
         *
         * Example:
         * ```
         * isValidUrl("https://example.com") // Returns true
         * isValidUrl("http://example.com") // Returns true
         * isValidUrl("ftp://example.com") // Returns false
         * isValidUrl("example.com") // Returns false
         * ```
         */
        fun isValidUrl(url: String): Boolean {
            return url.startsWith("http://") || url.startsWith("https://")
        }

        /**
         * Validates if a string is a valid email address.
         *
         * Uses Android's built-in email pattern matcher for validation.
         *
         * @param email The email string to validate
         * @return `true` if the email format is valid, `false` otherwise
         *
         * Example:
         * ```
         * isValidEmail("user@example.com") // Returns true
         * isValidEmail("invalid.email") // Returns false
         * isValidEmail("user@") // Returns false
         * ```
         */
        fun isValidEmail(email: String): Boolean {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        }

        // OPERATIONS

        /**
         * URL-encodes a string using UTF-8 encoding.
         *
         * Converts special characters in the input string to percent-encoded format
         * suitable for use in URLs.
         *
         * @param input The string to encode
         * @return The URL-encoded string
         *
         * Example:
         * ```
         * encodeUrl("hello world") // Returns "hello+world"
         * encodeUrl("user@domain.com") // Returns "user%40domain.com"
         * ```
         */
        fun encodeUrl(input: String): String {
            return URLEncoder.encode(input, StandardCharsets.UTF_8.toString())
        }

    }

}