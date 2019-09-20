/*
 * Copyright (C) 2019 iFLYTEK CO.,LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iflytek.cyber.iot.show.core.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.text.Html
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.iflytek.cyber.iot.show.core.R


class HighlightTextView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : AppCompatTextView(context, attrs, defStyleAttr) {
    private val timestampArray = ArrayList<Long>()

    private val textArray = ArrayList<String>()

    private var currentHighlightLine = -1

    private var mHighlightColor = Color.BLACK
    private var mNormalColor = Color.GRAY

    var onHighlightChangeListener: OnHighlightChangeListener? = null

    private var animationStopped = false

    companion object {
        private const val sTag = "HighlightTextView"
    }

    init {
        val typedArray = context.obtainStyledAttributes(
            attrs, R.styleable.HighlightTextView, 0, 0
        )

        mHighlightColor = typedArray.getColor(R.styleable.HighlightTextView_highlightColor, mHighlightColor)
        mNormalColor = typedArray.getColor(R.styleable.HighlightTextView_normalColor, mNormalColor)

        typedArray.recycle()
    }

    fun startAnimation() {
        timestampArray.clear()
        textArray.clear()
        currentHighlightLine = 0
        var durationCount = 150L
        timestampArray.add(durationCount) // 第一行一定从零开始，开始时有接近 150 毫秒的空白
        for (i in 0 until lineCount) {
            val lineStart = layout.getLineStart(i)
            val lineEnd = layout.getLineEnd(i)
            if (lineStart < text.length && lineEnd <= text.length) {
                val lineText = text.substring(lineStart, lineEnd)
                durationCount += textToDuration(lineText)
                timestampArray.add(durationCount)
                textArray.add(lineText)
            } else {
                timestampArray.add(durationCount) // 开始时有接近 150 毫秒的空白
            }
        }
    }

    fun stopAnimation() {
        animationStopped = true
        currentHighlightLine = -1

        val stringBuilder = StringBuilder()
        textArray.mapIndexed { sIndex, s ->
            stringBuilder.append("<font color=${toHexColor(mHighlightColor)}>$s</font><br/>")
        }
        val htmlText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(stringBuilder.toString(), Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(stringBuilder.toString())
        }
        text = htmlText
    }

    fun updatePosition(position: Long) {
        for (index in 0 until timestampArray.size) {
            val value = timestampArray[index]
            if (position < value) {
                if (currentHighlightLine != index) {
                    val target = Math.max(0, index - 1)

                    this.currentHighlightLine = target

                    val stringBuilder = StringBuilder()
                    textArray.mapIndexed { sIndex, s ->
                        if (sIndex == target) {
                            stringBuilder.append("<font color=${toHexColor(mHighlightColor)}>$s</font><br/>")
                        } else {
                            stringBuilder.append("<font color=${toHexColor(mNormalColor)}>$s</font><br>")
                        }
                    }
                    val htmlText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Html.fromHtml(stringBuilder.toString(), Html.FROM_HTML_MODE_LEGACY)
                    } else {
                        Html.fromHtml(stringBuilder.toString())
                    }
                    text = htmlText
                    onHighlightChangeListener?.onHighlightChange(this, target, target * lineHeight)
                    return
                }
            }
        }
    }

    private fun toHexColor(color: Int): String {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        return String.format("\"#%02X%02X%02X\"", red, green, blue)
    }

    private fun textToDuration(text: String): Long {
        val pureText = text.replace(Regex("[^\u4E00-\u9FA5]"), "")
        val shortSymbol = Regex("[，|,|:|：|、]")
        val shortSum = shortSymbol.findAll(text).count()
        val longSymbol = Regex("[\\.|。|!|！|？|?|…|；]")
        val longSum = longSymbol.findAll(text).count()
        return pureText.length * 218L + shortSum * 300 + longSum * 450
    }

    interface OnHighlightChangeListener {
        fun onHighlightChange(view: HighlightTextView, line: Int, offset: Int)
    }
}