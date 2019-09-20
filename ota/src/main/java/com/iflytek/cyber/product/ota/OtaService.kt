package com.iflytek.cyber.product.ota

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.jaredrummler.apkparser.ApkParser
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okio.ByteString.Companion.toByteString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.collections.HashSet

/**
 * 一个简易的检查更新服务
 */
class OtaService : Service() {
    companion object {
        const val ACTION_REQUEST_CHECKING = "com.iflytek.cyber.product.ota.action.REQUEST_CHECKING"
        const val ACTION_START_SERVICE = "com.iflytek.cyber.product.ota.action.START_SERVICE"
        const val ACTION_NEW_UPDATE_DOWNLOADED = "com.iflytek.cyber.product.ota.action.NEW_UPDATE_DOWNLOADED"
        const val ACTION_NO_UPDATE_FOUND = "com.iflytek.cyber.product.ota.action.NO_UPDATE_FOUND"
        const val ACTION_CHECK_UPDATE_FAILED = "com.iflytek.cyber.product.ota.action.CHECK_UPDATE_FAILED"
        const val ACTION_NEW_UPDATE_DOWNLOAD_STARTED = "com.iflytek.cyber.product.ota.action.NEW_UPDATE_DOWNLOAD_STARTED"
        const val ACTION_CHECK_UPDATE_RESULT = "com.iflytek.cyber.product.ota.action.CHECK_UPDATE_RESULT"
        const val ACTION_REQUEST_DOWNLOAD = "com.iflytek.cyber.product.ota.action.REQUEST_DOWNLOAD"
        private const val TAG = "OtaService"
        private const val OTA_URL = "https://ota.iflyos.cn"
        private const val PREF_NAME = "com.iflytek.cyber.product.ota.pref"

        const val EXTRA_DEVICE_ID = "device_id"
        const val EXTRA_CLIENT_ID = "client_id"
        const val EXTRA_VERSION_ID = "version_id"
        const val EXTRA_OTA_SECRET = "ota_secret"
        const val EXTRA_DOWNLOAD_PATH = "download_path"
        @Deprecated("Won't be use", replaceWith = ReplaceWith("EXTRA_VERSION_ID"))
        const val EXTRA_VERSION = "version"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_PACKAGE_ENTITY = "package_entity"
        const val EXTRA_URL = "url"
        const val EXTRA_ID = "id"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_DOWNLOAD_DIRECTLY = "download_directly"

        const val EXTRA_PATH = "path"

        const val CHECK_INTERVAL = 24 * 3600 * 1000 // 每两次自动检查更新的时间间隔
    }

    private lateinit var otaApi: OtaApi
    private lateinit var client: OkHttpClient
    private lateinit var retrofit: Retrofit
    private lateinit var pref: SharedPreferences

    private var deviceId: String? = null
    private var clientId: String? = null
    private var versionId: Int? = null
    private var clientSecret: String? = null
    private var downloadPath: String? = null
    private var otaPackageName: String? = null

    private var downloadingId: Long? = null
    private var currentPackageEntity: PackageEntityNew? = null

    private var currentConnection: HttpURLConnection? = null

    private val downloadProgressCallbacks = HashSet<DownloadProgressCallback>()

    private val binder = OtaBinder(this)

    class OtaBinder(private val service: OtaService) : Binder() {
        fun service(): OtaService {
            return service
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        val clientBuilder = OkHttpClient.Builder()

        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            clientBuilder.addInterceptor(interceptor)
        }

        clientBuilder.addInterceptor { chain ->
            val timestamp = System.currentTimeMillis() / 1000
            val nonce = (Math.random() * 10000).toInt().toString()

            val signature = sha1(String.format(Locale.US, "%s:%s:%s:%s:%s",
                clientId, deviceId, timestamp, nonce, clientSecret))

            Log.d(TAG, "Using deviceId: $deviceId, clientId: $clientId, clientSecret: $clientSecret, versionId: $versionId")
            val request = chain.request()
                .newBuilder()
                .addHeader("X-Client-ID", clientId.toString())
                .addHeader("X-Device-ID", deviceId.toString())
                .addHeader("X-Version-ID", (versionId ?: 0).toString())
                .addHeader("X-Timestamp", timestamp.toString())
                .addHeader("X-Nonce", nonce)
                .addHeader("X-Signature", signature)
                .build()

            chain.proceed(request)
        }

        client = clientBuilder
            .build()
        retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(OTA_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        otaApi = retrofit.create(OtaApi::class.java)

        pref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun sha1(data: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-1")
            digest.update(data.toByteArray())
            ByteBuffer.wrap(digest.digest()).toByteString().hex()
        } catch (e: NoSuchAlgorithmException) {
            ""
        }

    }

    fun isUrlDownloaded(url: String?): Boolean {
        url ?: return false
        val filePath = getUrlDownloadedPath(url)
        val file = File(filePath)
        if (file.exists()) {
            return try {
                ApkParser.create(file)
                true
            } catch (e: ApkParser.InvalidApkException) {
                false
            }
        }
        return false
    }

    fun getUrlDownloadedPath(url: String?): String? {
        url?.let {
            return "$downloadPath/${sha1(url)}.apk"
        }
        return null
    }

    private fun downloadFile(id: Long, url: String) {
        downloadingId = id

        val filePath = getUrlDownloadedPath(url)
        val file = File(filePath)
        var ifNeedNewFile: Boolean
        if (file.exists()) {
            try {
                val apkParser = ApkParser.create(file)
                if (apkParser.apkMeta.versionCode >= versionId!!
                    && apkParser.apkMeta.packageName == otaPackageName) {
                    // 判断本地下载的 apk 版本已经比已安装的版本新
                    ifNeedNewFile = false

                    // 发送已经下载好新的 apk 的广播
                    val intent = Intent(ACTION_NEW_UPDATE_DOWNLOADED)
                    intent.putExtra(EXTRA_PATH, file.path)
                    currentPackageEntity?.let { packageEntityNew ->
                        intent.putExtra(EXTRA_PACKAGE_ENTITY, packageEntityNew)
                    }
                    sendBroadcast(intent)
                } else {
                    file.delete()

                    ifNeedNewFile = true
                }
            } catch (e: ApkParser.InvalidApkException) {
                // 如果文件下载未完成就被打断，就会导致无法解析
                e.printStackTrace()
                file.delete()
                ifNeedNewFile = true
            }
        } else {
            ifNeedNewFile = true
        }
        if (ifNeedNewFile) {
            file.createNewFile()
            sendBroadcast(Intent(ACTION_NEW_UPDATE_DOWNLOAD_STARTED))
            Thread {
                var cacheInput: InputStream? = null
                var cacheOutput: OutputStream? = null
                try {
                    val urlObj = URL(url)
                    val connection = urlObj.openConnection() as HttpURLConnection

                    currentConnection = connection

                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.setRequestProperty("Connection", "Keep-Alive")
                    connection.setRequestProperty("Charset", "UTF-8")
                    connection.readTimeout = 10000
                    connection.doInput = true
                    connection.useCaches = false

                    connection.connect()

                    val contentLength = connection.contentLength

                    val outputStream = FileOutputStream(file)
                    val inputStream = connection.inputStream

                    cacheInput = inputStream
                    cacheOutput = outputStream

                    val bytes = ByteArray(2048)
                    var totalRead = 0
                    var readSize = 0

                    while (readSize != -1) {
                        readSize = inputStream.read(bytes)

                        if (readSize != -1) {
                            totalRead += readSize

                            outputStream.write(bytes, 0, readSize)

                            downloadProgressCallbacks.map {
                                try {
                                    it.onDownloadProgress(id, (100f * totalRead / contentLength).toInt())
                                } catch (t: Throwable) {
                                    t.printStackTrace()
                                }
                            }
                        }
                    }
                    outputStream.flush()

                    // 判断是否被一个新的下载请求抢了
                    if (currentConnection == connection) {
                        try {
                            val apkParser = ApkParser.create(file)

                            if (apkParser.apkMeta.packageName != otaPackageName) {
                                val intent = Intent(ACTION_CHECK_UPDATE_FAILED)
                                intent.putExtra(EXTRA_MESSAGE, getString(R.string.update_failed_apk_package_wrong))
                                sendBroadcast(intent)
                            } else {
                                // 发送已经下载好新的 apk 的广播
                                val intent = Intent(ACTION_NEW_UPDATE_DOWNLOADED)
                                intent.putExtra(EXTRA_PATH, file.path)
                                currentPackageEntity?.let { packageEntityNew ->
                                    intent.putExtra(EXTRA_PACKAGE_ENTITY, packageEntityNew)
                                }
                                sendBroadcast(intent)
                            }
                        } catch (e: ApkParser.InvalidApkException) {
                            e.printStackTrace()

                            val intent = Intent(ACTION_CHECK_UPDATE_FAILED)
                            intent.putExtra(EXTRA_MESSAGE, getString(R.string.update_failed_invalid_apk))
                            sendBroadcast(intent)
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()

                    val intent = Intent(ACTION_CHECK_UPDATE_FAILED)
                    intent.putExtra(EXTRA_MESSAGE, getString(R.string.update_failed_download_error))
                    sendBroadcast(intent)
                } finally {
                    cacheInput?.close()
                    cacheOutput?.close()
                }
            }.start()
        }
    }

    /**
     * 开始检查更新
     */
    private fun startChecking(downloadDirectly: Boolean = false) {
        otaApi.getClientCheckPackages().enqueue(object : Callback<List<PackageEntityNew>> {
            override fun onFailure(call: Call<List<PackageEntityNew>>, t: Throwable) {
                t.printStackTrace()
                sendBroadcast(Intent(ACTION_CHECK_UPDATE_FAILED))
            }

            override fun onResponse(call: Call<List<PackageEntityNew>>, response: Response<List<PackageEntityNew>>) {
                if (response.isSuccessful) {
                    val body = response.body()

                    val updateResult = Intent(ACTION_CHECK_UPDATE_RESULT)
                    val packageEntity = if (body?.isNotEmpty() == true) {
                        val packageEntity = body[0]
                        updateResult.putExtra(EXTRA_PACKAGE_ENTITY, packageEntity)
                        packageEntity
                    } else {
                        null
                    }
                    currentPackageEntity = packageEntity
                    sendBroadcast(updateResult)

                    if (downloadDirectly) {
                        packageEntity?.let { packageEntityNew ->
                            packageEntityNew.id ?: return
                            packageEntityNew.url ?: return
                            downloadFile(packageEntityNew.id, packageEntityNew.url)
                        }
                    }
                } else {
                    sendBroadcast(Intent(ACTION_CHECK_UPDATE_FAILED))
                }
            }
        })
    }

    fun registerDownloadProgressCallback(downloadProgressCallback: DownloadProgressCallback) {
        downloadProgressCallbacks.add(downloadProgressCallback)
    }

    fun unregisterDownloadProgressCallback(downloadProgressCallback: DownloadProgressCallback) {
        downloadProgressCallbacks.remove(downloadProgressCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REQUEST_CHECKING -> {
                val downloadDirectly = intent.getBooleanExtra(EXTRA_DOWNLOAD_DIRECTLY, false)

                startChecking(downloadDirectly)

                // 在指定时间间隔后继续检查更新
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val serviceIntent = Intent(this, OtaService::class.java)
                serviceIntent.action = ACTION_REQUEST_CHECKING
                serviceIntent.putExtra(EXTRA_DOWNLOAD_DIRECTLY, true)
                val pendingIntent = PendingIntent.getService(this, 1000, serviceIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT)
                alarmManager.set(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + CHECK_INTERVAL, pendingIntent)
            }
            ACTION_START_SERVICE -> {
                clientId = intent.getStringExtra(EXTRA_CLIENT_ID)
                deviceId = intent.getStringExtra(EXTRA_DEVICE_ID)
                versionId = intent.getIntExtra(EXTRA_VERSION_ID, 0)
                clientSecret = intent.getStringExtra(EXTRA_OTA_SECRET)
                downloadPath = intent.getStringExtra(EXTRA_DOWNLOAD_PATH)
                otaPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)

                val downloadDirectly = intent.getBooleanExtra(EXTRA_DOWNLOAD_DIRECTLY, false)

                startChecking(downloadDirectly)

                // 在指定时间间隔后继续检查更新
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val serviceIntent = Intent(this, OtaService::class.java)
                serviceIntent.action = ACTION_REQUEST_CHECKING
                serviceIntent.putExtra(EXTRA_DOWNLOAD_DIRECTLY, true)
                val pendingIntent = PendingIntent.getService(this, 1000, serviceIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT)
                alarmManager.set(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + CHECK_INTERVAL, pendingIntent)
            }
            ACTION_REQUEST_DOWNLOAD -> {
                val id = intent.getLongExtra(EXTRA_ID, -1L)
                intent.getStringExtra(EXTRA_URL)?.let { url ->
                    downloadFile(id, url)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    interface DownloadProgressCallback {
        fun onDownloadProgress(id: Long, progress: Int)
    }

}