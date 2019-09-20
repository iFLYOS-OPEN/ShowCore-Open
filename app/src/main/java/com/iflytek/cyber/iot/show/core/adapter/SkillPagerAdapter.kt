package com.iflytek.cyber.iot.show.core.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.iflytek.cyber.iot.show.core.fragment.SkillSectionFragment
import com.iflytek.cyber.iot.show.core.model.SkillSection

class SkillPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val items = ArrayList<SkillSection>()

    fun addItems(items: ArrayList<SkillSection>) {
        this.items.clear()
        this.items.addAll(items)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun createFragment(position: Int): Fragment {
        val skill = items[position]
        return SkillSectionFragment.newInstance(skill)
    }
}