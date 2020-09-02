package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.iflytek.cyber.iot.show.core.BuildConfig
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.UserInfo
import com.iflytek.cyber.iot.show.core.utils.ConfigUtils
import com.iflytek.cyber.iot.show.core.utils.OnItemClickListener
import kotlin.math.max
import kotlin.math.min

class AccountFragment : BaseFragment(), PageScrollable {
    private var recyclerView: RecyclerView? = null
    private val infoList = mutableListOf<Item>()
    private val adapter = ListAdapter()

    private var backCount = 0

    private val onItemClickListener = object : OnItemClickListener {
        override fun onItemClick(parent: ViewGroup, itemView: View, position: Int) {
            if (position == adapter.itemCount - 1) {
                start(PairFragment2())
            } else if (position == adapter.itemCount - 2) {
                val webViewFragment = WebViewFragment()
                val arguments = Bundle()
                arguments.putString("url", "https://${BuildConfig.PREFIX}homev2.iflyos.cn/accounts")
                webViewFragment.arguments = arguments
                start(webViewFragment)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.back).setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            pop()
        }

        recyclerView = view.findViewById(R.id.info_list)
        recyclerView?.adapter = adapter

        post {
            initInfoFromPref()
        }
    }

    private fun initInfoFromPref() {
        ConfigUtils.getString(ConfigUtils.KEY_CACHE_USER_INFO, null)?.let { userInfo ->
            try {
                infoList.clear()

                val userInfoObj = Gson().fromJson(userInfo, UserInfo::class.java)

                // 用户名
                userInfoObj.nickname?.let { nickname ->
                    val nameItem = Item("用户名", nickname)

                    infoList.add(nameItem)
                }

                // 手机号
                userInfoObj.phone?.let { phone ->
                    val phoneItem = Item("手机号", phone)

                    infoList.add(phoneItem)
                }

                infoList.add(Item("内容账号", null))

                // 更换绑定
                val changeBindingItem = Item("更换绑定", null)
                infoList.add(changeBindingItem)

                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun scrollToNext(): Boolean {
        (recyclerView?.layoutManager as? LinearLayoutManager)?.let { linearLayoutManager ->
            val itemCount = adapter.itemCount
            val firstItem = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
            val lastItem = linearLayoutManager.findLastCompletelyVisibleItemPosition()
            if (lastItem == itemCount - 1) {
                return false
            } else {
                val pageCount = lastItem - firstItem
                val target = min(itemCount - 1, lastItem + pageCount)
                recyclerView?.smoothScrollToPosition(target)
            }
        }
        return true
    }

    override fun scrollToPrevious(): Boolean {
        (recyclerView?.layoutManager as? LinearLayoutManager)?.let { linearLayoutManager ->
            val firstItem = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
            val lastItem = linearLayoutManager.findLastCompletelyVisibleItemPosition()
            if (firstItem == 0) {
                return false
            } else {
                val pageCount = lastItem - firstItem
                val target = max(0, firstItem - pageCount)
                recyclerView?.smoothScrollToPosition(target)
            }
        }
        return true
    }

    private inner class ListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val holder = AboutItemViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_two_lines, parent, false))
            holder.itemView.setOnClickListener {
                onItemClickListener.onItemClick(parent, it, holder.adapterPosition)
            }
            return holder
        }

        override fun getItemCount(): Int {
            return infoList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is AboutItemViewHolder) {
                holder.tvTitle.text = infoList[position].title
                val summary = infoList[position].summary
                if (!summary.isNullOrEmpty()) {
                    holder.tvSummary.visibility = View.VISIBLE
                    holder.tvSummary.text = summary
                } else {
                    holder.tvSummary.visibility = View.GONE
                }
            }
        }

    }

    private class AboutItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.title)
        val tvSummary: TextView = itemView.findViewById(R.id.summary)
    }

    private data class Item(val title: String, val summary: String?)
}