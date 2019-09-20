package com.iflytek.cyber.iot.show.core.widget

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.iflytek.cyber.iot.show.core.R

class StyledSwitch @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val trackPaint = Paint()
    private val thumbShadowPaint = Paint()
    private val thumbPaint = Paint()
    private val pressPaint = Paint()

    private var trackRectF = RectF()

    private var thumbSize = 0f
    private var trackSize = 0f
    private var pressSize = 0f

    private var trackShadowRadius = 0f
    private var trackShadowDy = 0f

    private var trackMargin = 0f
    private var thumbMargin = 0f

    private var thumbLeft = 0f

    private var progress = 0f
    private var pressProgress = 0f

    private var touchX = 0f
    private var touchY = 0f
    private var touchDownTime = 0L

    private var animator: Animator? = null
    private var pressAnimator: Animator? = null

    private val thumbColor = Color.WHITE
    private val thumbEnableColor = resources.getColor(R.color.setup_primary)
    private val trackColor = resources.getColor(R.color.tablet_grey_100)
    private val trackEnableColor = Color.argb(50, Color.red(thumbEnableColor),
        Color.green(thumbEnableColor), Color.blue(thumbEnableColor))
    private val shadowColor = Color.parseColor("#1C000000")
    private val shadowEnableColor = Color.parseColor("#500E65FF")

    private var onCheckedChangeListener: OnCheckedChangeListener? = null

    var isChecked = false
        set(value) {
            if (value) {
                animatingTo(1f)
            } else {
                animatingTo(0f)
            }
            onCheckedChangeListener?.onCheckedChange(this, value)
            field = value
        }

    init {
        isClickable = true
        isFocusable = true

        thumbPaint.isAntiAlias = true
        pressPaint.isAntiAlias = true
        trackPaint.isAntiAlias = true
        thumbShadowPaint.isAntiAlias = true

        thumbPaint.color = thumbColor
        trackPaint.color = trackColor

        pressPaint.color = Color.BLACK
        pressPaint.alpha = 10

        setLayerType(LAYER_TYPE_SOFTWARE, Paint())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val minimumWidth = resources.getDimensionPixelSize(R.dimen.dp_60)
        val minimumHeight = resources.getDimensionPixelSize(R.dimen.dp_42)

        setMeasuredDimension(
            measureSize(widthMeasureSpec, minimumWidth),
            measureSize(heightMeasureSpec, minimumHeight)
        )
    }

    private fun measureSize(measureSpec: Int, minWidth: Int): Int {
        val mode = MeasureSpec.getMode(measureSpec)
        val size = MeasureSpec.getSize(measureSpec)
        return when (mode) {
            MeasureSpec.AT_MOST -> minWidth
            MeasureSpec.EXACTLY -> size
            else -> minWidth
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        pressSize = h.toFloat()

        thumbSize = pressSize * 30 / 56

        trackShadowRadius = 5f / 24 * thumbSize
        trackShadowDy = 3f / 24 * thumbSize

        thumbMargin = (h - thumbSize) / 2
        trackMargin = thumbSize / 15

        trackSize = thumbSize - 2 * trackMargin

        trackRectF = RectF(trackMargin + thumbMargin, height / 2 - trackSize / 2,
            width - trackMargin - thumbMargin, height / 2 + trackSize / 2)
        updateProgress(progress, false)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return

        canvas.drawRoundRect(trackRectF, trackSize / 2, trackSize / 2, trackPaint)

        canvas.drawCircle(thumbLeft, height / 2f, pressProgress * pressSize / 2, pressPaint)

        canvas.drawCircle(thumbLeft, height / 2f, thumbSize / 2, thumbShadowPaint)

        canvas.drawCircle(thumbLeft, height / 2f, thumbSize / 2, thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (isEnabled) {
            parent.requestDisallowInterceptTouchEvent(true)
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchX = event.x
                    touchY = event.y

                    touchDownTime = System.currentTimeMillis()

                    showPress()
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isChecked) {
                        val offset = Math.min(0f, event.x - touchX)
                        val percent = Math.min(-offset / width, 1f)
                        updateProgress(1 - percent)
                    } else {
                        val offset = Math.max(0f, event.x - touchX)
                        val percent = Math.min(offset / width, 1f)
                        updateProgress(percent)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    dismissPress()

                    if (System.currentTimeMillis() - touchDownTime < 200
                        && Math.pow((event.x - touchX).toDouble(), 2.0)
                        + Math.pow((event.y - touchY).toDouble(), 2.0) < 20 * 20) {
                        isChecked = !isChecked

                        performClick()
                    } else {
                        isChecked = progress >= 0.5
                    }
                }

            }
            return true
        } else {
            return false
        }
    }

    private fun updateProgress(progress: Float, invalidate: Boolean = true) {
        this.progress = progress

        trackPaint.color = Color.rgb(
            ((1 - progress) * Color.red(trackColor)
                + progress * Color.red(trackEnableColor)).toInt(),
            ((1 - progress) * Color.green(trackColor)
                + progress * Color.green(trackEnableColor)).toInt(),
            ((1 - progress) * Color.blue(trackColor)
                + progress * Color.blue(trackEnableColor)).toInt()
        )
        trackPaint.alpha = (255 - 128 * progress).toInt()

        thumbPaint.color = Color.rgb(
            ((1 - progress) * Color.red(thumbColor)
                + progress * Color.red(thumbEnableColor)).toInt(),
            ((1 - progress) * Color.green(thumbColor)
                + progress * Color.green(thumbEnableColor)).toInt(),
            ((1 - progress) * Color.blue(thumbColor)
                + progress * Color.blue(thumbEnableColor)).toInt()
        )

        thumbShadowPaint.alpha = ((0.1f * (1 - progress) + 0.5f * progress) * 255).toInt()
        thumbShadowPaint.setShadowLayer(trackShadowRadius, 0f, trackShadowDy,
            Color.rgb(
                ((1 - progress) * Color.red(shadowColor)
                    + progress * Color.red(shadowEnableColor)).toInt(),
                ((1 - progress) * Color.green(shadowColor)
                    + progress * Color.green(shadowEnableColor)).toInt(),
                ((1 - progress) * Color.blue(shadowColor)
                    + progress * Color.blue(shadowEnableColor)).toInt()
            ))

        thumbLeft = thumbMargin + thumbSize / 2 +
            (width - thumbSize - 2 * thumbMargin) * progress

        if (invalidate)
            invalidate()
    }

    private fun showPress() {
        pressAnimator?.cancel()

        val animator = ValueAnimator.ofFloat(pressProgress, 1f)

        animator.addUpdateListener {
            pressProgress = it.animatedValue as Float

            invalidate()
        }
        animator.duration = (200 * Math.abs(1f - pressProgress)).toLong()
        animator.start()

        pressAnimator = animator
    }

    private fun dismissPress() {
        pressAnimator?.cancel()

        val animator = ValueAnimator.ofFloat(pressProgress, 0f)

        animator.addUpdateListener {
            pressProgress = it.animatedValue as Float

            invalidate()
        }
        animator.duration = (200 * Math.abs(0f - pressProgress)).toLong()
        animator.start()

        pressAnimator = animator
    }

    private fun animatingTo(target: Float) {
        this.animator?.cancel()

        val animator = ValueAnimator.ofFloat(progress, target)

        animator.addUpdateListener {
            val value = it.animatedValue as Float
            updateProgress(value)
        }
        animator.duration = (200 * Math.abs(target - progress)).toLong()
        animator.start()

        this.animator = animator
    }

    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        this.onCheckedChangeListener = listener
    }

    interface OnCheckedChangeListener {
        fun onCheckedChange(switch: StyledSwitch, isChecked: Boolean)
    }
}