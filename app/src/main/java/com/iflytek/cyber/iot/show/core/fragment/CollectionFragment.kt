package com.iflytek.cyber.iot.show.core.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.TagAdapter
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
    private lateinit var viewPager: ViewPager2
    private var tagAdapter: TagAdapter? = null

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

        viewPager = view.findViewById(R.id.view_pager)
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

        viewPager.isUserInputEnabled = false

        getTags()
    }

    private fun setupTag(tags: ArrayList<Tags>) {
        if (tagAdapter == null) {
            tagAdapter = TagAdapter(tags) { tag, position ->
                viewPager.setCurrentItem(position, false)
            }
        }
        tagList.adapter = tagAdapter

        val pagerAdapter = CategoryViewPager(tags, this)
        viewPager.adapter = pagerAdapter
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

    inner class CategoryViewPager(val items: ArrayList<Tags>, fragment: Fragment) :
        FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int {
            return items.size
        }

        override fun createFragment(position: Int): Fragment {
            val item = items[position]
            return CollectionListFragment.newInstance(item.id)
        }
    }

    private fun getMediaApi(): MediaApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(MediaApi::class.java)
        } else {
            null
        }
    }
}