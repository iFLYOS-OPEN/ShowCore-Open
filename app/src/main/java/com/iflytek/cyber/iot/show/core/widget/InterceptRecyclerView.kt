package com.iflytek.cyber.iot.show.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.utils.dp2Px
import kotlin.math.abs

class InterceptRecyclerView(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs) {

    /*override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        if (childCount == 0) {
            return super.onInterceptTouchEvent(e)
        }
        var targetChild: View? = null
        val loc = IntArray(2)
        for (i in childCount - 1..0) {
            val childParent = getChildAt(i) as ViewGroup
            var child: View? = null
            if (childParent.childCount > 0) {
                child = childParent.getChildAt(0)
            }

            if (child == null) {
                return super.onInterceptTouchEvent(e)
            }

            child.getLocationInWindow(loc)

            if (e.x >= loc[0] && e.x <= loc[0] + child.measuredWidth &&
                e.y >= loc[1] && e.y <= loc[1] + child.measuredHeight
            ) {
                targetChild = child
                break
            }
        }

        Log.e("InterceptRecycler", "target child: " + targetChild.toString())

        if (targetChild != null && targetChild is ViewPager2) {
            return false
        }

        return super.onInterceptTouchEvent(e)
    }*/

    /*override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        if (childCount == 0) {
            return super.onInterceptTouchEvent(e)
        }
        val childParent = findChildViewUnder(e.x, e.y) as ViewGroup

        var targetChild: View? = null
        val loc = IntArray(2)
        if (childParent.childCount > 0) {
            var child: View? = null
            if (childParent.childCount > 0) {
                child = childParent.getChildAt(0)
            }

            if (child == null) {
                return super.onInterceptTouchEvent(e)
            }

            child.getLocationInWindow(loc)

            if (e.x >= loc[0] && e.x <= loc[0] + child.measuredWidth &&
                e.y >= loc[1] && e.y <= loc[1] + child.measuredHeight
            ) {
                targetChild = child
            }
        }

        if (targetChild != null && targetChild is ViewPager2) {
            return false
        }

        return super.onInterceptTouchEvent(e)
    }*/

    /*override fun onInterceptTouchEvent(e: MotionEvent?): Boolean {
        return false
    }*/

    private var threshold = 0

    private var touchX = 0f

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        val view = findChildViewUnder(e.x, e.y)
        if (view?.id != R.id.main_item) {
            return super.onInterceptTouchEvent(e)
        }
        val viewPager2 = (view as ViewGroup).findViewById<ViewPager2>(R.id.view_pager)
        if (e.x > 0 && e.x < viewPager2.measuredWidth && e.y > 0 && e.y < viewPager2.measuredHeight) {
            threshold = viewPager2.measuredWidth
        } else {
            threshold = 8.dp2Px()
        }
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                touchX = e.rawX
            }
            MotionEvent.ACTION_MOVE -> {
                val currentX = e.rawX
                val distance = abs(currentX - touchX)
                return if (distance > threshold) {
                    super.onInterceptTouchEvent(e)
                } else {
                    false
                }
            }
        }
        return super.onInterceptTouchEvent(e)
    }
}