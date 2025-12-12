package com.devrinth.launchpad.fragments

import android.os.Bundle
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
                requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
            } catch (e: Exception) {
                "Unknown"
            }
        }

        findPreference<androidx.preference.Preference>("setting_about_mail")?.apply {
            summary = "support@devrinth.com"
            setOnPreferenceClickListener {
                try {
                    startActivity(com.devrinth.launchpad.utils.IntentUtils.getEmailIntent("support@devrinth.com"))
                } catch (e: Exception) { e.printStackTrace() }
                true
            }
        }

        findPreference<androidx.preference.Preference>("setting_about_website")?.apply {
            summary = "https://devrinth.com"
            setOnPreferenceClickListener {
                try {
                    startActivity(com.devrinth.launchpad.utils.IntentUtils.getLinkIntent("https://devrinth.com"))
                } catch (e: Exception) { e.printStackTrace() }
                true
            }
        }

        findPreference<androidx.preference.Preference>("setting_about_privacy")?.apply {
            setOnPreferenceClickListener {
                try {
                    startActivity(com.devrinth.launchpad.utils.IntentUtils.getLinkIntent("https://devrinth.com/legal/privacy"))
                } catch (e: Exception) { e.printStackTrace() }
                true
            }
        }

        findPreference<androidx.preference.Preference>("setting_about_bmc")?.apply {
            setOnPreferenceClickListener {
                try {
                    startActivity(com.devrinth.launchpad.utils.IntentUtils.getLinkIntent("https://ko-fi.com/devrinth"))
                } catch (e: Exception) { e.printStackTrace() }
                true
            }
        }
    }
}
