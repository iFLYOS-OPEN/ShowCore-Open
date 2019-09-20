package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.MediaSectionAdapter
import com.iflytek.cyber.iot.show.core.model.Group

class MediaSectionFragment : BaseFragment() {

    companion object {
        fun newInstance(name: String?, groups: List<Group>): MediaSectionFragment {
            return MediaSectionFragment().apply {
                arguments = bundleOf(Pair("name", name),
                        Pair("groups", groups))
            }
        }
    }

    private lateinit var sectionList: RecyclerView

    private var mediaSectionAdapter: MediaSectionAdapter? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_music, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val name = arguments?.getString("name")
        val groups = arguments?.getParcelableArrayList<Group>("groups")

        if (name.isNullOrEmpty() || groups == null) {
            return
        }

        sectionList = view.findViewById(R.id.section_list)

        mediaSectionAdapter = MediaSectionAdapter(name, groups, {
            (parentFragment as BaseFragment).start(SongListFragment.instance(it.id, it.name, name))
        }, { position ->
            (parentFragment as BaseFragment).start(MediaSectionListFragment.instance(groups[position].id, name))
        })
        sectionList.adapter = mediaSectionAdapter
    }
}