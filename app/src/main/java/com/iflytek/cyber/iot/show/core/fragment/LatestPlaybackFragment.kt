package com.iflytek.cyber.iot.show.core.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
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

class LatestPlaybackFragment : BaseFragment() {

    private var tagList: RecyclerView? = null
    private var viewPager: ViewPager2? = null
    private var retryView: View? = null
    private var loadingImageView: LottieAnimationView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_latest_playback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        retryView = view.findViewById(R.id.retry_view)
        loadingImageView = view.findViewById(R.id.loading_image)
        tagList = view.findViewById(R.id.tag_list)
        viewPager = view.findViewById(R.id.view_pager)
        viewPager?.isUserInputEnabled = false

        view.findViewById<View>(R.id.back).setOnClickListener {
            pop()
        }

        view.findViewById<View>(R.id.retry).setOnClickListener {
            showLoading()
            loadTags()
        }

        showLoading()
        tagList?.postDelayed(300) {
            loadTags()
        }
    }

    private fun setupTag(tags: ArrayList<Tags>) {
        val tagAdapter = TagAdapter(tags) { tag, position ->
            viewPager?.setCurrentItem(position, false)
        }
        tagAdapter.setItemColor(
            Color.WHITE, Color.parseColor("#CCFFFFFF"),
            Color.WHITE
        )
        tagList?.adapter = tagAdapter

        val pagerAdapter = CategoryViewPager(tags, this)
        viewPager?.adapter = pagerAdapter
    }

    private fun showLoading() {
        retryView?.isVisible = false
        loadingImageView?.isVisible = true
        loadingImageView?.playAnimation()
    }

    private fun dismissLoading() {
        loadingImageView?.isVisible = false
        loadingImageView?.pauseAnimation()
    }

    private fun loadTags() {
        getMediaApi()?.getLatestTags()?.enqueue(object : Callback<CollectionTag> {
            override fun onFailure(call: Call<CollectionTag>, t: Throwable) {
                t.printStackTrace()
                dismissLoading()
                retryView?.isVisible = true
            }

            override fun onResponse(call: Call<CollectionTag>, response: Response<CollectionTag>) {
                if (context == null || !isAdded) {
                    return
                }
                dismissLoading()
                if (response.isSuccessful) {
                    retryView?.isVisible = false
                    val tag = response.body()
                    tag?.let { setupTag(it.tags) }
                } else {
                    retryView?.isVisible = true
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
            return LatestMediaFragment.newInstance(item)
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