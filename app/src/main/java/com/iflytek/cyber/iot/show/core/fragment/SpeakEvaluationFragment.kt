package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.iflytek.cyber.iot.show.core.R

class SpeakEvaluationFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_speak_evaluation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.back).setOnClickListener {
            pop()
        }

        view.findViewById<View>(R.id.mandarin_type).setOnClickListener {
            start(SpeakTypeEvaluationFragment.newInstance(0))
        }

        view.findViewById<View>(R.id.american_english_type).setOnClickListener {
            start(SpeakTypeEvaluationFragment.newInstance(1))
        }
    }
}