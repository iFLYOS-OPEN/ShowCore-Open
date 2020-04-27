package com.iflytek.cyber.iot.show.core.fragment

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.alibaba.fastjson.JSONObject
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.bumptech.glide.request.transition.Transition
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.TemplateAppMediaAdapter
import com.iflytek.cyber.iot.show.core.api.AppApi
import com.iflytek.cyber.iot.show.core.impl.prompt.PromptManager
import com.iflytek.cyber.iot.show.core.model.MediaItem
import com.iflytek.cyber.iot.show.core.model.TemplateApp
import com.iflytek.cyber.iot.show.core.model.XmlyQueryResponse
import com.iflytek.cyber.iot.show.core.utils.OnItemClickListener
import com.iflytek.cyber.iot.show.core.widget.StyledProgressDialog
import com.kk.taurus.playerbase.utils.NetworkUtils
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.UnknownHostException

class TemplateXmlyAlbumFragment : BaseFragment() {
    companion object {
        private const val LIMIT = 20

        fun newInstance(
            templateApp: TemplateApp?,
            albumId: String?,
            name: String?
        ): TemplateXmlyAlbumFragment {
            val fragment = TemplateXmlyAlbumFragment()
            fragment.templateApp = templateApp
            fragment.albumId = albumId
            fragment.name = name
            return fragment
        }
    }

    private var templateApp: TemplateApp? = null
    private var albumId: String? = null
    private var name: String? = null

    private var page = 1
    private var backCount = 0

    private var ivBack: ImageView? = null
    private var ivIcon: ImageView? = null
    private var ivBackground: ImageView? = null
    private var recyclerView: RecyclerView? = null
    private var tvTitle: TextView? = null

    private var errorContainer: View? = null
    private var tvError: TextView? = null
    private var errorRetry: View? = null
    private var tvRetry: TextView? = null

    private var loading: LottieAnimationView? = null
    private var loadingBottom: LottieAnimationView? = null

    private var progressDialog: StyledProgressDialog? = null

    private val mediaAdapter = TemplateAppMediaAdapter()

    private var isLoading = false
    private var isLoadComplete = false

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if (isLoadComplete || isLoading) return

            (recyclerView.layoutManager as? LinearLayoutManager)?.let {
                if (it.findLastCompletelyVisibleItemPosition() == it.findLastVisibleItemPosition() &&
                    recyclerView.adapter?.itemCount == it.findLastVisibleItemPosition() + 1
                ) {
                    page++
                    getAlbum()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_template_app_media_list_fragment,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.back)?.setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            pop()
        }

        ivBack = view.findViewById(R.id.iv_back)
        ivIcon = view.findViewById(R.id.iv_icon)
        ivBackground = view.findViewById(R.id.background)
        recyclerView = view.findViewById(R.id.recycler_view)
        loading = view.findViewById(R.id.loading)
        loadingBottom = view.findViewById(R.id.loading_bottom)
        tvTitle = view.findViewById(R.id.title)
        tvRetry = view.findViewById(R.id.tv_retry)
        tvError = view.findViewById(R.id.tv_error)
        errorContainer = view.findViewById(R.id.error_container)
        errorRetry = view.findViewById(R.id.error_retry)

        errorRetry?.setOnClickListener {
            if (!isLoading) {
                page = 1
                getAlbum()
            }
        }

        recyclerView?.addOnScrollListener(onScrollListener)
        recyclerView?.adapter = mediaAdapter
        mediaAdapter.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(parent: ViewGroup, itemView: View, position: Int) {
                if (isAdded && context != null && !NetworkUtils.isNetConnected(context)) {
                    PromptManager.play(PromptManager.NETWORK_LOST)
                    return
                }
                mediaAdapter.getMediaItem(position)?.let { mediaItem ->
                    postPlay(mediaItem)
                }
            }
        }

        tvTitle?.text = name

        applyTheme(templateApp?.isDark != false)

        loadAppIcon()

        loadBackground()

        getAlbum()
    }

    override fun onSupportVisible() {
        super.onSupportVisible()

        backCount = 0
    }

    override fun onDestroy() {
        super.onDestroy()

        recyclerView?.removeOnScrollListener(onScrollListener)
    }

    private fun applyTheme(isDark: Boolean) {
        ivBack?.imageTintList = ColorStateList.valueOf(if (isDark) Color.WHITE else Color.BLACK)

        tvTitle?.setTextColor(if (isDark) Color.WHITE else Color.BLACK)

        mediaAdapter.isDark = isDark
        if (mediaAdapter.itemCount != 0) {
            mediaAdapter.notifyDataSetChanged()
        }

        tvError?.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        tvRetry?.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        errorRetry?.setBackgroundResource(
            if (isDark) R.drawable.bg_round_border_white_36dp
            else R.drawable.bg_round_border_black_36dp
        )
    }

    private fun loadBackground() {
        val templateApp = templateApp ?: return
        ivBackground?.let { imageView ->
            val url = templateApp.img
            if (url.isNullOrEmpty())
                return
            try {
                Uri.parse(url)?.let { uri ->
                    Glide.with(imageView)
                        .load(uri)
                        .transition(
                            DrawableTransitionOptions.with(
                                DrawableCrossFadeFactory.Builder()
                                    .setCrossFadeEnabled(true).build()
                            )
                        )
                        .into(imageView)
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    private fun loadAppIcon() {
        val templateApp = templateApp ?: return

        if (!templateApp.icon.isNullOrEmpty()) {
            try {
                Uri.parse(templateApp.icon)?.let { uri ->
                    ivIcon?.let { imageView ->
                        Glide.with(imageView)
                            .load(uri)
                            .placeholder(R.drawable.bg_default_template_app_2)
                            .error(R.drawable.bg_default_template_app_2)
                            .transition(
                                DrawableTransitionOptions.with(
                                    DrawableCrossFadeFactory.Builder()
                                        .setCrossFadeEnabled(true).build()
                                )
                            )
                            .into(imageView)
                    }
                } ?: run {
                    ivIcon?.setImageResource(R.drawable.bg_default_template_app_2)
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                ivIcon?.setImageResource(R.drawable.bg_default_template_app_2)
            }
        } else {
            ivIcon?.setImageResource(R.drawable.bg_default_template_app_2)
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

    private fun showLoading() {
        if (isRemoving || isDetached)
            return
        context ?: return
        if (page == 1) {
            loading?.isVisible = true
            loading?.repeatCount = LottieDrawable.INFINITE
            loading?.repeatMode = LottieDrawable.RESTART
            loading?.playAnimation()
        } else {
            loadingBottom?.isVisible = true
            loadingBottom?.repeatCount = LottieDrawable.INFINITE
            loadingBottom?.repeatMode = LottieDrawable.RESTART
            loadingBottom?.playAnimation()
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
            it.dismiss()
            progressDialog = null
        }
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

    private fun getAlbum() {
        val albumId = albumId ?: return

        isLoading = true
        showLoading()
        hideError()

        val json = JSONObject()
        json["id"] = albumId
        json["page"] = page
        json["limit"] = LIMIT
        val body = RequestBody.create(
            MediaType.parse("application/json"),
            json.toString()
        )
        getAppApi()?.getXmlyQuery(body)?.enqueue(object : Callback<XmlyQueryResponse> {
            override fun onFailure(call: Call<XmlyQueryResponse>, t: Throwable) {
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

            override fun onResponse(
                call: Call<XmlyQueryResponse>,
                response: Response<XmlyQueryResponse>
            ) {
                if (isRemoving || isDetached)
                    return
                context ?: return
                if (response.isSuccessful) {
                    hideLoading()
                    response.body()?.let { queryResponse ->
                        this@TemplateXmlyAlbumFragment.albumId = queryResponse.albumId

                        val mediaList = mutableListOf<MediaItem>()
                        var firstCover: String? = null
                        for (index in 0 until (queryResponse.result?.size ?: 0)) {
                            queryResponse.result?.get(index)?.let { mediaItem ->
                                if (firstCover.isNullOrEmpty()) {
                                    firstCover = mediaItem.cover
                                }
                                mediaList.add(mediaItem)
                            }
                        }
                        if (mediaList.size < LIMIT) {
                            isLoadComplete = true
                        }

                        if (page == 1) {
                            when {
                                mediaList.isEmpty() -> {
                                    hideLoading()

                                    showError("Ooooops，找不到结果", false)
                                }
                                firstCover.isNullOrEmpty() -> {
                                    mediaAdapter.setMediaList(mediaList)
                                    mediaAdapter.notifyDataSetChanged()

                                    hideLoading()
                                }
                                else -> Glide.with(this@TemplateXmlyAlbumFragment)
                                    .asBitmap()
                                    .load(firstCover)
                                    .into(object : CustomTarget<Bitmap>() {
                                        override fun onLoadFailed(errorDrawable: Drawable?) {
                                            super.onLoadFailed(errorDrawable)

                                            loading?.pauseAnimation()
                                            loading?.isVisible = false
                                        }

                                        override fun onResourceReady(
                                            resource: Bitmap,
                                            transition: Transition<in Bitmap>?
                                        ) {
                                            val ratio =
                                                1f * resource.width / resource.height

                                            mediaAdapter.ratio = ratio

                                            recyclerView?.let { recyclerView ->
                                                recyclerView.post {
                                                    when {
                                                        ratio <= 1f -> {
                                                            (recyclerView.layoutManager as? GridLayoutManager)?.apply {
                                                                spanCount = 5
                                                            }
                                                                ?: run {
                                                                    recyclerView.layoutManager =
                                                                        GridLayoutManager(
                                                                            context,
                                                                            5
                                                                        )
                                                                }
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
                                                        ratio > 1f -> {
                                                            (recyclerView.layoutManager as? GridLayoutManager)?.apply {
                                                                spanCount = 3
                                                            }
                                                                ?: run {
                                                                    recyclerView.layoutManager =
                                                                        GridLayoutManager(
                                                                            context,
                                                                            3
                                                                        )
                                                                }
                                                            recyclerView.setPadding(
                                                                recyclerView.resources.getDimensionPixelSize(
                                                                    R.dimen.dp_24
                                                                ),
                                                                recyclerView.paddingTop,
                                                                recyclerView.resources.getDimensionPixelSize(
                                                                    R.dimen.dp_24
                                                                ),
                                                                recyclerView.paddingBottom
                                                            )
                                                        }
                                                    }

                                                    hideLoading()

                                                    mediaAdapter.setMediaList(mediaList)
                                                    mediaAdapter.notifyDataSetChanged()
                                                }

                                            }
                                        }

                                        override fun onLoadCleared(placeholder: Drawable?) {

                                        }
                                    })
                            }
                        } else {
                            mediaAdapter.appendMediaList(mediaList)
                            mediaAdapter.notifyDataSetChanged()

                            hideLoading()
                        }
                    } ?: run {
                        hideLoading()
                    }
                } else {
                    hideLoading()
                }
            }
        })
    }

    private fun postPlay(mediaItem: MediaItem) {
        val audioId = mediaItem.id ?: return
        val sourceType = mediaItem.source ?: templateApp?.source ?: return
        val albumId = mediaItem.albumId ?: albumId ?: return

        showProgress(getString(R.string.loading), getString(R.string.please_wait))

        val json = JSONObject()
        json["audio_id"] = audioId
        json["source_type"] = sourceType
        json["album_id"] = albumId
        val requestBody = RequestBody.create(
            MediaType.parse("application/json"),
            json.toString()
        )
        getAppApi()?.postPlayMedia(requestBody)?.enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                if (isRemoving || isDetached)
                    return
                context ?: return
                dismissProgress()
                Toast.makeText(context, "请求出错", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (isRemoving || isDetached)
                    return
                context ?: return

                dismissProgress()
                if (response.isSuccessful) {
                    Toast.makeText(context, "开始播放", Toast.LENGTH_SHORT).show()
                } else {
                    response.errorBody()?.let { errorBody ->
                        val errorString = errorBody.string()

                        val errorJson = org.json.JSONObject(errorString)

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

    private fun getAppApi(): AppApi? {
        val context = context ?: return null
        return CoreApplication.from(context).createApi(AppApi::class.java)
    }
}