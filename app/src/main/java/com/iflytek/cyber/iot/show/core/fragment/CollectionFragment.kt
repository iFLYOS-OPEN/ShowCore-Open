package com.iflytek.cyber.iot.show.core.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.model.CollectionTag
import com.iflytek.cyber.iot.show.core.model.Tags
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CollectionFragment : BaseFragment() {

    private lateinit var loadingImageView: LottieAnimationView
    private lateinit var loadingFrame: LinearLayout
    private lateinit var retryFrame: LinearLayout
    private lateinit var retryTextView: TextView
    private lateinit var backContainer: FrameLayout

    private lateinit var tagList: RecyclerView
    private var tagAdapter: TagAdapter? = null

    private var fragmentList = ArrayList<CollectionListFragment>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_collection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.back).setOnClickListener {
            pop()
        }

        backContainer = view.findViewById(R.id.back_container)
        loadingImageView = view.findViewById(R.id.loading_image)
        loadingFrame = view.findViewById(R.id.loading_frame)
        retryFrame = view.findViewById(R.id.retry_frame)
        retryTextView = view.findViewById(R.id.retry)
        retryTextView.setOnClickListener {
            retryFrame.isVisible = false
            backContainer.setBackgroundColor(Color.parseColor("#E2E7EB"))
            getTags()
        }

        tagList = view.findViewById(R.id.tag_list)

        getTags()
    }

    private fun setupTag(tags: ArrayList<Tags>) {
        fragmentList.clear()
        for (tag in tags) {
            fragmentList.add(CollectionListFragment.newInstance(tag.id))
        }
        if (tagAdapter == null) {
            tagAdapter = TagAdapter(tags) { tag, position ->
                showHideFragment(fragmentList[position])
            }
        }
        tagList.adapter = tagAdapter

        loadMultipleRootFragment(R.id.fragment, 0, fragmentList[0], fragmentList[1], fragmentList[2])
    }

    private fun getTags() {
        loadingImageView.isVisible = true
        loadingImageView.playAnimation()
        getMediaApi()?.getTags()?.enqueue(object : Callback<CollectionTag> {
            override fun onFailure(call: Call<CollectionTag>, t: Throwable) {
                t.printStackTrace()
                loadingImageView.pauseAnimation()
                loadingImageView.isVisible = false
                retryFrame.isVisible = true
                backContainer.setBackgroundColor(Color.WHITE)
            }

            override fun onResponse(call: Call<CollectionTag>, response: Response<CollectionTag>) {
                loadingImageView.pauseAnimation()
                loadingImageView.isVisible = false
                if (response.isSuccessful) {
                    loadingFrame.isVisible = false
                    val collectionTag = response.body()
                    if (collectionTag != null) {
                        setupTag(collectionTag.tags)
                    }
                } else {
                    backContainer.setBackgroundColor(Color.WHITE)
                    retryFrame.isVisible = true
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

    inner class TagAdapter(val items: ArrayList<Tags>, val onItemClick: (Tags, Int) -> Unit) : RecyclerView.Adapter<TagAdapter.TagHolder>() {

        private var selectorColor = Color.parseColor("#262626")
        private var defaultColor = Color.parseColor("#9E9FA7")

        private var currentPosition = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tag, parent, false)
            return TagHolder(view)
        }

        override fun getItemCount(): Int {
            return items.size
        }

        private fun updateIndicator(position: Int) {
            currentPosition = position
            notifyDataSetChanged()
        }

        override fun onBindViewHolder(holder: TagHolder, position: Int) {
            val item = items[position]
            holder.tagTextView.text = item.name
            if (currentPosition == position) {
                holder.indicatorView.isInvisible = false
                holder.tagTextView.setTextColor(selectorColor)
            } else {
                holder.indicatorView.isInvisible = true
                holder.tagTextView.setTextColor(defaultColor)
            }
            holder.itemView.setOnClickListener {
                updateIndicator(position)
                onItemClick.invoke(item, position)
            }
        }

        inner class TagHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val indicatorView = itemView.findViewById<View>(R.id.indicator_view)
            val tagTextView = itemView.findViewById<TextView>(R.id.name_text)
        }
    }
}