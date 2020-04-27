package com.iflytek.cyber.iot.show.core.widget

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.iflytek.cyber.iot.show.core.R

class StyledProgressDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return inflater.inflate(R.layout.layout_styled_progress_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString("text")
        val message = arguments?.getString("message")
        val isCancelable = arguments?.getBoolean("is_cancelable", true)

        this.isCancelable = isCancelable == true

        view.findViewById<TextView>(R.id.dialog_title)?.let { tvTitle ->
            if (title.isNullOrEmpty()) {
                (tvTitle.layoutParams as? ConstraintLayout.LayoutParams)?.let { layoutParams ->
                    layoutParams.width = resources.getDimensionPixelSize(R.dimen.dp_40)
                    layoutParams.constrainedWidth = false
                    tvTitle.layoutParams = layoutParams
                }
            } else {
                tvTitle.text = title
            }
        }
        view.findViewById<TextView>(R.id.dialog_message)?.let { tvMessage ->
            if (message.isNullOrEmpty()) {
                (tvMessage.layoutParams as? ConstraintLayout.LayoutParams)?.let { layoutParams ->
                    layoutParams.width = resources.getDimensionPixelSize(R.dimen.dp_40)
                    layoutParams.constrainedWidth = false
                    tvMessage.layoutParams = layoutParams
                }
            } else {
                tvMessage.text = message
            }
        }
    }

    override fun onStart() {
        super.onStart()

        dialog?.let { dialog ->
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(
                resources.getDimensionPixelSize(R.dimen.dp_380),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }

    class Builder {
        private var message: String? = null
        private var title: String? = null
        private var isCancelable = true

        fun setTitle(title: String?): Builder {
            this.title = title
            return this
        }

        fun setMessage(message: String?): Builder {
            this.message = message
            return this
        }

        fun setCancelable(isCancelable: Boolean): Builder {
            this.isCancelable = isCancelable
            return this
        }

        fun build(): StyledProgressDialog {
            val dialog = StyledProgressDialog()
            val arguments = Bundle()
            arguments.putString("text", title)
            arguments.putString("message", message)
            arguments.putBoolean("is_cancelable", isCancelable)
            dialog.arguments = arguments
            return dialog
        }

        fun show(fragmentManager: FragmentManager?): StyledProgressDialog {
            build().apply {
                fragmentManager?.let {
                    show(fragmentManager, "Progress")
                }
                return this
            }
        }
    }
}