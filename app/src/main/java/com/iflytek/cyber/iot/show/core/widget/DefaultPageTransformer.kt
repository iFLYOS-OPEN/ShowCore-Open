package com.iflytek.cyber.iot.show.core.widget

import android.view.View
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.iflytek.cyber.iot.show.core.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val MIN_SCALE = 0.95f
private const val MIN_ALPHA = 0.7f

class DepthPageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageWidth = width
            when {
                position < -1 -> { // [-Infinity,-1)
                    // This page is way off-screen to the left.
                    alpha = 0f
                }
                position <= 0 -> { // [-1,0]
                    // Use the default slide transition when moving to the left page
                    alpha = 1f
                    translationX = 0f
                    scaleX = 1f
                    scaleY = 1f
                }
                position <= 1 -> { // (0,1]
                    // Fade the page out.
                    alpha = 1 - position

                    // Counteract the default slide transition
                    translationX = pageWidth * -position

                    // Scale the page down (between MIN_SCALE and 1)
                    val scaleFactor = (MIN_SCALE + (1 - MIN_SCALE) * (1 - abs(position)))
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                }
                else -> { // (1,+Infinity]
                    // This page is way off-screen to the right.
                    alpha = 0f
                }
            }
        }
    }
}

class FadeInPageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageWidth = width
            when {
                position <= -1 -> { // [-Infinity,-1)
                    // This page is way off-screen to the left.
                    alpha = 0f
                    translationX = 0f
                }
                position <= 0 -> { // [-1,0]
                    // Use the default slide transition when moving to the left page
                    alpha = max(1f + 2 * position, 0f)

                    translationX = pageWidth * -position
                }
                position < 1 -> { // (0,1)
                    alpha = max(1f - 2 * position, 0f)

                    translationX = pageWidth * -position
                }
                else -> { // [1,+Infinity]
                    alpha = 0f
                    translationX = 0f
                }
            }
        }
    }
}

class ZoomOutPageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageWidth = width
            val pageHeight = height
            when {
                position < -1 -> { // [-Infinity,-1)
                    // This page is way off-screen to the left.
                    alpha = 0f
                }
                position <= 1 -> { // [-1,1]
                    // Modify the default slide transition to shrink the page as well
                    val scaleFactor = max(MIN_SCALE, 1 - abs(position))
                    val vertMargin = pageHeight * (1 - scaleFactor) / 2
                    val horzMargin = pageWidth * (1 - scaleFactor) / 2
                    translationX = if (position < 0) {
                        horzMargin - vertMargin / 2
                    } else {
                        horzMargin + vertMargin / 2
                    }

                    // Scale the page down (between MIN_SCALE and 1)
                    scaleX = scaleFactor
                    scaleY = scaleFactor

                    // Fade the page relative to its size.
                    alpha = (MIN_ALPHA +
                        (((scaleFactor - MIN_SCALE) / (1 - MIN_SCALE)) * (1 - MIN_ALPHA)))
                }
                else -> { // (1,+Infinity]
                    // This page is way off-screen to the right.
                    alpha = 0f
                }
            }
        }
    }
}

class CustomPageTransformer(private val pager: ViewPager2) : ViewPager2.PageTransformer, ViewPager2.OnPageChangeCallback() {
    private var currentItem = 0

    init {
        pager.registerOnPageChangeCallback(this)
    }

    override fun transformPage(view: View, position: Float) {
        view.apply {
            val tvTitle: TextView? = view.findViewById(R.id.title)
            val tvSummary: TextView? = view.findViewById(R.id.summary)
            val recommendContainer: View? = view.findViewById(R.id.banner_container)

            val pageWidth = width
            val currentPosition = recommendContainer?.tag as? Int

            val isEnter = currentItem != currentPosition
            when {
                position <= -1 -> { // [-Infinity,-1)
                    // This page is way off-screen to the left.
                    alpha = 0f
                    translationX = 0f
                }
                position <= 0 -> { // [-1,0]
                    // Use the default slide transition when moving to the left page
                    if (isEnter) {
                        alpha = max(0f, position * 4 + 1)

                        // Counteract the default slide transition
                        translationX = -pageWidth * position

                        tvTitle?.translationX = pageWidth / 4 * position
                        tvSummary?.translationX = pageWidth / 4 * position
                    } else {
                        alpha = max(0f, position * 4 + 1)

                        translationX = pageWidth / 4 * -position * 3
                    }
                }
                position < 1 -> { // (0,1)
                    if (isEnter) {
                        alpha = max(0f, 1 - position * 4)

                        // Counteract the default slide transition
                        translationX = -pageWidth * position

                        tvTitle?.translationX = pageWidth / 4 * position
                        tvSummary?.translationX = pageWidth / 4 * position
                    } else {
                        alpha = max(0f, 1 - position * 4)

                        translationX = pageWidth / 4 * -position * 3
                    }
                }
                else -> { // [1,+Infinity]
                    alpha = 0f
                    translationX = 0f
                }
            }
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        super.onPageScrolled(position, positionOffset, positionOffsetPixels)
        if (positionOffset == 0f) {
            currentItem = position
        }
    }

    fun destroy() {
        pager.unregisterOnPageChangeCallback(this)
    }
}