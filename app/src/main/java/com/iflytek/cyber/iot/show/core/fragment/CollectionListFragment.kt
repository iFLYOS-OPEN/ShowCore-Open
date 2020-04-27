package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.airbnb.lottie.LottieAnimationView
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.model.CollectionV3
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CollectionListFragment : BaseFragment() {

    companion object {
        fun newInstance(tagId: Int): CollectionListFragment {
            return CollectionListFragment().apply {
                arguments = bundleOf(Pair("tagId", tagId))
            }
        }
    }

    private lateinit var singleTag: TextView
    private lateinit var albumTag: TextView
    private lateinit var retryFrame: LinearLayout
    private lateinit var loadingImage: LottieAnimationView

    private var collectionSingleFragment: CollectionSingleFragment? = null
    private var collectionAlbumFragment: CollectionAlbumFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_collection_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        singleTag = view.findViewById(R.id.single_tag)
        albumTag = view.findViewById(R.id.album_tag)

        singleTag.setOnClickListener {
            singleTag.isSelected = true
            albumTag.isSelected = false
            if (collectionSingleFragment != null) {
                showHideFragment(collectionSingleFragment!!)
            }
        }

        albumTag.setOnClickListener {
            albumTag.isSelected = true
            singleTag.isSelected = false
            if (collectionAlbumFragment != null) {
                showHideFragment(collectionAlbumFragment!!)
            }
        }

        retryFrame = view.findViewById(R.id.retry_frame)
        loadingImage = view.findViewById(R.id.loading_image)

        val tagId = arguments?.getInt("tagId")

        view.findViewById<View>(R.id.retry).setOnClickListener {
            retryFrame.isVisible = false
            loadingImage.isVisible = true
            if (tagId != null) {
                getCollection(tagId)
            }
        }

        singleTag.isSelected = true
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        val tagId = arguments?.getInt("tagId")
        if (tagId != null) {
            getCollection(tagId)
        }
    }

    private fun setupFragment(collectionV3: CollectionV3, tagId: Int) {
        collectionSingleFragment = CollectionSingleFragment()
        collectionSingleFragment?.setItems(collectionV3.single.collections, tagId)
        collectionAlbumFragment = CollectionAlbumFragment()
        collectionAlbumFragment?.setItems(collectionV3.album.collections, tagId)
        loadMultipleRootFragment(R.id.fragment, 0, collectionSingleFragment!!, collectionAlbumFragment!!)
    }

    private fun getCollection(tagId: Int) {
        loadingImage.isVisible = true
        loadingImage.playAnimation()
        getMediaApi()?.getCollection(tagId)?.enqueue(object : Callback<CollectionV3> {
            override fun onResponse(call: Call<CollectionV3>, response: Response<CollectionV3>) {
                if (!isAdded || context == null) {
                    return
                }
                loadingImage.isVisible = false
                loadingImage.pauseAnimation()
                if (response.isSuccessful) {
                    retryFrame.isVisible = false
                    val collectionV3 = response.body()
                    if (collectionV3 != null) {
                        setupFragment(collectionV3, tagId)
                    }
                } else {
                    retryFrame.isVisible = true
                }
            }

            override fun onFailure(call: Call<CollectionV3>, t: Throwable) {
                t.printStackTrace()
                retryFrame.isVisible = true
                loadingImage.isVisible = false
                loadingImage.pauseAnimation()
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