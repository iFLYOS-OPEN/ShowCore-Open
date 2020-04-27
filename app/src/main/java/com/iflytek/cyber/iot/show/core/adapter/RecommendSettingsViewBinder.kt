package com.iflytek.cyber.iot.show.core.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewBinder
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.DeskApi
import com.iflytek.cyber.iot.show.core.fragment.MainFragment2
import com.iflytek.cyber.iot.show.core.model.MainTemplate
import com.iflytek.cyber.iot.show.core.utils.ConfigUtils
import com.iflytek.cyber.iot.show.core.widget.StyledSwitch
import com.kk.taurus.playerbase.utils.NetworkUtils

class RecommendSettingsViewBinder(val onCheckedChangeListener: (Boolean) -> Unit) :
    ItemViewBinder<MainTemplate, RecommendSettingsViewBinder.RecommendSettingsHolder>() {

    var switchCanClickable = true

    override fun onCreateViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): RecommendSettingsHolder {
        val view = inflater.inflate(R.layout.item_recommend_settings, parent, false)
        return RecommendSettingsHolder(view)
    }

    fun canSwitchClickable(recyclerView: RecyclerView, position: Int, isLoading: Boolean) {
        if (recyclerView.childCount == 0) {
            return
        }
        val switchItem = recyclerView.layoutManager?.findViewByPosition(position) as? ViewGroup
        val stateContent = switchItem?.findViewById<FrameLayout>(R.id.state_content)
        stateContent?.isVisible = isLoading
    }

    override fun onBindViewHolder(holder: RecommendSettingsHolder, item: MainTemplate) {
        holder.stateContent.isVisible = !NetworkUtils.isNetConnected(holder.itemView.context) || !switchCanClickable
        holder.stateContent.setOnClickListener {
            if (!NetworkUtils.isNetConnected(holder.itemView.context) || !switchCanClickable) {
                Toast.makeText(holder.itemView.context, "网络连接异常，请重新设置", Toast.LENGTH_SHORT).show()
            }
        }

        val mode = ConfigUtils.getInt(MainFragment2.RECOMMEND_CARD_MODE, DeskApi.MODEL_ADULT)

        if (mode == DeskApi.MODEL_ADULT) {
            holder.switch.setChecked(false, false)
        } else {
            holder.switch.setChecked(true, false)
        }

        holder.switch.setOnCheckedChangeListener(object : StyledSwitch.OnCheckedChangeListener {
            override fun onCheckedChange(switch: StyledSwitch, isChecked: Boolean) {
                onCheckedChangeListener.invoke(isChecked)
            }
        })
    }

    inner class RecommendSettingsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val switch = itemView.findViewById<StyledSwitch>(R.id.styled_switch)
        val stateContent = itemView.findViewById<FrameLayout>(R.id.state_content)

        init {
            switch.setTrackColor(Color.parseColor("#E9EBED"))
            switch.setThumbColor(Color.WHITE)
        }
    }
}