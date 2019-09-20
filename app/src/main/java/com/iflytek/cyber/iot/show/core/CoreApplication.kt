package com.iflytek.cyber.iot.show.core

import android.content.Context
import androidx.multidex.MultiDexApplication
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
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