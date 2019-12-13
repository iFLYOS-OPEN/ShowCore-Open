package com.iflytek.cyber.iot.show.core

import android.content.Context
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
import java.util.concurrent.TimeUnit

class CoreApplication : MultiDexApplication() {

    private var retrofit: Retrofit? = null

    private var apis: HashMap<Class<out Any>, Any> = HashMap()

    companion object {
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

    @Suppress("UNCHECKED_CAST")
    fun <T> createApi(clazz: Class<out T>): T? {
        if (!apis.containsKey(clazz)) {
            val api = retrofit?.create(clazz)
            apis.put(clazz, api as Any)
        }
        return apis[clazz] as T
    }
}