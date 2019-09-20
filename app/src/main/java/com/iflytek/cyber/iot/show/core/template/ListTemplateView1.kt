package com.iflytek.cyber.iot.show.core.template

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.gson.JsonParser
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation

class ListTemplateView1 @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val listItems = mutableListOf<ListItem>()
    private val adapter = ListAdapter()

    private val recyclerView: RecyclerView
    private val skillIconImage: ImageView
    private val backgroundImage: ImageView
    private val mainTitle: TextView
    private val subTitle: TextView

    private var currentPayload: String? = null

    private val innerOnClickBackListener = OnClickListener { v ->
        onClickBackListener?.onClick(v)
    }
    private var onClickBackListener: OnClickListener? = null

    init {
        val childView = LayoutInflater.from(context)
            .inflate(R.layout.layout_list_template_1, null)

        skillIconImage = childView.findViewById(R.id.skill_icon)
        backgroundImage = childView.findViewById(R.id.background_image)
        mainTitle = childView.findViewById(R.id.main_title)
        subTitle = childView.findViewById(R.id.sub_title)
        recyclerView = childView.findViewById(R.id.recycler_view)

        recyclerView.adapter = adapter

        childView.findViewById<View>(R.id.back)?.setOnClickListener(innerOnClickBackListener)

        addView(childView)
    }

    fun updatePayload(payload: String) {
        val json = JsonParser().parse(payload).asJsonObject

        mainTitle.text = json.get(Constant.PAYLOAD_MAIN_TITLE)?.asString
        json.get(Constant.PAYLOAD_SUB_TITLE)?.asString?.let { subTitle ->
            if (subTitle.isNotEmpty()) {
                this.subTitle.visibility = View.VISIBLE
                this.subTitle.text = subTitle
            } else {
                this.subTitle.visibility = View.GONE
            }
        } ?: run {
            this.subTitle.visibility = View.GONE
        }

        json.get(Constant.PAYLOAD_BACK_BUTTON)?.asString?.let { backButton ->
            when (backButton) {
                Constant.BACK_BUTTON_HIDDEN -> {
                    findViewById<View>(R.id.back_icon).visibility = View.GONE
                }
                else -> {
                    findViewById<View>(R.id.back_icon).visibility = View.VISIBLE
                }
            }
        } ?: run {
            findViewById<View>(R.id.back_icon).visibility = View.VISIBLE
        }

        json.get(Constant.PAYLOAD_LIST_ITEMS)?.asJsonArray?.let {
            listItems.clear()

            val arrayIterator = it.iterator()
            while (arrayIterator.hasNext()) {
                val item = arrayIterator.next().asJsonObject

                listItems.add(ListItem(
                    item.get(Constant.PAYLOAD_LEFT_TEXT).asString,
                    item.get(Constant.PAYLOAD_RIGHT_TEXT).asString))
            }

            adapter.notifyDataSetChanged()
        } ?: run {
            listItems.clear()

            adapter.notifyDataSetChanged()
        }

        json.get(Constant.PAYLOAD_SKILL_ICON_URL)?.asString?.let { skillIconUrl ->
            if (skillIconUrl.isNotEmpty()) {
                skillIconImage.visibility = View.VISIBLE

                Glide.with(skillIconImage)
                    .load(skillIconUrl)
                    .apply(RequestOptions()
                        .transform(RoundedCornersTransformation(
                            resources.getDimensionPixelSize(R.dimen.dp_8), 0)))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(skillIconImage)
            } else {
                skillIconImage.visibility = View.GONE
            }
        } ?: run {
            skillIconImage.visibility = View.GONE
        }
        json.get(Constant.PAYLOAD_BACKGROUND_IMAGE_URL)?.asString?.let { backgroundImageUrl ->
            if (backgroundImageUrl.isNotEmpty()) {
                backgroundImage.visibility = View.VISIBLE

                Glide.with(backgroundImage)
                    .load(backgroundImageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(backgroundImage)
            } else {
                backgroundImage.visibility = View.GONE
            }
        } ?: run {
            backgroundImage.visibility = View.GONE
        }

        currentPayload = payload
    }

    fun setOnClickBackListener(onClickListener: OnClickListener?) {
        this.onClickBackListener = onClickListener
    }

    private inner class ListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return ListItemViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_list_template_new, parent, false))
        }

        override fun getItemCount(): Int {
            return listItems.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ListItemViewHolder) {
                holder.tvLeft.text = listItems[position].leftText
                holder.tvRight.text = listItems[position].rightText
            }
        }
    }

    private data class ListItem(val leftText: String?, val rightText: String?)

    private class ListItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLeft: TextView = itemView.findViewById(R.id.left_text)
        val tvRight: TextView = itemView.findViewById(R.id.right_text)
    }

    class Builder(private val context: Context) {
        private var payload: String? = null
        private var onClickListener: OnClickListener? = null
        fun payload(payload: String): Builder {
            this.payload = payload
            return this
        }

        fun onClickBackListener(onClickListener: OnClickListener?): Builder {
            this.onClickListener = onClickListener
            return this
        }

        fun build(): ListTemplateView1 {
            val view = ListTemplateView1(context)
            payload?.let { payload ->
                view.updatePayload(payload)
            }
            onClickListener?.let {
                view.setOnClickBackListener(it)
            }
            return view
        }
    }
}