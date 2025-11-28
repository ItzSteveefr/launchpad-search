package com.devrinth.launchpad.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.devrinth.launchpad.R
import com.devrinth.launchpad.fragments.CustomKeywordsPreferences

class CustomKeywordsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_layout)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, CustomKeywordsPreferences())
            .commit()
    }
}
