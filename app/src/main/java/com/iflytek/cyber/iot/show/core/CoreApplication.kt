package com.iflytek.cyber.iot.show.core

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.multidex.MultiDexApplication
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.kk.taurus.exoplayer.ExoMediaPlayer
import com.kk.taurus.ijkplayer.IjkPlayer
import com.kk.taurus.playerbase.config.PlayerConfig
import com.kk.taurus.playerbase.config.PlayerLibrary
import com.kk.taurus.playerbase.entity.DecoderPlan
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit

class CoreApplication : MultiDexApplication() {

    private var retrofit: Retrofit? = null

    private var okHttpClient: OkHttpClient? = null

    private var apis: HashMap<Class<out Any>, Any> = HashMap()

    companion object {
        private const val TAG = "CoreApplication"
        private const val PLAN_ID_IJK = 1
        private const val PLAN_ID_EXO = 2

        fun from(context: Context): CoreApplication {
            return context.applicationContext as CoreApplication
        }
    }

    override fun onCreate() {
        super.onCreate()

        //AuthDelegate.setAuthUrl("https://staging-auth.iflyos.cn")

        val builder = OkHttpClient.Builder()

        builder.connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .pingInterval(1, TimeUnit.SECONDS)
            .addInterceptor(getNetworkInterceptor())

        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(interceptor)
        }

        val client = builder.build()

        okHttpClient = client

        retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.HOST)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        PlayerConfig.addDecoderPlan(
            DecoderPlan(
                PLAN_ID_IJK,
                IjkPlayer::class.java.name,
                "IjkPlayer"
            )
        )
        PlayerConfig.addDecoderPlan(
            DecoderPlan(
                PLAN_ID_EXO,
                ExoMediaPlayer::class.java.name,
                "ExoMediaPlayer"
            )
        )
        // 这里可以选择视频播放使用的默认解码方案
        // * EXO 方案更节省算力，低配置机器建议使用
        // * IJK 方案支持更全的视频编码，中高配机器建议使用
        PlayerConfig.setDefaultPlanId(PLAN_ID_IJK)
        PlayerConfig.setUseDefaultNetworkEventProducer(true)

        PlayerLibrary.init(this)
        IjkPlayer.init(this)
        ExoMediaPlayer.init(this)

        hookWebView()
    }

    private fun getNetworkInterceptor(): Interceptor {
        return Interceptor { chain ->
            val auth = AuthDelegate.getAuthResponseFromPref(this@CoreApplication)
            var request = chain.request()

            if (!auth?.accessToken.isNullOrEmpty()) {
                request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${auth?.accessToken}")
                    .build()
            }
            chain.proceed(request)
        }
    }

    private fun hookWebView() {
        val sdkInt = Build.VERSION.SDK_INT
        try {
            val factoryClass = Class.forName("android.webkit.WebViewFactory")
            val field = factoryClass.getDeclaredField("sProviderInstance")
            field.isAccessible = true
            var sProviderInstance = field.get(null)
            if (sProviderInstance != null) {
                Log.d(TAG, "sProviderInstance isn't null")
                return
            }
            val getProviderClassMethod: Method
            getProviderClassMethod = when {
                sdkInt > 22 -> // above 22
                    factoryClass.getDeclaredMethod("getProviderClass")
                sdkInt == 22 -> // method name is a little different
                    factoryClass.getDeclaredMethod("getFactoryClass")
                else -> { // no security check below 22
                    Log.i(TAG, "Don't need to Hook WebView")
                    return
                }
            }
            getProviderClassMethod.isAccessible = true
            val providerClass = getProviderClassMethod.invoke(factoryClass) as Class<*>
            val delegateClass = Class.forName("android.webkit.WebViewDelegate")
            val declaredConstructor = delegateClass.getDeclaredConstructor()
            declaredConstructor.isAccessible = true
            val delegate = declaredConstructor.newInstance()
            val providerConstructor = providerClass.getMethod("create", delegateClass)
            providerConstructor.isAccessible = true
            sProviderInstance = providerConstructor.invoke(null, delegate)
            Log.d(TAG, "sProviderInstance:{$sProviderInstance}")
            field.set("sProviderInstance", sProviderInstance)
            Log.d(TAG, "Hook done!")
        } catch (e: Throwable) {
            e.printStackTrace()
        }

    }

    fun getClient(): OkHttpClient? {
        return okHttpClient
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> createApi(clazz: Class<out T>): T? {
        if (!apis.containsKey(clazz)) {
            val api = retrofit?.create(clazz)
            apis.put(clazz, api as Any)
        }
        return apis[clazz] as T
    }
}