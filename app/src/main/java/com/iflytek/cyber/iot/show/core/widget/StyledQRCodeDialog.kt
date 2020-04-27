package com.iflytek.cyber.iot.show.core.widget

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.iflytek.cyber.iot.show.core.R
import java.util.*

class StyledQRCodeDialog : DialogFragment() {
    private var buttonPair: Pair<String, View.OnClickListener?>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return inflater.inflate(R.layout.layout_styled_qrcode_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString("text")
        val message = arguments?.getString("message")
        val code = arguments?.getString("code")

        view.findViewById<ImageView>(R.id.qrcode)?.let { imageView ->
            code?.let {
                imageView.post {
                    Thread {
                        val bitmap = createQRBitmap(code, imageView.width, imageView.height)
                        imageView.post {
                            imageView.setImageBitmap(bitmap)
                            view.findViewById<View>(R.id.progress_bar)?.isVisible = false
                        }
                    }.start()
                }
            }
        }
        view.findViewById<TextView>(R.id.dialog_title)?.let { tvTitle ->
            tvTitle.text = title
            tvTitle.isVisible = !title.isNullOrEmpty()
        }
        view.findViewById<TextView>(R.id.dialog_message)?.let { tvMessage ->
            tvMessage.text = message
            tvMessage.isVisible = !message.isNullOrEmpty()
        }
        buttonPair?.let {
            view.findViewById<TextView>(R.id.dialog_button)?.let { button ->
                button.isVisible = true
                button.text = it.first
                button.setOnClickListener { view ->
                    it.second?.onClick(view)
                    dismiss()
                }
            }
        } ?: run {
            view.findViewById<View>(R.id.dialog_button)?.isVisible = false
        }
    }

    private fun createQRBitmap(content: String, width: Int, height: Int): Bitmap? {
        val context = context ?: return null
        try {
            val colorBlack = ActivityCompat.getColor(context, android.R.color.black)
            val coloWhite = ActivityCompat.getColor(context, android.R.color.transparent)

            // 设置二维码相关配置,生成BitMatrix(位矩阵)对象
            val hints = Hashtable<EncodeHintType, String>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8" // 字符转码格式设置
            hints[EncodeHintType.ERROR_CORRECTION] = "H" // 容错级别设置
            hints[EncodeHintType.MARGIN] = "2" // 空白边距设置

            val bitMatrix =
                QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)

            // 创建像素数组,并根据BitMatrix(位矩阵)对象为数组元素赋颜色值
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * width + x] = colorBlack // 黑色色块像素设置
                    } else {
                        pixels[y * width + x] = coloWhite // 白色色块像素设置
                    }
                }
            }

            // 创建Bitmap对象,根据像素数组设置Bitmap每个像素点的颜色值,之后返回Bitmap对象
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
        }
        return null
    }


    override fun onStart() {
        super.onStart()

        dialog?.let { dialog ->
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(
                resources.getDimensionPixelSize(R.dimen.dp_260),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }

    class Builder {
        private var code: String? = null
        private var title: String? = null
        private var message: String? = null
        private var buttonPair: Pair<String, View.OnClickListener?>? = null

        fun setTitle(title: String?): Builder {
            this.title = title
            return this
        }

        fun setMessage(message: String?): Builder {
            this.message = message
            return this
        }

        fun setCode(code: String?): Builder {
            this.code = code
            return this
        }

        fun setButton(text: String, onClickListener: View.OnClickListener?): Builder {
            buttonPair = Pair(text, onClickListener)
            return this
        }

        fun build(): StyledQRCodeDialog {
            val dialog = StyledQRCodeDialog()
            val arguments = Bundle()
            arguments.putString("text", title)
            arguments.putString("message", message)
            arguments.putString("code", code)
            dialog.arguments = arguments
            dialog.buttonPair = buttonPair
            return dialog
        }

        fun show(fragmentManager: FragmentManager?): StyledQRCodeDialog {
            build().apply {
                fragmentManager?.let {
                    show(it, "QRCode")
                }
                return this
            }
        }
    }
}