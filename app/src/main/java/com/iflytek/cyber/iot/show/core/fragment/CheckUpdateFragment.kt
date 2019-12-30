package com.iflytek.cyber.iot.show.core.fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.iflytek.cyber.iot.show.core.BuildConfig
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.SelfBroadcastReceiver
import com.iflytek.cyber.iot.show.core.impl.system.EvsSystem
import com.iflytek.cyber.iot.show.core.utils.*
import com.iflytek.cyber.iot.show.core.widget.StyledAlertDialog
import com.iflytek.cyber.product.ota.OtaService
import com.iflytek.cyber.product.ota.PackageEntityNew

class CheckUpdateFragment : BaseFragment() {
    private var currentVersionContainer: View? = null
    private var currentVersion: TextView? = null
    private var loadingContainer: View? = null
    private var loadingView: LottieAnimationView? = null
    private var newVersionContainer: View? = null
    private var newVersionTitleView: TextView? = null
    private var newVersionDescriptionView: TextView? = null
    private var updateButton: TextView? = null
    private var progressContainer: View? = null
    private var tvProgress: TextView? = null
    private var progressBar: ProgressBar? = null
    private var failedContainer: View? = null
    private var tvProgressDescription: TextView? = null
    private var tvFailed: TextView? = null
    private var tvDownloadCompleted: TextView? = null
    private var progressBarCompleted: ProgressBar? = null

    private var packageEntity: PackageEntityNew? = null
    private var downloadedPath: String? = null
    private var otaService: OtaService? = null

    private var backCount = 0

    private val otaReceiver = object : SelfBroadcastReceiver(
        OtaService.ACTION_CHECK_UPDATE_RESULT,
        OtaService.ACTION_CHECK_UPDATE_FAILED,
        OtaService.ACTION_NEW_UPDATE_DOWNLOADED,
        OtaService.ACTION_NEW_UPDATE_DOWNLOAD_STARTED
    ) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                OtaService.ACTION_CHECK_UPDATE_RESULT -> {
                    if (intent.hasExtra(OtaService.EXTRA_PACKAGE_ENTITY)) {
                        val packageEntity =
                            intent.getParcelableExtra<PackageEntityNew>(
                                OtaService.EXTRA_PACKAGE_ENTITY
                            )

                        showNewVersion(packageEntity.versionName, packageEntity.description)

                        this@CheckUpdateFragment.packageEntity = packageEntity

                        if (otaService?.isUrlDownloaded(packageEntity.url) == true) {
                            showProgress()

                            downloadProgressCallback.onDownloadProgress(packageEntity.id!!, 100)

                            downloadedPath = otaService?.getUrlDownloadedPath(packageEntity.url)

                            updateButton?.post {
                                updateButton?.isVisible = true
                                updateButton?.setText(R.string.install_now)
                            }
                        }
                    } else {
                        showCurrent()

                        packageEntity = null
                    }
                }
                OtaService.ACTION_NEW_UPDATE_DOWNLOAD_STARTED -> {
                    showProgress()

                    updateButton?.isVisible = false

                    tvProgressDescription?.setText(R.string.downloading_software)
                }
                OtaService.ACTION_NEW_UPDATE_DOWNLOADED -> {
                    updateButton?.isVisible = true
                    updateButton?.setText(R.string.install_now)

                    tvDownloadCompleted?.isVisible = true
                    tvProgress?.isVisible = false
                    progressBar?.isVisible = false
                    progressBarCompleted?.isVisible = true

                    downloadProgressCallback.onDownloadProgress(packageEntity?.id!!, 100)

                    downloadedPath = intent.getStringExtra(OtaService.EXTRA_PATH)

                    tvProgressDescription?.setText(R.string.download_completed)
                }
                OtaService.ACTION_CHECK_UPDATE_FAILED -> {
                    showFailed()

                    intent.getStringExtra(OtaService.EXTRA_MESSAGE)?.let { message ->
                        tvFailed?.text = message
                    } ?: run {
                        tvFailed?.setText(R.string.update_failed_common)
                    }

                    packageEntity = null
                }
            }
        }
    }
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            otaService?.unregisterDownloadProgressCallback(downloadProgressCallback)

            otaService = null
        }

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder is OtaService.OtaBinder) {
                otaService = binder.service()

                otaService?.registerDownloadProgressCallback(downloadProgressCallback)
            }
        }
    }
    private val downloadProgressCallback = object : OtaService.DownloadProgressCallback {
        override fun onDownloadProgress(id: Long, progress: Int) {
            val context = context ?: return
            if (id != packageEntity?.id)
                return
            if (!isSupportVisible || isDetached)
                return
            tvProgress?.post {
                tvProgress?.text = context.getString(R.string.count_of_percent, progress)

                progressBar?.progress = progress

                if (progress == 100) {
                    tvProgressDescription?.setText(R.string.download_completed)
                } else {
                    showProgress()

                    tvProgressDescription?.setText(R.string.downloading_software)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        otaReceiver.register(context)

        val intent = Intent(context, OtaService::class.java)
        context?.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_check_update, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingContainer = view.findViewById(R.id.loading_container)
        currentVersion = view.findViewById(R.id.current_version)
        currentVersionContainer = view.findViewById(R.id.current_version_container)
        loadingView = view.findViewById(R.id.loading)
        newVersionContainer = view.findViewById(R.id.new_version_container)
        newVersionTitleView = view.findViewById(R.id.new_version_title)
        newVersionDescriptionView = view.findViewById(R.id.new_version_description)
        updateButton = view.findViewById(R.id.update_button)
        progressContainer = view.findViewById(R.id.progress_container)
        tvProgress = view.findViewById(R.id.tv_progress)
        progressBar = view.findViewById(R.id.progress_bar)
        failedContainer = view.findViewById(R.id.failed_container)
        tvProgressDescription = view.findViewById(R.id.progress_description)
        tvFailed = view.findViewById(R.id.failed_message)
        tvDownloadCompleted = view.findViewById(R.id.download_completed)
        progressBarCompleted = view.findViewById(R.id.progress_bar_completed)

        view.findViewById<View>(R.id.back)?.setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            pop()
        }
        view.findViewById<View>(R.id.retry)?.setOnClickListener {
            val checkUpdate = Intent(context, OtaService::class.java)
            checkUpdate.action = OtaService.ACTION_REQUEST_CHECKING
            context?.startService(checkUpdate)
        }
        updateButton?.setOnClickListener {
            val checkSize = if (downloadedPath != null) {
                1.0 * 1024 * 1024 * 1024
            } else {
                1.5 * 1024 * 1024 * 1024
            }
            if (CleanerUtils.getSdcardFreeSpace() <= checkSize) {
                // 下载或升级要求剩余控件 1.5G 以上
                fragmentManager?.let {
                    StyledAlertDialog.Builder()
                        .setTitle("设备可用空间不足")
                        .setMessage("设备可用空间不足，是否清理缓存")
                        .setPositiveButton("清理", View.OnClickListener { view ->
                            TerminalUtils.execute("rm /sdcard/ota_update.zip")
                            CleanerUtils.clearRecordDebugFiles()
                            CleanerUtils.clearWakeWordCache(view.context)
                            CleanerUtils.clearCache(view.context)
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show(it)
                }
                return@setOnClickListener
            }
            downloadedPath?.let { path ->
                if (DeviceUtils.getBatteryLevel(context) < 40) {
                    // 下载或升级要求剩余控件 1.5G 以上
                    fragmentManager?.let {
                        StyledAlertDialog.Builder()
                            .setTitle("设备电量不足")
                            .setMessage("请将设备电量保持在 40% 上再进行固件更新")
                            .setPositiveButton(getString(R.string.ensure), null)
                            .show(it)
                    }
                    return@setOnClickListener
                }

                PackageUtils.notifyInstallApk(it.context, path)

                ConfigUtils.putBoolean(ConfigUtils.KEY_OTA_REQUEST, true)
                packageEntity?.let { packageEntityNew ->
                    EvsSystem.get().sendUpdateSoftwareStarted(
                        packageEntityNew.versionName.toString(),
                        packageEntityNew.description
                    )

                    ConfigUtils.putInt(
                        ConfigUtils.KEY_OTA_VERSION_ID, packageEntityNew.versionId?.toInt()
                            ?: 0
                    )
                    ConfigUtils.putString(
                        ConfigUtils.KEY_OTA_VERSION_NAME,
                        packageEntityNew.versionName
                    )
                    ConfigUtils.putString(
                        ConfigUtils.KEY_OTA_VERSION_DESCRIPTION,
                        packageEntityNew.description
                    )
                }
            } ?: run {
                val packageEntity = packageEntity ?: return@run

                downloadProgressCallback.onDownloadProgress(packageEntity.id!!, 0)

                val intent = Intent(it.context, OtaService::class.java)
                intent.action = OtaService.ACTION_REQUEST_DOWNLOAD
                intent.putExtra(OtaService.EXTRA_ID, packageEntity.id ?: -1L)
                intent.putExtra(OtaService.EXTRA_URL, packageEntity.url)
                context?.startService(intent)
            }
        }

        loadingView?.repeatCount = LottieDrawable.INFINITE
        loadingView?.playAnimation()

        progressBar?.max = 100

        currentVersion?.text = getString(R.string.version_value, BuildConfig.VERSION_NAME)

        val checkUpdate = Intent(context, OtaService::class.java)
        checkUpdate.action = OtaService.ACTION_REQUEST_CHECKING
        context?.startService(checkUpdate)
    }

    override fun onDestroy() {
        super.onDestroy()

        otaReceiver.unregister(context)

        context?.unbindService(serviceConnection)
    }

    private fun showNewVersion(title: String?, message: String?) {
        newVersionTitleView?.text =
            getString(R.string.new_version_title, title)
        updateButton?.setText(R.string.update_now)
        newVersionDescriptionView?.text = message

        newVersionContainer?.isVisible = true

        loadingContainer?.isVisible = false
        failedContainer?.isVisible = false
        currentVersionContainer?.isVisible = false

        hideProgress()
    }

    private fun showProgress() {
        progressContainer?.isVisible = true

        updateButton?.isVisible = false
    }

    private fun hideProgress() {
        updateButton?.isVisible = true

        progressContainer?.isVisible = false
    }

    private fun showFailed() {
        failedContainer?.isVisible = true

        loadingContainer?.isVisible = false
        currentVersionContainer?.isVisible = false
        progressContainer?.isVisible = false
        newVersionContainer?.isVisible = false
    }

    private fun showCurrent() {
        currentVersionContainer?.isVisible = true

        newVersionContainer?.isVisible = false
        loadingContainer?.isVisible = false
        failedContainer?.isVisible = false
        progressContainer?.isVisible = false
    }
}