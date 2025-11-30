package com.devrinth.launchpad.adapters

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.devrinth.launchpad.R
import com.devrinth.launchpad.receivers.AssistantActionReceiver
import com.devrinth.launchpad.utils.IntentUtils

class PinnedActionListAdapter(
    private val mResults: List<PinnedActionAdapter>,
    private var mContext: Context
) : RecyclerView.Adapter<PinnedActionListAdapter.ViewHolder>() {

    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(mContext)

    private var closeOnClick =
        sharedPreferences.getBoolean("setting_close_on_action", true)

    private var reloadReceiver: AssistantActionReceiver = AssistantActionReceiver {
        closeOnClick =
            sharedPreferences.getBoolean("setting_close_on_action", true)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mContext.registerReceiver(
            reloadReceiver,
            IntentFilter(AssistantActionReceiver.ACTION_OVERLAY_SHOW),
            Context.RECEIVER_EXPORTED
        )
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        mContext.unregisterReceiver(reloadReceiver)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext)
            .inflate(R.layout.pinned_item_view, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mResults.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val actionAdapter: PinnedActionAdapter = mResults[position]

        holder.resultIcon.setImageDrawable(actionAdapter.image)

        holder.parentView.setOnClickListener {
            if (closeOnClick) {
                mContext.sendBroadcast(Intent(AssistantActionReceiver.ACTION_OVERLAY_HIDE))
            }
            mContext.startActivity(
                IntentUtils.getIntentFromString(
                    actionAdapter.action,
                    actionAdapter.uri
                )
            )
        }

    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val resultIcon: ImageView = itemView.findViewById(R.id.pinned_icon)
        val parentView: View = itemView
    }
}