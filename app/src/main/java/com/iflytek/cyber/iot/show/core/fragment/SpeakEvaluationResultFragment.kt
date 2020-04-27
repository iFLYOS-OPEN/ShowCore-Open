package com.iflytek.cyber.iot.show.core.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.ReadWord

class SpeakEvaluationResultFragment : BaseFragment() {

    companion object {

        fun newInstance(
            type: Int?,
            readWordList: ArrayList<ReadWord>
        ): SpeakEvaluationResultFragment {
            return SpeakEvaluationResultFragment().apply {
                arguments = bundleOf(
                    Pair("type", type),
                    Pair("wordList", readWordList)
                )
            }
        }

        fun newInstance(
            type: Int?,
            readWord: ReadWord?,
            punctuationList: ArrayList<String>
        ): SpeakEvaluationResultFragment {
            return SpeakEvaluationResultFragment().apply {
                arguments = bundleOf(
                    Pair("type", type),
                    Pair("word", readWord),
                    Pair("punctuationList", punctuationList)
                )
            }
        }
    }

    private lateinit var resultList: RecyclerView
    private lateinit var tvResultSentence: TextView
    private lateinit var ivScore: ImageView
    private lateinit var tvTotalScore: TextView
    private lateinit var phoneScore: LinearLayout
    private lateinit var toneScore: LinearLayout
    private lateinit var tvPhoneSocre: TextView
    private lateinit var tvToneScore: TextView
    private lateinit var tvTitle: TextView
    private lateinit var fluencyScore: LinearLayout
    private lateinit var tvFluencyScore: TextView
    private lateinit var englishArticleScore: LinearLayout
    private lateinit var tvStandardScore: TextView
    private lateinit var tvIntegrityScore: TextView
    private lateinit var tvEnglishFluencyScore: TextView
    private lateinit var tvAccuracyScore: TextView

    private var type: Int? = null
    private var punctuationList: ArrayList<String>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_speak_evaluation_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resultList = view.findViewById(R.id.result_list)
        tvResultSentence = view.findViewById(R.id.tv_result_sentence)
        ivScore = view.findViewById(R.id.iv_score)
        tvTotalScore = view.findViewById(R.id.tv_total_score)
        phoneScore = view.findViewById(R.id.phone_score)
        toneScore = view.findViewById(R.id.tone_score)
        tvPhoneSocre = view.findViewById(R.id.tv_phone_score)
        tvToneScore = view.findViewById(R.id.tv_tone_score)
        tvTitle = view.findViewById(R.id.tv_title)
        fluencyScore = view.findViewById(R.id.fluency_score)
        tvFluencyScore = view.findViewById(R.id.tv_fluency_score)
        englishArticleScore = view.findViewById(R.id.english_article_score)
        tvStandardScore = view.findViewById(R.id.tv_standard_score)
        tvIntegrityScore = view.findViewById(R.id.tv_integrity_score)
        tvEnglishFluencyScore = view.findViewById(R.id.tv_english_fluency_score)
        tvAccuracyScore = view.findViewById(R.id.tv_accuracy_score)

        view.findViewById<View>(R.id.back).setOnClickListener {
            pop()
        }

        type = arguments?.getInt("type")
        punctuationList = arguments?.getStringArrayList("punctuationList")
        val wordList = arguments?.getParcelableArrayList<ReadWord>("wordList")
        val readWord = arguments?.getParcelable<ReadWord>("word")
        if (type == SpeakEvaluatingFragment.CHINESE_SINGLE_WORD_TYPE) {
            if (wordList != null) {
                setChineseSingleWordScore(wordList)
                setupResultAdapter(wordList)
            }
        } else if (type == SpeakEvaluatingFragment.CHINESE_WORDS_TYPE) {
            if (wordList != null) {
                setChineseWordsScore(wordList)
                setupResultAdapter(wordList)
            }
        } else if (type == SpeakEvaluatingFragment.ENGLISH_WORD_TYPE) {
            if (wordList != null) {
                setEnglishSingleWordScore(wordList)
                setupResultAdapter(wordList)
            }
        } else if (type == SpeakEvaluatingFragment.CHINESE_SENTENCE_TYPE) {
            if (readWord != null) {
                setChineseSentenceScore(readWord)
                setupSentenceResult(readWord)
            }
        } else if (type == SpeakEvaluatingFragment.ENGLISH_SENTENCE_TYPE) {
            if (readWord != null) {
                setEnglishSentenceScore(readWord)
                setupEnglishSentenceResult(readWord)
            }
        } else if (type == SpeakEvaluatingFragment.ENGLISH_ARTICLE_TYPE) {
            if (readWord != null) {
                setEnglishArticleScore(readWord)
                setupEnglishSentenceResult(readWord)
            }
        }

        view.findViewById<View>(R.id.btn_again).setOnClickListener {
            if (type != null) {
                startWithPop(SpeakEvaluatingFragment.newInstance(type!!))
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setChineseSingleWordScore(wordList: ArrayList<ReadWord>) {
        var finalScore = 0f
        wordList.forEach { readWord ->
            val phoneScore = readWord.phoneScore ?: 0f
            val toneScore = readWord.toneScore ?: 0f
            finalScore += (phoneScore + toneScore) / 2f
        }
        finalScore /= 10f
        tvTotalScore.text = String.format("%.2f", finalScore) + "分"
        if (finalScore >= 60) {
            ivScore.setImageResource(R.drawable.ic_img_bg_score_high)
        } else {
            ivScore.setImageResource(R.drawable.ic_img_bg_score_low)
        }
        setResultTitle(finalScore)
    }

    @SuppressLint("SetTextI18n")
    private fun setChineseWordsScore(wordList: ArrayList<ReadWord>) {
        phoneScore.isVisible = true
        toneScore.isVisible = true
        var finalScore = 0f
        var totalPhoneScore = 0f
        var totalToneScore = 0f
        wordList.forEach { readWord ->
            finalScore += readWord.totalScore ?: 0f
            totalPhoneScore += readWord.phoneScore ?: 0f
            totalToneScore += readWord.toneScore ?: 0f
        }
        finalScore /= 10f
        tvTotalScore.text = String.format("%.2f", finalScore) + "分"
        tvPhoneSocre.text = String.format("%.2f", (totalPhoneScore / 10f))
        tvToneScore.text = String.format("%.2f", (totalToneScore / 10f))
        if (finalScore >= 60) {
            ivScore.setImageResource(R.drawable.ic_img_bg_score_high)
        } else {
            ivScore.setImageResource(R.drawable.ic_img_bg_score_low)
        }
        setResultTitle(finalScore)
    }

    @SuppressLint("SetTextI18n")
    private fun setChineseSentenceScore(readWord: ReadWord) {
        phoneScore.isVisible = true
        toneScore.isVisible = true
        fluencyScore.isVisible = true
        tvPhoneSocre.text = readWord.phoneScore.toString()
        tvToneScore.text = readWord.toneScore.toString()
        tvFluencyScore.text = readWord.fluencyScore.toString()
        tvTotalScore.text = readWord.totalScore.toString() + "分"
        if (readWord.totalScore ?: 0f >= 60f) {
            ivScore.setImageResource(R.drawable.ic_img_bg_score_high)
        } else {
            ivScore.setImageResource(R.drawable.ic_img_bg_score_low)
        }
        setResultTitle(readWord.totalScore ?: 0f)
    }

    private fun setResultTitle(finalScore: Float) {
        when {
            finalScore > 90 -> tvTitle.text = "你这么牛，让我膜拜一下可好？"
            finalScore in 60.0..90.0 -> tvTitle.text = "还不错，可对照评测结果改进"
            finalScore < 60 -> tvTitle.text = "不太理想，还需要加油呀"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setEnglishSingleWordScore(wordList: ArrayList<ReadWord>) {
        var finalScore = 0f
        wordList.forEach { readWord ->
            finalScore += readWord.totalScore ?: 0f
        }
        finalScore /= 10f
        tvTotalScore.text = String.format("%.2f", finalScore) + "分"
        if (finalScore >= 60) {
            ivScore.setImageResource(R.drawable.ic_img_bg_score_high)
        } else {
            ivScore.setImageResource(R.drawable.ic_img_bg_score_low)
        }
        setResultTitle(finalScore)
    }

    @SuppressLint("SetTextI18n")
    private fun setEnglishSentenceScore(readWord: ReadWord) {
        tvTotalScore.text = readWord.totalScore.toString() + "分"
        if (readWord.totalScore ?: 0f >= 60f) {
            ivScore.setImageResource(R.drawable.ic_img_bg_score_high)
        } else {
            ivScore.setImageResource(R.drawable.ic_img_bg_score_low)
        }
        if (readWord.exceptInfo == "0") {
            setResultTitle(readWord.totalScore ?: 0f)
        } else {
            setExceptInfo(readWord.exceptInfo)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setEnglishArticleScore(readWord: ReadWord) {
        englishArticleScore.isVisible = true
        tvTotalScore.text = readWord.totalScore.toString() + "分"
        if (readWord.totalScore ?: 0f >= 60f) {
            ivScore.setImageResource(R.drawable.ic_img_bg_score_high)
        } else {
            ivScore.setImageResource(R.drawable.ic_img_bg_score_low)
        }
        if (readWord.exceptInfo == "0") {
            setResultTitle(readWord.totalScore ?: 0f)
        } else {
            setExceptInfo(readWord.exceptInfo)
        }
        tvStandardScore.text = readWord.standardScore.toString()
        tvIntegrityScore.text = readWord.integrityScore.toString()
        tvEnglishFluencyScore.text = readWord.fluencyScore.toString()
        tvAccuracyScore.text = readWord.accurecyScore.toString()
    }

    private fun setExceptInfo(exceptInfo: String?) {
        when (exceptInfo) {
            "28673" -> tvTitle.text = "声音太小了，大点声音再试一次呀？"
            "28676" -> tvTitle.text = "老实说，你是不是在乱讲额"
            "28680", "28709" -> tvTitle.text = "杂音太大了，换个安静的环境再试一次？"
            "28690" -> tvTitle.text = "声音太大啦，下次声音小点呢"
        }
    }

    private fun setupSentenceResult(readWord: ReadWord) {
        tvResultSentence.isVisible = true
        var newWord = ""
        readWord.sentences.forEachIndexed { index, sentence ->
            sentence.words?.forEach { word ->
                word.syllables?.forEach {
                    when (it.dpMessage) {
                        "0" -> newWord += it.content
                        "16" -> newWord += "(${it.content})"
                        "32", "64", "128" -> newWord += "<S><font color='#b3b3b3'>${it.content}</font></S>"
                    }
                }
            }
            newWord = if (sentence.totalScore ?: 0f >= 90) {
                "<font color='#2AE79F'>${newWord}</font>"
            } else if (sentence.totalScore ?: 0f <= 60 && sentence.totalScore ?: 0f > 0) {
                "<font color='#FF9595'>${newWord}</font>"
            } else {
                "<font color='#FFFFFF'>${newWord}</font>"
            }
            if (punctuationList?.size ?: 0 > index) {
                newWord += punctuationList?.get(index)
            }
        }
        tvResultSentence.text = Html.fromHtml(newWord)
    }

    private fun setupEnglishSentenceResult(readWord: ReadWord) {
        tvResultSentence.isVisible = true
        var newWord = ""
        readWord.sentences.forEachIndexed { index, sentence ->
            sentence.words?.forEach { word ->
                when (word.dpMessage) {
                    "0" -> {
                        //newWord += word.content
                        newWord += if (word.totalScore ?: 0f >= 90) {
                            "<font color='#2AE79F'>${word.content}</font>"
                        } else if (word.totalScore ?: 0f <= 60 && word.totalScore ?: 0f > 0) {
                            "<font color='#FF9595'>${word.content}</font>"
                        } else {
                            "<font color='#FFFFFF'>${word.content}</font>"
                        }
                    }
                    "16" -> newWord += "(${word.content})"
                    "32", "64", "128" -> newWord += "<S><font color='#b3b3b3'>${word.content}</font></S>"
                }
                newWord += " "
            }
            if (punctuationList?.size ?: 0 > index) {
                newWord += punctuationList?.get(index)
            }
        }
        tvResultSentence.text = Html.fromHtml(newWord)
    }

    private fun setupResultAdapter(wordList: ArrayList<ReadWord>) {
        resultList.isVisible = true
        val adapter = ResultAdapter(wordList)
        resultList.adapter = adapter
    }

    private inner class ResultAdapter(val wordList: ArrayList<ReadWord>) :
        RecyclerView.Adapter<ResultAdapter.ResultHolder>() {

        private val badColor = Color.parseColor("#FF9595")
        private val goodColor = Color.parseColor("#2AE79F")

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_speaker_word, parent, false)
            return ResultHolder(view)
        }

        override fun getItemCount(): Int {
            return wordList.size
        }

        private fun getWord(readWord: ReadWord): String {
            var newWord = ""
            readWord.sentences.forEach { sentence ->
                sentence.words?.forEach { word ->
                    word.syllables?.forEach {
                        when (it.dpMessage) {
                            "0" -> newWord += it.content
                            "16" -> newWord += "(${it.content})"
                            "32", "64", "128" -> newWord += "<S><font color='#b3b3b3'>${it.content}</font></S>"
                        }
                    }
                }
            }
            return newWord
        }

        private fun getEnglishWord(readWord: ReadWord): String {
            var newWord = ""
            readWord.sentences.forEach { sentence ->
                val word = sentence.word
                when (word.dpMessage) {
                    "0" -> newWord += word.content
                    "16" -> newWord += "(${word.content})"
                    "32", "64", "128" -> newWord += "<S><font color='#b3b3b3'>${word.content}</font></S>"
                }
            }
            return newWord
        }

        override fun onBindViewHolder(holder: ResultHolder, position: Int) {
            val item = wordList[position]
            var text = ""
            when (type) {
                SpeakEvaluatingFragment.CHINESE_SINGLE_WORD_TYPE -> {
                    text = getWord(item)
                    holder.tvTag.textSize = 25f
                }
                SpeakEvaluatingFragment.CHINESE_WORDS_TYPE -> {
                    text = getWord(item)
                    holder.tvTag.textSize = 25f
                }
                SpeakEvaluatingFragment.ENGLISH_WORD_TYPE -> {
                    text = getEnglishWord(item)
                    holder.tvTag.textSize = 18f
                }
            }
            holder.tvTag.text = Html.fromHtml(text)
            if (item.totalScore ?: 0f >= 90) {
                holder.tvTag.setTextColor(goodColor)
            } else if (item.totalScore ?: 0f <= 60 && item.totalScore ?: 0f > 0) {
                holder.tvTag.setTextColor(badColor)
            } else {
                holder.tvTag.setTextColor(Color.WHITE)
            }
        }

        inner class ResultHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvTag = itemView.findViewById<TextView>(R.id.tv_tag)
        }
    }
}