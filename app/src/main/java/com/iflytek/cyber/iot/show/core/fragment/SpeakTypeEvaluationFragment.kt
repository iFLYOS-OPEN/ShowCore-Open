package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.utils.clickWithTrigger

class SpeakTypeEvaluationFragment : BaseFragment() {

    companion object {

        fun newInstance(type: Int): SpeakTypeEvaluationFragment {
            return SpeakTypeEvaluationFragment().apply {
                arguments = bundleOf(Pair("type", type))
            }
        }
    }

    private lateinit var wordType: FrameLayout
    private lateinit var wordsType: FrameLayout
    private lateinit var sentenceType: FrameLayout
    private lateinit var tvWord: TextView
    private lateinit var tvWords: TextView
    private lateinit var tvSentence: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_speak_type, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wordType = view.findViewById(R.id.word_type)
        wordsType = view.findViewById(R.id.words_type)
        sentenceType = view.findViewById(R.id.sentence_type)
        tvWord = view.findViewById(R.id.tv_word)
        tvWords = view.findViewById(R.id.tv_words)
        tvSentence = view.findViewById(R.id.tv_sentence)

        view.findViewById<View>(R.id.back).clickWithTrigger {
            pop()
        }

        val wordsImageView = view.findViewById<ImageView>(R.id.words_img)
        val sentenceImageView = view.findViewById<ImageView>(R.id.sentence_img)

        val type = arguments?.getInt("type", -1)
        if (type == 0) {
            wordType.setBackgroundResource(R.drawable.bg_orange_round_42dp)
            wordsType.setBackgroundResource(R.drawable.bg_green_round_42dp)
            sentenceType.setBackgroundResource(R.drawable.bg_blue_round_42dp)
            wordsImageView.setBackgroundResource(R.drawable.ic_ciyu_48)
            sentenceImageView.setBackgroundResource(R.drawable.ic_juzi_48)
            tvWord.setText(R.string.word_evaluation)
            tvWords.setText(R.string.words_evaluation)
            tvSentence.setText(R.string.short_sentence_evaluation)
        } else if (type == 1) {
            wordType.setBackgroundResource(R.drawable.bg_blue_round_42dp)
            wordsType.setBackgroundResource(R.drawable.bg_green_round_42dp)
            sentenceType.setBackgroundResource(R.drawable.bg_orange_round_42dp)
            wordsImageView.setBackgroundResource(R.drawable.ic_juzi_48)
            sentenceImageView.setBackgroundResource(R.drawable.ic_wenzhang_48)
            tvWord.setText(R.string.english_word_evaluation)
            tvWords.setText(R.string.short_sentence_evaluation)
            tvSentence.setText(R.string.article_evaluation)
        }

        view.findViewById<View>(R.id.word_type).clickWithTrigger {
            if (type == 0) {
                start(SpeakEvaluatingFragment.newInstance(SpeakEvaluatingFragment.CHINESE_SINGLE_WORD_TYPE))
            } else {
                start(SpeakEvaluatingFragment.newInstance(SpeakEvaluatingFragment.ENGLISH_WORD_TYPE))
            }
        }

        view.findViewById<View>(R.id.words_type).clickWithTrigger {
            if (type == 0) {
                start(SpeakEvaluatingFragment.newInstance(SpeakEvaluatingFragment.CHINESE_WORDS_TYPE))
            } else {
                start(SpeakEvaluatingFragment.newInstance(SpeakEvaluatingFragment.ENGLISH_SENTENCE_TYPE))
            }
        }

        view.findViewById<View>(R.id.sentence_type).clickWithTrigger {
            if (type == 0) {
                start(SpeakEvaluatingFragment.newInstance(SpeakEvaluatingFragment.CHINESE_SENTENCE_TYPE))
            } else {
                start(SpeakEvaluatingFragment.newInstance(SpeakEvaluatingFragment.ENGLISH_ARTICLE_TYPE))
            }
        }
    }
}