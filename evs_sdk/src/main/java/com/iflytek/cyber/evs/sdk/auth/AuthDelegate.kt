package com.iflytek.cyber.evs.sdk.auth

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONException
import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.model.AuthResponse
import com.iflytek.cyber.evs.sdk.model.DeviceCodeResponse
import com.iflytek.cyber.evs.sdk.utils.Log
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * 认证授权类。
 */
object AuthDelegate {
    private const val TAG = "AuthDelegate"

    private const val PREF_NAME = "com.iflytek.cyber.evs.sdk.auth.pref"
    const val PREF_KEY = "token"

    private var AUTH_URL = "https://auth.iflyos.cn"
    private var AUTH_URL_DEVICE_CODE = "$AUTH_URL/oauth/ivs/device_code"
    private var AUTH_URL_TOKEN = "$AUTH_URL/oauth/ivs/token"

    private const val GRANT_TYPE_DEVICE_CODE = "urn:ietf:params:oauth:grant-type:device_code"
    private const val GRANT_TYPE_REFRESH = "refresh_token"

    private const val KEY_USER_IVS_ALL = "user_ivs_all"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_DEVICE_CODE = "device_code"
    private const val KEY_CLIENT_ID = "client_id"
    private const val KEY_GRANT_TYPE = "grant_type"
    private const val KEY_ERROR = "error"

    const val SCOPE_DATA_DEFAULT = "user_ivs_all"

    const val ERROR_AUTHORIZATION_PENDING = "authorization_pending"
    const val ERROR_EXPIRED_TOKEN = "expired_token"
    const val ERROR_ACCESS_DENIED = "access_denied"

    private var httpClient: OkHttpClient? = null

    private val requestCache = HashSet<Thread>()

    /**
     * 设置auth请求的url。
     */
    fun setAuthUrl(url: String?) {
        if (!url.isNullOrEmpty()) {
            AUTH_URL = url
            AUTH_URL_DEVICE_CODE = "$AUTH_URL/oauth/ivs/device_code"
            AUTH_URL_TOKEN = "$AUTH_URL/oauth/ivs/token"
        }
    }

    /**
     * 从SharedPreference里面获取授权信息。
     * @return 授权结果，返回null则未授权
     */
    fun getAuthResponseFromPref(context: Context): AuthResponse? {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (pref.contains(PREF_KEY)) {
            pref.getString(PREF_KEY, null)?.let { json ->
                return JSON.parseObject(json, AuthResponse::class.java)
            }
        }
        return null
    }

    /**
     * 移除授权信息。
     */
    fun removeAuthResponseFromPref(context: Context) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (pref.contains(PREF_KEY)) {
            pref.edit().remove(PREF_KEY).commit()
        }
    }

    /**
     * 设置授权信息到SharedPreference。
     */
    fun setAuthResponseToPref(context: Context, authResponse: AuthResponse) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().run {
            putString(PREF_KEY, JSON.toJSONString(authResponse))
            apply()
        }
    }

    /**
     * 注册授权token变化监听器。
     */
    fun registerTokenChangedListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * 移除授权token变化监听器。
     */
    fun unregisterTokenChangedListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun createHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .writeTimeout(15L, TimeUnit.SECONDS)
            .readTimeout(15L, TimeUnit.SECONDS)
            .connectTimeout(15L, TimeUnit.SECONDS)
            .callTimeout(15L, TimeUnit.SECONDS)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val context = SSLContext.getInstance("TLSv1.2")
            context.init(null, null, SecureRandom())

            builder.sslSocketFactory(context.socketFactory,
                object : X509TrustManager {
                    override fun checkClientTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?
                    ) {

                    }

                    override fun checkServerTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?
                    ) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return emptyArray()
                    }

                })
        }
        return builder.build()
    }

    /**
     * 开启认证授权过程。
     * @param context Android上下文
     * @param clientId 客户端id，在设备接入控制平台的设备信息页可以找到
     * @param deviceId 端设备id，唯一标识一台设备
     * @param responseCallback 请求结果回调
     * @param authResponseCallback 授权结果回调
     * @param customScopeData 请求授权的能力范围
     */
    fun requestDeviceCode(
        context: Context,
        clientId: String,
        deviceId: String,
        responseCallback: ResponseCallback<DeviceCodeResponse>,
        authResponseCallback: AuthResponseCallback? = null,
        customScopeData: String = SCOPE_DATA_DEFAULT
    ) {
        cancelPolling()

        if (httpClient == null) {
            httpClient = createHttpClient()
        }
        httpClient?.let { httpClient ->
            Thread {
                try {
                    val scopeData = JSONObject()
                    val userIvsAll = JSONObject()
                    userIvsAll[KEY_DEVICE_ID] = deviceId
                    scopeData[KEY_USER_IVS_ALL] = userIvsAll

                    val requestBody =
                        "client_id=$clientId&scope=$customScopeData&scope_data=${scopeData.toJSONString()}"

                    Log.d(TAG, requestBody)

                    Log.e(TAG, "url: $AUTH_URL_DEVICE_CODE")

                    val request = Request.Builder()
                        .url(AUTH_URL_DEVICE_CODE)
                        .post(
                            RequestBody.create(
                                MediaType.get("application/x-www-form-urlencoded"),
                                requestBody
                            )
                        )
                        .build()
                    val response = httpClient.newCall(request).execute()

                    if (response.isSuccessful) {
                        response.body()?.string()?.let { body ->
                            val deviceCodeResponse =
                                JSON.parseObject(body, DeviceCodeResponse::class.java)
                            responseCallback.onResponse(deviceCodeResponse)

                            // 开始轮询 token
                            val newThread = PollingTokenThread(
                                context,
                                httpClient,
                                clientId,
                                deviceCodeResponse,
                                authResponseCallback
                            )
                            requestCache.add(newThread)
                            newThread.start()
                        }
                    } else {
                        responseCallback.onError(response.code(), response.body()?.string(), null)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    responseCallback.onError(null, null, e)
                }
            }.start()
        } ?: run {
            throw IllegalStateException("Cannot create an OkHttp Client.")
        }
    }

    /**
     * 取消授权结果轮询。如果要中途退出授权过程，必须调用该方法。
     */
    fun cancelPolling() {
        Log.d(TAG, "cancelPolling")

        requestCache.map {
            try {
                it.interrupt()
            } catch (_: Exception) {

            }
        }
        requestCache.clear()
    }

    /**
     * 刷新accessToken。当授权的accessToken过期时，需要刷新才能使用EVS。
     * @param context Android上下文
     * @param refreshToken 用于获取新accessToken的刷新token
     * @param refreshCallback 刷新结果回调
     */
    fun refreshAccessToken(
        context: Context,
        refreshToken: String,
        refreshCallback: RefreshCallBack
    ) {
        if (httpClient == null) {
            httpClient = createHttpClient()
        }

        httpClient?.let { httpClient ->
            Thread {
                try {
                    val requestBody = JSONObject()
                    requestBody[KEY_GRANT_TYPE] = GRANT_TYPE_REFRESH
                    requestBody[GRANT_TYPE_REFRESH] = refreshToken

                    val request = Request.Builder()
                        .url(AUTH_URL_TOKEN)
                        .post(
                            RequestBody.create(
                                MediaType.get("application/json"),
                                requestBody.toJSONString()
                            )
                        )
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val httpCode = response.code()
                    val body = response.body()?.string()

                    Log.d(TAG, "code: $httpCode, body: $body")

                    if (response.isSuccessful) {
                        val authResponse = JSON.parseObject(body, AuthResponse::class.java)
                        setAuthResponseToPref(context, authResponse)

                        refreshCallback.onRefreshSuccess(authResponse)
                    } else {
                        refreshCallback.onRefreshFailed(
                            httpCode,
                            null,
                            IllegalStateException("Server return $httpCode while requesting")
                        )
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "polling exception.")

                    e.printStackTrace()
                    refreshCallback.onRefreshFailed(-1, null, e)
                }
            }.start()
        }
    }

    private class PollingTokenThread(
        val context: Context,
        val httpClient: OkHttpClient,
        val clientId: String,
        val deviceCodeResponse: DeviceCodeResponse,
        val authResponseCallback: AuthResponseCallback? = null
    ) : Thread() {
        override fun run() {
            val current = System.currentTimeMillis() / 1000
            val interval = deviceCodeResponse.interval
            val expiresIn = deviceCodeResponse.expiresIn

            while (System.currentTimeMillis() / 1000 - current < expiresIn) {
                try {
                    val requestBody = JSONObject()
                    requestBody[KEY_CLIENT_ID] = clientId
                    requestBody[KEY_GRANT_TYPE] = GRANT_TYPE_DEVICE_CODE
                    requestBody[KEY_DEVICE_CODE] = deviceCodeResponse.deviceCode

                    val request = Request.Builder()
                        .url(AUTH_URL_TOKEN)
                        .post(
                            RequestBody.create(
                                MediaType.get("application/json"),
                                requestBody.toJSONString()
                            )
                        )
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val httpCode = response.code()
                    val body = response.body()?.string()

                    Log.d(TAG, "code: $httpCode, body: $body")

                    if (response.isSuccessful) {
                        val authResponse = JSON.parseObject(body, AuthResponse::class.java)
                        setAuthResponseToPref(context, authResponse)

                        authResponseCallback?.onAuthSuccess(authResponse)
                        return
                    } else {
                        if (httpCode in 400 until 500) {
                            try {
                                val json = JSON.parseObject(body)
                                if (json?.containsKey(KEY_ERROR) == true) {
                                    when (val error = json.getString(KEY_ERROR)) {
                                        ERROR_AUTHORIZATION_PENDING -> {
                                            sleep(interval * 1000L)
                                        }
                                        else -> {
                                            // 可能是 expired_token, 或 access_denied
                                            // 以上两种情况都不需要再轮询
                                            authResponseCallback?.onAuthFailed(error, null)
                                            return
                                        }
                                    }
                                } else {
                                    sleep(interval * 1000L)
                                }
                            } catch (e: JSONException) {
                                authResponseCallback?.onAuthFailed(
                                    body,
                                    null
                                )
                                e.printStackTrace()
                                return
                            }
                        } else {
                            authResponseCallback?.onAuthFailed(
                                null,
                                IllegalStateException("Server return $httpCode while requesting")
                            )
                            return
                        }
                    }
                } catch (e: InterruptedException) {
                    // interrupted, no need to callback
                    e.printStackTrace()
                    return
                } catch (e: Exception) {
                    e.printStackTrace()
                    authResponseCallback?.onAuthFailed(null, e)
                    return
                }
            }
            authResponseCallback?.onAuthFailed(ERROR_EXPIRED_TOKEN, null)
        }
    }

    interface ResponseCallback<T> {
        fun onResponse(response: T)
        fun onError(httpCode: Int?, errorBody: String?, throwable: Throwable?)
    }

    interface AuthResponseCallback {
        fun onAuthSuccess(authResponse: AuthResponse)
        fun onAuthFailed(errorBody: String?, throwable: Throwable?)
    }

    interface RefreshCallBack {
        fun onRefreshSuccess(authResponse: AuthResponse)
        fun onRefreshFailed(httpCode: Int, errorBody: String?, throwable: Throwable?)
    }
}