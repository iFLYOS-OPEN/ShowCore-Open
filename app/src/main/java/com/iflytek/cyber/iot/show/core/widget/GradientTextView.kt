package com.iflytek.cyber.iot.show.core.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.widget.TextView

class GradientTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TextView(context, attrs, defStyleAttr) {
    var startColor = Color.WHITE
        set(value) {
            field = value
            if (isGradient)
                paint.shader = currentShader()
            if (width > 0 || height > 0)
                text = text
        }
    var endColor = Color.WHITE
        set(value) {
            field = value
            if (isGradient)
                paint.shader = currentShader()
            if (width > 0 || height > 0)
                text = text
        }

    var isGradient = false
        set(value) {
            field = value
            if (!value) {
                paint.shader = null
            } else {
                paint.shader = currentShader()
            }
            if (width > 0 || height > 0)
                text = text
        }

    override fun onDraw(canvas: Canvas?) {
        if (isGradient && paint.shader !is LinearGradient)
            paint.shader = currentShader()
        super.onDraw(canvas)
    }

    private fun currentShader(): Shader {
        return LinearGradient(
            0f, 0f, width.toFloat(), 0f,
            intArrayOf(
                startColor,
                startColor,
                endColor
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }
}