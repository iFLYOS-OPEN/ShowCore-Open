/*
 * Copyright (C) 2019 iFLYTEK CO.,LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iflytek.cyber.evs.sdk.player

import android.net.Uri
import android.os.Build
import okhttp3.OkHttpClient
import okhttp3.OkUrlFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.regex.Pattern
import javax.net.ssl.SSLContext

class PlaylistParser {
    private val mExecutor = Executors.newSingleThreadExecutor()
    //    private val mOkHttpClient = Tls12SocketFactory.enableTls12OnPreLollipop(OkHttpClient.Builder()).build()
    private val mOkHttpClient: OkHttpClient
    private val mOkHttpFactory: OkUrlFactory

    init {
        val clientBuilder = OkHttpClient.Builder()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            val context = SSLContext.getInstance("TLSv1.2")
            context.init(null, null, SecureRandom())
            clientBuilder.sslSocketFactory(context.socketFactory)
        }
        mOkHttpClient = clientBuilder.build()
        mOkHttpFactory = OkUrlFactory(mOkHttpClient)
    }
    // Extracts Url from redirect Url. Note: not a complete playlist parser implementation
    @Throws(IOException::class)
    internal fun parseUri(uri: Uri): Uri {
        return parsePlaylist(parseResponse(getResponse(uri)))
    }

    @Throws(IOException::class)
    private fun getResponse(uri: Uri): InputStream {
        val response = mExecutor.submit(Callable {

            var con: HttpURLConnection? = null
            try {
                val obj = URL(uri.toString())
                con = mOkHttpFactory.open(obj)

                val responseCode = con!!.responseCode
                return@Callable if (responseCode == sResponseOk) {
                    con.inputStream
                } else {
                    throw IOException("$sTag: Unsuccessful response. Code: $responseCode")
                }
            } finally {
                con?.disconnect()
            }
        })

        try {
            return response.get()
        } catch (e: Exception) {
            throw IOException(sTag + ": Error getting response: " + e.message)
        }

    }

    @Throws(IOException::class)
    private fun parseResponse(inStream: InputStream?): String? {
        if (inStream != null) {
            val `in` = BufferedReader(InputStreamReader(inStream))
            var inputLine: String?
            val response = StringBuilder()

            try {
                inputLine = `in`.readLine()
                while (inputLine != null) {
                    response.append(inputLine)
                    inputLine = `in`.readLine()
                }
                return response.toString()
            } catch (e: Exception) {
                throw IOException("$sTag: Error parsing response")
            } finally {
                inStream.close()
            }
        }
        return null
    }

    companion object {
        private val sTag = "PlaylistParser"
        private val sResponseOk = 200
        private val sPattern = Pattern.compile("https?:.*")

        @Throws(IOException::class)
        private fun parsePlaylist(playlist: String?): Uri {
            if (playlist != null && !playlist.isEmpty()) {
                val matcher = sPattern.matcher(playlist)
                return if (matcher.find()) {
                    Uri.parse(playlist.substring(matcher.start(), matcher.end()))
                } else {
                    throw IOException("$sTag: Response did not contain a URL")
                }
            } else {
                throw IOException("$sTag: Response was empty")
            }
        }
    }
}
