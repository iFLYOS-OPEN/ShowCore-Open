package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.model.Group
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.UnknownHostException

class MediaFragment : BaseFragment(), PageScrollable {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private var placeholderView: View? = null

    private var adapter: MediaPagerAdapter? = null

    private var backCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_media, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.back).setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            pop()
        }

        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.view_pager)

        placeholderView = view.findViewById(R.id.placeholder)

        getMediaSections()

        view.findViewById<View>(R.id.refresh)?.setOnClickListener {
            showPlaceholder()
            getMediaSections()
        }
    }

    private fun showPlaceholder() {
        placeholderView?.let { placeholder ->
            placeholder.isVisible = true
            placeholder.animate().alpha(1f).setDuration(350)
                .start()
        }
    }

    private fun hidePlaceholder() {
        placeholderView?.let { placeholder ->
            placeholder.animate().alpha(0f).setDuration(350)
                .withEndAction {
                    placeholder.isVisible = false
                }
                .start()
        }
    }

    private fun setupTab(groups: List<Pair<String?, List<Group>>>) {
        adapter = MediaPagerAdapter(this)
        adapter?.addItems(groups)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = groups[position].first
        }.attach()
    }

    private fun getMediaSections() {
        getMediaApi()?.getMediaSections()?.enqueue(object : Callback<ArrayList<Group>> {
            override fun onFailure(call: Call<ArrayList<Group>>, t: Throwable) {
                t.printStackTrace()

                if (isDetached || isRemoving)
                    return
                context ?: return
                hidePlaceholder()
                if (t is UnknownHostException) {
                    view?.findViewById<View>(R.id.error_container)?.let { errorContainer ->
                        errorContainer.isVisible = true

                        val errorText = errorContainer.findViewById<TextView>(R.id.error_text)
                        errorText?.text = "网络出了点小差，请检查网络后重试"
                    }
                } else {
                    view?.findViewById<View>(R.id.error_container)?.let { errorContainer ->
                        errorContainer.isVisible = true

                        val errorText = errorContainer.findViewById<TextView>(R.id.error_text)
                        errorText?.text = "加载失败，请重试"
                    }
                }
            }

            override fun onResponse(
                call: Call<ArrayList<Group>>,
                response: Response<ArrayList<Group>>
            ) {
                if (isDetached || isRemoving)
                    return
                context ?: return
                hidePlaceholder()
                if (response.isSuccessful) {
                    val groups = response.body()
                    val groupMap = groups?.groupBy { it.abbr }
                    groupMap?.let { setupTab(it.toList()) }
                    view?.findViewById<View>(R.id.error_container)?.isVisible = false
                } else {
                    view?.findViewById<View>(R.id.error_container)?.let { errorContainer ->
                        errorContainer.isVisible = true

                        val errorText = errorContainer.findViewById<TextView>(R.id.error_text)
                        errorText?.text = "加载失败，请重试"
                    }
                }
            }
        })
    }

    private fun getMediaApi(): MediaApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(MediaApi::class.java)
        } else {
            null
        }
    }

    override fun scrollToNext(): Boolean {
        val currentItem = viewPager.currentItem
        if (currentItem < (adapter?.itemCount ?: 0) - 1 || currentItem < 0) {
            viewPager.post {
                viewPager.setCurrentItem(currentItem + 1, true)
            }
            return true
        }
        return false
    }

    override fun scrollToPrevious(): Boolean {
        val currentItem = viewPager.currentItem
        if (currentItem > 0) {
            viewPager.post {
                viewPager.setCurrentItem(currentItem - 1, true)
            }
            return true
        }
        return false
    }

    inner class MediaPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

        private var items = ArrayList<Pair<String?, List<Group>>>()

        fun addItems(groups: List<Pair<String?, List<Group>>>) {
            items.clear()
            items.addAll(groups)
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun createFragment(position: Int): Fragment {
            val item = items[position]
            return if (TextUtils.equals(item.first, "音乐")) {
                MusicFragment.instance(item.second[0].id)
            } else {
                MediaSectionFragment.newInstance(item.first, item.second)
            }
        }
    }
}