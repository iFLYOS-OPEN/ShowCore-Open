package com.iflytek.cyber.iot.show.core.fragment

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonParser
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.iot.show.core.BuildConfig
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.utils.WifiUtils
import okhttp3.OkHttpClient
import okhttp3.Request

class AboutFragment2 : BaseFragment() {
    private var recyclerView: RecyclerView? = null
    private val aboutList = mutableListOf<Item>()
    private val adapter = ListAdapter()

    private var deviceName: String? = null
    private var speakerName: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about_2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.back).setOnClickListener {
            pop()
        }

        recyclerView = view.findViewById(R.id.about_list)
        recyclerView?.itemAnimator = DefaultItemAnimator()

        // init about item
        updateAboutContent()

        recyclerView?.adapter = adapter

        requestDeviceInfo()
    }

    private fun requestDeviceInfo() {
        Thread {
            try {
                val context = context ?: return@Thread

                val client = OkHttpClient.Builder()
                    .build()

                val authResponse = AuthDelegate.getAuthResponseFromPref(context)

                val request = Request.Builder()
                    .url("https://staging-api.iflyos.cn/showcore/api/v1/device")
                    .header("Authorization", "Bearer ${authResponse?.accessToken}")
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    println(body)

                    val json = JsonParser().parse(body).asJsonObject

                    val alias = json.get("alias")?.asString
                    val speakerName = json.get("speaker")?.asString

                    this.deviceName = alias
                    this.speakerName = speakerName

                    post {
                        updateAboutContent()
                    }
                } else {
                    Log.d("About", "request failed: ${response.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun updateAboutContent() {
        val context = context ?: return
        val prefSize = aboutList.size
        aboutList.clear()

        deviceName?.let {
            val deviceNameItem = Item("设备名称", it)
            aboutList.add(deviceNameItem)
        }

        speakerName?.let {
            val speakerNameItem = Item("发音人", it)
            aboutList.add(speakerNameItem)
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

        val versionItem = Item("系统版本", BuildConfig.VERSION_NAME)
        aboutList.add(versionItem)

        if (prefSize > 0) {
            adapter.notifyItemRangeChanged(0, prefSize)
            if (aboutList.size > prefSize) {
                adapter.notifyItemRangeInserted(prefSize, aboutList.size)
            }
        } else {
            adapter.notifyItemRangeInserted(0, aboutList.size)
        }
    }

    private inner class ListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val holder = AboutItemViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_two_lines, parent, false))
            holder.itemView.setOnClickListener {

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
            }
        }

    }

    private class AboutItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.title)
        val tvSummary: TextView = itemView.findViewById(R.id.summary)
    }

    private data class Item(val title: String, val summary: String)
}