package com.iflytek.cyber.iot.show.core.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewBinder
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.MainTemplate

class MainLoadingBinder : ItemViewBinder<MainTemplate, MainLoadingBinder.MainLoadingHolder>() {

    override fun onBindViewHolder(holder: MainLoadingHolder, item: MainTemplate) {
    }

    override fun onCreateViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): MainLoadingHolder {
        val view  = inflater.inflate(R.layout.item_main_loading, parent, false)
        return MainLoadingHolder(view)
    }

    inner class MainLoadingHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}