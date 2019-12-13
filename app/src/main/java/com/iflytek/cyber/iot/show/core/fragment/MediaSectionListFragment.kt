package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.AlbumAdapter
import com.iflytek.cyber.iot.show.core.adapter.SpacesItemDecoration
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.model.Group
import com.iflytek.cyber.iot.show.core.model.GroupItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MediaSectionListFragment : BaseFragment(), PageScrollable {

    companion object {
        fun instance(id: String, typeName: String?): MediaSectionListFragment {
            return MediaSectionListFragment().apply {
                arguments = bundleOf(
                    Pair("id", id),
                    Pair("type", "SECTION"),
                    Pair("name", typeName)
                )
            }
        }

        fun instance(
            group: Group,
            abbr: String?,
            typeName: String? = null
        ): MediaSectionListFragment {
            return MediaSectionListFragment().apply {
                arguments = bundleOf(
                    Pair("group", group),
                    Pair("type", if (typeName == null) "MUSIC" else "SECTION"),
                    Pair("abbr", abbr),
                    Pair("name", typeName)
                )
            }
        }
    }

    private lateinit var title: TextView
    private lateinit var sectionList: RecyclerView
    private var albumAdapter: AlbumAdapter? = null
    private var name: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(context)
            .inflate(R.layout.fragment_media_section_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.back).setOnClickListener { pop() }

        sectionList = view.findViewById(R.id.section_list)
        title = view.findViewById(R.id.title)

        val type = arguments?.getString("type")
        val group = arguments?.getParcelable<Group>("group")
        val id = arguments?.getString("id")
        val abbr = arguments?.getString("abbr")
        name = arguments?.getString("name")
        if (TextUtils.equals(type, "MUSIC")) {
            title.text = abbr
            val items = findAnchorList(abbr, group?.items)
            setupRecyclerView(items)
        } else if (TextUtils.equals(type, "SECTION")) {
            if (group != null) {
                title.text = group.name
                setupRecyclerView(group.items)
            } else {
                id?.let { getMediaSections(it) }
            }
        }
    }

    private fun setupRecyclerView(items: List<GroupItem>?) {
        albumAdapter = AlbumAdapter()
        val horizontalSpacing = sectionList.context.resources.getDimensionPixelSize(R.dimen.dp_20)
        val itemDecoration = SpacesItemDecoration(horizontalSpacing)
        itemDecoration.left = 0
        itemDecoration.right = 0
        sectionList.addItemDecoration(itemDecoration)
        if (TextUtils.equals(name, "视频")) {
            albumAdapter?.itemViewType = AlbumAdapter.TYPE_RECTANGLE
        } else {
            albumAdapter?.itemViewType = AlbumAdapter.TYPE_SQUARE
        }
        albumAdapter?.onGroupItemClickListener = object : AlbumAdapter.OnGroupItemClickListener {
            override fun onGroupItemClick(itemView: View, groupItem: GroupItem) {
                start(SongListFragment.instance(groupItem.id, groupItem.name, name))
            }
        }
        albumAdapter?.setGroupList(items)
        sectionList.adapter = albumAdapter
    }

    private fun getMediaSections(id: String) {
        getMediaApi()?.getMediaSection(id)?.enqueue(object : Callback<Group> {
            override fun onFailure(call: Call<Group>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(call: Call<Group>, response: Response<Group>) {
                if (response.isSuccessful) {
                    val item = response.body()
                    title.text = item?.name
                    item?.let { setupRecyclerView(it.items) }
                }
            }
        })
    }

    private fun findAnchorList(title: String?, items: ArrayList<GroupItem>?): ArrayList<GroupItem> {
        val list = ArrayList<GroupItem>()
        items?.forEach {
            if (TextUtils.equals(it.categoryName, title)) {
                list.add(it)
            }
        }
        return list
    }

    private fun getMediaApi(): MediaApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(MediaApi::class.java)
        } else {
            null
        }
    }

    override fun scrollToNext(): Boolean {
        sectionList.let { recyclerView ->
            val lastItem =
                (recyclerView.layoutManager as? LinearLayoutManager)?.findLastCompletelyVisibleItemPosition()
            val itemCount = albumAdapter?.itemCount ?: 0
            if (lastItem == itemCount - 1 || itemCount == 0) {
                return false
            } else {
                recyclerView.smoothScrollBy(0, recyclerView.height)
            }
        }
        return true
    }

    override fun scrollToPrevious(): Boolean {
        sectionList.let { recyclerView ->
            val scrollY = recyclerView.computeVerticalScrollOffset()
            val itemCount = albumAdapter?.itemCount
            if (scrollY == 0 || itemCount == 0) {
                return false
            } else {
                recyclerView.smoothScrollBy(0, -recyclerView.height)
            }
        }
        return true
    }
}