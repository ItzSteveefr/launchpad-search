package com.devrinth.launchpad.adapters

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.devrinth.launchpad.R
import com.devrinth.launchpad.receivers.AssistantActionReceiver

class ResultScrollAdapter(
    private val mResults: List<ResultAdapter>,
    private var mContext: Context
) : RecyclerView.Adapter<ResultScrollAdapter.ViewHolder>() {

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
            .inflate(R.layout.result_view, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mResults.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mResultAdapter: ResultAdapter = mResults[position]

        holder.resultValue.text = mResultAdapter.value
        holder.resultExtra.text = mResultAdapter.extra

        holder.resultExtra.visibility = if (mResultAdapter.extra != null) View.VISIBLE else View.GONE
        holder.resultIcon.setImageDrawable(mResultAdapter.image)

        holder.parentView.setOnClickListener {
            mResultAdapter.action1?.let { intent ->
                val animation = AnimationUtils.loadAnimation(mContext, R.anim.scale_effect)
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                it.startAnimation(animation)
                if (closeOnClick) {
                    mContext.sendBroadcast(Intent(AssistantActionReceiver.ACTION_OVERLAY_HIDE))
                }
                mContext.startActivity(intent)
            }
        }

        holder.parentView.setOnLongClickListener {
            mResultAdapter.action2?.let { intent ->
                if (closeOnClick) {
                    mContext.sendBroadcast(Intent(AssistantActionReceiver.ACTION_OVERLAY_HIDE))
                }
                mContext.startActivity(intent)
                return@setOnLongClickListener true
            }
            false
        }

        val animation = AnimationUtils.loadAnimation(mContext, R.anim.fade_in)
        holder.itemView.startAnimation(animation)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val resultValue: TextView = itemView.findViewById(R.id.result_text)
        val resultExtra: TextView = itemView.findViewById(R.id.result_extra)
        val resultIcon: ImageView = itemView.findViewById(R.id.result_icon)

        val parentView: View = itemView
    }
}