package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.SkillPagerAdapter
import com.iflytek.cyber.iot.show.core.api.SkillApi
import com.iflytek.cyber.iot.show.core.model.SkillSection
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.UnknownHostException

class SkillsFragment : BaseFragment(), PageScrollable {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    private var adapter: SkillPagerAdapter? = null
    private var backCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_skills, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.view_pager)

        view.findViewById<View>(R.id.back).setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            pop()
        }

        view.postDelayed(200) {
            loadSkillSections()
        }

        view.findViewById<View>(R.id.refresh)?.setOnClickListener {
            loadSkillSections()
        }
    }

    private fun setupTabLayout(skillSections: ArrayList<SkillSection>) {
        if (!isAdded) {
            return
        }

        adapter = SkillPagerAdapter(this)
        adapter?.addItems(skillSections)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = skillSections[position].title
        }.attach()
    }

    private fun loadSkillSections() {
        getSkillApi()?.getSkillSections()?.enqueue(object : Callback<ArrayList<SkillSection>> {
            override fun onResponse(
                call: Call<ArrayList<SkillSection>>,
                response: Response<ArrayList<SkillSection>>
            ) {
                if (response.isSuccessful) {
                    val skillSections = response.body()
                    skillSections?.let {
                        setupTabLayout(it)
                    }
                    view?.findViewById<View>(R.id.error_container)?.isVisible = false
                } else {
                    view?.findViewById<View>(R.id.error_container)?.let { errorContainer ->
                        errorContainer.isVisible = true

                        val errorText = errorContainer.findViewById<TextView>(R.id.error_text)
                        errorText?.text = "加载失败，请重试"
                    }
                }
            }

            override fun onFailure(call: Call<ArrayList<SkillSection>>, t: Throwable) {
                t.printStackTrace()

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
        })
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

    private fun getSkillApi(): SkillApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(SkillApi::class.java)
        } else {
            null
        }
    }
}