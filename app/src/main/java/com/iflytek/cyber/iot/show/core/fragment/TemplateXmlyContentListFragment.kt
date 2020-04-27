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
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.bumptech.glide.request.transition.Transition
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.AppApi
import com.iflytek.cyber.iot.show.core.impl.prompt.PromptManager
import com.iflytek.cyber.iot.show.core.model.MediaItem
import com.iflytek.cyber.iot.show.core.model.TemplateApp
import com.iflytek.cyber.iot.show.core.model.XmlyAlbumItem
import com.iflytek.cyber.iot.show.core.utils.OnItemClickListener
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.widget.StyledProgressDialog
import com.kk.taurus.playerbase.utils.NetworkUtils
import retrofit2.Callback
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.net.UnknownHostException

class TemplateXmlyContentListFragment : BaseFragment() {
    companion object {
        private const val LIMIT = 20

        fun fromCategory(
            templateApp: TemplateApp?,
            section: String?,
            categoryId: String?,
            name: String?
        ): TemplateXmlyContentListFragment {
            val fragment = TemplateXmlyContentListFragment()
            fragment.isCategory = true
            fragment.templateApp = templateApp
            fragment.section = section
            fragment.categoryId = categoryId
            fragment.name = name
            return fragment
        }

        fun fromAlbum(
            templateApp: TemplateApp?,
            section: String?,
            albumId: String?,
            name: String?
        ): TemplateXmlyContentListFragment {
            val fragment = TemplateXmlyContentListFragment()
            fragment.isCategory = false
            fragment.templateApp = templateApp
            fragment.section = section
            fragment.albumId = albumId
            fragment.name = name
            return fragment
        }
    }

    private var isCategory = false

    private var templateApp: TemplateApp? = null
    private var section: String? = null
    private var categoryId: String? = null
    private var albumId: String? = null
    private var name: String? = null

    private var page = 1
    private var clickCount = 0

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

    private val adapter = TemplateXmlyContentAdapter()

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
                    getContentList()
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
            if (clickCount != 0)
                return@setOnClickListener
            clickCount++
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
                getContentList()
            }
        }

        recyclerView?.adapter = adapter
        recyclerView?.addOnScrollListener(onScrollListener)
        adapter.isCategory = isCategory
        adapter.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(parent: ViewGroup, itemView: View, position: Int) {
                if (isCategory) {
                    if (clickCount != 0)
                        return
                    clickCount++
                    adapter.getAlbumItem(position)?.let { albumItem ->
                        start(
                            TemplateXmlyAlbumFragment.newInstance(
                                templateApp,
                                albumItem.id,
                                albumItem.name
                            )
                        )
                    }
                } else {
                    adapter.getMediaItem(position)?.let { mediaItem ->
                        if (isAdded && context != null && !NetworkUtils.isNetConnected(context)) {
                            PromptManager.play(PromptManager.NETWORK_LOST)
                            return
                        }
                        postPlay(mediaItem)
                    }
                }
            }
        }

        tvTitle?.text = name

        loadBackground()

        loadAppIcon()

        getContentList()

        applyTheme(templateApp?.isDark != false)
    }

    override fun onSupportVisible() {
        super.onSupportVisible()

        clickCount = 0
    }

    override fun onDestroy() {
        super.onDestroy()

        recyclerView?.removeOnScrollListener(onScrollListener)
    }

    private fun applyTheme(isDark: Boolean) {
        ivBack?.imageTintList = ColorStateList.valueOf(if (isDark) Color.WHITE else Color.BLACK)

        adapter.isDark = isDark
        if (adapter.itemCount > 0) {
            adapter.notifyDataSetChanged()
        }

        tvTitle?.setTextColor(if (isDark) Color.WHITE else Color.BLACK)

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

    private fun getContentList() {
        val section = section ?: return
        val id = if (isCategory) {
            categoryId ?: return
        } else {
            albumId ?: return
        }

        showLoading()

        hideError()

        val json = JSONObject()

        json["section"] = section
        json["id"] = id
        json["page"] = page
        json["limit"] = LIMIT

        val body = RequestBody.create(
            MediaType.parse("application/json"),
            json.toString()
        )
        getAppApi()?.getXmlyShow(body)?.enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
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

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (isRemoving || isDetached)
                    return
                context ?: return

                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        val bodyString = body.string()
                        Thread {
                            val bodyJson = JSON.parseObject(bodyString)

                            val resultArray = bodyJson.getJSONArray("result")

                            if (isCategory) {
                                val albumList = mutableListOf<XmlyAlbumItem>()

                                var firstCover: String? = null
                                for (i in 0 until resultArray.size) {
                                    val jsonItem = resultArray.getJSONObject(i)
                                    val albumItem = JSON.parseObject(
                                        jsonItem.toString(),
                                        XmlyAlbumItem::class.java
                                    )
                                    if (firstCover.isNullOrEmpty()) {
                                        firstCover = albumItem.cover
                                    }
                                    albumList.add(albumItem)
                                }

                                if (page == 1) {
                                    when {
                                        albumList.isEmpty() -> {
                                            recyclerView?.post {
                                                hideLoading()

                                                showError("Ooooops，找不到结果", false)
                                            }
                                        }
                                        firstCover.isNullOrEmpty() -> recyclerView?.let { recyclerView ->
                                            adapter.setAlbumList(albumList)
                                            recyclerView.post {
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
                                                hideLoading()

                                                adapter.notifyDataSetChanged()
                                            }
                                        }
                                        else -> {
                                            Glide.with(this@TemplateXmlyContentListFragment)
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

                                                        adapter.ratio = ratio

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

                                                                adapter.setAlbumList(albumList)
                                                                adapter.notifyDataSetChanged()
                                                            }

                                                        }
                                                    }

                                                    override fun onLoadCleared(placeholder: Drawable?) {

                                                    }
                                                })
                                        }
                                    }
                                } else {
                                    adapter.appendAlbumList(albumList)
                                    recyclerView?.post {
                                        hideLoading()
                                        adapter.notifyDataSetChanged()
                                    }
                                }

                            } else {
                                val mediaList = mutableListOf<MediaItem>()

                                var firstCover: String? = null
                                for (i in 0 until resultArray.size) {
                                    val jsonItem = resultArray.getJSONObject(i)
                                    val mediaItem = JSON.parseObject(
                                        jsonItem.toString(),
                                        MediaItem::class.java
                                    )
                                    if (firstCover.isNullOrEmpty()) {
                                        firstCover = mediaItem.cover
                                    }
                                    mediaList.add(mediaItem)
                                }

                                if (page == 1) {
                                    when {
                                        mediaList.isEmpty() -> {
                                            hideLoading()

                                            showError("Ooooops，找不到结果", false)
                                        }
                                        firstCover.isNullOrEmpty() -> recyclerView?.let { recyclerView ->
                                            adapter.setMediaList(mediaList)
                                            recyclerView.post {
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
                                                hideLoading()

                                                adapter.notifyDataSetChanged()
                                            }
                                        }
                                        else -> {
                                            Glide.with(this@TemplateXmlyContentListFragment)
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

                                                        adapter.ratio = ratio

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

                                                                adapter.setMediaList(mediaList)
                                                                adapter.notifyDataSetChanged()
                                                            }

                                                        }
                                                    }

                                                    override fun onLoadCleared(placeholder: Drawable?) {

                                                    }
                                                })
                                        }
                                    }
                                } else {
                                    adapter.appendMediaList(mediaList)
                                    recyclerView?.post {
                                        hideLoading()
                                        adapter.notifyDataSetChanged()
                                    }
                                }
                            }
                        }.start()

                        body.close()
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

    class TemplateXmlyContentAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        companion object {
            const val TYPE_1P0 = 0
            const val TYPE_1P6 = 1
            const val TYPE_0P75 = 2
        }

        var isCategory = false

        private val mediaList = mutableListOf<MediaItem>()
        private val albumList = mutableListOf<XmlyAlbumItem>()

        var onItemClickListener: OnItemClickListener? = null

        var ratio = 1f

        var isDark = false

        fun setMediaList(mediaList: List<MediaItem>) {
            clear()
            this.mediaList.addAll(mediaList)
        }

        fun appendMediaList(mediaList: List<MediaItem>) {
            this.mediaList.addAll(mediaList)
        }

        fun setAlbumList(albumList: List<XmlyAlbumItem>) {
            clear()
            this.albumList.addAll(albumList)
        }

        fun appendAlbumList(albumList: List<XmlyAlbumItem>) {
            this.albumList.addAll(albumList)
        }

        fun getMediaItem(position: Int) = if (!isCategory) mediaList[position] else null

        fun getAlbumItem(position: Int) = if (isCategory) albumList[position] else null

        fun clear() {
            mediaList.clear()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val holder = when (viewType) {
                TYPE_1P6 -> MediaItemViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_template_app_media_1p6,
                        parent,
                        false
                    )
                )
                TYPE_1P0 -> MediaItemViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_template_app_media_1p0,
                        parent,
                        false
                    )
                )
                else -> MediaItemViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_template_app_media_0p75,
                        parent,
                        false
                    )
                )
            }
            holder.clickableView.setOnClickListener {
                onItemClickListener?.onItemClick(parent, it, holder.adapterPosition)
            }
            return holder
        }

        override fun getItemViewType(position: Int): Int {
            return when {
                ratio < 1 -> TYPE_0P75
                ratio == 1f -> TYPE_1P0
                else -> TYPE_1P6
            }
        }

        override fun getItemCount(): Int {
            return if (isCategory) albumList.size else mediaList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is MediaItemViewHolder) {
                holder.tvIndex.text = (position + 1).toString()

                if (isCategory) {
                    val albumItem = albumList[position]

                    if (!albumItem.cover.isNullOrEmpty()) {
                        try {
                            Uri.parse(albumItem.cover)?.let { uri ->
                                Glide.with(holder.ivImage)
                                    .load(uri)
                                    .placeholder(R.drawable.bg_default_template_app_2)
                                    .error(R.drawable.bg_default_template_app_2)
                                    .transition(
                                        DrawableTransitionOptions.with(
                                            DrawableCrossFadeFactory.Builder()
                                                .setCrossFadeEnabled(true).build()
                                        )
                                    )
                                    .transform(
                                        MultiTransformation(
                                            CenterCrop(), RoundedCornersTransformation(
                                                holder.itemView.resources.getDimensionPixelSize(
                                                    R.dimen.dp_6
                                                ), 0
                                            )
                                        )
                                    )
                                    .into(holder.ivImage)
                            } ?: run {
                                holder.ivImage.setImageResource(R.drawable.bg_default_template_app_2)
                            }
                        } catch (t: Throwable) {
                            t.printStackTrace()
                            holder.ivImage.setImageResource(R.drawable.bg_default_template_app_2)
                        }
                    } else {
                        holder.ivImage.setImageResource(R.drawable.bg_default_template_app_2)
                    }

                    holder.tvFirstLine.text = albumItem.name
                    holder.tvSecondLine.text = albumItem.subtitle
                    holder.tvSecondLine.isVisible = !albumItem.subtitle.isNullOrEmpty()
                } else {
                    val mediaItem = mediaList[position]

                    if (!mediaItem.cover.isNullOrEmpty()) {
                        try {
                            Uri.parse(mediaItem.cover)?.let { uri ->
                                Glide.with(holder.ivImage)
                                    .load(uri)
                                    .placeholder(R.drawable.bg_default_template_app_2)
                                    .error(R.drawable.bg_default_template_app_2)
                                    .transition(
                                        DrawableTransitionOptions.with(
                                            DrawableCrossFadeFactory.Builder()
                                                .setCrossFadeEnabled(true).build()
                                        )
                                    )
                                    .transform(
                                        MultiTransformation(
                                            CenterCrop(), RoundedCornersTransformation(
                                                holder.itemView.resources.getDimensionPixelSize(
                                                    R.dimen.dp_6
                                                ), 0
                                            )
                                        )
                                    )
                                    .into(holder.ivImage)
                            } ?: run {
                                holder.ivImage.setImageResource(R.drawable.bg_default_template_app_2)
                            }
                        } catch (t: Throwable) {
                            t.printStackTrace()
                            holder.ivImage.setImageResource(R.drawable.bg_default_template_app_2)
                        }
                    } else {
                        holder.ivImage.setImageResource(R.drawable.bg_default_template_app_2)
                    }
                    holder.tvFirstLine.text = mediaItem.title
                    holder.tvSecondLine.text = mediaItem.subtitle
                    holder.tvSecondLine.isVisible = !mediaItem.subtitle.isNullOrEmpty()
                }

                if (isDark) {
                    holder.tvFirstLine.setTextColor(Color.WHITE)
                    holder.tvSecondLine.setTextColor(Color.WHITE)
                } else {
                    holder.tvFirstLine.setTextColor(Color.BLACK)
                    holder.tvSecondLine.setTextColor(Color.BLACK)
                }
            }
        }


        private class MediaItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val clickableView: View = itemView.findViewById(R.id.clickable_view)
            val tvIndex: TextView = itemView.findViewById(R.id.tv_index)
            val ivImage: ImageView = itemView.findViewById(R.id.image)
            val tvFirstLine: TextView = itemView.findViewById(R.id.first_line)
            val tvSecondLine: TextView = itemView.findViewById(R.id.second_line)
        }

    }
}