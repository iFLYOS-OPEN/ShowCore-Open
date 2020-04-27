package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.model.Group
import com.iflytek.cyber.iot.show.core.utils.dp2Px
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.UnknownHostException

class MediaFragment : BaseFragment(), PageScrollable {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager
    private var placeholderView: View? = null

    private var adapter: MediaPagerAdapter? = null

    private var backCount = 0
    private var isSearchButtonClicked = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_media, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.back_container).setOnClickListener {
        }
        view.findViewById<View>(R.id.back).setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            pop()
        }

        view.findViewById<ImageView>(R.id.iv_search).setOnClickListener {
            if (!isSearchButtonClicked) {
                isSearchButtonClicked = true
                start(SearchFragment())
            }
        }

        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.view_pager)

        placeholderView = view.findViewById(R.id.placeholder)

        getMediaSections()

        view.findViewById<View>(R.id.refresh)?.setOnClickListener {
            showPlaceholder()
            getMediaSections()
        }

        try {
            val field = viewPager.javaClass.getDeclaredField("mTouchSlop")
            field.isAccessible = true
            field.setInt(viewPager, 18.dp2Px()) // set ViewPager touch slop bigger than default value
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        isSearchButtonClicked = false
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
        adapter = MediaPagerAdapter(childFragmentManager)
        groups.forEach { item ->
            if (TextUtils.equals(item.first, "音乐")) {
                adapter?.addItems(item.first, MusicFragment(), item.second)
            } else {
                adapter?.addItems(item.first, MediaSectionFragment(), item.second)
            }
        }
        viewPager.offscreenPageLimit = 5
        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)
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
        if (currentItem < (adapter?.count ?: 0) - 1 || currentItem < 0) {
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

    inner class MediaPagerAdapter(fragmentManager: FragmentManager) :
        FragmentPagerAdapter(fragmentManager) {

        private var titles = ArrayList<String?>()
        private var fragments = ArrayList<Fragment>()
        private var groups = ArrayList<List<Group>>()

        fun addItems(title: String?, fragment: Fragment, group: List<Group>) {
            titles.add(title)
            fragments.add(fragment)
            groups.add(group)
        }

        override fun getCount(): Int {
            return fragments.size
        }

        override fun getItem(position: Int): Fragment {
            val fragment = fragments[position]
            val title = titles[position]
            val group = groups[position]
            if (TextUtils.equals(title, "音乐")) {
                fragment.arguments = bundleOf(Pair("id", group[0].id))
            } else {
                fragment.arguments = bundleOf(
                    Pair("name", title),
                    Pair("groups", group)
                )
            }
            return fragment
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return titles[position]
        }
    }
}