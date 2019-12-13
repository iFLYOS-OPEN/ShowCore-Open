package com.iflytek.cyber.iot.show.core.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.customview.widget.ViewDragHelper
import com.iflytek.cyber.iot.show.core.R

class ControlPanel @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var panel: View? = null
    private var panelBackground: View? = null
    var onReleaseCallback: OnReleaseCallback? = null
    var onInterceptTouchListener: OnTouchListener? = null
    private val mDragger: ViewDragHelper =
        ViewDragHelper.create(this, 1f, object : ViewDragHelper.Callback() {
            private var currentY = 0
            private var currentX = 0

            override fun onViewPositionChanged(
                changedView: View,
                left: Int,
                top: Int,
                dx: Int,
                dy: Int
            ) {
                super.onViewPositionChanged(changedView, left, top, dx, dy)
                currentX = left
                currentY = top
            }

            override fun tryCaptureView(child: View, pointerId: Int): Boolean {
                return panel == child
            }

            override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
                return 0
            }

            override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
                return Math.min(top, 0)
            }

            override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
                super.onViewReleased(releasedChild, xvel, yvel)

                if (releasedChild == panel) {
                    onReleaseCallback?.onRelease(releasedChild, currentX, currentY)?.let { result ->
                        if (!result) {
                            getDragger().settleCapturedViewAt(0, 0)
                            invalidate()
                        }
                    }
                }
            }

            override fun getViewHorizontalDragRange(child: View): Int {
                return measuredWidth - child.measuredWidth
            }

            override fun getViewVerticalDragRange(child: View): Int {
                return measuredHeight - child.measuredHeight
            }
        })

    init {
        mDragger.setEdgeTrackingEnabled(ViewDragHelper.EDGE_TOP)
    }

    private fun getDragger() = mDragger

    override fun onFinishInflate() {
        super.onFinishInflate()

        panel = findViewById(R.id.panel_area)
        panelBackground = findViewById(R.id.panel_background)
    }

    override fun computeScroll() {
        if (mDragger.continueSettling(true)) {
            invalidate()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        onInterceptTouchListener?.onTouch(this, ev)
        return mDragger.shouldInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        mDragger.processTouchEvent(event)
        return true
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                onReleaseCallback?.onRequestRelease()
            }
        }
        return super.dispatchKeyEvent(event)
    }

    interface OnReleaseCallback {
        fun onRelease(panel: View?, x: Int, y: Int): Boolean
        fun onRequestRelease()
    }
}