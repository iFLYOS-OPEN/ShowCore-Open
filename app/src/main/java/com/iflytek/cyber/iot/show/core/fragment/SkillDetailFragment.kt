package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.SkillApi
import com.iflytek.cyber.iot.show.core.model.Skill
import com.iflytek.cyber.iot.show.core.model.SkillDetail
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.utils.dp2Px
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.max
import kotlin.math.min

class SkillDetailFragment : BaseFragment(), PageScrollable {

    private lateinit var tvSkillName: TextView
    private lateinit var tvSkillDesc: TextView
    private lateinit var tvDev: TextView
    private lateinit var tvVersion: TextView
    private lateinit var tvDate: TextView
    private lateinit var ivSkillIcon: ImageView
    private lateinit var recyclerView: RecyclerView
    private var scrollView: NestedScrollView? = null
    private var contentContainer: View? = null

    companion object {
        fun newInstance(skill: Skill): SkillDetailFragment {
            return SkillDetailFragment().apply {
                arguments = bundleOf(Pair("skill", skill))
            }
        }

        fun newInstance(skillDetail: SkillDetail): SkillDetailFragment {
            return SkillDetailFragment().apply {
                arguments = bundleOf(Pair("skill_detail", skillDetail))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(context)
            .inflate(R.layout.fragment_skill_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.back).setOnClickListener { launcher?.onBackPressed() }

        scrollView = view.findViewById(R.id.scroll_view)
        contentContainer = view.findViewById(R.id.content_container)

        tvSkillName = view.findViewById(R.id.tv_skill_name)
        tvSkillDesc = view.findViewById(R.id.tv_skill_desc)
        tvDev = view.findViewById(R.id.tv_dev)
        tvVersion = view.findViewById(R.id.tv_version)
        tvDate = view.findViewById(R.id.tv_date)
        ivSkillIcon = view.findViewById(R.id.iv_skill_icon)
        recyclerView = view.findViewById(R.id.example_list)

        val skillDetail = arguments?.getParcelable<SkillDetail>("skill_detail")
        if (skillDetail != null) {
            setupUI(skillDetail)
        } else {
            val skill = arguments?.getParcelable<Skill>("skill")
            skill?.let { loadDetail(it.id) }
        }
    }

    private fun setupUI(detail: SkillDetail) {
        Glide.with(ivSkillIcon.context)
            .load(detail.icon)
            .apply(
                RequestOptions()
                    .transform(RoundedCornersTransformation(16.dp2Px(), 0))
            )
            .into(ivSkillIcon)
        tvSkillName.text = detail.name
        tvSkillDesc.text = detail.description
        tvDate.text = detail.updatedTime
        tvVersion.text = detail.version
        tvDev.text = detail.developer

        val adapter = ExampleAdapter(detail.examples)
        recyclerView.adapter = adapter
    }

    private fun loadDetail(id: String) {
        getSkillApi()?.getSkillDetail(id)?.enqueue(object : Callback<SkillDetail> {
            override fun onResponse(call: Call<SkillDetail>, response: Response<SkillDetail>) {
                if (response.isSuccessful) {
                    val detail = response.body()
                    detail?.let { setupUI(it) }
                }
            }

            override fun onFailure(call: Call<SkillDetail>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    private fun getSkillApi(): SkillApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(SkillApi::class.java)
        } else {
            null
        }
    }

    override fun scrollToNext(): Boolean {
        scrollView?.let { scrollView ->
            val pageHeight = scrollView.height
            val scrollY = scrollView.scrollY
            val contentHeight = contentContainer?.height ?: 0
            if (scrollY == contentHeight - pageHeight) {
                return false
            }
            val target = min(contentHeight - pageHeight, scrollY + pageHeight)
            smoothScrollTo(target)
            return true
        } ?: run {
            return false
        }
    }

    override fun scrollToPrevious(): Boolean {
        scrollView?.let { scrollView ->
            val pageHeight = scrollView.height
            val scrollY = scrollView.scrollY
            if (scrollY == 0) {
                return false
            }
            val target = max(0, scrollY - pageHeight)
            smoothScrollTo(target)
            return true
        } ?: run {
            return false
        }
    }

    private fun smoothScrollTo(scrollY: Int) {
        scrollView?.isSmoothScrollingEnabled = true
        scrollView?.smoothScrollTo(0, scrollY)
    }

    inner class ExampleAdapter(val examples: ArrayList<String>) :
        RecyclerView.Adapter<ExampleAdapter.ExampleHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExampleHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_example, parent, false)
            return ExampleHolder(view)
        }

        override fun getItemCount(): Int {
            return examples.size
        }

        override fun onBindViewHolder(holder: ExampleHolder, position: Int) {
            holder.desc.text = examples[position]
        }

        inner class ExampleHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val desc = itemView.findViewById<TextView>(R.id.tv_example)
        }
    }
}