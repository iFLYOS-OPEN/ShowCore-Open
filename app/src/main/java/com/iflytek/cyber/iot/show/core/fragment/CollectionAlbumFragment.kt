package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.CollectionAlbumAdapter
import com.iflytek.cyber.iot.show.core.model.CollectionSong
import com.kk.taurus.playerbase.utils.NetworkUtils

class CollectionAlbumFragment : BaseFragment() {

    private lateinit var emptyContent: LinearLayout
    private lateinit var recyclerView: RecyclerView

    private var adapter: CollectionAlbumAdapter? = null

    private var items = ArrayList<CollectionSong>()
    private var tagId: Int = -1

    fun setItems(items: ArrayList<CollectionSong>, tagId: Int) {
        this.tagId = tagId
        this.items.clear()
        this.items.addAll(items)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragement_collection_media, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recycler_view)
        emptyContent = view.findViewById(R.id.empty_content)
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        adapter = CollectionAlbumAdapter {
            if (isAdded && context != null && !NetworkUtils.isNetConnected(context)) {
                Toast.makeText(context, "网络连接异常，请重新设置", Toast.LENGTH_SHORT).show()
                return@CollectionAlbumAdapter
            }
            (parentFragment?.parentFragment as? BaseFragment)?.start(SongListFragment.newInstance(it, 1))
        }
        adapter?.items = items
        recyclerView.adapter = adapter

        if (items.isEmpty()) {
            emptyContent.isVisible = true
            recyclerView.isVisible = false
        } else {
            emptyContent.isVisible = false
            recyclerView.isVisible = true
        }
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        adapter?.notifyDataSetChanged()
    }
}