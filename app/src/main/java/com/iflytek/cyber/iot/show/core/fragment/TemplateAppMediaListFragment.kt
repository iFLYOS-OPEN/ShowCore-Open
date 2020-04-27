package com.iflytek.cyber.iot.show.core.fragment

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import com.iflytek.cyber.iot.show.core.model.AppShowResult
import com.iflytek.cyber.iot.show.core.model.MediaItem
import com.iflytek.cyber.iot.show.core.model.TemplateApp
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

class TemplateAppMediaListFragment : BaseFragment() {
    companion object {
        private const val LIMIT = 20
        fun newInstance(
            templateApp: TemplateApp?,
            category: String?,
            album: String?
        ): TemplateAppMediaListFragment {
            val fragment = TemplateAppMediaListFragment()
            fragment.templateApp = templateApp
            fragment.category = category
            fragment.album = album
            return fragment
        }
    }

    private var templateApp: TemplateApp? = null
    private var category: String? = null
    private var album: String? = null

    private var page = 1
    private var clickCount = 0

    private var ivBack: ImageView? = null
    private var ivIcon: ImageView? = null
    private var ivBackground: ImageView? = null
    private var recyclerView: RecyclerView? = null
    private var tvTitle: TextView? = null

    private var loading: LottieAnimationView? = null
    private var loadingBottom: LottieAnimationView? = null
    private var tvError: TextView? = null
    private var tvRetry: TextView? = null
    private var errorRetry: View? = null
    private var errorContainer: View? = null

    private val mediaAdapter = TemplateAppMediaAdapter()

    private var progressDialog: StyledProgressDialog? = null

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
                    getMediaList()
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
                getMediaList()
            }
        }

        tvTitle?.text = album

        recyclerView?.adapter = mediaAdapter
        recyclerView?.addOnScrollListener(onScrollListener)

        mediaAdapter.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(parent: ViewGroup, itemView: View, position: Int) {
                mediaAdapter.getMediaItem(position)?.let { mediaItem ->
                    if (isAdded && context != null && !NetworkUtils.isNetConnected(context)) {
                        clickCount = 0
                        PromptManager.play(PromptManager.NETWORK_LOST)
                        return
                    }
                    postPlay(mediaItem)
                }
            }
        }

        applyTheme(templateApp?.isDark != false)

        loadBackground()

        loadAppIcon()

        getMediaList()
    }

    private fun applyTheme(isDark: Boolean) {
        ivBack?.imageTintList = ColorStateList.valueOf(if (isDark) Color.WHITE else Color.BLACK)

        mediaAdapter.isDark = isDark
        if (mediaAdapter.itemCount != 0) {
            mediaAdapter.notifyDataSetChanged()
        }

        tvTitle?.setTextColor(if (isDark) Color.WHITE else Color.BLACK)

        tvError?.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        tvRetry?.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        errorRetry?.setBackgroundResource(
            if (isDark) R.drawable.bg_round_border_white_36dp
            else R.drawable.bg_round_border_black_36dp
        )
    }


    override fun onSupportInvisible() {
        super.onSupportInvisible()

        clickCount = 0
    }

    override fun onDestroy() {
        super.onDestroy()

        recyclerView?.removeOnScrollListener(onScrollListener)
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
            it.dismiss()
            progressDialog = null
        }
    }

    private fun getMediaList() {
        val templateApp = templateApp ?: return
        val source = templateApp.source ?: return
        val category = category
        val business = templateApp.business ?: return
        val album = album ?: return

        isLoading = true

        showLoading()

        hideError()

        val json = JSONObject()
        json.put("source", source)
        category?.let {
            json.put("category", category)
        }
        json.put("business", business)
        json.put("album", album)
        json.put("page", page)
        json.put("limit", LIMIT)

        val requestBody = RequestBody.create(
            MediaType.parse("application/json"),
            json.toString()
        )
        getAppApi()?.getAlbumShow(requestBody)?.enqueue(object : Callback<AppShowResult> {
            override fun onFailure(call: Call<AppShowResult>, t: Throwable) {
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

            override fun onResponse(call: Call<AppShowResult>, response: Response<AppShowResult>) {
                if (isRemoving || isDetached)
                    return
                context ?: return

                isLoading = false

                if (response.isSuccessful) {
                    val body = response.body()

                    val mediaList = mutableListOf<MediaItem>()
                    var firstCover: String? = null
                    for (index in 0 until (body?.result?.size ?: 0)) {
                        body?.result?.get(index)?.let { mediaItem ->
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
                            else -> Glide.with(this@TemplateAppMediaListFragment)
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
                        hideLoading()

                        mediaAdapter.appendMediaList(mediaList)
                        mediaAdapter.notifyDataSetChanged()
                    }

                } else {
                    hideLoading()

                    showError("请求出错，请稍后再试")
                }
            }

        })
    }

    private fun postPlay(mediaItem: MediaItem) {
        val audioId = mediaItem.id ?: return
        val sourceType = mediaItem.source ?: templateApp?.source ?: return
        val business = templateApp?.business ?: return

        //showProgress(getString(R.string.loading), getString(R.string.please_wait))
        loading?.isVisible = true
        loading?.repeatCount = LottieDrawable.INFINITE
        loading?.repeatMode = LottieDrawable.RESTART
        loading?.playAnimation()

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
                //dismissProgress()
                loading?.isVisible = false
                loading?.pauseAnimation()
                Toast.makeText(context, "请求出错", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (isRemoving || isDetached)
                    return
                context ?: return

                //dismissProgress()
                loading?.isVisible = false
                loading?.pauseAnimation()
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

    private fun getAppApi(): AppApi? {
        val context = context ?: return null
        return CoreApplication.from(context).createApi(AppApi::class.java)
    }
}