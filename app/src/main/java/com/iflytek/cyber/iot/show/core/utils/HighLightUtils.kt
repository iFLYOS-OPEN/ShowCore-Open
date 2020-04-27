package com.iflytek.cyber.iot.show.core.utils

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
    }
}