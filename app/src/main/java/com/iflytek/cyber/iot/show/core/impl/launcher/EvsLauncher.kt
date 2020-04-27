package com.iflytek.cyber.iot.show.core.impl.launcher

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.os.bundleOf
import androidx.core.os.postDelayed
import com.alibaba.fastjson.JSONObject
import com.google.android.exoplayer2.offline.DownloadService.start
import com.iflytek.cyber.evs.sdk.agent.Launcher
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.EvsLauncherActivity
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.api.AppApi
import com.iflytek.cyber.iot.show.core.fragment.*
import com.iflytek.cyber.iot.show.core.model.TemplateApp
import com.iflytek.cyber.iot.show.core.utils.NavigationUtils
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.SoftReference

class EvsLauncher private constructor() : Launcher() {
    companion object {
        private var instance: EvsLauncher? = null

        fun get(): EvsLauncher {
            instance?.let {
                return it
            } ?: run {
                val launcher = EvsLauncher()
                instance = launcher
                return launcher
            }
        }
    }

    private var contextRef: SoftReference<Context>? = null
    private var activityRef: SoftReference<EvsLauncherActivity>? = null

    private val handler = Handler()

    private var internalAppId: String? = null
    private var currentAppType: String? = null

    var pageScrollCallback: PageScrollCallback? = null

    fun init(context: Context) {
        contextRef = SoftReference(context)
    }

    fun clearInternalAppId() {
        currentAppType = null
        internalAppId = null
    }

    fun getCurrentAppType(): String? {
        return currentAppType
    }

    fun setActivity(activity: EvsLauncherActivity) {
        activityRef = SoftReference(activity)
    }

    override fun startActivity(page: String, callback: ExecuteCallback): Boolean {
        try {
            if (page == PAGE_NEXT || page == PAGE_PREVIOUS) {
                val result =
                    when (page) {
                        PAGE_NEXT -> pageScrollCallback?.onScrollToNext()
                        PAGE_PREVIOUS -> pageScrollCallback?.onScrollToPrevious()
                        else -> null
                    }
                if (result?.isSuccessful == true) {
                    callback.onSuccess(result.feedback)
                } else {
                    callback.onFailed(FAILURE_CODE_INTERNAL_ERROR, result?.feedback)
                }
            } else {
                contextRef?.get()?.let { context ->
                    val intent = Intent(context, EvsLauncherActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    intent.action = EvsLauncherActivity.ACTION_LAUNCHER_CONTROL
                    intent.putExtra(EvsLauncherActivity.EXTRA_PAGE, page)
                    context.startActivity(intent)
                    if (page == PAGE_ALARMS) {
                        callback.onSuccess("")
                    } else {
                        callback.onSuccess()
                    }
                } ?: run {
                    callback.onFailed(FAILURE_CODE_NOT_FOUND_PAGE, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            callback.onFailed(FAILURE_CODE_INTERNAL_ERROR, null)
        }
        return true
    }

    override fun back(callback: ExecuteCallback) {
        if (currentAppType == TYPE_SKILL) {
            activityRef?.get()?.let {
                val intent = Intent(it, FloatingService::class.java)
                intent.action = FloatingService.ACTION_CLOSE_SKILL_APP
                it.startService(intent)
            }
        } else {
            NavigationUtils.clickBack({
                callback.onSuccess()
            }, {
                callback.onFailed(FAILURE_CODE_INTERNAL_ERROR, null)
            })
        }
    }

    override fun startInternalApp(payload: JSONObject, callback: ExecuteCallback): Boolean {
        val type = payload.getString("type")
        val data = payload.getJSONObject("data")
        internalAppId = payload.getString("id")
        when (type) {
            TYPE_TEMPLATE -> {
                currentAppType = type
                val name = data.getString("name")
                val icon = data.getString("icon")
                val img = data.getString("img")
                val template = data.getIntValue("template")
                val isDark = data.getBooleanValue("is_dark")
                val templateApp = TemplateApp("", name, icon, img, template, "", isDark)
                when (templateApp.template) {
                    TemplateApp.TEMPLATE_TEMPLATE_1 -> {
                        activityRef?.get()
                            ?.start(TemplateApp1Fragment.newInstance(templateApp))
                        activityRef?.get()?.getService()?.requestLauncherVisualFocus()
                        callback.onSuccess("")
                    }
                    TemplateApp.TEMPLATE_TEMPLATE_2 -> {
                        activityRef?.get()
                            ?.start(TemplateApp2Fragment.newInstance(templateApp))
                        activityRef?.get()?.getService()?.requestLauncherVisualFocus()
                        callback.onSuccess("")
                    }
                    TemplateApp.TEMPLATE_TEMPLATE_3 -> {
                        playTemplate3(templateApp, callback)
                    }
                    else -> {
                        callback.onFailed(FAILURE_CODE_NOT_FOUND_APP, null)
                    }
                }
            }
            TYPE_SKILL -> {
                val url = data.getString("url")
                val timeout = payload.getIntValue("timeout")
                val semanticData = data.getString("semanticData")
                val context = activityRef?.get()

                context?.let {
                    if (currentAppType == TYPE_SKILL) {
                        currentAppType = type
                        val intent = Intent(it, FloatingService::class.java)
                        intent.action = FloatingService.ACTION_SET_SEMANTIC_DATA
                        intent.putExtra("semanticData", semanticData)
                        it.startService(intent)
                    } else  {
                        return@let handler.postDelayed(80) {
                            currentAppType = type
                            val intent = Intent(it, FloatingService::class.java)
                            intent.action = FloatingService.ACTION_RENDER_SKILL_APP
                            intent.putExtra("url", url)
                            intent.putExtra("timeout", timeout)
                            it.startService(intent)
                            it.getService()?.requestLauncherVisualFocus()
                        }
                    }
                }
            }
            TYPE_EVALUATE -> {
                currentAppType = type
                activityRef?.get()?.start(SpeakEvaluationFragment())
                activityRef?.get()?.getService()?.requestLauncherVisualFocus()
            }
            TYPE_H5_APP -> {
                currentAppType = type
                internalAppId = payload.getString("id")
                val url = data.getString("url")
                val webViewFragment = WebViewFragment().apply {
                    arguments = bundleOf(Pair(WebViewFragment.EXTRA_URL, url))
                }
                activityRef?.get()?.start(webViewFragment)
                activityRef?.get()?.getService()?.requestLauncherVisualFocus()
            }
        }
        return true
    }

    private fun playTemplate3(templateApp: TemplateApp, callback: ExecuteCallback) {
        val json = JSONObject()
        json["appName"] = templateApp.name
        val requestBody = RequestBody.create(
            MediaType.parse("application/json"),
            json.toString()
        )

        val context = activityRef?.get()
        if (context == null) {
            callback.onFailed(FAILURE_CODE_INTERNAL_ERROR, null)
            return
        }
        val appApi = CoreApplication.from(context).createApi(AppApi::class.java)
        appApi?.playTemplate3(requestBody)?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    callback.onSuccess("")
                } else {
                    callback.onFailed(FAILURE_CODE_INTERNAL_ERROR, null)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback.onFailed(FAILURE_CODE_INTERNAL_ERROR, null)
            }
        })
    }

    override fun getForegroundAppType(): String? {
        activityRef?.get()?.let {
            val currentFragment = it.getTopFragment()
            var currentAppType: String? = null
            if (currentFragment is TemplateApp1Fragment || currentFragment is TemplateApp2Fragment) {
                currentAppType = TYPE_TEMPLATE
            } else if (currentFragment is SpeakEvaluationFragment ||
                currentFragment is SpeakTypeEvaluationFragment ||
                currentFragment is SpeakEvaluatingFragment ||
                currentFragment is SpeakEvaluationResultFragment
            ) {
                currentAppType = TYPE_EVALUATE
            } else if (this.currentAppType == TYPE_SKILL) {
                currentAppType = TYPE_SKILL
            } else if (this.currentAppType == TYPE_H5_APP) {
                currentAppType = TYPE_H5_APP
            }

            return currentAppType
        }

        return null
    }

    override fun getForegroundAppId(): String? {
        return internalAppId
    }

    override fun getSupportedType(): List<String> {
        return listOf(TYPE_TEMPLATE, TYPE_EVALUATE, TYPE_H5_APP, TYPE_SKILL)
    }

    interface PageScrollCallback {
        fun onScrollToPrevious(): ScrollResult
        fun onScrollToNext(): ScrollResult
    }

    data class ScrollResult(
        val isSuccessful: Boolean,
        val feedback: String? = null
    )
}