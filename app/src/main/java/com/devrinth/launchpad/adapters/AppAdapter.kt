package com.devrinth.launchpad.adapters

import android.content.Context
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.devrinth.launchpad.R

class AppAdapter(
    private val context: Context,
    private var apps: List<ResolveInfo>
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    private var filteredApps: List<ResolveInfo> = apps

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = filteredApps[position]
        holder.bind(app)
    }

    override fun getItemCount(): Int {
        return filteredApps.size
    }

    fun filter(query: String) {
        filteredApps = if (query.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.loadLabel(context.packageManager).toString().contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
        private val appName: TextView = itemView.findViewById(R.id.app_name)
        private val appKeyword: TextView = itemView.findViewById(R.id.app_keyword)

        fun bind(app: ResolveInfo) {
            val pm = context.packageManager
            appName.text = app.loadLabel(pm)
            appIcon.setImageDrawable(app.loadIcon(pm))

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val keyword = sharedPreferences.getString(app.activityInfo.packageName, "")
            appKeyword.text = if (keyword.isNullOrEmpty()) "No keyword set" else "Keyword: $keyword"

            itemView.setOnClickListener {
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Set Custom Keyword")

                val input = EditText(context)
                input.setText(keyword)
                builder.setView(input)

                builder.setPositiveButton("OK") { _, _ ->
                    val newKeyword = input.text.toString()
                    sharedPreferences.edit()
                        .putString(app.activityInfo.packageName, newKeyword).apply()
                    appKeyword.text =
                        if (newKeyword.isEmpty()) "No keyword set" else "Keyword: $newKeyword"
                }
                builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

                builder.show()
            }
        }
    }
}
