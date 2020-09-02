package com.iflytek.cyber.iot.show.core


import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_PACKAGE_ADDED
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import com.iflytek.cyber.iot.show.core.internal.pm.PackageManagerCompat
import com.iflytek.cyber.iot.show.core.utils.ACache
import com.iflytek.cyber.iot.show.core.utils.AsyncScheduler
import java.io.File

class PreInstallService : Service() {

    companion object {
        const val KEY_INSTALL_LIST = "INSTALL_LIST"
        const val KEY_ALREADY_INSTALLED = "ALREADY_INSTALLED"
    }

    private var installList = arrayListOf(
        "com.dianshijia.newlive",
        "com.qiyi.video.speaker",
        "tv.danmaku.bili",
        "net.myvst.v2",
        "com.sina.news.pad"
    )
    private var needInstalledList: ArrayList<InstallFile>? = null
    private var isRegister = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    data class InstallFile(
        val packageName: String,
        val fileName: String,
        val filePath: String
    )

    override fun onCreate() {
        super.onCreate()

        val alreadyInstalled = ACache.get(this).getAsObject(KEY_ALREADY_INSTALLED) as? Boolean
        Log.d("PreInstallService", "already installed: " + alreadyInstalled)
        if (alreadyInstalled == null || alreadyInstalled == false) {
            isRegister = true
            val intentFilter = IntentFilter()
            intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
            intentFilter.addDataScheme("package")
            registerReceiver(installReceiver, intentFilter)

            installApk()
        } else {
            stopSelf()
        }
    }

    private fun installApk() {
        val apkDirectory = File("/system/preinstall-apk")
        if (!apkDirectory.exists()) {
            stopSelf()
            return
        }

        val apkList = apkDirectory.listFiles()
        /*if (needInstalledList == null) {
            needInstalledList = ArrayList()
            apkList.forEachIndexed { index, file ->
                val packageName = installList[index]
                needInstalledList?.add(InstallFile(packageName, file.name, file.path))
            }
        }*/
        if (apkList.isNullOrEmpty()) {
            stopSelf()
            return
        }
        /*var installList = ACache.get(this).getAsObject(KEY_INSTALL_LIST) as? ArrayList<*>
        if (installList == null) {
            installList = this.installList
            ACache.get(this).put(KEY_INSTALL_LIST, installList)
        }*/
        AsyncScheduler.execute {
            apkList.forEach {
                /*if (installList.contains(it.packageName)) {
                    PackageManagerCompat.installApk(this, packageManager, it.filePath)
                    Thread.sleep(1000)
                }*/
                PackageManagerCompat.installApk(this, packageManager, it.path)
                Thread.sleep(1500)
            }
        }
    }

    private val installReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val packageName = intent?.data?.encodedSchemeSpecificPart
            Log.d(
                "PreInstallService",
                "install action: " + intent?.action + "  package name: " + packageName
            )
            if (intent?.action == ACTION_PACKAGE_ADDED) {
                //val installList = ACache.get(this@PreInstallService).getAsObject(KEY_INSTALL_LIST) as? ArrayList<*> ?: return
                if (installList.contains(packageName)) {
                    installList.remove(packageName)
                    ACache.get(this@PreInstallService).put(KEY_INSTALL_LIST, installList)
                }
                if (installList.size == 0) { //install finish, stop service
                    ACache.get(this@PreInstallService).put(KEY_ALREADY_INSTALLED, true)
                    this@PreInstallService.stopSelf()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRegister) {
            unregisterReceiver(installReceiver)
        }
    }
}
