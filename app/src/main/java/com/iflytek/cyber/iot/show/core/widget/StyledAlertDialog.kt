package com.iflytek.cyber.iot.show.core.widget

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.iflytek.cyber.iot.show.core.R

class StyledAlertDialog : DialogFragment() {
    private var dialogIconView: ImageView? = null
    private var dialogTitleView: TextView? = null
    private var dialogMessageView: TextView? = null
    private var dialogPositiveButton: TextView? = null
    private var dialogNegativeButton: TextView? = null

    private var positiveButton: Pair<String, View.OnClickListener?>? = null
    private var negativeButton: Pair<String, View.OnClickListener?>? = null

    var title: String? = null
    var message: String? = null

    var iconDrawable: Drawable? = null
        set(value) {
            value?.let {
                dialogIconView?.visibility = View.VISIBLE
            } ?: run {
                dialogIconView?.visibility = View.GONE
            }
            dialogIconView?.setImageDrawable(value)
            field = value
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return inflater.inflate(R.layout.layout_styled_dialog, container, false)
    }

    override fun onStart() {
        super.onStart()

        dialog?.let { dialog ->
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(
                resources.getDimensionPixelSize(R.dimen.dp_380),
                WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialogIconView = view.findViewById(R.id.dialog_icon)
        dialogTitleView = view.findViewById(R.id.dialog_title)
        dialogMessageView = view.findViewById(R.id.dialog_message)
        dialogPositiveButton = view.findViewById(R.id.dialog_positive)
        dialogNegativeButton = view.findViewById(R.id.dialog_negative)

        title?.let { title ->
            dialogTitleView?.text = title
            dialogTitleView?.visibility = View.VISIBLE
        } ?: run {
            dialogTitleView?.visibility = View.GONE
        }
        message?.let { message ->
            dialogMessageView?.text = message
            dialogMessageView?.visibility = View.VISIBLE
        } ?: run {
            dialogMessageView?.visibility = View.GONE
        }
        iconDrawable?.let {
            (dialogIconView?.layoutParams as? ConstraintLayout.LayoutParams)
                ?.let { layoutParams ->
                    layoutParams.width = resources.getDimensionPixelSize(R.dimen.dp_40)
                    layoutParams.constrainedWidth = false
                    dialogIconView?.layoutParams = layoutParams
                }
        } ?: run {
            (dialogIconView?.layoutParams as? ConstraintLayout.LayoutParams)
                ?.let { layoutParams ->
                    layoutParams.width = 0
                    layoutParams.constrainedWidth = true
                    layoutParams.matchConstraintPercentWidth = 0f
                    dialogIconView?.layoutParams = layoutParams
                }
        }
        dialogIconView?.setImageDrawable(iconDrawable)

        dialogPositiveButton?.let { dialogPositiveButton ->
            matchPairToButton(positiveButton, dialogPositiveButton)
        }
        dialogNegativeButton?.let { dialogNegativeButton ->
            matchPairToButton(negativeButton, dialogNegativeButton)
        }
    }

    private fun matchPairToButton(pair: Pair<String, View.OnClickListener?>?, button: TextView) {
        pair?.let {
            button.text = it.first
            button.setOnClickListener { view ->
                dismiss()
                it.second?.onClick(view)
            }
            button.visibility = View.VISIBLE
        } ?: run {
            button.visibility = View.GONE
        }
    }

    class Builder {
        private var title: String? = null
        private var message: String? = null
        private var iconDrawable: Drawable? = null
        private var positiveButton: Pair<String, View.OnClickListener?>? = null
        private var negativeButton: Pair<String, View.OnClickListener?>? = null

        fun setTitle(title: String): Builder {
            this.title = title
            return this
        }

        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }

        fun setIcon(iconDrawable: Drawable): Builder {
            this.iconDrawable = iconDrawable
            return this
        }

        fun setPositiveButton(text: String, onClickListener: View.OnClickListener?): Builder {
            positiveButton = Pair(text, onClickListener)
            return this
        }

        fun setNegativeButton(text: String, onClickListener: View.OnClickListener?): Builder {
            negativeButton = Pair(text, onClickListener)
            return this
        }

        fun build(): StyledAlertDialog {
            val dialog = StyledAlertDialog()
            dialog.title = title
            dialog.message = message
            dialog.iconDrawable = iconDrawable
            dialog.positiveButton = positiveButton
            dialog.negativeButton = negativeButton
            return dialog
        }

        fun show(manager: FragmentManager, tag: String? = null): StyledAlertDialog {
            val dialog = build()
            dialog.show(manager, tag)
            return dialog
        }
    }
}