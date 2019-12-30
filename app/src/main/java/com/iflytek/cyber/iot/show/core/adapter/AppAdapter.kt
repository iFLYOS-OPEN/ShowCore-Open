package com.iflytek.cyber.iot.show.core.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.launcher.AppData
import com.iflytek.cyber.iot.show.core.utils.OnItemClickListener

class AppAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val internalApps = mutableListOf<AppData>()
    private val partyApps = mutableListOf<AppData>()
    private val templateApps = mutableListOf<AppData>()
    private val privateApps = mutableListOf<AppData>()

    var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val holder = AppViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_app,
                parent,
                false
            )
        )
        holder.itemView.findViewById<View>(R.id.clickable_view).setOnClickListener {
            val position = holder.adapterPosition
            onItemClickListener?.onItemClick(parent, holder.itemView, position)
        }
        return holder
    }

    fun setInternalAppData(apps: List<AppData>) {
        this.internalApps.clear()
        this.internalApps.addAll(apps)
    }

    fun setPartyAppData(apps: List<AppData>) {
        this.partyApps.clear()
        this.partyApps.addAll(apps)
    }

    fun setPrivateAppData(apps: List<AppData>) {
        this.privateApps.clear()
        this.privateApps.addAll(apps)
    }

    fun setTemplateAppData(apps: List<AppData>) {
        this.templateApps.clear()
        this.templateApps.addAll(apps)
    }

    /**
     * 应用显示顺序  内置页面 -> 内置 App -> 应用模板 -> 第三方应用
     */
    fun getAppData(position: Int): AppData? {
        return when (position) {
            in 0 until internalApps.size -> {
                internalApps[position]
            }
            in internalApps.size until internalApps.size + privateApps.size -> {
                privateApps[position - internalApps.size]
            }
            in internalApps.size + privateApps.size until templateApps.size + internalApps.size + privateApps.size -> {
                templateApps[position - internalApps.size - privateApps.size]
            }
            in templateApps.size + internalApps.size + privateApps.size until itemCount -> {
                partyApps[position - internalApps.size - privateApps.size - templateApps.size]
            }
            else -> {
                null
            }
        }
    }

    override fun getItemCount(): Int {
        return partyApps.size + internalApps.size + templateApps.size + privateApps.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AppViewHolder) {
            val context = holder.itemView.context
            try {
                val app = getAppData(position) ?: return
                when (app.appType) {
                    AppData.TYPE_PARTY, AppData.TYPE_INTERNAL -> {
                        holder.iconView.setImageDrawable(app.icon)
                    }
                    AppData.TYPE_TEMPLATE -> {
                        Glide.with(context)
                            .asDrawable()
                            .load(app.iconUrl)
                            .transition(
                                DrawableTransitionOptions.with(
                                    DrawableCrossFadeFactory.Builder()
                                        .setCrossFadeEnabled(true).build()
                                )
                            )
                            .into(holder.iconView)
                    }
                }
                holder.nameView.text = app.name

                (holder.itemView.parent as? RecyclerView)?.let { recyclerView ->
                    val layoutManager =
                        recyclerView.layoutManager as? GridLayoutManager ?: return@let
                    val paddingBottom =
                        if (position / layoutManager.spanCount == itemCount / layoutManager.spanCount) {
                            recyclerView.resources.getDimensionPixelSize(R.dimen.dp_30)
                        } else {
                            0
                        }
                    if (paddingBottom != holder.itemView.paddingBottom) {
                        holder.itemView.updatePadding(bottom = paddingBottom)
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }

        }
    }

    private class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconView = itemView.findViewById<ImageView>(R.id.icon)
        val nameView = itemView.findViewById<TextView>(R.id.name)
    }
}