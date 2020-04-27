package com.iflytek.cyber.iot.show.core.fragment

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.*
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.PopupWindowCompat
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.IotApi
import com.iflytek.cyber.iot.show.core.model.Device
import com.iflytek.cyber.iot.show.core.model.DeviceList
import com.iflytek.cyber.iot.show.core.model.Message
import com.iflytek.cyber.iot.show.core.utils.clickWithTrigger
import com.iflytek.cyber.iot.show.core.utils.dp2Px
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.Exception

class SmartHomeFragment : BaseFragment() {

    companion object {

        private const val REQUEST_BRAND_CODE = 10343

        fun newInstance(needSync: Boolean): SmartHomeFragment {
            return SmartHomeFragment().apply {
                arguments = bundleOf(Pair("need_sync", needSync))
            }
        }
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var roomContent: LinearLayout
    private lateinit var tvRoom: TextView
    private lateinit var emptyContent: LinearLayout
    private lateinit var addDeviceButton: Button
    private lateinit var tabContent: View
    private lateinit var errorContainer: View
    private lateinit var loading: LottieAnimationView
    private lateinit var tvSync: TextView
    private lateinit var ivSync: ImageView
    private var popupWindow: PopupWindow? = null
    private var menuList: RecyclerView? = null

    private var menuAdapter: MenuAdapter? = null
    private var deviceAdapter: DeviceAdapter? = null

    private var rotateAnimation: RotateAnimation? = null
    private var isSync = false

    private var menuDeviceList = ArrayList<Pair<String?, List<Device>>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_smart_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recycler_view)
        roomContent = view.findViewById(R.id.room_content)
        tvRoom = view.findViewById(R.id.tv_room)
        emptyContent = view.findViewById(R.id.empty_content)
        addDeviceButton = view.findViewById(R.id.btn_add_device)
        tabContent = view.findViewById(R.id.tab_content)
        errorContainer = view.findViewById(R.id.error_container)
        loading = view.findViewById(R.id.loading)
        tvSync = view.findViewById(R.id.tv_sync)
        ivSync = view.findViewById(R.id.iv_sync)

        view.findViewById<Button>(R.id.retry).setOnClickListener {
            showLoading()
            getDeviceList()
        }
        view.findViewById<View>(R.id.add_device_content).setOnClickListener {
            startForResult(BrandFragment(), REQUEST_BRAND_CODE)
        }
        view.findViewById<View>(R.id.back).clickWithTrigger {
            pop()
        }
        addDeviceButton.setOnClickListener {
            startForResult(BrandFragment(), REQUEST_BRAND_CODE)
        }

        roomContent.setOnClickListener {
            if (menuDeviceList.size > 1) {
                createPopupMenu(menuDeviceList)
            }
        }

        view.findViewById<View>(R.id.sync_content).clickWithTrigger {
            if (!isSync) {
                syncDevice()
            }
        }

        showLoading()
        getDeviceList()

        val needSync = arguments?.getBoolean("need_sync", false)
        if (needSync == true) {
            syncDevice()
        }
    }

    private fun createPopupMenu(items: List<Pair<String?, List<Device>>>) {
        if (!isAdded || context == null) {
            return
        }

        if (popupWindow == null) {
            popupWindow = PopupWindow(context)
            popupWindow?.width = ViewGroup.LayoutParams.WRAP_CONTENT
            val dp270 = context!!.resources.getDimensionPixelSize(R.dimen.dp_270)
            if (items.size > 5) {
                popupWindow?.height = dp270
            } else {
                popupWindow?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            popupWindow?.isOutsideTouchable = true
            popupWindow?.isFocusable = true
            popupWindow?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val view =
                LayoutInflater.from(context).inflate(R.layout.layout_device_popup, null, false)
            menuList = view.findViewById(R.id.recycler_view)
            popupWindow?.contentView = view
        }

        if (menuAdapter == null) {
            menuAdapter = MenuAdapter(items) { title, items ->
                popupWindow?.dismiss()
                val text = if (title.isNullOrEmpty()) {
                    "其他位置"
                } else {
                    title
                }
                tvRoom.text = getString(R.string.room_text, text)
                deviceAdapter?.items = items
                deviceAdapter?.notifyDataSetChanged()
            }
            menuList?.adapter = menuAdapter
        } else {
            menuList?.scrollToPosition(menuAdapter?.selectedPosition ?: 0)
            menuAdapter?.items = items
            menuAdapter?.notifyDataSetChanged()
        }

        val yOff = -(16.dp2Px())
        val xOff = roomContent.measuredWidth / 2 - 70.dp2Px()
        if (popupWindow != null) {
            PopupWindowCompat.showAsDropDown(
                popupWindow!!,
                roomContent,
                xOff,
                yOff,
                Gravity.START
            )
        }
    }

    private fun setupDeviceList(items: ArrayList<Device>) {
        if (items.isEmpty()) {
            return
        }
        menuDeviceList.clear()
        menuDeviceList.add(0, Pair("全部", items))

        val map = items.groupBy { it.zone }
        var index = 1
        map.forEach {
            menuDeviceList.add(index, Pair(it.key, it.value))
            index += 1
        }
        tvRoom.text = getString(R.string.room_text, menuDeviceList[0].first)

        if (deviceAdapter == null) {
            deviceAdapter = DeviceAdapter(menuDeviceList[0].second) {
                start(DeviceDetailFragment.newInstance(it.deviceId, it.iotInfoId))
            }
            recyclerView.adapter = deviceAdapter
        } else {
            deviceAdapter?.items = menuDeviceList[0].second
            deviceAdapter?.notifyDataSetChanged()
        }
    }

    private fun showLoading() {
        tabContent.isVisible = false
        recyclerView.isVisible = false
        emptyContent.isVisible = false
        errorContainer.isVisible = false
        loading.isVisible = true
        loading.playAnimation()
    }

    private fun hideLoading() {
        loading.isVisible = false
        loading.pauseAnimation()
    }

    private fun getDeviceList() {
        getIotApi()?.getDeviceList()?.enqueue(object : Callback<DeviceList> {
            override fun onResponse(call: Call<DeviceList>, response: Response<DeviceList>) {
                hideLoading()
                if (response.isSuccessful) {
                    errorContainer.isVisible = false
                    val deviceList = response.body()
                    val devices = deviceList?.devices
                    if (devices != null && devices.size > 0) {
                        tabContent.isVisible = true
                        recyclerView.isVisible = true
                        emptyContent.isVisible = false
                        setupDeviceList(devices)
                    } else {
                        tabContent.isVisible = false
                        recyclerView.isVisible = false
                        emptyContent.isVisible = true
                    }
                } else {
                    errorContainer.isVisible = true
                    emptyContent.isVisible = false
                    tabContent.isVisible = false
                    recyclerView.isVisible = false
                }
            }

            override fun onFailure(call: Call<DeviceList>, t: Throwable) {
                t.printStackTrace()
                hideLoading()
                errorContainer.isVisible = true
                emptyContent.isVisible = false
                tabContent.isVisible = false
                recyclerView.isVisible = false
            }
        })
    }

    private fun startSyncAnimation() {
        if (!isAdded || context == null) {
            return
        }

        val dp24 = context!!.resources.getDimensionPixelSize(R.dimen.dp_24)
        val pivot = dp24 / 2f
        if (rotateAnimation == null) {
            rotateAnimation = RotateAnimation(0f, 360f, pivot, pivot)
            rotateAnimation?.repeatCount = Animation.INFINITE
            rotateAnimation?.duration = 1500
            ivSync.animation = rotateAnimation
        }
        ivSync.startAnimation(rotateAnimation)
    }

    private fun stopSyncAnimation() {
        if (isAdded && context != null) {
            rotateAnimation?.cancel()
            ivSync.setColorFilter(Color.parseColor("#1784E9"))
            tvSync.text = getString(R.string.sync_device)
            tvSync.isSelected = false
        }
    }

    private fun syncDevice() {
        isSync = true
        Toast.makeText(context, "正在同步...", Toast.LENGTH_SHORT).show()
        tvSync.text = getString(R.string.in_synchronous)
        tvSync.isSelected = true
        ivSync.setColorFilter(Color.parseColor("#CAD0D6"))
        startSyncAnimation()
        getIotApi()?.syncDevice()?.enqueue(object : Callback<Message> {
            override fun onResponse(call: Call<Message>, response: Response<Message>) {
                isSync = false
                stopSyncAnimation()
                if (response.isSuccessful) {
                    if (isAdded && context != null) {
                        val message = response.body()
                        Toast.makeText(context, message?.message, Toast.LENGTH_SHORT).show()
                        getDeviceList()
                    }
                } else {
                    syncFailed(response.errorBody()?.string())
                }
            }

            override fun onFailure(call: Call<Message>, t: Throwable) {
                isSync = false
                stopSyncAnimation()
                t.printStackTrace()
            }
        })
    }

    private fun syncFailed(json: String?) {
        if (!isAdded || context == null) {
            return
        }
        try {
            val message = Gson().fromJson<Message>(json, Message::class.java)
            Toast.makeText(context, message.message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getIotApi(): IotApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(IotApi::class.java)
        } else {
            null
        }
    }

    override fun onFragmentResult(requestCode: Int, resultCode: Int, data: Bundle) {
        super.onFragmentResult(requestCode, resultCode, data)
        if (resultCode == 0) {
            val shouldSync = data.getBoolean("shouldSync")
            if (shouldSync) {
                getDeviceList()
            }
        }
    }

    private inner class DeviceAdapter(
        var items: List<Device>,
        val onItemClickListener: (Device) -> Unit
    ) :
        RecyclerView.Adapter<DeviceAdapter.DeviceHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_room_device, parent, false)
            return DeviceHolder(view)
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(holder: DeviceHolder, position: Int) {
            val item = items[position]
            Glide.with(holder.itemView.context)
                .load(item.icon)
                .into(holder.ivDeviceIcon)
            holder.tvDevice.text = item.friendlyName
            if (item.reacheable == true) {
                holder.tvStatus.text = "在线"
                holder.tvStatus.isSelected = true
                holder.ivDeviceStatus.isSelected = true
            } else {
                holder.tvStatus.text = "离线"
                holder.tvStatus.isSelected = false
                holder.ivDeviceStatus.isSelected = false
            }
            Glide.with(holder.itemView.context)
                .load(item.iotInfo?.iconUrl)
                .into(holder.ivBrand)
            holder.tvZone.isVisible = !item.zone.isNullOrEmpty()
            holder.tvZone.text = item.zone
            holder.clickable.setOnClickListener {
                onItemClickListener.invoke(item)
            }
        }

        inner class DeviceHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivDeviceIcon = itemView.findViewById<ImageView>(R.id.iv_device_icon)
            val tvDevice = itemView.findViewById<TextView>(R.id.tv_device)
            val tvStatus = itemView.findViewById<TextView>(R.id.tv_status)
            val ivBrand = itemView.findViewById<ImageView>(R.id.iv_brand)
            val tvZone = itemView.findViewById<TextView>(R.id.tv_zone)
            val ivDeviceStatus = itemView.findViewById<ImageView>(R.id.iv_device_status)
            val clickable = itemView.findViewById<View>(R.id.clickable)
        }
    }

    private inner class MenuAdapter(
        var items: List<Pair<String?, List<Device>>>,
        val onItemClickListener: (String?, List<Device>) -> Unit
    ) :
        RecyclerView.Adapter<MenuAdapter.MenuHolder>() {

        var selectedPosition = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuHolder {
            val view = createItem(parent.context)
            return MenuHolder(view)
        }

        private fun createItem(context: Context): TextView {
            val view = TextView(context)
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            view.layoutParams = layoutParams
            view.setPadding(16.dp2Px(), 14.dp2Px(), 16.dp2Px(), 14.dp2Px())
            view.textSize = 14f
            view.setTextColor(Color.parseColor("#262626"))
            return view
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(holder: MenuHolder, position: Int) {
            val item = items[position]
            val textView = holder.itemView as TextView
            if (item.first.isNullOrEmpty()) {
                textView.text = "其他位置"
            } else {
                textView.text = item.first
            }
            if (selectedPosition == holder.adapterPosition) {
                textView.setBackgroundColor(Color.parseColor("#80E2E7EB"))
            } else {
                textView.setBackgroundColor(Color.WHITE)
            }
            holder.itemView.setOnClickListener {
                selectedPosition = holder.adapterPosition
                onItemClickListener.invoke(item.first, item.second)
            }
        }

        inner class MenuHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }
}