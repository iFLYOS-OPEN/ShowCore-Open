package com.iflytek.cyber.iot.show.core.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import com.iflytek.cyber.iot.show.core.R

/**
 * fork from https://github.com/lijiankun24/ShadowLayout
 *
 */

class ShadowLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : RelativeLayout(context, attrs, defStyleAttr) {

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val mRectF = RectF()

    /**
     * 阴影的颜色
     */
    private var mShadowColor = Color.TRANSPARENT

    /**
     * 是否使用 shadowColor 作为 paint 的 颜色（color）
     */
    private var mUseShadowColor = false

    private var mShadowPaddingTop = 0f
    private var mShadowPaddingLeft = 0f
    private var mShadowPaddingRight = 0f
    private var mShadowPaddingBottom = 0f

    /**
     * 阴影的大小范围
     */
    private var mShadowRadius = 0f

    /**
     * 阴影的圆角大小
     */
    private var mShadowCornerRadius = 0f

    /**
     * 阴影 x 轴的偏移量
     */
    private var mShadowDx = 0f

    /**
     * 阴影 y 轴的偏移量
     */
    private var mShadowDy = 0f

    /**
     * 阴影显示的边界
     */
    private var mShadowSide = ALL

    /**
     * 阴影的形状，圆形/矩形
     */
    private var mShadowShape = SHAPE_RECTANGLE

    init {
        init(attrs)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var rectLeft = 0f
        var rectTop = 0f
        var rectRight = this.measuredWidth.toFloat()
        var rectBottom = this.measuredHeight.toFloat()
        this.width
        if (mShadowSide and LEFT == LEFT) {
            rectLeft = mShadowPaddingLeft
        }
        if (mShadowSide and TOP == TOP) {
            rectTop = mShadowPaddingTop
        }
        if (mShadowSide and RIGHT == RIGHT) {
            rectRight = this.measuredWidth - mShadowPaddingRight
        }
        if (mShadowSide and BOTTOM == BOTTOM) {
            rectBottom = this.measuredHeight - mShadowPaddingBottom
        }
        if (mShadowDy != 0.0f) {
            rectBottom -= mShadowDy
        }
        if (mShadowDx != 0.0f) {
            rectRight -= mShadowDx
        }
        mRectF.left = rectLeft
        mRectF.top = rectTop
        mRectF.right = rectRight
        mRectF.bottom = rectBottom
    }

    /**
     * 真正绘制阴影的方法
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isInEditMode) {
            setUpShadowPaint()
            if (mShadowShape == SHAPE_RECTANGLE) {
                if (mShadowCornerRadius <= 0)
                    canvas.drawRect(mRectF, mPaint)
                else
                    canvas.drawRoundRect(mRectF, mShadowCornerRadius, mShadowCornerRadius, mPaint)
            } else if (mShadowShape == SHAPE_OVAL) {
                canvas.drawCircle(mRectF.centerX(), mRectF.centerY(), Math.min(mRectF.width(), mRectF.height()) / 2, mPaint)
            }
        }
    }

    @Suppress("unused")
    fun setShadowColor(shadowColor: Int) {
        mShadowColor = shadowColor
        requestLayout()
        postInvalidate()
    }

    @Suppress("unused")
    fun setShadowRadius(shadowRadius: Float) {
        mShadowRadius = shadowRadius
        requestLayout()
        postInvalidate()
    }

    /**
     * 读取设置的阴影的属性
     *
     * @param attrs 从其中获取设置的值
     */
    private fun init(attrs: AttributeSet?) {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)  // 关闭硬件加速
        this.setWillNotDraw(false)                    // 调用此方法后，才会执行 onDraw(Canvas) 方法

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ShadowLayout)
        if (typedArray != null) {
            mShadowColor = typedArray.getColor(R.styleable.ShadowLayout_shadowColor, Color.BLACK)
            mShadowRadius = typedArray.getDimension(R.styleable.ShadowLayout_shadowRadius, dip2px(0f))
            mShadowCornerRadius = typedArray.getDimension(R.styleable.ShadowLayout_shadowCornerRadius, dip2px(0f))
            mShadowPaddingLeft = typedArray.getDimension(R.styleable.ShadowLayout_shadowPaddingLeft, dip2px(5f) + mShadowRadius)
            mShadowPaddingRight = typedArray.getDimension(R.styleable.ShadowLayout_shadowPaddingRight, dip2px(5f) + mShadowRadius)
            mShadowPaddingTop = typedArray.getDimension(R.styleable.ShadowLayout_shadowPaddingTop, dip2px(5f) + mShadowRadius)
            mShadowPaddingBottom = typedArray.getDimension(R.styleable.ShadowLayout_shadowPaddingBottom, dip2px(5f) + mShadowRadius)
            mShadowDx = typedArray.getDimension(R.styleable.ShadowLayout_shadowDx, dip2px(0f))
            mShadowDy = typedArray.getDimension(R.styleable.ShadowLayout_shadowDy, dip2px(0f))
            mShadowSide = typedArray.getInt(R.styleable.ShadowLayout_shadowSide, ALL)
            mShadowShape = typedArray.getInt(R.styleable.ShadowLayout_shadowShape, SHAPE_RECTANGLE)
            mUseShadowColor = typedArray.getBoolean(R.styleable.ShadowLayout_useShadowColor, false)
            typedArray.recycle()
        }
        setUpShadowPaint()
    }

    private fun setUpShadowPaint() {
        mPaint.reset()
        mPaint.isAntiAlias = true
        if (mUseShadowColor) {
            mPaint.color = mShadowColor
        } else {
            mPaint.color = Color.TRANSPARENT
        }
        if (!isInEditMode) {
            mPaint.setShadowLayer(if (mShadowRadius >= 0) mShadowRadius else 0f, mShadowDx, mShadowDy, mShadowColor)
        }
    }

    /**
     * dip2px dp 值转 px 值
     *
     * @param dpValue dp 值
     * @return px 值
     */
    private fun dip2px(dpValue: Float): Float {
        val dm = context.resources.displayMetrics
        val scale = dm.density
        return dpValue * scale + 0.5f
    }

    companion object {

        const val ALL = 0x1111

        const val LEFT = 0x0001

        const val TOP = 0x0010

        const val RIGHT = 0x0100

        const val BOTTOM = 0x1000

        const val SHAPE_RECTANGLE = 0x0001

        const val SHAPE_OVAL = 0x0010
    }
}
