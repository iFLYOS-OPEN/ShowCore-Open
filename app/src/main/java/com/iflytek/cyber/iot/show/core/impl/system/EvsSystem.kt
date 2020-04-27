package com.iflytek.cyber.iot.show.core.impl.system

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.EvsError
import com.iflytek.cyber.evs.sdk.agent.System
import com.iflytek.cyber.iot.show.core.EngineService
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.SelfBroadcastReceiver
import com.iflytek.cyber.iot.show.core.utils.DeviceUtils
import com.iflytek.cyber.product.ota.OtaService
import com.iflytek.cyber.product.ota.PackageEntityNew
import java.lang.ref.SoftReference

class EvsSystem private constructor() : System() {
    override fun onPing(timestamp: Long) {
    }

    companion object {
        private var instance: EvsSystem? = null

        fun get(): EvsSystem {
            instance?.let {
                return it
            } ?: run {
                val system = EvsSystem()
                instance = system
                return system
            }
        }
    }

    private val receiver = object : SelfBroadcastReceiver(
        OtaService.ACTION_NEW_UPDATE_DOWNLOADED,
        OtaService.ACTION_CHECK_UPDATE_FAILED,
        OtaService.ACTION_CHECK_UPDATE_RESULT
    ) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                OtaService.ACTION_CHECK_UPDATE_FAILED -> {
                    sendCheckSoftwareUpdateFailed()
                }
                OtaService.ACTION_CHECK_UPDATE_RESULT -> {
                    if (intent.hasExtra(OtaService.EXTRA_PACKAGE_ENTITY)) {
                        val packageEntity =
                            intent.getParcelableExtra<PackageEntityNew>(OtaService.EXTRA_PACKAGE_ENTITY)
                        sendCheckSoftwareUpdateSucceed(
                            true,
                            packageEntity.versionName,
                            packageEntity.description
                        )
                    } else {
                        sendCheckSoftwareUpdateSucceed(false, null, null)

                        sendUpdateSoftwareFailed(ERROR_TYPE_CHECK_ERROR)
                    }
                }
            }
        }
    }

    private var contextRef: SoftReference<Context>? = null
    var onDeviceModeChangeListener: OnDeviceModeChangeListener? = null
    var onEvsErrorListener: OnEvsErrorListener? = null

    fun init(context: Context) {
        contextRef = SoftReference(context)
        receiver.register(context)

        hasPowerController = true
        hasDeviceModes = true
        hasSoftwareUpdater = true
    }

    override fun checkSoftWareUpdate() {
        val context = contextRef?.get()

        val intent = Intent(context, OtaService::class.java)
        intent.action = OtaService.ACTION_REQUEST_CHECKING
        context?.startService(intent)
    }

    override fun updateSoftware() {
        val context = contextRef?.get()

        val intent = Intent(context, OtaService::class.java)
        intent.action = OtaService.ACTION_REQUEST_CHECKING
        intent.putExtra(OtaService.EXTRA_DOWNLOAD_DIRECTLY, false)
        context?.startService(intent)
    }

    fun destroy(context: Context) {
        receiver.unregister(context)
    }

    override fun onError(payload: JSONObject) {
        super.onError(payload)

        onEvsErrorListener?.onError(payload)

        try {
            val code = payload.getIntValue(PAYLOAD_CODE)
            when {
                code >= EvsError.Code.ERROR_SERVER_INTERNAL && code < 600 -> {
                    val context = contextRef?.get() ?: return
                    val showNotification = Intent(context, FloatingService::class.java)
                    showNotification.action = FloatingService.ACTION_SHOW_NOTIFICATION
                    showNotification.putExtra(
                        FloatingService.EXTRA_ICON_RES,
                        R.drawable.ic_default_error_black_40dp
                    )
                    showNotification.putExtra(FloatingService.EXTRA_MESSAGE, "服务端暂无响应，请稍后再试")
                    showNotification.putExtra(
                        FloatingService.EXTRA_POSITIVE_BUTTON_TEXT,
                        context.getString(R.string.i_got_it)
                    )
                    context.startService(showNotification)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPowerOff(payload: JSONObject) {
        try {
            val context = contextRef?.get() ?: return
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val clz = PowerManager::class.java
            val method = clz.getDeclaredMethod(
                "shutdown",
                Boolean::class.java,
                String::class.java,
                Boolean::class.java
            )
            method.isAccessible = true
            method.invoke(pm, false, "userrequested", false)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    override fun onUpdateDeviceModes(payload: JSONObject) {
        // ignore
    }

    override fun onFactoryReset() {
        super.onFactoryReset()

        val context = contextRef?.get() ?: return

        DeviceUtils.doFactoryReset(context)
    }

    override fun revokeAuth(context: Context?) {
        super.revokeAuth(context)

        val intent = Intent(context, EngineService::class.java)
        intent.action = EngineService.ACTION_AUTH_REVOKED
        context?.startService(intent)
    }

    override fun onDeviceModeChanged(kid: Boolean) {
        onDeviceModeChangeListener?.onDeviceModeChanged(kid)
    }

    interface OnDeviceModeChangeListener {
        fun onDeviceModeChanged(kid: Boolean)
    }

    interface OnEvsErrorListener {
        fun onError(payload: JSONObject)
    }
}