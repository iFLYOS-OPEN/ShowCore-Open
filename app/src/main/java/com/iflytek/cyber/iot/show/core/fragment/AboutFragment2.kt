package com.iflytek.cyber.iot.show.core.fragment

import android.app.ProgressDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonParser
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.iot.show.core.BuildConfig
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.DeviceApi
import com.iflytek.cyber.iot.show.core.utils.*
import com.iflytek.cyber.iot.show.core.widget.StyledAlertDialog
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class AboutFragment2 : BaseFragment(), PageScrollable {

    private var recyclerView: RecyclerView? = null
    private val aboutList = mutableListOf<Item>()
    private val adapter = ListAdapter()

    private var deviceName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about_2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.back).setOnClickListener {
            pop()
        }

        recyclerView = view.findViewById(R.id.about_list)
        recyclerView?.itemAnimator = DefaultItemAnimator()

        recyclerView?.adapter = adapter

        requestDeviceInfo()
    }

    override fun onSupportVisible() {
        super.onSupportVisible()

        // init about item
        updateAboutContent()
    }

    override fun scrollToNext(): Boolean {
        recyclerView?.let { recyclerView ->
            val lastItem =
                (recyclerView.layoutManager as? LinearLayoutManager)?.findLastCompletelyVisibleItemPosition()
            val itemCount = adapter.itemCount
            if (lastItem == itemCount - 1 || itemCount == 0) {
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
            val itemCount = adapter.itemCount
            if (scrollY == 0 || itemCount == 0) {
                return false
            } else {
                recyclerView.smoothScrollBy(0, -recyclerView.height)
            }
        }
        return true
    }

    private fun requestDeviceInfo() {
        getDeviceApi()?.get()?.enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    val body = responseBody?.string()

                    val json = JsonParser().parse(body).asJsonObject

                    val alias = json.get("alias")?.asString
                    val speakerName = json.get("speaker")?.asString

                    this@AboutFragment2.deviceName = alias

                    responseBody?.close()

                    post {
                        updateAboutContent()
                    }
                } else {
                    Log.d("About", "request failed: ${response.code()}")
                }
            }

        })
    }

    private fun updateAboutContent() {
        val context = context ?: return
        val prefSize = aboutList.size
        aboutList.clear()

        deviceName?.let {
            val deviceNameItem = Item("设备名称", it)
            aboutList.add(deviceNameItem)
        }

        DeviceUtils.getIvwVersion().let {
            val ivwVersionItem =
                if (it.isNullOrEmpty())
                    Item("唤醒词引擎版本", "暂不支持显示")
                else
                    Item("唤醒词引擎版本", it)
            aboutList.add(ivwVersionItem)
        }

        try {
            @Suppress("DEPRECATION")
            (if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1)
                Build.SERIAL
            else Build.getSerial())?.let {
                val serialItem = Item("设备序列号", it)
                aboutList.add(serialItem)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        WifiUtils.getIPAddress(context)?.let {
            val ipAddressItem = Item("IP 地址", it)
            aboutList.add(ipAddressItem)
        }

        WifiUtils.getMacAddress(context)?.let {
            val macAddressItem = Item("MAC 地址", it)
            aboutList.add(macAddressItem)
        }

        val versionItem = Item(getString(R.string.system_version), DeviceUtils.getSystemVersionName())
        aboutList.add(versionItem)

        aboutList.add(Item(getString(R.string.factory_reset), ""))

        if (prefSize > 0) {
            adapter.notifyItemRangeChanged(0, prefSize)
            if (aboutList.size > prefSize) {

                adapter.notifyItemRangeInserted(prefSize, aboutList.size)
            }
        } else {
            adapter.notifyItemRangeInserted(0, aboutList.size)
        }
    }

    private fun getDeviceApi(): DeviceApi? {
        val context = context ?: return null
        return CoreApplication.from(context).createApi(DeviceApi::class.java)
    }

    private inner class ListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val holder = AboutItemViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_two_lines, parent, false)
            )
            holder.itemView.setOnClickListener {
                val position = holder.adapterPosition
                val item = aboutList[position]
                when (item.title) {
                    getString(R.string.factory_reset) -> {
                        val fragmentManager = fragmentManager ?: return@setOnClickListener
                        StyledAlertDialog.Builder()
                            .setTitle(getString(R.string.factory_reset))
                            .setMessage(getString(R.string.factory_reset_message))
                            .setPositiveButton(
                                getString(R.string.ensure),
                                View.OnClickListener { view ->
                                    val context = view.context

                                    val progressDialog = ProgressDialog(context)
                                    progressDialog.setMessage("正在恢复")
                                    progressDialog.show()

                                    CoreApplication.from(context).createApi(DeviceApi::class.java)
                                        ?.postRestoreFactory()
                                        ?.enqueue(object : Callback<ResponseBody> {
                                            override fun onFailure(
                                                call: Call<ResponseBody>,
                                                t: Throwable
                                            ) {
                                                t.printStackTrace()
                                                try {
                                                    progressDialog.dismiss()
                                                } catch (tInside: Throwable) {

                                                }
                                                Toast.makeText(
                                                    context,
                                                    "恢复出厂设置服务端验证失败，请稍后再试",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }

                                            override fun onResponse(
                                                call: Call<ResponseBody>,
                                                response: Response<ResponseBody>
                                            ) {
                                                try {
                                                    progressDialog.dismiss()
                                                } catch (t: Throwable) {

                                                }
                                                if (!response.isSuccessful) {
                                                    Toast.makeText(
                                                        context,
                                                        "恢复出厂设置服务端验证失败，请稍后再试",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }

                                        })
                                })
                            .setNegativeButton(getString(R.string.cancel), null)
                            .setWarningAction(true)
                            .show(fragmentManager)
                    }
                }
            }
            return holder
        }

        override fun getItemCount(): Int {
            return aboutList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is AboutItemViewHolder) {
                holder.tvTitle.text = aboutList[position].title
                holder.tvSummary.text = aboutList[position].summary
                holder.tvSummary.isVisible = aboutList[position].summary.isNotEmpty()
            }
        }

    }

    private class AboutItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.title)
        val tvSummary: TextView = itemView.findViewById(R.id.summary)
    }

    private data class Item(val title: String, val summary: String)
}