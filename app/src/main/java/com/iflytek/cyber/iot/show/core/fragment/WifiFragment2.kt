package com.iflytek.cyber.iot.show.core.fragment

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.SelfBroadcastReceiver
import com.iflytek.cyber.iot.show.core.utils.WifiUtils
import com.iflytek.cyber.iot.show.core.widget.DividerItemDecoration
import com.iflytek.cyber.iot.show.core.widget.StyledSwitch
import java.util.*
import kotlin.Comparator

class WifiFragment2 : BaseFragment() {
    private val scanReceiver = ScanReceiver()

    private val uiHandler = Handler(Looper.getMainLooper())

    private var wm: WifiManager? = null

    private var scans: List<ScanResult>? = null

    private val configs = HashMap<String, WifiConfiguration>()

    private var connected: String? = null

    private var adapter: WifiAdapter? = null
    private var wifiSwitch: StyledSwitch? = null
    private var ivRefresh: ImageView? = null
    private var loadingView: LottieAnimationView? = null
    private var refreshContainer: View? = null

    private var warningAlert: AlertDialog? = null

    private var progressDialog: ProgressDialog? = null

    private var isScanning = false

    companion object {
        private const val TAG = "WifiFragment2"
        private const val REQUEST_LOCATION_CODE = 10423
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wifi_2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = WifiAdapter(view.context)

        val list = view.findViewById<RecyclerView>(R.id.wifi_list)
        list.addItemDecoration(DividerItemDecoration.Builder(list.context)
            .setPadding(resources.getDimensionPixelSize(R.dimen.dp_40))
            .setDividerColor(ContextCompat.getColor(list.context, R.color.dividerLight))
            .setDividerWidth(resources.getDimensionPixelSize(R.dimen.dp_1))
            .build())
        list.adapter = adapter

        wifiSwitch = view.findViewById(R.id.wifi_enabled)
        loadingView = view.findViewById(R.id.loading)
        refreshContainer = view.findViewById(R.id.refresh_container)
        ivRefresh = view.findViewById(R.id.refresh)

        wifiSwitch?.let { wifiSwitch ->
            wifiSwitch.setOnClickListener {
                progressDialog?.dismiss()
                progressDialog = ProgressDialog.show(context, null,
                    if (wifiSwitch.isChecked) "正在开启" else "正在关闭", false, true)

                post {
                    val target = wifiSwitch.isChecked
                    val result = wm?.setWifiEnabled(target)
                    progressDialog?.dismiss()
                    if (result != true) {
                        wifiSwitch.isChecked = wm?.isWifiEnabled == true

                        if (wm?.isWifiEnabled == true) {
                            refreshContainer?.visibility = View.VISIBLE

                            scans = null
                            startScan()
                        } else {
                            refreshContainer?.visibility = View.GONE

                            scans = null
                            adapter?.notifyDataSetChanged()
                        }
                    }
                }
            }
            wifiSwitch.isChecked = wm?.isWifiEnabled == true
            refreshContainer!!.visibility = if (wifiSwitch.isChecked) View.VISIBLE else View.GONE
        }
        wifiSwitch?.setOnCheckedChangeListener(object : StyledSwitch.OnCheckedChangeListener {
            override fun onCheckedChange(switch: StyledSwitch, isChecked: Boolean) {
                refreshContainer?.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        })

        ivRefresh?.setOnClickListener {
            if (!isScanning) {
                startScan()
            }
        }

        Handler().postDelayed({
            if (!isDetached) {
                startScan()
            }
        }, 30 * 1000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_FINE_LOCATION)
            != PermissionChecker.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_CODE)
        }
        wm = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_CODE && grantResults.isNotEmpty()
            && grantResults[0] == PermissionChecker.PERMISSION_GRANTED &&
            permissions.isNotEmpty() && permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION) {
            startScan()
        } else {
            pop()
        }
    }

    @Suppress("DEPRECATION")
    private fun startScan() {
        if (wm?.isWifiEnabled != true)
            return
        wm?.startScan()

        isScanning = true

        ivRefresh?.post {
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
    }

    override fun onResume() {
        super.onResume()

        scans = null
        connected = WifiUtils.getConnectedSsid(context)

        adapter?.notifyDataSetChanged()

        scanReceiver.register(context)

        if (wm?.isWifiEnabled == true) {
            startScan()
        }
    }

    override fun onPause() {
        super.onPause()

        scanReceiver.unregister(context)
        uiHandler.removeCallbacksAndMessages(null)
        warningAlert?.dismiss()
    }

    private fun handleScanResult() {
        configs.clear()
        wm?.let {
            val configuredNetworks = it.configuredNetworks
            if (configuredNetworks?.isNotEmpty() == true)
                for (config in configuredNetworks) {
                    val ssid = config.SSID.substring(1, config.SSID.length - 1)
                    configs[ssid] = config
                }

            connected = WifiUtils.getConnectedSsid(context)

            val map = HashMap<String, ScanResult>()
            for (o1 in it.scanResults) {
                val o2 = map[o1.SSID]
                if ((o2 == null || o2.level < o1.level) && o1.level != 0) {
                    map[o1.SSID] = o1
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
//            Log.d(TAG, scans.toString())
            adapter?.notifyDataSetChanged()
        }

        Handler().postDelayed({
            if (!isDetached) {
                startScan()
            }
        }, 30 * 1000)
    }

    private fun handleOnItemClick(scan: ScanResult) {
        val context = context ?: return
        if (connected != scan.SSID) {
            configs[scan.SSID]?.let { config ->
                WifiUtils.connect(context, config.networkId)
                start(WifiConnectingFragment(scan.SSID))
            } ?: run {
                if (!WifiUtils.isEncrypted(scan)) {
                    start(WifiConnectingFragment(scan.SSID))
                } else {
                    start(InputNetworkFragment(scan))
                }
            }
        } else {
            start(PairFragment2())
        }
    }

    private fun handleOpenWifiInfo(scan: ScanResult) {
        start(WifiInfoFragment(scan))
    }

    private fun handleAddManual() {
        start(InputNetworkFragment())
    }

    private inner class WifiAdapter internal constructor(context: Context) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val inflater: LayoutInflater = LayoutInflater.from(context)

        override fun getItemViewType(position: Int): Int {
            return if (position == itemCount - 1) {
                1
            } else 0
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == 1) {
                ManualViewHolder(inflater.inflate(R.layout.item_access_point_manual, parent, false))
            } else {
                ItemViewHolder(inflater.inflate(R.layout.item_access_point_2, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ItemViewHolder) {
                val scan = scans!![position]
                holder.bind(scan)
            }
        }

        override fun getItemCount(): Int {
            return if (wifiSwitch?.isChecked == true) (scans?.size ?: 0) + 1 else 0
        }

        internal inner class ManualViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            init {
                itemView.setOnClickListener {
                    handleAddManual()
                }
            }
        }

        internal inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val icon: ImageView = itemView.findViewById(R.id.icon)
            private val ssid: TextView = itemView.findViewById(R.id.ssid)
            private val status: TextView = itemView.findViewById(R.id.status)
            private val info: ImageView = itemView.findViewById(R.id.info)

            init {
                itemView.setOnClickListener { handleOnItemClick(scans!![this@ItemViewHolder.layoutPosition]) }
                info.setOnClickListener { handleOpenWifiInfo(scans!![this@ItemViewHolder.layoutPosition]) }
            }

            fun bind(scan: ScanResult) {
                if (WifiUtils.isEncrypted(scan)) {
                    icon.setImageResource(R.drawable.ic_signal_wifi_4_bar_lock_white_24dp)
                } else {
                    icon.setImageResource(R.drawable.ic_signal_wifi_4_bar_white_24dp)
                }

                ssid.text = scan.SSID

                info.visibility = if (scan.SSID == connected) View.VISIBLE else View.GONE

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
        SelfBroadcastReceiver(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {

        override fun onReceiveAction(action: String, intent: Intent) {
            handleScanResult()
            isScanning = false

            loadingView?.animate()?.alpha(0f)?.setDuration(150)?.withEndAction {
                ivRefresh?.animate()?.alpha(1f)?.setDuration(200)?.start()
            }?.start()
        }
    }
}