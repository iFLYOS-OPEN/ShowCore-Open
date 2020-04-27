package com.iflytek.cyber.iot.show.core.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.EngineService
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.AppAdapter
import com.iflytek.cyber.iot.show.core.api.AppApi
import com.iflytek.cyber.iot.show.core.launcher.AppData
import com.iflytek.cyber.iot.show.core.launcher.LauncherDataHelper
import com.iflytek.cyber.iot.show.core.launcher.LauncherMemory
import com.iflytek.cyber.iot.show.core.launcher.TemplateAppData
import com.iflytek.cyber.iot.show.core.model.TemplateApp
import com.iflytek.cyber.iot.show.core.utils.OnItemClickListener
import kotlinx.android.synthetic.main.fragment_skill.*
import me.yokeyword.fragmentation.ISupportFragment
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LauncherFragment2 : BaseFragment(), PageScrollable {
    companion object {
        private const val TAG = "LauncherFragment2"

        private const val PAGE_ALARMS = "Launcher.Alarms"
        private const val PAGE_SETTINGS = "Launcher.Settings"
        private const val PAGE_SKILLS = "Launcher.Skills"
        private const val PAGE_CONTENTS = "Launcher.Contents"
        private const val PAGE_SPEAK_EVALUATION = "Launcher.Speak.Evaluation"
        private const val PAGE_SMART_HOME = "Launcher.Smart.Home"

        /** 位于黑名单中的包名不予显示 **/
        private val blackPartyAppArray = arrayOf(
            "com.android.documentsui",
            "com.android.quicksearchbox",
            "com.android.contacts",
            "com.android.camera2",
            "com.android.soundrecorder",
            "com.android.calendar",
            "com.android.apkinstaller",
            "com.android.rk",
            "com.android.gallery3d",
            "com.android.settings",
            "acr.browser.barebones",
            "com.android.calculator2",
            "com.android.email",
            "com.android.music",
            "com.android.deskclock",
            "com.android.traceur",
            "android.rk.RockVideoPlayer",
            "org.chromium.webview_shell",
            // iflytek
            "com.iflytek.ageing",
            "com.iflytek.iflyos.reliablityfortablet",
            "com.iflytek.cyber.iot.show.core"
        )
        private val privateSortedApps = arrayOf(
            "com.qiyi.video.speaker",
            "com.dianshijia.newlive"
        )
    }

    private var recyclerView: RecyclerView? = null
    private val adapter = AppAdapter()
    private var launcherDataHelper: LauncherDataHelper? = null

    private var backCount = 0

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Thread {
                postAppUpdated()
            }.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_PACKAGE_ADDED)
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED)
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        filter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED)
        filter.addDataScheme("package")
        context?.registerReceiver(packageReceiver, filter)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_launcher_2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = view.context
        view.findViewById<View>(R.id.back)?.setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            pop()
        }

        recyclerView = view.findViewById(R.id.recycler_view)

        // init internal apps
        val internalApps = mutableListOf<AppData>()
        internalApps.add(
            AppData(
                getString(R.string.content_discovery),
                PAGE_CONTENTS,
                resources.getDrawable(R.drawable.app_music),
                null,
                AppData.TYPE_INTERNAL
            )
        )
        internalApps.add(
            AppData(
                getString(R.string.alarm_title),
                PAGE_ALARMS,
                resources.getDrawable(R.drawable.app_alarm),
                null,
                AppData.TYPE_INTERNAL
            )
        )
        internalApps.add(
            AppData(
                getString(R.string.skills_store),
                PAGE_SKILLS,
                resources.getDrawable(R.drawable.app_skill),
                null,
                AppData.TYPE_INTERNAL
            )
        )
        internalApps.add(
            AppData(
                getString(R.string.settings),
                PAGE_SETTINGS,
                resources.getDrawable(R.drawable.app_setting),
                null,
                AppData.TYPE_INTERNAL
            )
        )
        internalApps.add(
            AppData(
                getString(R.string.smart_home),
                PAGE_SMART_HOME,
                resources.getDrawable(R.drawable.app_iot),
                null,
                AppData.TYPE_INTERNAL
            )
        )
        internalApps.add(
            AppData(
                getString(R.string.speak_evaluation),
                PAGE_SPEAK_EVALUATION,
                resources.getDrawable(R.drawable.app_speak_evaluation),
                null,
                AppData.TYPE_INTERNAL
            )
        )

        adapter.setInternalAppData(internalApps)

        run {
            // update from memory
            LauncherMemory.partyApps?.let {
                if (it.isNotEmpty()) {
                    adapter.setPartyAppData(it)
                }
            }
            LauncherMemory.templateApps?.let {
                if (it.isNotEmpty()) {
                    adapter.setTemplateAppData(it)
                }
            }
            LauncherMemory.privateApps?.let {
                if (it.isNotEmpty()) {
                    adapter.setPrivateAppData(it)
                }
            }
        }

        adapter.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(parent: ViewGroup, itemView: View, position: Int) {
                adapter.getAppData(position)?.let {
                    when (it.appType) {
                        AppData.TYPE_TEMPLATE -> {
                            if (it is TemplateAppData) {
                                val templateApp = it.toTemplateApp()
                                when (templateApp.type) {
                                    TemplateAppData.TYPE_TEMPLATE -> {
                                        setTemplate(templateApp)
                                    }
                                    TemplateAppData.TYPE_H5_APP -> {
                                        val webViewFragment = WebViewFragment().apply {
                                            arguments = bundleOf(Pair(WebViewFragment.EXTRA_URL, templateApp.url))
                                        }
                                        start(webViewFragment)
                                    }
                                    TemplateAppData.TYPE_SKILL -> {
                                        val textIn = Intent(context, EngineService::class.java)
                                        textIn.action = EngineService.ACTION_SEND_TEXT_IN
                                        textIn.putExtra(EngineService.EXTRA_QUERY, templateApp.textIn)
                                        context?.startService(textIn)
                                    }
                                    else -> {
                                    }
                                }
                            } else {
                                // ignore
                            }
                        }
                        AppData.TYPE_PARTY -> {
                            val packageManager = context.packageManager
                            packageManager.getLaunchIntentForPackage(it.packageName)
                                ?.let { intent ->
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                }
                        }
                        AppData.TYPE_INTERNAL -> {
                            when (it.packageName) {
                                PAGE_ALARMS -> {
                                    extraTransaction().startDontHideSelf(
                                        AlarmFragment(),
                                        ISupportFragment.SINGLETOP
                                    )
                                }
                                PAGE_CONTENTS -> {
                                    extraTransaction().startDontHideSelf(
                                        MediaFragment(),
                                        ISupportFragment.SINGLETOP
                                    )
                                }
                                PAGE_SETTINGS -> {
                                    extraTransaction().startDontHideSelf(
                                        SettingsFragment2(),
                                        ISupportFragment.SINGLETOP
                                    )
                                }
                                PAGE_SKILLS -> {
                                    extraTransaction().startDontHideSelf(
                                        SkillsFragment(),
                                        ISupportFragment.SINGLETOP
                                    )
                                }
                                PAGE_SPEAK_EVALUATION -> {
                                    extraTransaction().startDontHideSelf(
                                        SpeakEvaluationFragment(),
                                        ISupportFragment.SINGLETOP
                                    )
                                }
                                PAGE_SMART_HOME -> {
                                    extraTransaction().startDontHideSelf(
                                        SmartHomeFragment(),
                                        ISupportFragment.SINGLETOP
                                    )
                                }
                                else -> {
                                    // ignore
                                }
                            }
                        }
                        else -> {
                            // ignore
                        }
                    }
                }
            }
        }

        recyclerView?.itemAnimator = DefaultItemAnimator()
        recyclerView?.adapter = adapter
    }

    private fun setTemplate(templateApp: TemplateApp) {
        when (templateApp.template) {
            TemplateApp.TEMPLATE_TEMPLATE_1 -> {
                start(TemplateApp1Fragment.newInstance(templateApp))
            }
            TemplateApp.TEMPLATE_TEMPLATE_2 -> {
                start(TemplateApp2Fragment.newInstance(templateApp))
            }
            TemplateApp.TEMPLATE_XMLR -> {
                start(TemplateAppXmlyFragment.newInstance(templateApp))
            }
            TemplateApp.TEMPLATE_TEMPLATE_3 -> {
                playTemplate3(templateApp)
            }
            else -> {
                // ignore
            }
        }
    }

    private fun playTemplate3(templateApp: TemplateApp) {
        val json = com.alibaba.fastjson.JSONObject()
        json["appName"] = templateApp.name
        val requestBody = RequestBody.create(
            MediaType.parse("application/json"),
            json.toString()
        )
        getAppApi()?.playTemplate3(requestBody)?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (!isAdded || context == null) {
                    return
                }

                if (response.isSuccessful) {
                    Toast.makeText(context, "开始播放", Toast.LENGTH_SHORT).show()
                } else {
                    response.errorBody()?.let { errorBody ->
                        val errorString = errorBody.string()

                        val errorJson = JSONObject(errorString)

                        if (errorJson.has("message")) {
                            Toast.makeText(
                                context,
                                errorJson.optString("message"),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(context, "播放失败", Toast.LENGTH_SHORT).show()
                        }
                        errorBody.close()
                    } ?: run {
                        Toast.makeText(context, "播放失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    override fun onStart() {
        super.onStart()

        Thread {
            val context = context ?: return@Thread

            launcherDataHelper = LauncherDataHelper(context)

            if (LauncherMemory.partyApps == null && LauncherMemory.privateApps == null) {
                val appsForDb = launcherDataHelper?.queryPartyApps()

                if (!appsForDb.isNullOrEmpty()) {
                    recyclerView?.post {
                        setAppDataToAdapter(appsForDb)
                    }
                }
            }

            if (LauncherMemory.templateApps == null) {
                val templateAppsForDb = launcherDataHelper?.queryTemplateApps()

                if (!templateAppsForDb.isNullOrEmpty()) {
                    recyclerView?.post {
                        setTemplateAppToAdapter(templateAppsForDb)

                        LauncherMemory.templateApps = templateAppsForDb
                    }
                }
            }

            postAppUpdated()

            getTemplateApp()
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()

        context?.unregisterReceiver(packageReceiver)
    }

    override fun scrollToPrevious(): Boolean {
        recyclerView?.let { recyclerView ->
            val scrollY = recyclerView.computeVerticalScrollOffset()
            val itemCount = adapter.itemCount
            if (scrollY == 0 || itemCount == 0) {
                return false
            } else {
                recyclerView.smoothScrollBy(0, -recyclerView.height)
            }
        }
        return true
    }

    override fun scrollToNext(): Boolean {
        recyclerView?.let { recyclerView ->
            val lastItem =
                (recyclerView.layoutManager as? LinearLayoutManager)?.findLastCompletelyVisibleItemPosition()
            val itemCount = adapter.itemCount
            if (lastItem == itemCount - 1 || adapter.itemCount == 0
            ) {
                return false
            } else {
                recyclerView.smoothScrollBy(0, recyclerView.height)
            }
        }
        return true
    }

    private fun setAppDataToAdapter(apps: List<AppData>) {
        val oldSize = adapter.itemCount
        val oldPackages = mutableListOf<String>()
        for (i in 0 until adapter.itemCount) {
            adapter.getAppData(i)?.let {
                oldPackages.add(it.packageName)
            }
        }
        setPartyAppToAdapter(apps)

        val newSize = adapter.itemCount
        val newPackages = mutableListOf<String>()
        for (i in 0 until adapter.itemCount) {
            adapter.getAppData(i)?.let {
                newPackages.add(it.packageName)
            }
        }

        if (newSize - oldSize == 1 || oldSize - newSize == 1) {
            val isAdded = newSize > oldSize
            for (i in 0 until oldPackages.size) {
                if (i < newPackages.size) {
                    if (oldPackages[i] != newPackages[i]) {
                        if (isAdded) {
                            adapter.notifyItemInserted(i)
                            break
                        } else {
                            adapter.notifyItemRemoved(i)
                            break
                        }
                    }
                } else {
                    if (!isAdded) {
                        adapter.notifyItemRemoved(i)
                        break
                    }
                }
            }
        } else {
            when {
                newSize > oldSize -> {
                    if (oldSize > 4)
                        adapter.notifyItemRangeChanged(4, oldSize)
                    adapter.notifyItemRangeInserted(oldSize, newSize - oldSize)
                }
                newSize == oldSize -> {
                    if (oldSize > 4)
                        adapter.notifyItemRangeChanged(4, oldSize)
                }
                else -> {
                    if (newSize > 4)
                        adapter.notifyItemRangeChanged(4, newSize)
                    adapter.notifyItemRangeRemoved(newSize, oldSize - newSize)
                }
            }
        }
    }

    private fun setPartyAppToAdapter(apps: List<AppData>) {
        val privateApps = mutableListOf<AppData>()
        val partyApps = mutableListOf<AppData>()

        privateSortedApps.map { packageName ->
            apps.map { appData ->
                if (appData.packageName == packageName) {
                    privateApps.add(appData)
                }
            }
        }
        apps.map { appData ->
            var shouldNotAdd = false
            privateSortedApps.map { packageName ->
                if (appData.packageName == packageName) {
                    shouldNotAdd = true
                }
            }
            if (!shouldNotAdd) {
                partyApps.add(appData)
            }
        }

        LauncherMemory.partyApps = partyApps
        LauncherMemory.privateApps = privateApps

        adapter.setPrivateAppData(privateApps)
        adapter.setPartyAppData(partyApps)
    }

    private fun setTemplateAppToAdapter(apps: List<TemplateAppData>) {
        apps.distinctBy { it.name } //去重

        adapter.setTemplateAppData(apps)

        val memoryTemplateApps = LauncherMemory.templateApps
        if (memoryTemplateApps.isNullOrEmpty()) {
            adapter.notifyItemRangeInserted(6, apps.size)
        } else {
            val oldSize = memoryTemplateApps.size
            val newSize = apps.size
            if (oldSize <= newSize) {
                adapter.notifyItemRangeChanged(6, oldSize)
                if (oldSize != newSize)
                    adapter.notifyItemRangeInserted(
                        6 + oldSize,
                        newSize - oldSize
                    )
            } else {
                adapter.notifyItemRangeChanged(6, newSize)
                adapter.notifyItemRangeRemoved(6 + newSize, oldSize - newSize)
            }
        }
    }

    private fun getTemplateApp() {
        getAppApi()?.getAppList()?.enqueue(object : Callback<List<TemplateApp>> {
            override fun onFailure(call: Call<List<TemplateApp>>, t: Throwable) {
                t.printStackTrace()
                if (isRemoving || isDetached) {
                    return
                }
                context ?: return

                // 请求失败则忽略
            }

            override fun onResponse(
                call: Call<List<TemplateApp>>,
                response: Response<List<TemplateApp>>
            ) {
                if (isRemoving || isDetached) {
                    return
                }
                context ?: return

                if (response.isSuccessful) {
                    response.body()?.let { appList ->
                        if (appList.isNotEmpty()) {
                            Thread {
                                val appData = mutableListOf<TemplateAppData>()

                                @Suppress("IMPLICIT_CAST_TO_ANY")
                                appList.map {
                                    if (it.name.isNullOrEmpty()) {
                                        Log.w(TAG, "App name or source is null or empty")
                                    } else {
                                        appData.add(TemplateAppData.fromTemplateApp(it))
                                    }
                                }

                                launcherDataHelper?.updateTemplateAppData(appData)

                                recyclerView?.post {
                                    setTemplateAppToAdapter(appData)

                                    LauncherMemory.templateApps = appData
                                }
                            }.start()
                        }
                    }
                } else {
                    Log.e(TAG, "Get template app list failed.")
                }
            }

        })
    }

    private fun postAppUpdated() {
        val context = context ?: return
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledPackages(0)

        val apps = mutableListOf<AppData>()
        packages?.map {
            if (!blackPartyAppArray.contains(it.packageName)
                && packageManager.getLaunchIntentForPackage(it.packageName) != null
                && (packageManager.getApplicationEnabledSetting(it.packageName)
                    == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    || packageManager.getApplicationEnabledSetting(it.packageName)
                    == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
            ) {
                apps.add(
                    AppData(
                        it.applicationInfo.loadLabel(packageManager).toString(),
                        it.packageName,
                        packageManager.getApplicationIcon(it.packageName)
                    )
                )
            }
        }

        recyclerView?.post {
            setAppDataToAdapter(apps)
        }

        launcherDataHelper?.updatePartyApps(apps)
    }

    private fun getAppApi(): AppApi? {
        val context = context ?: return null
        return CoreApplication.from(context).createApi(AppApi::class.java)
    }
}