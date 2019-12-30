package com.iflytek.cyber.iot.show.core.template

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewTreeObserver
import android.widget.*
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.JsonParser
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.widget.HighlightTextView

class BodyTemplateView1 @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val bodyText: HighlightTextView
    private val bodyImage: ImageView
    private val skillIconImage: ImageView
    private val backgroundImage: ImageView
    private val bodyContainer: LinearLayout
    private val bodyHorizontalContainer: LinearLayout
    private val bodyScrollView: ScrollView
    private val mainTitle: TextView
    private val subTitle: TextView
    private val bodyHorizontalImage: ImageView
    private val horizontalBodyText: HighlightTextView
    private val horizontalScrollView: ScrollView
    private val imageMaxHeight =
        resources.getDimensionPixelSize(R.dimen.body_template_image_max_height)

    private var currentPayload: String? = null

    private val innerOnClickBackListener = OnClickListener { v ->
        onClickBackListener?.onClick(v)
    }
    private var onClickBackListener: OnClickListener? = null

    init {
        val childView = LayoutInflater.from(context)
            .inflate(R.layout.layout_body_template_1, null)

        bodyContainer = childView.findViewById(R.id.body_vertical_container)
        bodyHorizontalContainer = childView.findViewById(R.id.body_horizontal_container)
        bodyScrollView = childView.findViewById(R.id.body_scroll_view)
        bodyText = childView.findViewById(R.id.body_text)
        bodyImage = childView.findViewById(R.id.body_image)
        bodyHorizontalImage = childView.findViewById(R.id.body_horizontal_image)
        skillIconImage = childView.findViewById(R.id.skill_icon)
        backgroundImage = childView.findViewById(R.id.background_image)
        mainTitle = childView.findViewById(R.id.main_title)
        subTitle = childView.findViewById(R.id.sub_title)
        horizontalBodyText = childView.findViewById(R.id.horizontal_body_text)
        horizontalScrollView = childView.findViewById(R.id.horizontal_scroll_view)

        bodyText.onHighlightChangeListener = object : HighlightTextView.OnHighlightChangeListener {
            override fun onHighlightChange(view: HighlightTextView, line: Int, offset: Int) {
                bodyScrollView.smoothScrollTo(0, offset)
            }
        }
        horizontalBodyText.onHighlightChangeListener =
            object : HighlightTextView.OnHighlightChangeListener {
                override fun onHighlightChange(view: HighlightTextView, line: Int, offset: Int) {
                    horizontalScrollView.smoothScrollTo(0, offset)
                }
            }
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

        bodyText.text = json.get(Constant.PAYLOAD_BODY_TEXT)?.asString
        horizontalBodyText.text = bodyText.text

        val imageUrl = json.get(Constant.PAYLOAD_BODY_IMAGE_URL)?.asString
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        val width = resource.width
                        val height = resource.height
                        if (width.toFloat() / height.toFloat() < 1) {
                            bodyScrollView.isVisible = false
                            bodyHorizontalContainer.isVisible = true
                            showHorizontalTemplate(imageUrl, resource)
                        } else {
                            bodyScrollView.isVisible = true
                            bodyHorizontalContainer.isVisible = false
                            showVerticalTemplate(imageUrl, resource)
                        }
                    }
                })
        } else {
            bodyContainer.isVisible = false
            bodyScrollView.isVisible = true
            bodyHorizontalContainer.isVisible = false
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

    private fun showVerticalTemplate(url: String, bitmap: Bitmap) {
        val transformer = MultiTransformation(
            CenterCrop(),
            RoundedCornersTransformation(
                context.resources.getDimensionPixelSize(R.dimen.dp_12), 0
            )
        )
        //bodyImage.setOriginalSize(bitmap.width, bitmap.height)
        Glide.with(context)
            .load(url)
            .transform(transformer)
            .override(bitmap.width, bitmap.height)
            .into(bodyImage)
    }

    private fun showHorizontalTemplate(url: String, bitmap: Bitmap) {
        val transformer = MultiTransformation(
            CenterCrop(),
            RoundedCornersTransformation(
                context.resources.getDimensionPixelSize(R.dimen.dp_12), 0
            )
        )
        Glide.with(context)
            .load(url)
            .transform(transformer)
            .override(bitmap.width, bitmap.height)
            .into(bodyHorizontalImage)
    }

    fun updateBodyPosition(position: Long) {
        bodyText.updatePosition(position)
        horizontalBodyText.updatePosition(position)
    }

    fun startBodyAnimation() {
        bodyText.startAnimation()
        horizontalBodyText.startAnimation()
        if (!(bodyText.text.isNullOrEmpty()) && bodyText.lineCount > 0) {
            bodyText.startAnimation()
        } else {
            bodyText.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (!(bodyText.text.isNullOrEmpty()) && bodyText.lineCount > 0) {
                        bodyText.startAnimation()
                        bodyText.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            })
        }
        if (!(horizontalBodyText.text.isNullOrEmpty()) && horizontalBodyText.lineCount > 0) {
            horizontalBodyText.startAnimation()
        } else {
            horizontalBodyText.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (!(horizontalBodyText.text.isNullOrEmpty()) && horizontalBodyText.lineCount > 0) {
                        horizontalBodyText.startAnimation()
                        horizontalBodyText.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            })
        }
    }

    fun isBodyAnimating() = bodyText.isAnimationStarted() || horizontalBodyText.isAnimationStarted()

    fun stopBodyAnimation() {
        post {
            bodyText.stopAnimation()
            horizontalBodyText.stopAnimation()
        }
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

        fun build(): BodyTemplateView1 {
            val view = BodyTemplateView1(context)
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