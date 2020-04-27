package com.iflytek.cyber.iot.show.core.widget


import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.iot.show.core.R

/**
 * DividerItemDecoration is a [RecyclerView.ItemDecoration] that can be used as a divider
 * between result of a [LinearLayoutManager]. It supports both [HORIZONTAL] and
 * [VERTICAL] orientations.
 *
 * <pre>
 * mDividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
 * mLayoutManager.getOrientation());
 * recyclerView.addItemDecoration(mDividerItemDecoration);
</pre> *
 */
class DividerItemDecoration
/**
 * Creates a divider [RecyclerView.ItemDecoration] that can be used with a
 * [LinearLayoutManager].
 *
 * @param context Current context, it will be used to access resources.
 * @param orientation Divider orientation. Should be [HORIZONTAL] or [VERTICAL].
 */
(context: Context, orientation: Int) : RecyclerView.ItemDecoration() {

    private var mDivider: Drawable? = null

    /**
     * Current orientation. Either [HORIZONTAL] or [VERTICAL].
     */
    private var mOrientation: Int = 0

    private val mBounds = Rect()

    private var padding: Rect? = null

    var drawable: Drawable?
        /**
         * @return the [Drawable] for this divider.
         */
        get() = mDivider
        /**
         * Sets the [Drawable] for this divider.
         *
         * @param drawable Drawable that should be used as a divider.
         */
        set(drawable) {
            if (drawable == null) {
                throw IllegalArgumentException("Drawable cannot be null.")
            }
            mDivider = drawable
        }

    init {
        val a = context.obtainStyledAttributes(ATTRS)
        mDivider = a.getDrawable(0)
        if (mDivider == null) {
            Log.w(TAG, "@android:attr/listDivider was not set in the theme used for this " + "DividerItemDecoration. Please set that attribute all call setDrawable()")
        }
        a.recycle()

        setOrientation(orientation)
    }

    /**
     * Sets the orientation for this divider. This should be called if
     * [RecyclerView.LayoutManager] changes orientation.
     *
     * @param orientation [HORIZONTAL] or [VERTICAL]
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun setOrientation(orientation: Int) {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw IllegalArgumentException(
                    "Invalid orientation. It should be either HORIZONTAL or VERTICAL")
        }
        mOrientation = orientation
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.layoutManager == null || mDivider == null) {
            return
        }
        if (mOrientation == VERTICAL) {
            drawVertical(c, parent)
        } else {
            drawHorizontal(c, parent)
        }
    }

    private fun drawVertical(canvas: Canvas, parent: RecyclerView) {
        canvas.save()
        mDivider?.let { divider ->
            val left: Int
            val right: Int

            val paddingLeft = padding?.left ?: 0
            val paddingRight = padding?.right ?: 0

            if (parent.clipToPadding) {
                left = parent.paddingLeft + paddingLeft
                right = parent.width - parent.paddingRight - paddingRight
                canvas.clipRect(left, parent.paddingTop, right,
                        parent.height - parent.paddingBottom)
            } else {
                left = paddingLeft
                right = parent.width - paddingRight
            }

            val childCount = parent.childCount
            for (i in 0 until childCount) {
                val child = parent.getChildAt(i)
                parent.getDecoratedBoundsWithMargins(child, mBounds)
                val bottom = mBounds.bottom + Math.round(child.translationY)
                val top = bottom - divider.intrinsicHeight
                divider.setBounds(left, top, right, bottom)
//                Log.d(TAG, "draw($left, $top, $right, $bottom)")
                divider.draw(canvas)
            }
        }
        canvas.restore()
    }

    private fun drawHorizontal(canvas: Canvas, parent: RecyclerView) {
        canvas.save()
        mDivider?.let { divider ->
            val top: Int
            val bottom: Int

            val paddingTop = padding?.top ?: 0
            val paddingBottom = padding?.bottom ?: 0

            if (parent.clipToPadding) {
                top = parent.paddingTop + paddingTop
                bottom = parent.height - parent.paddingBottom - paddingBottom
                canvas.clipRect(parent.paddingLeft, top,
                        parent.width - parent.paddingRight, bottom)
            } else {
                top = paddingTop
                bottom = parent.height - paddingBottom
            }

            val childCount = parent.childCount
            for (i in 0 until childCount) {
                val child = parent.getChildAt(i)
                parent.layoutManager?.getDecoratedBoundsWithMargins(child, mBounds)
                val right = mBounds.right + Math.round(child.translationX)
                val left = right - divider.intrinsicWidth
                divider.setBounds(left, top, right, bottom)
//                Log.d(TAG, "draw($left, $top, $right, $bottom)")
                divider.draw(canvas)
            }
        }
        canvas.restore()
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView,
                                state: RecyclerView.State) {
        mDivider?.let { divider ->
            if (mOrientation == VERTICAL) {
                outRect.set(0, 0, 0, divider.intrinsicHeight)
            } else {
                outRect.set(0, 0, divider.intrinsicWidth, 0)
            }
        } ?: run {
            outRect.set(0, 0, 0, 0)
            return
        }
    }

    companion object {
        const val HORIZONTAL = LinearLayout.HORIZONTAL
        const val VERTICAL = LinearLayout.VERTICAL

        private const val TAG = "DividerItem"
        private val ATTRS = intArrayOf(android.R.attr.listDivider)
    }

    @Suppress("unused")
    class Builder(private val context: Context) {
        private var orientation = LinearLayoutManager.VERTICAL
        private val padding = Rect()
        private var dividerColor: Int? = null
        private var dividerWidth = context.resources.getDimensionPixelSize(R.dimen.dp_1)

        fun setOrientation(orientation: Int): Builder {
            this.orientation = orientation
            return this
        }

        fun setPadding(padding: Int): Builder {
            this.padding.top = padding
            this.padding.bottom = padding
            this.padding.left = padding
            this.padding.right = padding
            return this
        }

        fun setPaddingLeft(padding: Int): Builder {
            this.padding.left = padding
            return this
        }

        fun setPaddingRight(padding: Int): Builder {
            this.padding.right = padding
            return this
        }

        fun setPaddingTop(padding: Int): Builder {
            this.padding.top = padding
            return this
        }

        fun setPaddingBottom(padding: Int): Builder {
            this.padding.bottom = padding
            return this
        }

        fun setDividerColor(dividerColor: Int): Builder {
            this.dividerColor = dividerColor
            return this
        }

        fun setDividerWidth(width: Int): Builder {
            this.dividerWidth = width
            return this
        }

        fun build(): DividerItemDecoration {
            val decoration = DividerItemDecoration(context, orientation)
            dividerColor?.let { color ->
                val drawable = GradientDrawable()
                drawable.setColor(color)
                if (orientation == LinearLayoutManager.VERTICAL) {
                    drawable.setSize(0, dividerWidth)
                } else {
                    drawable.setSize(dividerWidth, 0)
                }
                decoration.drawable = drawable
            }
            decoration.padding = padding
            return decoration
        }
    }
}
