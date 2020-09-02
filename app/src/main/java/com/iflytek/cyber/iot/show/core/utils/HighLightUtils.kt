package com.iflytek.cyber.iot.show.core.utils

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import java.util.regex.Pattern

class HighLightUtils {

    companion object {

        fun highLightText(
            str: String,
            inputs: ArrayList<String>,
            resStr: StringBuffer
        ): StringBuffer {
            var index = str.length//用来做为标识,判断关键字的下标
            var next = "" //保存str中最先找到的关键字
            for (i in inputs.size - 1 downTo 0) {
                val theNext = inputs[i]
                val theIndex = str.indexOf(theNext)
                if (theIndex == -1) {
                    inputs.removeAt(i)
                } else {
                    index = theIndex
                    next = theNext
                }
            }

            if (index == str.length) {
                resStr.append(str)
            } else {
                resStr.append(str.substring(0, index))
                resStr.append(
                    "<font color='#4C93D4'>" + str.substring(
                        index,
                        index + next.length
                    ) + "</font>"
                )
                val str1 = str.substring(index + next.length, str.length)
                highLightText(str1, inputs, resStr)
            }
            return resStr
        }

        private val selectColor = Color.parseColor("#4C93D4")

        fun hightLightText(text: String, keyword: String): SpannableString {
            val spannableString = SpannableString(text)
            //条件 keyword
            val pattern = Pattern.compile(keyword)
            val matcher = pattern.matcher(SpannableString(text))
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                //ForegroundColorSpan 需要new 不然也只能是部分变色
                spannableString.setSpan(ForegroundColorSpan(selectColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return spannableString
        }
    }
}