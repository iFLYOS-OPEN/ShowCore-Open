package com.iflytek.cyber.iot.show.core.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatDrawableManager
import androidx.core.content.ContextCompat
import com.iflytek.cyber.iot.show.core.R
import kotlin.math.max
import kotlin.math.min

class BatteryView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val backgroundPaint = Paint()
    var isCharging = false
        set(value) {
            field = value
            if (width != 0 && height != 0)
                invalidate()
        }
    var level: Int = 0
        set(value) {
            field = max(0, min(value, 100))
            if (width != 0 && height != 0) {
                makeBackgroundRectF(width, height)
                invalidate()
            }
        }
    var contentPadding = 0
        set(value) {
            field = value
            if (width != 0 && height != 0)
                invalidate()
        }
    var lowPowerColor = Color.RED
        set(value) {
            field = value
            if (width != 0 && height != 0)
                invalidate()
        }
    var lowPowerLimit = 20
        set(value) {
            field = max(0, min(100, value))
            if (width != 0 && height != 0)
                invalidate()
        }
    var contentColor = Color.WHITE
        set(value) {
            field = value
            if (width != 0 && height != 0)
                invalidate()
        }
    var borderColor = Color.WHITE
        set(value) {
            field = value
            if (width != 0 && height != 0)
                invalidate()
        }
    private var backgroundRectF = RectF()

    init {
        val typedArray = context.obtainStyledAttributes(
            attrs, R.styleable.BatteryView, 0, 0
        )

        contentPadding = typedArray.getDimensionPixelSize(
            R.styleable.BatteryView_contentPadding, resources.getDimensionPixelSize(R.dimen.dp_1)
        )
        contentColor = typedArray.getColor(R.styleable.BatteryView_contentColor, Color.WHITE)
        borderColor = typedArray.getColor(R.styleable.BatteryView_borderColor, contentColor)
        lowPowerColor = typedArray.getColor(
            R.styleable.BatteryView_lowPowerColor,
            ContextCompat.getColor(context, R.color.tablet_red)
        )
        lowPowerLimit = typedArray.getInt(R.styleable.BatteryView_lowPowerLimit, lowPowerLimit)
        level = typedArray.getInt(R.styleable.BatteryView_level, 0)
        isCharging = typedArray.getBoolean(R.styleable.BatteryView_isCharging, false)

        backgroundPaint.color = contentColor
        backgroundPaint.isAntiAlias = true

        typedArray.recycle()

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        makeBackgroundRectF(w, h)
    }

    private fun makeBackgroundRectF(width: Int, height: Int) {
        val backgroundHeight = height * 9 / 24f
        val backgroundMaxWidth = width * 14.5f / 24f - 2 * contentPadding
        val percent = level / 100f
        val left = width / 24f * 3.5f
        backgroundRectF.set(
            left + contentPadding,
            height / 2 - backgroundHeight / 2f + contentPadding,
            left + contentPadding + percent * backgroundMaxWidth,
            height / 2 + backgroundHeight / 2f - contentPadding
        )
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return

        if (!isCharging) {
            val targetColor = if (level <= lowPowerLimit)
                lowPowerColor
            else
                contentColor
            if (backgroundPaint.color != targetColor) {
                backgroundPaint.color = targetColor
            }
            canvas.drawRoundRect(
                backgroundRectF,
                (backgroundRectF.bottom - backgroundRectF.top) / 8,
                (backgroundRectF.bottom - backgroundRectF.top) / 8,
                backgroundPaint
            )
        }

        val borderResId = R.drawable.ic_battery_border_white_24dp
        AppCompatDrawableManager.get().getDrawable(context, borderResId)?.apply {
            setColorFilter(borderColor, PorterDuff.Mode.SRC_IN)
            setBounds(0, 0, width, height)
            draw(canvas)
        }

        if (isCharging) {
            val thunderResId = R.drawable.ic_battery_thunder_black_24dp
            AppCompatDrawableManager.get().getDrawable(context, thunderResId)?.apply {
                setColorFilter(contentColor, PorterDuff.Mode.SRC_IN)
                setBounds(0, 0, width, height)
                draw(canvas)
            }
        }
    }

}