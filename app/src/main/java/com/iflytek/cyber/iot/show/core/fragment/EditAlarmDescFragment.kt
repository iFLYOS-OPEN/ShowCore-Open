package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.core.os.bundleOf
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.utils.KeyboardUtils

class EditAlarmDescFragment : BaseFragment() {

    companion object {
        fun instance(desc: String): EditAlarmDescFragment {
            return EditAlarmDescFragment().apply {
                arguments = bundleOf(Pair("desc", desc))
            }
        }
    }

    private lateinit var editText: EditText

    private var desc: String? = null

    private var backCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(context)
            .inflate(R.layout.fragment_edit_alarm_desc, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        desc = arguments?.getString("desc")

        view.findViewById<View>(R.id.iv_back).setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            KeyboardUtils.closeKeyboard(editText)
            val bundle = bundleOf(Pair("desc", desc), Pair("isEdited", false))
            setFragmentResult(0, bundle)
            pop()
        }

        editText = view.findViewById(R.id.edit_text)

        editText.setText(desc)
        editText.setSelection(desc?.length ?: 0)

        view.findViewById<Button>(R.id.done).setOnClickListener {
            KeyboardUtils.closeKeyboard(editText)
            val bundle = bundleOf(Pair("desc", editText.text.toString()), Pair("isEdited", true))
            setFragmentResult(0, bundle)
            pop()
        }
    }

    override fun onBackPressedSupport(): Boolean {
        KeyboardUtils.closeKeyboard(editText)
        val bundle = bundleOf(Pair("desc", desc))
        setFragmentResult(0, bundle)
        return super.onBackPressedSupport()
    }
}