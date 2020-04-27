package com.iflytek.cyber.iot.show.core.template

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.gson.JsonParser
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation

class BodyTemplateView3 @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val bodyText: TextView
    private val bodySubText: TextView
    private val skillIconImage: ImageView
    private val backgroundImage: ImageView
    private val mainTitle: TextView

    private var currentPayload: String? = null

    private val innerOnClickBackListener = OnClickListener { v ->
        onClickBackListener?.onClick(v)
    }
    private var onClickBackListener: OnClickListener? = null

    init {
        val childView = LayoutInflater.from(context).inflate(R.layout.layout_body_template_3, null)

        bodyText = childView.findViewById(R.id.body_text)
        bodySubText = childView.findViewById(R.id.body_sub_text)
        skillIconImage = childView.findViewById(R.id.skill_icon)
        backgroundImage = childView.findViewById(R.id.background_image)
        mainTitle = childView.findViewById(R.id.main_title)

        childView.findViewById<View>(R.id.back)?.setOnClickListener(innerOnClickBackListener)

        addView(childView)
    }

    fun updatePayload(payload: String) {
        val json = JsonParser().parse(payload).asJsonObject

        mainTitle.text = json.get(Constant.PAYLOAD_MAIN_TITLE)?.asString

        if (json.has(Constant.PAYLOAD_BODY_TEXT)) {
            bodyText.isVisible = true
            bodyText.text = json.get(Constant.PAYLOAD_BODY_TEXT)?.asString
        } else {
            bodyText.isVisible = false
        }
        json.get(Constant.PAYLOAD_BODY_SUB_TEXT)?.asString?.let {
            if(it.isNotEmpty()){
                bodySubText.text = it
                bodySubText.visibility = View.VISIBLE
            } else  {
                bodySubText.visibility = View.GONE
            }
        }?:run {
            bodySubText.visibility = View.GONE
        }

        json.get(Constant.PAYLOAD_BACK_BUTTON)?.asString?.let { backButton ->
            when (backButton) {
                Constant.BACK_BUTTON_HIDDEN -> {
                    findViewById<View>(R.id.back_icon).visibility = View.GONE
                    findViewById<View>(R.id.back)?.isClickable = false
                }
                else -> {
                    findViewById<View>(R.id.back_icon).visibility = View.VISIBLE
                    findViewById<View>(R.id.back)?.isClickable = true
                }
            }
        } ?: run {
            findViewById<View>(R.id.back_icon).visibility = View.VISIBLE
            findViewById<View>(R.id.back)?.isClickable = true
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

        fun build(): BodyTemplateView3 {
            val view = BodyTemplateView3(context)
            payload?.let { payload ->
                view.updatePayload(payload)
            }
            view.setOnClickBackListener(onClickListener)
            return view
        }
    }
}