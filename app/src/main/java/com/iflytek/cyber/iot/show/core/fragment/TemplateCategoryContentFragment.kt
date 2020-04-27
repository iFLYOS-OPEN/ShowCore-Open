package com.iflytek.cyber.iot.show.core.fragment

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.alibaba.fastjson.JSON
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.AlbumSectionContentAdapter
import com.iflytek.cyber.iot.show.core.adapter.TemplateAppAlbumAdapter
import com.iflytek.cyber.iot.show.core.adapter.TemplateAppMediaAdapter
import com.iflytek.cyber.iot.show.core.api.AppApi
import com.iflytek.cyber.iot.show.core.impl.prompt.PromptManager
import com.iflytek.cyber.iot.show.core.model.*
import com.iflytek.cyber.iot.show.core.utils.OnItemClickListener
import com.iflytek.cyber.iot.show.core.widget.StyledProgressDialog
import com.kk.taurus.playerbase.utils.NetworkUtils
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.UnknownHostException

class TemplateCategoryContentFragment : Fragment() {
    companion object {
        const val TAG = "TemplateCategoryContentFragment"

        private const val LIMIT = 20

        fun newInstance(
            templateApp: TemplateApp,
            category: String,
            initialMediaList: List<MediaItem>? = null,
            initialAlbumList: List<AlbumItem>? = null,
            justMedia: Boolean = false
        ): TemplateCategoryContentFragment {
            val fragment = TemplateCategoryContentFragment()
            fragment.templateApp = templateApp
            fragment.category = category
            initialMediaList?.let {
                fragment.mediaList.addAll(it)
            }
            initialAlbumList?.let {
                fragment.albumList.addAll(it)
            }
            fragment.justMedia = justMedia
            return fragment
        }

        fun newInstance(
            templateApp: TemplateApp,
            sourceItem: SourceItem?
        ): TemplateCategoryContentFragment {
            val fragment = TemplateCategoryContentFragment()
            fragment.templateApp = templateApp
            fragment.sourceItem = sourceItem
            return fragment
        }
    }

    private var templateApp: TemplateApp? = null
    private var category: String? = null
    private val mediaList = mutableListOf<MediaItem>()
    private val albumList = mutableListOf<AlbumItem>()
    private var sourceItem: SourceItem? = null

    private var justMedia = false

    private val albumSectionContentAdapter = AlbumSectionContentAdapter()

    private var page = 1
    private var totalListSize = 0

    private var recyclerView: RecyclerView? = null
    private var loading: LottieAnimationView? = null
    private var loadingBottom: LottieAnimationView? = null
    private var tvError: TextView? = null
    private var tvRetry: TextView? = null
    private var errorRetry: View? = null
    private var errorContainer: View? = null
    private var loadingCenter: LottieAnimationView? = null

    private var progressDialog: StyledProgressDialog? = null

    private var isLoading = false
    private var isLoadComplete = false

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (isLoadComplete || isLoading) return

            val itemCount = recyclerView.adapter?.itemCount
            (recyclerView.layoutManager as? GridLayoutManager)?.let {
                if (it.findLastCompletelyVisibleItemPosition() == it.findLastVisibleItemPosition() &&
                    itemCount == it.findLastVisibleItemPosition() + 1 &&
                    itemCount < totalListSize
                ) {
                    page++
                    //getCategoryContent()
                    getTemplateList()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_template_category_content, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.template_category_content)
        loading = view.findViewById(R.id.loading)
        loadingBottom = view.findViewById(R.id.loading_bottom)
        tvRetry = view.findViewById(R.id.tv_retry)
        tvError = view.findViewById(R.id.tv_error)
        errorContainer = view.findViewById(R.id.error_container)
        errorRetry = view.findViewById(R.id.error_retry)
        loadingCenter = view.findViewById(R.id.loading_center)

        errorRetry?.setOnClickListener {
            if (!isLoading) {
                page = 1
                getTemplateList()
            }
        }

        recyclerView?.adapter = albumSectionContentAdapter
        albumSectionContentAdapter.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(parent: ViewGroup, itemView: View, position: Int) {
                val albumItem = albumSectionContentAdapter.getAlbumItem(position)
                val templateApp = templateApp ?: return
                if (albumItem != null) {
                    if (albumItem.type == 1) {
                        (parentFragment as? BaseFragment)?.apply {
                            start(
                                TemplateApp1Fragment.newInstance(templateApp, sourceItem)
                            )
                        }
                    } else if (albumItem.type > 1) {
                        if (!albumItem.metadata?.resourceId.isNullOrEmpty()) {
                            postPlay(albumItem)
                        } else {
                            (parentFragment as? BaseFragment)?.apply {
                                start(
                                    SongListFragment.newInstance(albumItem, 2, 1002)
                                )
                            }
                        }
                    }
                }
            }
        }
        recyclerView?.addOnScrollListener(onScrollListener)
        setLayoutManager(1.0f)

        applyTheme(templateApp?.isDark != false)

        getTemplateList()
    }

    private fun setLayoutManager(ratio: Float) {
        val layoutManager = if (ratio <= 1) {
            GridLayoutManager(context, 5)
        } else {
            GridLayoutManager(context, 3)
        }
        recyclerView?.post {
            recyclerView?.layoutManager = layoutManager
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        recyclerView?.removeOnScrollListener(onScrollListener)
    }

    private fun showError(message: String?, showRetry: Boolean = true) {
        if (isRemoving || isDetached)
            return
        context ?: return
        recyclerView?.isVisible = false
        errorContainer?.isVisible = true
        errorRetry?.isVisible = showRetry
        tvError?.text = message
    }

    private fun hideError() {
        if (isRemoving || isDetached)
            return
        context ?: return
        recyclerView?.isVisible = true
        errorContainer?.isVisible = false
    }

    private fun showLoading() {
        if (isRemoving || isDetached)
            return
        context ?: return
        if (page == 1) {
            loading?.isVisible = true
            loading?.repeatMode = LottieDrawable.RESTART
            loading?.repeatCount = LottieDrawable.INFINITE
            loading?.playAnimation()
        } else {
            loadingBottom?.isVisible = true
            loadingBottom?.repeatMode = LottieDrawable.RESTART
            loadingBottom?.repeatCount = LottieDrawable.INFINITE
            loadingBottom?.playAnimation()
        }
    }

    private fun hideLoading() {
        if (isRemoving || isDetached)
            return
        context ?: return
        if (loading?.isVisible == true) {
            loading?.pauseAnimation()
            loading?.isVisible = false
        }
        if (loadingBottom?.isVisible == true) {
            loadingBottom?.pauseAnimation()
            loadingBottom?.isVisible = false
        }
    }

    private fun showProgress(title: String, message: String? = null) {
        if (isRemoving || isDetached)
            return
        context ?: return
        progressDialog = StyledProgressDialog.Builder()
            .setTitle(title)
            .setMessage(message)
            .show(fragmentManager)
    }

    private fun dismissProgress() {
        if (isRemoving || isDetached)
            return
        context ?: return
        progressDialog?.let {
            it.dismissAllowingStateLoss()
            progressDialog = null
        }
    }

    private fun getFirstItemCover(items: ArrayList<SourceItem>): String? {
        var firstCover: String? = null
        for (item in items) {
            if (firstCover.isNullOrEmpty()) {
                firstCover = item.cover
            }
        }
        return firstCover
    }

    private fun getTemplateList() {
        val templateApp = templateApp ?: return
        val metaData = sourceItem?.metadata ?: return
        isLoading = true
        showLoading()
        hideError()

        getAppApi()?.getTemplateList(
            templateApp.name,
            metaData.topLevel,
            metaData.secondLevel,
            metaData.thirdLevel,
            page
        )?.enqueue(object : Callback<TemplateMediaItem> {
            override fun onResponse(
                call: Call<TemplateMediaItem>,
                response: Response<TemplateMediaItem>
            ) {
                if (isRemoving || isDetached || context == null) {
                    return
                }

                isLoading = false

                if (response.isSuccessful) {
                    val templateMediaItem = response.body()
                    if (templateMediaItem != null) {
                        totalListSize = templateMediaItem.total
                        handleResult(templateMediaItem)
                    }
                } else {
                    hideLoading()
                    if (albumSectionContentAdapter.itemCount == 0) {
                        showError("请求出错，请稍后重试")
                    }
                }
            }

            override fun onFailure(call: Call<TemplateMediaItem>, t: Throwable) {
                if (isRemoving || isDetached)
                    return
                context ?: return

                isLoading = false
                hideLoading()

                if (t is UnknownHostException) {
                    showError("Ooooops，请检查网络后重试")
                } else {
                    showError("请求出错，请稍后重试")
                }
            }
        })
    }

    private fun setRecyclerViewPadding() {
        recyclerView?.let { recyclerView ->
            recyclerView.post {
                recyclerView.setPadding(
                    recyclerView.resources.getDimensionPixelSize(
                        R.dimen.dp_26
                    ),
                    recyclerView.paddingTop,
                    recyclerView.resources.getDimensionPixelSize(
                        R.dimen.dp_26
                    ),
                    recyclerView.paddingBottom
                )
            }
        }
    }

    private fun handleResult(templateMediaItem: TemplateMediaItem) {
        if (templateMediaItem.items == null) {
            return
        }
        val firstCover = getFirstItemCover(templateMediaItem.items)
        if (firstCover.isNullOrEmpty()) {
            hideLoading()
            setupAlbumList(1.0f, templateMediaItem.items)
            return
        }
        Glide.with(this)
            .asBitmap()
            .load(firstCover)
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {
                    hideLoading()
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    hideLoading()
                    showError("请求出错，请稍后重试")
                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    hideLoading()
                    val ratio = 1f * resource.width / resource.height
                    setupAlbumList(ratio, templateMediaItem.items)
                }
            })
    }

    private fun setupAlbumList(ratio: Float, items: ArrayList<SourceItem>) {
        if (page == 1) {
            setLayoutManager(ratio)
            albumSectionContentAdapter.ratio = ratio
            setRecyclerViewPadding()
            albumSectionContentAdapter.setAlbumList(items)
        } else {
            albumSectionContentAdapter.appendAlbumList(items)
        }
        albumSectionContentAdapter.notifyDataSetChanged()
    }

    private fun postPlay(sourceItem: SourceItem) {
        val audioId = sourceItem.metadata?.resourceId
        val sourceType = sourceItem.metadata?.source
        val business = sourceItem.metadata?.business

        loadingCenter?.isVisible = true
        loadingCenter?.playAnimation()

        val json = com.alibaba.fastjson.JSONObject()
        json["audio_id"] = audioId
        json["source_type"] = sourceType
        json["business"] = business
        val requestBody = RequestBody.create(
            MediaType.parse("application/json"),
            json.toString()
        )
        getAppApi()?.postPlayMedia(requestBody)?.enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                if (isRemoving || isDetached)
                    return
                context ?: return
                loadingCenter?.isVisible = false
                loadingCenter?.pauseAnimation()
                Toast.makeText(context, "请求出错", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (isRemoving || isDetached)
                    return
                context ?: return

                loadingCenter?.isVisible = false
                loadingCenter?.pauseAnimation()

                if (response.isSuccessful) {
                    Toast.makeText(context, "开始播放", Toast.LENGTH_SHORT).show()
                } else {
                    response.errorBody()?.let { errorBody ->
                        val errorString = errorBody.string()

                        val errorJson = JSONObject(errorString)

                        if (errorJson.has("message")) {
                            Toast.makeText(
                                context,
                                errorJson.optString("message"),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(context, "播放失败", Toast.LENGTH_SHORT).show()
                        }
                        errorBody.close()
                    } ?: run {
                        Toast.makeText(context, "播放失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    fun applyTheme(isDark: Boolean) {
        if (isDark) {
            loading?.setAnimation(R.raw.animation_loading_l_white)
            loadingBottom?.setAnimation(R.raw.animation_loading_l_white)
        } else {
            loading?.setAnimation(R.raw.animation_loading_l)
            loadingBottom?.setAnimation(R.raw.animation_loading_l)
        }

        albumSectionContentAdapter.isDark = isDark
        if (albumSectionContentAdapter.itemCount != 0) {
            albumSectionContentAdapter.notifyDataSetChanged()
        }

        tvError?.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        tvRetry?.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        errorRetry?.setBackgroundResource(
            if (isDark) R.drawable.bg_round_border_white_36dp
            else R.drawable.bg_round_border_black_36dp
        )
    }

    private fun getAppApi(): AppApi? {
        val context = context ?: return null
        return CoreApplication.from(context).createApi(AppApi::class.java)
    }

}