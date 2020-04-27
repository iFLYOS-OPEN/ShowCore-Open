package com.iflytek.cyber.iot.show.core.recommend

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.iflytek.cyber.iot.show.core.CoreApplication
import okhttp3.Request
import java.lang.Exception
import com.google.gson.JsonParser


object RecommendAgent {
    private const val TAG = "RecommendAgent"

    fun <T> getRecommendList(context: Context, url: String, cls : Class<T>): List<T>? {
        val client = CoreApplication.from(context).getClient()
        val request = Request.Builder().get()
                .url(url)
                .build()

        if (client != null) {
            val call = client.newCall(request)
            try {
                val response = call.execute()
                if (response.isSuccessful) {
                    response.body()?.let {
                        val result = it.string()
                        val list = ArrayList<T>()

                        try {
                            val array = JsonParser().parse(result).asJsonArray
                            for (jsonElement in array) {
                                list.add(Gson().fromJson<T>(jsonElement, cls))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        return list
                    }
                } else {
                    Log.d(TAG, "error=${response.code()}")
                }
            } catch (e1: Exception) {
                e1.printStackTrace()
            }
        }

        return null
    }

}