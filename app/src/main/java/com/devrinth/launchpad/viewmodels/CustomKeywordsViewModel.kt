package com.devrinth.launchpad.viewmodels

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.devrinth.launchpad.modals.AppKeyword
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomKeywordsViewModel(application: Application) : AndroidViewModel(application) {

    private val _apps = MutableLiveData<List<AppKeyword>>(emptyList())
    val apps: LiveData<List<AppKeyword>> = _apps

    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(application)
    private var isLoaded = false

    fun loadApps() {
        if (isLoaded) return
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val appInfos = pm.queryIntentActivities(mainIntent, 0)

            val appKeywords = appInfos.map { appInfo ->
                val packageName = appInfo.activityInfo.packageName
                val label = appInfo.loadLabel(pm)
                val icon = appInfo.loadIcon(pm)
                val keyword = sharedPreferences.getString(packageName, "")
                AppKeyword(packageName, label, icon, keyword)
            }

            withContext(Dispatchers.Main) {
                _apps.value = appKeywords
                isLoaded = true
            }
        }
    }

    fun saveKeyword(packageName: String, keyword: String) {
        sharedPreferences.edit().putString(packageName, keyword).apply()
        _apps.value?.find { it.packageName == packageName }?.keyword = keyword
        _apps.value = _apps.value
    }
}
