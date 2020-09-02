package com.iflytek.cyber.iot.show.core.fragment

import android.graphics.Bitmap
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.IotApi
import com.iflytek.cyber.iot.show.core.model.AuthUrl
import com.iflytek.cyber.iot.show.core.model.DeviceAuthState
import com.iflytek.cyber.iot.show.core.utils.clickWithTrigger
import com.iflytek.cyber.iot.show.core.widget.StyledAlertDialog
import com.iflytek.cyber.iot.show.core.widget.StyledWebDialog
import com.kk.taurus.playerbase.utils.NetworkUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


class DeviceAuthFragment : BaseFragment() {

    companion object {
        fun newInstance(iotId: Int): DeviceAuthFragment {
            return DeviceAuthFragment().apply {
                arguments = bundleOf(Pair("iot_id", iotId))
            }
        }
    }

    private lateinit var qrcode: ImageView
    private lateinit var errorFiled: LinearLayout
    private lateinit var tvError: TextView
    private var authDialog: StyledWebDialog? = null

    private var authUrl: String? = null
    private var shouldLoadAuthState = true

    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_device_auth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        qrcode = view.findViewById(R.id.qrcode)
        errorFiled = view.findViewById(R.id.error_field)
        tvError = view.findViewById(R.id.tv_error)

        view.findViewById<View>(R.id.back).clickWithTrigger {
            pop()
        }

        view.findViewById<View>(R.id.cannot_use_phone).setOnClickListener {
            if (isAdded && context != null && !NetworkUtils.isNetConnected(context)) {
                Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            authUrl?.let {
                showAuthDialog(it)
            }
        }

        val iotId = arguments?.getInt("iot_id")
        if (iotId != null) {
            getAuthUrl(iotId)
        }

        errorFiled.setOnClickListener {
            if (isAdded && context != null && !NetworkUtils.isNetConnected(context)) {
                Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (iotId != null) {
                getAuthUrl(iotId)
            }
        }
    }

    override fun onSupportInvisible() {
        super.onSupportInvisible()
        authDialog?.dismissAllowingStateLoss()
    }

    private fun startCount() {
        countDownTimer = object : CountDownTimer(180 * 1000, 2000) {
            override fun onFinish() {
                shouldLoadAuthState = false

                if (!isAdded || context == null) {
                    return
                }
                StyledAlertDialog.Builder()
                    .setTitle("添加设备超时")
                    .setMessage("请重新扫描二维码添加设备")
                    .setPositiveButton("我知道了", View.OnClickListener {
                        //retry load auth state
                        startCount()
                    })
                    .show(childFragmentManager)
            }

            override fun onTick(millisUntilFinished: Long) {
                getAuthState()
            }
        }
        countDownTimer?.start()
    }

    private fun showAuthDialog(url: String) {
        if (!isAdded || context == null) {
            return
        }
        authDialog = StyledWebDialog()
        authDialog?.setOnWebPageCallback(object : StyledWebDialog.OnWebPageCallback {
            override fun onClosePage() {
                startWithPopTo(
                    SmartHomeFragment.newInstance(true),
                    SmartHomeFragment::class.java,
                    true
                )
            }

            override fun openNewPage(tag: String, params: String?) {
            }
        })
        authDialog?.arguments = bundleOf(Pair("url", url))
        authDialog?.show(childFragmentManager, "Web")
    }

    private fun getAuthState() {
        if (!shouldLoadAuthState) {
            return
        }

        if (authUrl.isNullOrEmpty()) {
            return
        }

        val index = authUrl?.indexOf("state")
        var state: String? = null
        if (index ?: 0 > 0) {
            state = authUrl?.substring(index!! + 6, authUrl!!.length)
        }
        if (state.isNullOrEmpty()) {
            return
        }
        getIotApi()?.getAuthState(state)?.enqueue(object : Callback<DeviceAuthState> {
            override fun onResponse(
                call: Call<DeviceAuthState>,
                response: Response<DeviceAuthState>
            ) {
                if (!isAdded || context == null) {
                    return
                }

                if (response.isSuccessful) {
                    val deviceAuthState = response.body()
                    if (deviceAuthState?.revokedYet == true) {
                        shouldLoadAuthState = false
                        Toast.makeText(context, "成功绑定设备", Toast.LENGTH_SHORT).show()
                        startWithPopTo(
                            SmartHomeFragment.newInstance(true),
                            SmartHomeFragment::class.java,
                            true
                        )
                    }
                }
            }

            override fun onFailure(call: Call<DeviceAuthState>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    private fun getAuthUrl(iotId: Int) {
        getIotApi()?.getAuthUrl(iotId)?.enqueue(object : Callback<AuthUrl> {
            override fun onResponse(call: Call<AuthUrl>, response: Response<AuthUrl>) {
                if (response.isSuccessful) {
                    errorFiled.isVisible = false
                    val url = response.body()?.authUrl
                    authUrl = url

                    getAuthState()

                    startCount()

                    if (url.isNullOrEmpty()) {
                        return
                    }
                    qrcode.post {
                        Thread {
                            val bitmap = createQRBitmap(url, qrcode.width, qrcode.height)
                            qrcode.post {
                                qrcode.setImageBitmap(bitmap)
                            }
                        }.start()
                    }
                } else {
                    if (isAdded && context != null) {
                        errorFiled.isVisible = true
                        tvError.text = getString(R.string.qr_code_client_network_error_message)
                    }
                }
            }

            override fun onFailure(call: Call<AuthUrl>, t: Throwable) {
                t.printStackTrace()
                if (isAdded && context != null) {
                    errorFiled.isVisible = true
                    tvError.text = getString(R.string.qr_code_client_network_error_message)
                }
            }
        })
    }

    private fun createQRBitmap(content: String, width: Int, height: Int): Bitmap? {
        if (!isAdded) return null
        val context = context ?: return null
        try {
            val colorBlack = ActivityCompat.getColor(context, android.R.color.black)
            val coloWhite = ActivityCompat.getColor(context, android.R.color.transparent)

            // 设置二维码相关配置,生成BitMatrix(位矩阵)对象
            val hints = Hashtable<EncodeHintType, String>()
            hints[EncodeHintType.CHARACTER_SET] = "utf-8" // 字符转码格式设置
            hints[EncodeHintType.ERROR_CORRECTION] = "H" // 容错级别设置
            hints[EncodeHintType.MARGIN] = "0" // 空白边距设置

            var bitMatrix =
                QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)

            //bitMatrix = deleteWhite(bitMatrix) //删除白边

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

    private fun deleteWhite(matrix: BitMatrix): BitMatrix {
        val rec = matrix.enclosingRectangle
        val resWidth = rec[2] + 1
        val resHeight = rec[3] + 1

        val resMatrix = BitMatrix(resWidth, resHeight)
        resMatrix.clear()
        for (i in 0 until resWidth) {
            for (j in 0 until resHeight) {
                if (matrix.get(i + rec[0], j + rec[1]))
                    resMatrix.set(i, j)
            }
        }
        return resMatrix
    }

    private fun getIotApi(): IotApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(IotApi::class.java)
        } else {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}