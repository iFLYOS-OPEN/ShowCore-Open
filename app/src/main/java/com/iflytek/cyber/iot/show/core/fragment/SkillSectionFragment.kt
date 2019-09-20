package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.Skill
import com.iflytek.cyber.iot.show.core.model.SkillSection

class SkillSectionFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView

    companion object {
        fun newInstance(skill: SkillSection): SkillSectionFragment {
            return SkillSectionFragment().apply {
                arguments = bundleOf(Pair("skill", skill))
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_skill_section, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view)

        val skill = arguments?.getParcelable<SkillSection>("skill") ?: return
        val adapter = SkillAdapter(skill.skills) {
            (parentFragment as BaseFragment).start(SkillDetailFragment.newInstance(it))
        }
        recyclerView.adapter = adapter
    }

    inner class SkillAdapter(private val skills: ArrayList<Skill>,
                             private val onItemClickListener: (skill: Skill) -> Unit)
        : RecyclerView.Adapter<SkillAdapter.SkillHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkillHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_skill, parent, false)
            return SkillHolder(view)
        }

        override fun getItemCount(): Int {
            return skills.size
        }

        override fun onBindViewHolder(holder: SkillHolder, position: Int) {
            val skill = skills[position]
            holder.tvSkill.text = skill.name
            holder.tvDesc.text = skill.description
            Glide.with(holder.ivSkillIcon)
                    .load(skill.icon)
                    .into(holder.ivSkillIcon)
            holder.itemView.setOnClickListener {
                onItemClickListener.invoke(skill)
            }
        }

        inner class SkillHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvSkill = itemView.findViewById<TextView>(R.id.tv_skill)
            val tvDesc = itemView.findViewById<TextView>(R.id.tv_description)
            val ivSkillIcon = itemView.findViewById<ImageView>(R.id.iv_skill_icon)
        }
    }
}
