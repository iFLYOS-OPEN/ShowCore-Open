package com.iflytek.cyber.iot.show.core.widget

import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView
import com.google.gson.Gson
import com.iflytek.cyber.iot.show.core.model.WakeWord
import com.iflytek.cyber.iot.show.core.utils.ConfigUtils

class BannerDescTextView(context: Context?, attrs: AttributeSet?) : AppCompatTextView(context, attrs) {

    private var descriptions = ArrayList<String>()

    private val timeHandler = Handler()

    private var currentSummary = 0

    fun setDescs(desc: ArrayList<String>?) {
        if (desc == null) {
            return
        }
        val newDesc = ArrayList<String>()
        desc.forEach {
            val newText = parseDescription(it)
            newDesc.add(newText)
        }
        this.descriptions.clear()
        this.descriptions.addAll(newDesc)
    }

    fun setDescText(text: String?) {
        val desc = if (!text.isNullOrEmpty()) {
            parseDescription(text)
        } else {
            text
        }
        setText(desc)
        postNextUpdateSummary()
    }

    fun stopScroll() {
        timeHandler.removeCallbacksAndMessages(null)
    }

    private val updateSummaryRunnable: Runnable = Runnable {
        if(descriptions.isNotEmpty() && descriptions.size > 1) {
            val next = (currentSummary + 1) % descriptions.size
            currentSummary = next
            startUpdateSummaryAnimator(descriptions[next])
            postNextUpdateSummary()
        }
    }

    private fun postNextUpdateSummary() {
        timeHandler.removeCallbacksAndMessages(null)
        timeHandler.postDelayed(updateSummaryRunnable, 6 * 1000)
    }

    private fun startUpdateSummaryAnimator(newText: String) {
        val animator = ValueAnimator.ofFloat(0f, 2f)
        animator.addUpdateListener {
            val value = it.animatedValue as Float
            if (value > 1) {
                if (this.text.toString() != newText) {
                    val name = parseDescription(newText)
                    this.text = name
                }
                this.alpha = value - 1
            } else {
                this.alpha = 1 - value
            }
        }
        animator.duration = 600
        animator.start()
    }

    private fun parseDescription(description: String): String {
        try {
            val cacheWakeWord = ConfigUtils.getString(ConfigUtils.KEY_CACHE_WAKE_WORD, null)
            if (!cacheWakeWord.isNullOrEmpty()) {
                val wakeWord = Gson().fromJson(cacheWakeWord, WakeWord::class.java)
                if (wakeWord?.name?.isEmpty() == false) {
                    return description.replace(
                        "蓝小飞",
                        wakeWord.name,
                        false
                    )
                }
            }
        } catch (t: Throwable) {

        }
        return description
    }
}