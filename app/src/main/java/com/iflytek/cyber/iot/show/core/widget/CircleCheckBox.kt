package com.iflytek.cyber.iot.show.core.widget

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.iflytek.cyber.iot.show.core.R

class CircleCheckBox @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private val checkBoxView = LottieAnimationView(context)
    private val textView = AppCompatTextView(context)

    private var currentAnimator: ValueAnimator? = null

    var isChecked: Boolean = false
        set(value) {
            if (isChecked == value)
                return
            setCheckedVisible(value, isAttachedToWindow)
            field = value
            listener?.invoke(this, value)
        }

    var text: String = ""
        set(value) {
            textView.text = value
            field = value
        }
        get() {
            return textView.text.toString()
        }

    init {
        val checkBoxSize = resources.getDimensionPixelSize(R.dimen.dp_24)
        val checkBoxTextMargin = resources.getDimensionPixelSize(R.dimen.dp_8)

        setOnClickListener {
            isChecked = !isChecked
        }

        // get attribute
        val attr = context.obtainStyledAttributes(attrs, R.styleable.CircleCheckBox)

        val isChecked = attr.getBoolean(R.styleable.CircleCheckBox_isChecked, false)
        val text = attr.getString(R.styleable.CircleCheckBox_text)
        val textSize = attr.getDimensionPixelSize(R.styleable.CircleCheckBox_textSize, -1)
        val textInsetStart = attr.getDimensionPixelSize(R.styleable.CircleCheckBox_textInsetStart, -1)

        attr.recycle()

        val checkBoxOnLp = LayoutParams(checkBoxSize, checkBoxSize)
        checkBoxOnLp.gravity = Gravity.CENTER_VERTICAL
        checkBoxView.setAnimation(R.raw.animation_checkbox_1)
        addView(checkBoxView, checkBoxOnLp)

        val textLp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        textLp.leftMargin = checkBoxSize + (if (textInsetStart != -1) textInsetStart else checkBoxTextMargin)
        textLp.gravity = Gravity.CENTER_VERTICAL
        if (textSize != -1) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
        }
        textView.text = text
        textView.setTextColor(ContextCompat.getColor(context, R.color.semi_black))
        addView(textView, textLp)

        this.isChecked = isChecked
    }

    private var listener: ((CircleCheckBox, Boolean) -> Unit)? = null

    private fun setCheckedVisible(isChecked: Boolean, shouldAnimate: Boolean) {
        if (shouldAnimate) {
            currentAnimator = null

            val target = if (isChecked) 1f else 0f
            val animator = ValueAnimator.ofFloat(checkBoxView.progress, target)
            animator.addUpdateListener {
                val value = it.animatedValue as Float
                checkBoxView.progress = value
            }
            animator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {

                }

                override fun onAnimationEnd(animation: Animator?) {
                    currentAnimator = null
                }

                override fun onAnimationCancel(animation: Animator?) {
                    currentAnimator = null
                }

                override fun onAnimationStart(animation: Animator?) {

                }
            })
            animator.duration = 250
            animator.start()

            currentAnimator = animator
        } else {
            if (isChecked) {
                checkBoxView.progress = 1f
            } else
                checkBoxView.progress = 0f
        }
    }

    fun setOnCheckedChangeListener(listener: ((CircleCheckBox, Boolean) -> Unit)) {
        this.listener = listener
    }

    fun setText(@StringRes resId: Int) {
        textView.setText(resId)
    }
}