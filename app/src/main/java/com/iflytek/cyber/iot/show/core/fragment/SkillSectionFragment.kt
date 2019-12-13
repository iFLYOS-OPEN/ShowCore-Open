package com.iflytek.cyber.iot.show.core.fragment

import android.content.Intent
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
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.iflytek.cyber.evs.sdk.socket.Result
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.EngineService
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.SkillApi
import com.iflytek.cyber.iot.show.core.model.Skill
import com.iflytek.cyber.iot.show.core.model.SkillDetail
import com.iflytek.cyber.iot.show.core.model.SkillSection
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.UnknownHostException

class SkillSectionFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView

    private var requestingSkillId: String? = null

    companion object {
        fun newInstance(skill: SkillSection): SkillSectionFragment {
            return SkillSectionFragment().apply {
                arguments = bundleOf(Pair("skill", skill))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(context)
            .inflate(R.layout.fragment_skill_section, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view)

        val skill = arguments?.getParcelable<SkillSection>("skill") ?: return
        val adapter = SkillAdapter(skill.skills) {
            getSkillApi()?.getSkillDetail(it.id)?.enqueue(object : Callback<SkillDetail> {
                override fun onFailure(call: Call<SkillDetail>, t: Throwable) {
                    if (requestingSkillId != it.id)
                        return

                    if (t is UnknownHostException) {
                        val intent = Intent(EngineService.ACTION_SEND_REQUEST_FAILED)
                        intent.putExtra(
                            EngineService.EXTRA_RESULT,
                            Result(Result.CODE_DISCONNECTED, null)
                        )
                        context?.sendBroadcast(intent)
                    }
                }

                override fun onResponse(call: Call<SkillDetail>, response: Response<SkillDetail>) {
                    if (requestingSkillId != it.id)
                        return
                    if (response.isSuccessful) {
                        val detail = response.body() ?: return

                        (parentFragment as? BaseFragment)?.start(
                            SkillDetailFragment.newInstance(
                                detail
                            )
                        )
                    } else {
                        val disconnectNotification =
                            Intent(context, FloatingService::class.java)
                        disconnectNotification.action = FloatingService.ACTION_SHOW_NOTIFICATION
                        disconnectNotification.putExtra(
                            FloatingService.EXTRA_MESSAGE,
                            "请求出错，请稍后再试"
                        )
                        disconnectNotification.putExtra(
                            FloatingService.EXTRA_ICON_RES,
                            R.drawable.ic_default_error_white_40dp
                        )
                        disconnectNotification.putExtra(
                            FloatingService.EXTRA_POSITIVE_BUTTON_TEXT,
                            getString(R.string.i_got_it)
                        )
                        context?.startService(disconnectNotification)
                    }
                }
            })
            requestingSkillId = it.id
        }
        recyclerView.adapter = adapter
    }

    private fun getSkillApi(): SkillApi? {
        val context = context ?: return null
        return CoreApplication.from(context).createApi(SkillApi::class.java)
    }

    inner class SkillAdapter(
        private val skills: ArrayList<Skill>,
        private val onItemClickListener: (skill: Skill) -> Unit
    ) : RecyclerView.Adapter<SkillAdapter.SkillHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkillHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_skill, parent, false)
            return SkillHolder(view)
        }

        override fun getItemCount(): Int {
            return skills.size
        }

        override fun onBindViewHolder(holder: SkillHolder, position: Int) {
            val skill = skills[position]
            holder.tvSkill.text = skill.name
            holder.tvDesc.text = skill.description

            val transformer = MultiTransformation(
                CenterCrop(),
                RoundedCornersTransformation(
                    holder.itemView.resources.getDimensionPixelSize(R.dimen.dp_8), 0
                )
            )
            Glide.with(holder.ivSkillIcon)
                .load(skill.icon)
                .transform(transformer)
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
