package com.devrinth.launchpad.fragments

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.devrinth.launchpad.viewmodels.CustomKeywordsViewModel

class CustomKeywordsPreferences : PreferenceFragmentCompat() {

    private val viewModel: CustomKeywordsViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        val appCategory = PreferenceCategory(context)
        appCategory.title = "Apps"
        screen.addPreference(appCategory)

        lifecycleScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null)
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = pm.queryIntentActivities(mainIntent, 0)

            withContext(Dispatchers.Main) {
                if (isAdded) {
                    for (app in apps) {
                        val appPreference = Preference(context)
                        appPreference.title = app.loadLabel(pm)
                    appPreference.icon = app.loadIcon(pm)
                    appPreference.key = app.activityInfo.packageName

                    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                    val keyword = sharedPreferences.getString(app.activityInfo.packageName, "")
                    appPreference.summary =
                        if (keyword.isNullOrEmpty()) "No keyword set" else "Keyword: $keyword"

                    appPreference.setOnPreferenceClickListener {
                        val builder = AlertDialog.Builder(context)
                        builder.setTitle("Set Custom Keyword")

                        val input = EditText(context)
                        input.inputType = InputType.TYPE_CLASS_TEXT
                        builder.setView(input)

                        builder.setPositiveButton("OK") { _, _ ->
                            val newKeyword = input.text.toString()
                            sharedPreferences.edit()
                                .putString(app.activityInfo.packageName, newKeyword).apply()
                            appPreference.summary =
                                if (newKeyword.isEmpty()) "No keyword set" else "Keyword: $newKeyword"
                        }
                        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        viewModel.apps.observe(this) { apps ->
            appCategory.removeAll()
            apps.forEach { app ->
                val preference = Preference(context).apply {
                    title = app.label
                    icon = app.icon
                    key = app.packageName
                    summary = if (app.keyword.isNullOrEmpty()) "No keyword set" else "Keyword: ${app.keyword}"
                    setOnPreferenceClickListener {
                        showKeywordDialog(app.packageName, app.keyword ?: "")
                        true
                    }
                    appCategory.addPreference(appPreference)
                    }
                }
                appCategory.addPreference(preference)
            }
        }

        viewModel.loadApps()
    }

    private fun showKeywordDialog(packageName: String, currentKeyword: String) {
        val context = requireContext()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Set Custom Keyword")

        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(currentKeyword)
        }
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            val newKeyword = input.text.toString()
            viewModel.saveKeyword(packageName, newKeyword)
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }
}
