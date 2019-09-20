package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.MusicAdapter
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.model.Group
import com.iflytek.cyber.iot.show.core.model.GroupItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MusicFragment : BaseFragment() {

    companion object {
        fun instance(id: String): MusicFragment {
            return MusicFragment().apply {
                arguments = bundleOf(Pair("id", id))
            }
        }
    }

    private lateinit var sectionList: RecyclerView

    private var musicAdapter: MusicAdapter? = null

    private var group: Group? = null
    private var groupId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_music, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sectionList = view.findViewById(R.id.section_list)

        groupId = arguments?.getString("id")
        groupId?.let { getMediaSection(it) }
    }

    private fun setupRecyclerView(items: List<Pair<String?, List<GroupItem>>>) {
        musicAdapter = MusicAdapter(items, {
            (parentFragment as BaseFragment).start(SongListFragment.instance(it.id, it.name, null))
        }, {
            group?.let { group ->
                (parentFragment as BaseFragment)
                        .start(MediaSectionListFragment.instance(group, it))
            }
        })
        sectionList.adapter = musicAdapter
    }

    private fun getMediaSection(id: String) {
        getMediaApi()?.getMediaSection(id)?.enqueue(object : Callback<Group> {
            override fun onFailure(call: Call<Group>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(call: Call<Group>, response: Response<Group>) {
                if (response.isSuccessful) {
                    group = response.body()
                    val itemMap = group?.items?.groupBy { it.categoryName }
                    itemMap?.let { setupRecyclerView(it.toList()) }
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
}