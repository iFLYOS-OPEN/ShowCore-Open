package com.iflytek.cyber.iot.show.core.fragment

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.iflytek.cyber.iot.show.core.R
import java.util.*

class KugouQRCodeFragment : BaseFragment() {

    companion object {
        const val EXTRA_TITLE = "text"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_QRCODE_MESSAGE = "qrcode_message"
        const val EXTRA_URL = "url"

        fun newInstance(
            title: String?,
            message: String?,
            qrCodeMessage: String?,
            url: String
        ): KugouQRCodeFragment {
            val fragment = KugouQRCodeFragment()
            val arguments = Bundle()
            arguments.putString(EXTRA_TITLE, title)
            arguments.putString(EXTRA_MESSAGE, message)
            arguments.putString(EXTRA_QRCODE_MESSAGE, qrCodeMessage)
            arguments.putString(EXTRA_URL, url)
            fragment.arguments = arguments
            return fragment
        }
    }

    private var backCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_kugou_qrcode, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.back)?.setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            pop()
        }
        view.findViewById<View>(R.id.cannot_use_phone)?.setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            pop()
        }

        val tvTitle = view.findViewById<TextView>(R.id.title)
        val tvQrCodeMessage = view.findViewById<TextView>(R.id.message)
        val ivQrCode = view.findViewById<ImageView>(R.id.qrcode)

        arguments?.let { arguments ->
            val title = arguments.getString(EXTRA_TITLE)
            val message = arguments.getString(EXTRA_MESSAGE)
            val qrCodeMessage = arguments.getString(EXTRA_QRCODE_MESSAGE)
            val url = arguments.getString(EXTRA_URL)

            tvTitle?.text = title ?: message

            qrCodeMessage?.let {
                tvQrCodeMessage?.text = qrCodeMessage
            }

            url?.let {
                view.findViewById<View>(R.id.cannot_use_phone)?.setOnClickListener {
                    if (backCount != 0)
                        return@setOnClickListener
                    backCount++
                    pop()

                    val webViewFragment = WebViewFragment()
                    val webArguments = Bundle()
                    webArguments.putString("url", url)
                    webViewFragment.arguments = webArguments
                    start(webViewFragment)
                }
                ivQrCode?.post {
                    Thread {
                        val bitmap = createQRBitmap(url, ivQrCode.width, ivQrCode.height)
                        ivQrCode.post {
                            ivQrCode.setImageBitmap(bitmap)
                        }
                    }.start()
                }
            }
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
}