package com.iflytek.cyber.iot.show.core.widget


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.iflytek.cyber.iot.show.core.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class BoxedVertical : View {

    /**
     * The min value of progress value.
     */
    private var mMin = MIN

    /**
     * The Maximum value that this SeekArc can be set to
     */
    private var mMax = MAX

    var isEnable = true

    /**
     * mTouchDisabled touches will not move the slider
     * only swipe motion will activate it
     */
    private var mTouchDisabled = true

    private var mProgressSweep = 0f
    private var mProgressPaint: Paint? = null
    private var mOnValuesChangeListener: OnValuesChangeListener? = null
    private var customBackgroundColor: Int = 0
    private var mDefaultValue: Int = 0
    private val mPaint = Paint()
    private var mPath: Path? = null

    var value: Int = 0
        private set

    var isPreventTouch = false
    var max: Int = MAX
        get() = mMax
        set(value) {
            if (value <= mMin)
                throw IllegalArgumentException("Max should not be less than zero")
            this.mMax = value
            field = value
        }
    var min: Int = MIN
        get() = mMin
        set(value) {
            if (value >= max)
                throw IllegalArgumentException("Min should not be bigger than max value")
            mMin = value
            field = value
        }

    /**
     * The corner radius of the view.
     */
    var cornerRadius: Int = 10
        set(value) {
            field = value

            if (width > 0 && height > 0) {
                mPath = Path()
                mPath?.addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()),
                    field.toFloat(), field.toFloat(), Path.Direction.CCW)

                invalidate()
            }
        }

    var defaultValue: Int
        get() = mDefaultValue
        set(mDefaultValue) {
            if (mDefaultValue > mMax)
                throw IllegalArgumentException("Default value should not be bigger than max value.")
            this.mDefaultValue = mDefaultValue

        }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        isClickable = true
        isFocusable = true

        // Defaults, may need to link this into theme settings
        var progressColor = ContextCompat.getColor(context, R.color.slide_progress)
        customBackgroundColor = ContextCompat.getColor(context, R.color.slide_background)
        customBackgroundColor = ContextCompat.getColor(context, R.color.slide_background)

        mDefaultValue = mMax / 2

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs,
                R.styleable.BoxedVertical, 0, 0)

            mMax = a.getInteger(R.styleable.BoxedVertical_bv_max, mMax)
            mMin = a.getInteger(R.styleable.BoxedVertical_bv_min, mMin)

            mDefaultValue = a.getInteger(R.styleable.BoxedVertical_bv_defaultValue, mDefaultValue)
            value = a.getInteger(R.styleable.BoxedVertical_bv_points, mDefaultValue)
            cornerRadius = a.getDimensionPixelSize(R.styleable.BoxedVertical_bv_cornerRadius, cornerRadius)

            progressColor = a.getColor(R.styleable.BoxedVertical_bv_progressColor, progressColor)
            customBackgroundColor = a.getColor(R.styleable.BoxedVertical_bv_backgroundColor, customBackgroundColor)

            isEnable = a.getBoolean(R.styleable.BoxedVertical_bv_enabled, isEnable)
            mTouchDisabled = a.getBoolean(R.styleable.BoxedVertical_bv_touchDisabled, mTouchDisabled)

            a.recycle()
        }

        // range check
        value = if (value > mMax) mMax else value
        value = if (value < mMin) mMin else value

        val progressPaint = Paint()
        progressPaint.color = progressColor
        progressPaint.isAntiAlias = true
        progressPaint.style = Paint.Style.STROKE
        mProgressPaint = progressPaint

        mPaint.alpha = 255
        mPaint.color = customBackgroundColor
        mPaint.isAntiAlias = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mProgressPaint?.strokeWidth = h.toFloat()

        mPath = Path()
        mPath?.addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()),
            cornerRadius.toFloat(), cornerRadius.toFloat(), Path.Direction.CCW)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.translate(0f, 0f)
        mPath?.let { path ->
            canvas.clipPath(path)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), mPaint)

        mProgressPaint?.let { paint ->
            if (isInEditMode) {
                val progress = getPointsByValue(defaultValue)
                //convert min-max range to progress
                val sweep = ((progress - mMin) * width / (mMax - mMin)).toFloat()

                canvas.drawLine((width / 2).toFloat(), height.toFloat(),
                    (width / 2).toFloat(), sweep, paint)
            } else {
                canvas.drawLine((width / 2).toFloat(), height.toFloat(),
                    (width / 2).toFloat(), mProgressSweep, paint)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isEnable) {
            this.parent.requestDisallowInterceptTouchEvent(true)

            if (!isPreventTouch)
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        mOnValuesChangeListener?.onStartTrackingTouch(this)
                        if (!mTouchDisabled)
                            updateOnTouch(event)
                    }
                    MotionEvent.ACTION_MOVE -> updateOnTouch(event)
                    MotionEvent.ACTION_UP -> {
                        updateOnTouch(event)
                        mOnValuesChangeListener?.onStopTrackingTouch(this)
                        isPressed = false
                        this.parent.requestDisallowInterceptTouchEvent(false)
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        mOnValuesChangeListener?.onStopTrackingTouch(this)
                        isPressed = false
                        this.parent.requestDisallowInterceptTouchEvent(false)
                    }
                }
            return true
        }
        return false
    }

    /**
     * Update the UI components on touch events.
     *
     * @param event MotionEvent
     */
    private fun updateOnTouch(event: MotionEvent) {
        isPressed = true
        val mTouch = convertTouchEventPoint(event.y)
        val progress = mTouch.roundToInt()
        updateProgress(height - progress)
    }

    private fun convertTouchEventPoint(yPos: Float): Float {
        return min(height.toFloat(), max(0f, yPos))
    }

    private fun updateProgress(progress: Int) {
        val realProgress = 1f * max(0, min(height, progress))

        mProgressSweep = realProgress
        //reverse value because progress is descending
        mProgressSweep = height - mProgressSweep

        //convert progress to min-max range
        value = (realProgress / height * (mMax - mMin) + mMin).toInt()

        mOnValuesChangeListener?.onPointsChanged(this, value, true)

        if (width > 0 && height > 0) {
            mPath = Path()
            mPath?.addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()),
                cornerRadius.toFloat(), cornerRadius.toFloat(), Path.Direction.CCW)

            invalidate()
        }
    }

    /**
     * Gets a value, converts it to progress for the seekBar and updates it.
     * @param value The value given
     */
    private fun updateProgressByValue(value: Int, fromTouch: Boolean = true) {
        //convert min-max range to progress
        mProgressSweep = ((value - mMin) * height / (mMax - mMin)).toFloat()
        //reverse value because progress is descending
        mProgressSweep = height - mProgressSweep

        mOnValuesChangeListener?.onPointsChanged(this, value, fromTouch)

        if (width > 0 && height > 0 && mPath == null) {
            mPath = Path()
            mPath?.addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()),
                cornerRadius.toFloat(), cornerRadius.toFloat(), Path.Direction.CCW)
        }

        invalidate()
    }

    fun setValue(value: Int) {
        this.value = value

        updateProgressByValue(getPointsByValue(value), false)
    }

    private fun getPointsByValue(value: Int): Int {
        return max(mMin, min(mMax, value))
    }

    interface OnValuesChangeListener {
        /**
         * Notification that the point value has changed.
         *
         * @param boxedPoints The SwagPoints view whose value has changed
         * @param points     The current point value.
         * @param fromTouch     Is this callback from touch event
         */
        fun onPointsChanged(boxedPoints: BoxedVertical, points: Int, fromTouch: Boolean)

        fun onStartTrackingTouch(boxedPoints: BoxedVertical)
        fun onStopTrackingTouch(boxedPoints: BoxedVertical)
    }

    fun setOnBoxedPointsChangeListener(onValuesChangeListener: OnValuesChangeListener) {
        mOnValuesChangeListener = onValuesChangeListener
    }

    companion object {
        private val TAG = BoxedVertical::class.java.simpleName

        private const val MAX = 100
        private const val MIN = 0
    }
}