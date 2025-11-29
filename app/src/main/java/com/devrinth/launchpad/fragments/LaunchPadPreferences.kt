package com.devrinth.launchpad.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.devrinth.launchpad.R
import com.devrinth.launchpad.utils.ThemeManager

class LaunchPadPreferences : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preferences, rootKey)

        setupThemePreference()
        setupAboutPreferences()
    }

    private fun setupThemePreference() {
        findPreference<ListPreference>("setting_theme")?.setOnPreferenceChangeListener { _, newValue ->
            ThemeManager.applyTheme(newValue as String)
            requireActivity().recreate()
            true
        }
    }

    private fun setupAboutPreferences() {
        findPreference<androidx.preference.Preference>("setting_about_app_version")?.apply {
            summary = try {
                requireContext().packageManager.getPackageInfo(
                    requireContext().packageName,
                    0
                ).versionName
            } catch (e: Exception) {
                "Unknown"
            }
        }

        findPreference<androidx.preference.Preference>("setting_about_mail")?.apply {
            summary = "support@devrinth.com"
            setOnPreferenceClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("mailto:support@devrinth.com")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT)
                        .show()
                }
                true
            }
        }

        findPreference<androidx.preference.Preference>("setting_about_website")?.apply {
            summary = "https://devrinth.com"
            setOnPreferenceClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://devrinth.com")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "No web browser found", Toast.LENGTH_SHORT)
                        .show()
                }
                true
            }
        }

        findPreference<androidx.preference.Preference>("setting_about_privacy")?.apply {
            setOnPreferenceClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://devrinth.com/privacy")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "No web browser found", Toast.LENGTH_SHORT)
                        .show()
                }
                true
            }
        }

        findPreference<androidx.preference.Preference>("setting_about_bmc")?.apply {
            setOnPreferenceClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://www.buymeacoffee.com/devrinth")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "No web browser found", Toast.LENGTH_SHORT)
                        .show()
                }
                true
            }
        }
    }
}
