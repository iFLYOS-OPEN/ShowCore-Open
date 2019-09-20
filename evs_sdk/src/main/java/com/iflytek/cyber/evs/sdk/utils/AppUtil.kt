package com.iflytek.cyber.evs.sdk.utils

import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import java.util.ArrayList

object AppUtil {
    private const val TYPE_ACTIVITY = "activity"
    private const val TYPE_SERVICE = "service"
    private const val TYPE_BROADCAST = "broadcast"

    data class AppInfo(val appName: String?, val pkgName: String?, val curActivity: String?,
                       val version: Int = 0)

    private var lastForeApp: AppInfo? = null
    private var lastQueryTime: Long = 0

    fun getForegroundApp(context: Context): AppInfo? {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningTasks: List<ActivityManager.RunningTaskInfo> = activityManager.getRunningTasks(1)

            if (runningTasks.isNotEmpty()) {
                val pkgName = runningTasks[0].topActivity.packageName
                val clsName = runningTasks[0].topActivity.className

                return if (pkgName != context.packageName)
                    AppInfo("", pkgName, clsName)
                else
                    AppInfo("", "DEFAULT", clsName)
            }
        } else {
            val time: Long = System.currentTimeMillis()
            val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            val events: UsageEvents = usageManager.queryEvents(lastQueryTime, time)
            var lastEvent: UsageEvents.Event? = null

            while (events.hasNextEvent()) {
                val curEvent: UsageEvents.Event = UsageEvents.Event()
                events.getNextEvent(curEvent)
                if (curEvent.packageName.isNullOrEmpty() || curEvent.className.isNullOrEmpty()) {
                    continue
                }

                if (lastEvent == null || curEvent.timeStamp > lastEvent.timeStamp) {
                    lastEvent = curEvent
                }
            }

            if (lastEvent != null) {
                lastForeApp =
                    if (lastEvent.packageName != context.packageName)
                        AppInfo("", lastEvent.packageName, lastEvent.className)
                    else
                        AppInfo("", "DEFAULT", lastEvent.className)
                lastQueryTime = time - 60 * 60 * 1000
            }

            return lastForeApp
        }

        return null
    }

    fun getAppInfo(context: Context, pkgName: String): AppInfo? {
        val pm = context.packageManager
        val appInfo: AppInfo?

        try {
            val info = pm.getPackageInfo(pkgName, 0)
            val appName = info.applicationInfo.loadLabel(pm)
            val version = info.versionCode

            appInfo = AppInfo(appName.toString(), pkgName, "", version)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }

        return appInfo
    }

    fun isActionSupported(context: Context, intent: Intent, type: String = ""): Boolean {
        val pm = context.packageManager

        when (type) {
            TYPE_ACTIVITY -> {
                val activityList: List<ResolveInfo> = pm.queryIntentActivities(intent,
                    PackageManager.GET_RESOLVED_FILTER)
                if (activityList.isNotEmpty()) {
                    return true
                }
            }
            TYPE_SERVICE -> {
                val activityList: List<ResolveInfo> = pm.queryIntentServices(intent,
                    PackageManager.GET_RESOLVED_FILTER)
                if (activityList.isNotEmpty()) {
                    return true
                }
            }
            TYPE_BROADCAST -> {
                val activityList: List<ResolveInfo> = pm.queryBroadcastReceivers(intent,
                    PackageManager.GET_RESOLVED_FILTER)
                if (activityList.isNotEmpty()) {
                    return true
                }
            }
            else -> {
                val list1: List<ResolveInfo> = pm.queryIntentActivities(intent,
                    PackageManager.GET_RESOLVED_FILTER)
                val list2: List<ResolveInfo> = pm.queryIntentServices(intent,
                    PackageManager.GET_RESOLVED_FILTER)
                val list3: List<ResolveInfo> = pm.queryBroadcastReceivers(intent,
                    PackageManager.GET_RESOLVED_FILTER)

                if (list1.isNotEmpty() || list2.isNotEmpty() || list3.isNotEmpty()) {
                    return true
                }
            }
        }

        return false
    }

    fun isPackageExist(context: Context, pkgName: String?, acceptUri: String?): Boolean {
        val pm = context.packageManager
        val intent = Intent()
        intent.setPackage(pkgName)

        var tmpUri: Uri? = null
        if (!acceptUri.isNullOrEmpty()) {
            tmpUri = Uri.parse(acceptUri)
            intent.data = tmpUri
        }

        val activityList: List<ResolveInfo> = pm.queryIntentActivities(intent,
                            PackageManager.GET_RESOLVED_FILTER)
        val serviceList: List<ResolveInfo> = pm.queryIntentServices(intent,
                            PackageManager.GET_RESOLVED_FILTER)
        val broadcastList: List<ResolveInfo> = pm.queryBroadcastReceivers(intent,
                            PackageManager.GET_RESOLVED_FILTER)

        val infoList: MutableList<ResolveInfo> = ArrayList()
        infoList.addAll(activityList)
        infoList.addAll(serviceList)
        infoList.addAll(broadcastList)

        if (!infoList.isNullOrEmpty()) {
            return true
        }

        return false
    }
}