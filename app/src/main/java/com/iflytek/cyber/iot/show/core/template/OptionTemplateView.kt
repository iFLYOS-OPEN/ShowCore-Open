package com.iflytek.cyber.iot.show.core.template

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation

class OptionTemplateView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val optionList = mutableListOf<OptionElement>()

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
    private var onElementSelectedListener: OnElementSelectedListener? = null

    private var itemViewType = VIEW_TYPE_VERTICAL

    companion object {
        private const val VIEW_TYPE_VERTICAL = 0
        private const val VIEW_TYPE_HORIZONTAL = 1
        private const val VIEW_TYPE_NO_IMAGE = 2
    }

    init {
        val childView = LayoutInflater.from(context)
            .inflate(R.layout.layout_option_template_2, null)

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

        when (json.get(Constant.PAYLOAD_TYPE)?.asString) {
            Constant.TYPE_OPTION_TEMPLATE_1 -> {
                itemViewType = VIEW_TYPE_NO_IMAGE
                recyclerView.layoutManager = LinearLayoutManager(context)
            }
            Constant.TYPE_OPTION_TEMPLATE_2 -> {
                itemViewType = VIEW_TYPE_VERTICAL
                recyclerView.layoutManager = GridLayoutManager(context, 4)
            }
            Constant.TYPE_OPTION_TEMPLATE_3 -> {
                itemViewType = VIEW_TYPE_HORIZONTAL
                recyclerView.layoutManager = GridLayoutManager(context, 4)
            }
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

        json.get(Constant.PAYLOAD_OPTION_ELEMENTS)?.asJsonArray?.let {
            optionList.clear()

            val arrayIterator = it.iterator()
            val gson = Gson()
            while (arrayIterator.hasNext()) {
                val item = arrayIterator.next().asJsonObject

                optionList.add(gson.fromJson(item, OptionElement::class.java))
            }

            adapter.notifyDataSetChanged()
        } ?: run {
            optionList.clear()

            adapter.notifyDataSetChanged()
        }

        json.get(Constant.PAYLOAD_SKILL_ICON_URL)?.asString?.let { skillIconUrl ->
            if (skillIconUrl.isNotEmpty()) {
                skillIconImage.visibility = View.VISIBLE

                Glide.with(skillIconImage)
                    .load(skillIconUrl)
                    .apply(
                        RequestOptions()
                            .transform(
                                RoundedCornersTransformation(
                                    resources.getDimensionPixelSize(R.dimen.dp_8), 0
                                )
                            )
                    )
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
            val holder =
                ListItemViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(
                            when (itemViewType) {
                                VIEW_TYPE_NO_IMAGE ->
                                    R.layout.item_option_template_1
                                VIEW_TYPE_VERTICAL ->
                                    R.layout.item_option_template_2
                                else ->
                                    R.layout.item_option_template_3
                            },
                            parent, false
                        )
                )
            holder.itemView.setOnClickListener {
                onElementSelectedListener?.onElementSelected(
                    this@OptionTemplateView, it, holder.adapterPosition,
                    optionList[holder.adapterPosition]
                )
            }
            return holder
        }

        override fun getItemViewType(position: Int): Int {
            return itemViewType
        }

        override fun getItemCount(): Int {
            return optionList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ListItemViewHolder) {
                val item = optionList[position]

                holder.tvPrimary.text = item.primaryText
                holder.tvSecondary.text = item.secondaryText
                holder.tvTertiary.text = item.tertiaryText
                holder.tvTertiary.visibility =
                    if (item.tertiaryText.isNullOrEmpty()) View.GONE else View.VISIBLE
                holder.secondaryContainer?.isVisible =
                    !(item.secondaryText.isNullOrEmpty() || item.tertiaryText.isNullOrEmpty())

                holder.tvIndex?.text = (position + 1).toString()

                holder.ivImage?.let { imageView ->
                    val placeholder = if (itemViewType == VIEW_TYPE_VERTICAL)
                        R.drawable.placeholder_option_2
                    else
                        R.drawable.placeholder_option_3
                    Glide.with(imageView)
                        .load(item.optionImageUrl ?: "")
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(placeholder)
                        .error(placeholder)
                        .into(imageView)
                }
            }
        }
    }

    private class ListItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPrimary: TextView = itemView.findViewById(R.id.primary_text)
        val tvSecondary: TextView = itemView.findViewById(R.id.secondary_text)
        val tvTertiary: TextView = itemView.findViewById(R.id.tertiary_text)
        val tvIndex: TextView? = itemView.findViewById(R.id.tv_index)
        val ivImage: ImageView? = itemView.findViewById(R.id.iv_image)
        val secondaryContainer: View? = itemView.findViewById(R.id.secondary_container)
    }

    interface OnElementSelectedListener {
        fun onElementSelected(
            parent: OptionTemplateView,
            itemView: View,
            position: Int,
            element: OptionElement
        )
    }

    data class OptionElement(
        @SerializedName(Constant.PAYLOAD_ELEMENT_ID) val elementId: String?,
        @SerializedName(Constant.PAYLOAD_PRIMARY_TEXT) val primaryText: String?,
        @SerializedName(Constant.PAYLOAD_SECONDARY_TEXT) val secondaryText: String?,
        @SerializedName(Constant.PAYLOAD_TERTIARY_TEXT) val tertiaryText: String?,
        @SerializedName(Constant.PAYLOAD_OPTION_IMAGE_URL) val optionImageUrl: String?
    )

    class Builder(private val context: Context) {
        private var payload: String? = null
        private var onClickListener: OnClickListener? = null
        private var onElementSelectedListener: OnElementSelectedListener? = null

        fun payload(payload: String): Builder {
            this.payload = payload
            return this
        }

        fun onClickBackListener(onClickListener: OnClickListener?): Builder {
            this.onClickListener = onClickListener
            return this
        }

        fun onElementSelectedListener(onElementSelectedListener: OnElementSelectedListener?): Builder {
            this.onElementSelectedListener = onElementSelectedListener
            return this
        }

        fun build(): OptionTemplateView {
            val view = OptionTemplateView(context)
            payload?.let { payload ->
                view.updatePayload(payload)
            }
            onClickListener?.let {
                view.setOnClickBackListener(it)
            }
            view.onElementSelectedListener = onElementSelectedListener
            return view
        }
    }
}