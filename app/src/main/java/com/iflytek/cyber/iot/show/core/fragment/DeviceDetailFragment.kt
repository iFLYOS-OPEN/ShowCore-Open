package com.iflytek.cyber.iot.show.core.fragment

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.alibaba.fastjson.JSONObject
import com.bumptech.glide.Glide
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.IotApi
import com.iflytek.cyber.iot.show.core.model.Action
import com.iflytek.cyber.iot.show.core.model.Device
import com.iflytek.cyber.iot.show.core.utils.clickWithTrigger
import com.iflytek.cyber.iot.show.core.utils.dp2Px
import com.zhy.view.flowlayout.FlowLayout
import com.zhy.view.flowlayout.TagAdapter
import com.zhy.view.flowlayout.TagFlowLayout
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DeviceDetailFragment : BaseFragment() {

    companion object {
        fun newInstance(deviceId: String?, iotInfoId: Int?): DeviceDetailFragment {
            return DeviceDetailFragment().apply {
                arguments = bundleOf(Pair("deviceId", deviceId), Pair("iotInfoId", iotInfoId))
            }
        }
    }

    private lateinit var ivDeviceIcon: ImageView
    private lateinit var tvDeviceName: TextView
    private lateinit var ivDeviceStatus: ImageView
    private lateinit var tvDeviceStatus: TextView
    private lateinit var tvZone: TextView
    private lateinit var tvDeviceType: TextView
    private lateinit var ivBrandIcon: ImageView
    private lateinit var tvBrand: TextView
    private lateinit var actionList: RecyclerView
    private lateinit var mainContent: NestedScrollView
    private lateinit var errorContainer: View
    private lateinit var loading: LottieAnimationView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_device_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ivDeviceIcon = view.findViewById(R.id.iv_device_icon)
        tvDeviceName = view.findViewById(R.id.tv_device_name)
        ivDeviceStatus = view.findViewById(R.id.iv_device_status)
        tvDeviceStatus = view.findViewById(R.id.tv_status)
        tvZone = view.findViewById(R.id.tv_zone)
        tvDeviceType = view.findViewById(R.id.tv_device_type)
        ivBrandIcon = view.findViewById(R.id.iv_brand_icon)
        tvBrand = view.findViewById(R.id.tv_brand)
        actionList = view.findViewById(R.id.action_list)
        mainContent = view.findViewById(R.id.main_content)
        errorContainer = view.findViewById(R.id.error_container)
        loading = view.findViewById(R.id.loading)

        view.findViewById<View>(R.id.back).clickWithTrigger {
            pop()
        }

        showLoading()
        val deviceId = arguments?.getString("deviceId")
        val iotInfoId = arguments?.getInt("iotInfoId")
        view.postDelayed(200) {
            getDeviceDetail(deviceId, iotInfoId)
        }


        view.findViewById<Button>(R.id.retry).setOnClickListener {
            showLoading()
            getDeviceDetail(deviceId, iotInfoId)
        }
    }

    private fun setupUI(device: Device) {
        if (!isAdded || context == null) {
            return
        }

        Glide.with(context!!)
            .load(device.icon)
            .into(ivDeviceIcon)
        tvDeviceName.text = device.friendlyName
        if (device.reacheable == true) {
            ivDeviceStatus.isSelected = true
            tvDeviceStatus.isSelected = true
            tvDeviceStatus.text = "在线"
        } else {
            ivDeviceStatus.isSelected = false
            tvDeviceStatus.isSelected = false
            tvDeviceStatus.text = "离线"
        }
        if (!device.zone.isNullOrEmpty()) {
            tvZone.isVisible = true
            tvZone.text = device.zone
        } else {
            tvZone.isVisible = false
        }
        tvDeviceType.text = device.deviceType.describe
        Glide.with(context!!)
            .load(device.iotInfo?.iconUrl)
            .into(ivBrandIcon)
        tvBrand.text = device.iotInfo?.name

        if (device.actions != null) {
            actionList.adapter = ActionAdapter(device.actions)
        }
    }

    private fun showLoading() {
        mainContent.isVisible = false
        errorContainer.isVisible = false
        loading.isVisible = true
        loading.playAnimation()
    }

    private fun hideLoading() {
        loading.isVisible = false
        loading.pauseAnimation()
    }

    private fun getDeviceDetail(deviceId: String?, iotInfoId: Int?) {
        val json = JSONObject()
        json["device_id"] = deviceId
        json["iot_info_id"] = iotInfoId
        val body = RequestBody.create(
            MediaType.parse("application/json"),
            json.toString()
        )
        getIotApi()?.getDeviceDetail(body)?.enqueue(object : Callback<Device> {
            override fun onResponse(call: Call<Device>, response: Response<Device>) {
                hideLoading()
                if (response.isSuccessful) {
                    mainContent.isVisible = true
                    errorContainer.isVisible = false
                    val device = response.body()
                    if (device != null) {
                        setupUI(device)
                    }
                } else {
                    errorContainer.isVisible = true
                    mainContent.isVisible = false
                }
            }

            override fun onFailure(call: Call<Device>, t: Throwable) {
                t.printStackTrace()
                hideLoading()
                errorContainer.isVisible = true
                mainContent.isVisible = false
            }
        })
    }

    private fun getIotApi(): IotApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(IotApi::class.java)
        } else {
            null
        }
    }

    private inner class ActionAdapter(val actions: ArrayList<Action>) :
        RecyclerView.Adapter<ActionAdapter.ActionHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_device_action, parent, false)
            return ActionHolder(view)
        }

        override fun getItemCount(): Int {
            return actions.size
        }

        private fun createActionText(context: Context, action: String?): TextView {
            val textView = TextView(context)
            textView.setPadding(16.dp2Px(), 12.dp2Px(), 16.dp2Px(), 12.dp2Px())
            textView.setTextColor(Color.parseColor("#1784E9"))
            textView.textSize = 16f
            textView.setBackgroundResource(R.drawable.bg_light_blue_round_12dp)
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.marginStart = 8.dp2Px()
            layoutParams.marginEnd = 8.dp2Px()
            layoutParams.topMargin = 8.dp2Px()
            layoutParams.bottomMargin = 8.dp2Px()
            textView.layoutParams = layoutParams
            textView.text = action
            return textView
        }

        override fun onBindViewHolder(holder: ActionHolder, position: Int) {
            val action = actions[position]
            holder.tvName.text = action.name
            holder.actionTag.adapter = object : TagAdapter<String>(action.values) {
                override fun getView(parent: FlowLayout?, position: Int, t: String?): View {
                    return createActionText(holder.itemView.context, t)
                }
            }
        }

        inner class ActionHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName = itemView.findViewById<TextView>(R.id.tv_name)
            val actionTag = itemView.findViewById<TagFlowLayout>(R.id.action_tag)
        }
    }
}