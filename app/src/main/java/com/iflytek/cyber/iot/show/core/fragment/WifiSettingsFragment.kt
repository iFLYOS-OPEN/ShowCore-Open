package com.iflytek.cyber.iot.show.core.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.*
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.SelfBroadcastReceiver
import com.iflytek.cyber.iot.show.core.utils.ConfigUtils
import com.iflytek.cyber.iot.show.core.utils.WifiUtils
import com.iflytek.cyber.iot.show.core.widget.DividerItemDecoration
import com.iflytek.cyber.iot.show.core.widget.StyledSwitch
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.Comparator

class WifiSettingsFragment : BaseFragment(), PageScrollable {
    private val scanReceiver = ScanReceiver()

    private val uiHandler = Handler(Looper.getMainLooper())

    private var wm: WifiManager? = null

    private var scans: List<ScanResult>? = null
    private val configs = HashMap<String, WifiConfiguration>()

    private var connected: String? = null
    private var isAvailable = true

    private var recyclerView: RecyclerView? = null
    private var adapter: WifiAdapter? = null
    private var wifiSwitch: StyledSwitch? = null
    private var ivRefresh: ImageView? = null
    private var nextStep: TextView? = null
    private var loadingView: LottieAnimationView? = null
    private var refreshContainer: View? = null
    private var wifiWakeLock: PowerManager.WakeLock? = null

    private var warningAlert: AlertDialog? = null

    private var progressDialog: ProgressDialog? = null

    private var isScanning = false

    private var fromSetup = false

    private var backCount = 0

    private var wakeLock: PowerManager.WakeLock? = null

    @Suppress("DEPRECATION")
    private val connectionReceiver = object : SelfBroadcastReceiver(
        ConnectivityManager.CONNECTIVITY_ACTION
    ) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    val network = intent.getParcelableExtra<NetworkInfo>(
                        ConnectivityManager.EXTRA_NETWORK_INFO
                    )
                    val detailed = network.detailedState

                    if (detailed == NetworkInfo.DetailedState.CONNECTED) {
                        WifiUtils.getConnectedSsid(context)?.let { ssid ->
                            if (ssid.isNotEmpty()) {
                                connected = ssid
                            } else {
                                connected = null
                            }
                        } ?: run {
                            connected = null
                        }
                    } else {
                        connected = null
                    }
                    adapter?.notifyDataSetChanged()
                }
            }
        }
    }
    private var networkCallback: Any? = null // 不声明 NetworkCallback 的类，否则 L 以下会找不到类

    companion object {
        private const val TAG = "WifiSettingsFragment"
        private const val REQUEST_LOCATION_CODE = 10423

        fun newInstance(fromSetup: Boolean = false): WifiSettingsFragment {
            val fragment = WifiSettingsFragment()
            val arguments = Bundle()
            arguments.putBoolean("from_setup", fromSetup)
            fragment.arguments = arguments
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wifi_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fromSetup = arguments?.getBoolean("from_setup")

        adapter = WifiAdapter(view.context)

        val list = view.findViewById<RecyclerView>(R.id.wifi_list)
        list.addItemDecoration(
            DividerItemDecoration.Builder(list.context)
                .setPadding(resources.getDimensionPixelSize(R.dimen.dp_40))
                .setDividerColor(ContextCompat.getColor(list.context, R.color.dividerLight))
                .setDividerWidth(resources.getDimensionPixelSize(R.dimen.dp_1))
                .build()
        )
        list.adapter = adapter
        recyclerView = list

        view.findViewById<View>(R.id.back).setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            pop()
        }

        wifiSwitch = view.findViewById(R.id.wifi_enabled)
        loadingView = view.findViewById(R.id.loading)
        refreshContainer = view.findViewById(R.id.refresh_container)
        ivRefresh = view.findViewById(R.id.refresh)
        nextStep = view.findViewById(R.id.next_step)

        ivRefresh?.setOnClickListener {
            if (!isScanning) {
                startScan()
            }
        }

        view.findViewById<View>(R.id.next_step_container)?.isVisible = fromSetup == true
        nextStep?.setOnClickListener {
            start(PairFragment2())
        }

        if (wm?.isWifiEnabled == false) {
            if (!ConfigUtils.getBoolean(ConfigUtils.KEY_SETUP_COMPLETED, false)) {
                post {
                    wm?.isWifiEnabled = true
                }
            }
        }

        if (context?.let { AuthDelegate.getAuthResponseFromPref(it) == null } == true)
            acquireWakeLock()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_FINE_LOCATION)
            != PermissionChecker.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_CODE
            )
        }
        wm = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager

        scanReceiver.register(context)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            connectionReceiver.register(context)
        } else {
            val connectivityManager =
                context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network?) {
                    super.onAvailable(network)

                    WifiUtils.getConnectedSsid(context)?.let { ssid ->
                        if (ssid.isNotEmpty()) {
                            connected = ssid
                        } else {
                            connected = null
                        }
                    } ?: run {
                        connected = null
                    }

                    post {
                        adapter?.notifyDataSetChanged()
                    }
                }

                override fun onLost(network: Network?) {
                    super.onLost(network)

                    connected = null

                    post {
                        adapter?.notifyDataSetChanged()
                    }
                }
            }
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)

            this.networkCallback = networkCallback
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_CODE && grantResults.isNotEmpty()
            && grantResults[0] == PermissionChecker.PERMISSION_GRANTED &&
            permissions.isNotEmpty() && permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION
        ) {
            startScan()
        } else {
            pop()
        }
    }

    override fun onSupportVisible() {
        super.onSupportVisible()

        fromSetup = arguments?.getBoolean("from_setup") == true

        if (fromSetup) {
            view?.findViewById<TextView>(R.id.title)?.let { titleView ->
                titleView.setText(R.string.connect_wifi)
                (titleView.layoutParams as? LinearLayout.LayoutParams)?.let { layoutParams ->
                    val margin = resources.getDimensionPixelSize(R.dimen.dp_40)
                    if (layoutParams.marginStart != margin) {
                        layoutParams.marginStart = margin
                        titleView.layoutParams = layoutParams
                    }
                }
            }
            view?.findViewById<View>(R.id.back)?.visibility = View.GONE
        } else {
            view?.findViewById<TextView>(R.id.title)?.let { titleView ->
                titleView.setText(R.string.wlan)
                (titleView.layoutParams as? LinearLayout.LayoutParams)?.let { layoutParams ->
                    if (layoutParams.marginStart != 0) {
                        layoutParams.marginStart = 0
                        titleView.layoutParams = layoutParams
                    }
                }
            }
            view?.findViewById<View>(R.id.back)?.visibility = View.VISIBLE
        }

        scans = null
        connected = WifiUtils.getConnectedSsid(context)

        adapter?.notifyDataSetChanged()

        if (wm?.isWifiEnabled == true) {
            startScan()
        }
    }

    @Suppress("DEPRECATION")
    private fun startScan() {
        if (wm?.isWifiEnabled != true)
            return
        wm?.startScan()

        isScanning = true

        ivRefresh?.animate()?.alpha(0f)?.setDuration(150)?.withEndAction {
            loadingView?.let { animationView ->
                animationView.repeatCount = Animation.INFINITE
                animationView.playAnimation()
                animationView.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
        }?.start()
    }

    override fun onSupportInvisible() {
        super.onSupportInvisible()

        uiHandler.removeCallbacksAndMessages(null)
        warningAlert?.dismiss()
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        wifiWakeLock?.acquire() ?: run {
            val powerManager = context?.getSystemService(Context.POWER_SERVICE) as PowerManager
            val flag = PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.SCREEN_BRIGHT_WAKE_LOCK
            val wakeLock = powerManager.newWakeLock(flag, "iflytek:wifi")

            wakeLock.acquire()
            wifiWakeLock = wakeLock
        }
    }

    private fun releaseWakeLock() {
        wifiWakeLock?.let {
            it.release()

            val powerManager = context?.getSystemService(Context.POWER_SERVICE) as PowerManager
            val flag = PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.SCREEN_BRIGHT_WAKE_LOCK
            val wakeLock = powerManager.newWakeLock(flag, "iflytek:wifi.release")

            wakeLock.acquire(TimeUnit.SECONDS.toMillis(10))

            wifiWakeLock = null
        }
    }

    override fun onBackPressedSupport(): Boolean {
        if (getPreFragment() == null)
            return true
        return super.onBackPressedSupport()
    }

    override fun onDestroy() {
        super.onDestroy()

        scanReceiver.unregister(context)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            connectionReceiver.unregister(context)
        } else {
            (context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                ?.let { connectivityManager ->
                    val networkCallback =
                        (this.networkCallback as? ConnectivityManager.NetworkCallback)
                            ?: return
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                }
        }

        releaseWakeLock()
    }

    private fun handleScanResult() {
        Log.d(TAG, "handleScanResult")
        if (!isDetached) {
            ivRefresh?.postDelayed({
                if (!isDetached) {
                    startScan()
                }
            }, 30 * 1000)
        }
        configs.clear()
        wm?.let {
            val configuredNetworks = it.configuredNetworks
            if (configuredNetworks?.isNotEmpty() == true)
                for (config in it.configuredNetworks) {
                    val ssid = config.SSID?.substring(1, config.SSID.length - 1)
                    if (!ssid.isNullOrEmpty() && ssid.trim().isNotEmpty())
                        configs[ssid] = config
                }

            connected = WifiUtils.getConnectedSsid(context)

            val scanResults = it.scanResults
            val map = HashMap<String, ScanResult>()
            for (o1 in scanResults) {
                if (!o1.SSID.isNullOrEmpty() && o1.SSID.trim().isNotEmpty()) {
                    val o2 = map[o1.SSID]
                    if ((o2 == null || o2.level < o1.level) && o1.level != 0) {
                        map[o1.SSID] = o1
                    }
                }
            }
            val list = ArrayList(map.values)
            Collections.sort(list, Comparator { o1, o2 ->
                val c1 = configs[o1.SSID]
                val c2 = configs[o2.SSID]

                if (c1 != null && c1.status == WifiConfiguration.Status.CURRENT) {
                    return@Comparator -1
                } else if (c2 != null && c2.status == WifiConfiguration.Status.CURRENT) {
                    return@Comparator 1
                }

                if (c1 != null && c2 == null) {
                    return@Comparator -1
                } else if (c1 == null && c2 != null) {
                    return@Comparator 1
                }

                o2.level - o1.level
            })
            scans = list
            adapter?.notifyDataSetChanged()

            if (!isDetached && scans.isNullOrEmpty()) {
                Log.d(TAG, "result is empty. retry in 3 seconds. source: ${scanResults.size}")
                ivRefresh?.postDelayed({
                    if (!isDetached) {
                        startScan()
                    }
                }, 3 * 1000)
            }
        }
    }

    private fun handleOnItemClick(scan: ScanResult) {
        val context = context ?: return
        if (connected != scan.SSID) {
            configs[scan.SSID]?.let { config ->
                WifiUtils.connect(context, config.networkId)
                start(WifiConnectingFragment(scan.SSID))
            } ?: run {
                if (!WifiUtils.isEncrypted(scan)) {
                    WifiUtils.connect(context, scan.SSID)
                    start(WifiConnectingFragment(scan.SSID))
                } else {
                    start(InputNetworkFragment(scan))
                }
            }
        } else {
            start(WifiInfoFragment(scan))
        }
    }

    private fun handleOpenWifiInfo(scan: ScanResult) {
        start(WifiInfoFragment(scan))
    }

    private fun handleAddManual() {
        start(InputNetworkFragment())
    }

    override fun scrollToNext(): Boolean {
        recyclerView?.let { recyclerView ->
            val lastItem =
                (recyclerView.layoutManager as? LinearLayoutManager)?.findLastCompletelyVisibleItemPosition()
            val itemCount = adapter?.itemCount ?: 0
            if (lastItem == itemCount - 1 || adapter?.itemCount == 0
            ) {
                return false
            } else {
                recyclerView.smoothScrollBy(0, recyclerView.height)
            }
        }
        return true
    }

    override fun scrollToPrevious(): Boolean {
        recyclerView?.let { recyclerView ->
            val scrollY = recyclerView.computeVerticalScrollOffset()
            val itemCount = adapter?.itemCount ?: 0
            if (scrollY == 0 || itemCount == 0) {
                return false
            } else {
                recyclerView.smoothScrollBy(0, -recyclerView.height)
            }
        }
        return true
    }

    private inner class WifiAdapter internal constructor(context: Context) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val inflater: LayoutInflater = LayoutInflater.from(context)

        override fun getItemViewType(position: Int): Int {
            return when (position) {
                0 -> 2
                itemCount - 1 -> 1
                else -> 0
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                0 ->
                    ItemViewHolder(inflater.inflate(R.layout.item_access_point_2, parent, false))
                1 ->
                    ManualViewHolder(
                        inflater.inflate(
                            R.layout.item_access_point_manual,
                            parent,
                            false
                        )
                    )
                2 -> {
                    val holder = WifiSwitchViewHolder(
                        inflater.inflate(
                            R.layout.item_wifi_switch,
                            parent,
                            false
                        )
                    )
                    wifiSwitch = holder.itemView.findViewById<StyledSwitch>(R.id.wifi_enabled)
                    wifiSwitch?.let { wifiSwitch ->
                        wifiSwitch.isChecked = wm?.isWifiEnabled == true
                        refreshContainer?.visibility =
                            if (wifiSwitch.isChecked) View.VISIBLE else View.GONE

                        wifiSwitch.setOnCheckedChangeListener(object :
                            StyledSwitch.OnCheckedChangeListener {
                            override fun onCheckedChange(switch: StyledSwitch, isChecked: Boolean) {
                                progressDialog?.dismiss()
                                val target = wifiSwitch.isChecked
                                if (target == wm?.isWifiEnabled)
                                    return
                                progressDialog = ProgressDialog.show(
                                    context, null,
                                    if (isChecked) "正在开启" else "正在关闭", false, true
                                )

                                post {
                                    val result = wm?.setWifiEnabled(target)
                                    progressDialog?.dismiss()

                                    if (result != true) {
                                        wifiSwitch.isChecked = wm?.isWifiEnabled == true
                                    }

                                    if (!wifiSwitch.isChecked)
                                        refreshContainer?.isVisible = false
                                }
                            }
                        })
                    }

                    holder
                }
                else ->
                    ItemViewHolder(inflater.inflate(R.layout.item_access_point_2, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ItemViewHolder) {
                scans?.get(position - 1)?.let { scan ->
                    holder.bind(scan)
                }
            }
        }

        override fun getItemCount(): Int {
            val isWifiEnabled = wm?.isWifiEnabled == true
            return if (isWifiEnabled) (scans?.size ?: 0) + 2 else 1
        }

        internal inner class ManualViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            init {
                itemView.setOnClickListener {
                    handleAddManual()
                }
            }
        }

        internal inner class WifiSwitchViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView)

        internal inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val icon: ImageView = itemView.findViewById(R.id.icon)
            private val ssid: TextView = itemView.findViewById(R.id.ssid)
            private val status: TextView = itemView.findViewById(R.id.status)
            private val info: ImageView = itemView.findViewById(R.id.info)

            init {
                itemView.setOnClickListener { handleOnItemClick(scans!![this@ItemViewHolder.layoutPosition - 1]) }
                info.setOnClickListener { handleOpenWifiInfo(scans!![this@ItemViewHolder.layoutPosition - 1]) }
            }

            fun bind(scan: ScanResult) {
                if (WifiUtils.isEncrypted(scan)) {
                    icon.setImageResource(R.drawable.ic_wifi_locked_black_24dp)
                } else {
                    icon.setImageResource(R.drawable.ic_wifi_black_24dp)
                }

                ssid.text = scan.SSID

                when {
                    connected == scan.SSID -> {
                        status.visibility = View.VISIBLE
                        status.text = "已连接"
                        info.visibility = View.VISIBLE
                    }
                    configs[scan.SSID] != null -> {
                        status.visibility = View.VISIBLE
                        status.text = "已保存"
                        info.visibility = View.VISIBLE
                    }
                    else -> {
                        status.visibility = View.GONE
                        info.visibility = View.GONE
                    }
                }
            }
        }
    }

    private inner class ScanReceiver
    internal constructor() :
        SelfBroadcastReceiver(
            WifiManager.SCAN_RESULTS_AVAILABLE_ACTION,
            WifiManager.WIFI_STATE_CHANGED_ACTION
        ) {

        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> {
                    handleScanResult()
                    isScanning = false

                    loadingView?.animate()?.alpha(0f)?.setDuration(150)?.withEndAction {
                        ivRefresh?.animate()?.alpha(1f)?.setDuration(200)?.start()
                    }?.start()
                }
                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)

                    if (state == WifiManager.WIFI_STATE_ENABLED) {
                        if (wifiSwitch?.isChecked != true)
                            wifiSwitch?.isChecked = true

                        refreshContainer?.visibility = View.VISIBLE
                        startScan()
                    } else if (state == WifiManager.WIFI_STATE_DISABLED) {
                        if (wifiSwitch?.isChecked != false)
                            wifiSwitch?.isChecked = false

                        refreshContainer?.visibility = View.GONE
                    }

                    scans = null
                    adapter?.notifyDataSetChanged()

                    if (refreshContainer?.visibility == View.GONE && wifiSwitch?.isChecked == true) {
                        ivRefresh?.alpha = 0f
                        loadingView?.alpha = 1f
                        loadingView?.playAnimation()
                    }
                }
            }
        }
    }
}