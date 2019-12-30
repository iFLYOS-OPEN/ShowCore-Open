package com.iflytek.cyber.iot.show.core.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.AppAdapter
import com.iflytek.cyber.iot.show.core.launcher.AppData
import com.iflytek.cyber.iot.show.core.launcher.LauncherDataHelper
import com.iflytek.cyber.iot.show.core.launcher.LauncherMemory
import com.iflytek.cyber.iot.show.core.utils.OnItemClickListener
import me.yokeyword.fragmentation.ISupportFragment

class LauncherFragment2 : BaseFragment(), PageScrollable {
    companion object {
        private const val TAG = "LauncherFragment2"

        private const val PAGE_ALARMS = "Launcher.Alarms"
        private const val PAGE_SETTINGS = "Launcher.Settings"
        private const val PAGE_SKILLS = "Launcher.Skills"
        private const val PAGE_CONTENTS = "Launcher.Contents"

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

        ((view.findViewById<View>(R.id.container)?.layoutParams
            as? CoordinatorLayout.LayoutParams)?.behavior as? BottomSheetBehavior)?.let { bottomSheetBehavior ->
            bottomSheetBehavior.skipCollapsed = true
            bottomSheetBehavior.setBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN
                        || newState == BottomSheetBehavior.STATE_COLLAPSED
                    ) {
                        pop()
                    }
                }
            })
        }
    }

    override fun onStart() {
        super.onStart()

        ((view?.findViewById<View>(R.id.container)?.layoutParams
            as? CoordinatorLayout.LayoutParams)?.behavior as? BottomSheetBehavior)?.state =
            BottomSheetBehavior.STATE_EXPANDED

        Thread {
            val context = context ?: return@Thread

            launcherDataHelper = LauncherDataHelper(context)

            val appsForDb = launcherDataHelper?.queryPartyApps()

            if (!appsForDb.isNullOrEmpty()) {
                recyclerView?.post {
                    setAppDataToAdapter(appsForDb)
                }
            }

            postAppUpdated()
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
}