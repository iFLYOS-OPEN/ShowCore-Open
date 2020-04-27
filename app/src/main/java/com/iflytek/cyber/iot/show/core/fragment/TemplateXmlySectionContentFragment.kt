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
import androidx.fragment.app.Fragment
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
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.AppApi
import com.iflytek.cyber.iot.show.core.model.*
import com.iflytek.cyber.iot.show.core.utils.OnItemClickListener
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.widget.StyledProgressDialog
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.UnknownHostException
import kotlin.math.min

class TemplateXmlySectionContentFragment : Fragment() {
    companion object {
        const val TAG = "TemplateCategoryContentFragment"

        private const val LIMIT = 10

        fun fromCategoryList(
            templateApp: TemplateApp?,
            section: String,
            categoryList: List<XmlyCategoryItem>
        ): TemplateXmlySectionContentFragment {
            val fragment = TemplateXmlySectionContentFragment()
            fragment.templateApp = templateApp
            fragment.isCategoryList = true
            fragment.section = section
            fragment.categoryList.addAll(categoryList)
            //fragment.sectionContentAdapter.isCategoryList = true
            return fragment
        }

        fun fromAlbumList(
            templateApp: TemplateApp?,
            section: String,
            albumList: List<XmlyAlbumItem>
        ): TemplateXmlySectionContentFragment {
            val fragment = TemplateXmlySectionContentFragment()
            fragment.templateApp = templateApp
            fragment.isCategoryList = false
            fragment.section = section
            fragment.albumList.addAll(albumList)
            //fragment.sectionContentAdapter.isCategoryList = false
            return fragment
        }

        fun newInstance(
            templateApp: TemplateApp?,
            section: String
        ): TemplateXmlySectionContentFragment {
            val fragment = TemplateXmlySectionContentFragment()
            fragment.templateApp = templateApp
            fragment.section = section
            return fragment
        }
    }

    private var isCategoryList = false

    private var section: String? = null

    private var templateApp: TemplateApp? = null
    private val categoryList = mutableListOf<XmlyCategoryItem>()
    private val albumList = mutableListOf<XmlyAlbumItem>()

    //private val sectionContentAdapter = XmlySectionContentAdapter()
    private val sectionContentAdapter = SectionContentAdapter()

    private var page = 1

    private var isLoading = false
    private var isLoadComplete = false

    private var recyclerView: RecyclerView? = null
    private var loading: LottieAnimationView? = null
    private var loadingBottom: LottieAnimationView? = null

    private var errorContainer: View? = null
    private var tvError: TextView? = null
    private var errorRetry: View? = null
    private var tvRetry: TextView? = null

    private var progressDialog: StyledProgressDialog? = null

    private var onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if (isLoading || isLoadComplete)
                return

            (recyclerView.layoutManager as? LinearLayoutManager)?.let {
                if (it.findLastCompletelyVisibleItemPosition() == it.findLastVisibleItemPosition() &&
                    recyclerView.adapter?.itemCount == it.findLastVisibleItemPosition() + 1
                ) {
                    page++
                    getSectionContent()
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
        tvError = view.findViewById(R.id.tv_error)
        errorContainer = view.findViewById(R.id.error_container)
        errorRetry = view.findViewById(R.id.error_retry)
        tvRetry = view.findViewById(R.id.tv_retry)


        recyclerView?.adapter = sectionContentAdapter
        recyclerView?.addOnScrollListener(onScrollListener)
        //sectionContentAdapter.isCategoryList = isCategoryList

        errorRetry?.setOnClickListener {
            getSectionContent()
        }

        applyTheme(templateApp?.isDark != false)

        /*sectionContentAdapter.onSubItemClickListener = object : OnSubItemClickListener {
            override fun onSubItemClick(
                parent: ViewGroup,
                view: View,
                position: Int,
                subPosition: Int
            ) {
                if (sectionContentAdapter.isCategoryList) {
                    sectionContentAdapter.getCategoryItem(position)?.let { categoryItem ->
                        categoryItem.result?.get(subPosition)?.let { album ->
                            (parentFragment as? BaseFragment)?.start(
                                TemplateXmlyAlbumFragment.newInstance(
                                    templateApp,
                                    album.id,
                                    album.name
                                )
                            )
                        }
                    }
                } else {
                    sectionContentAdapter.getAlbumItem(position)?.let { albumItem ->
                        if (isAdded && context != null && !NetworkUtils.isNetConnected(context)) {
                            PromptManager.play(PromptManager.NETWORK_LOST)
                            return
                        }
                        albumItem.result?.get(subPosition)?.let { mediaItem ->
                            postPlay(mediaItem, albumItem.albumId)
                        }
                    }
                }
            }
        }
        sectionContentAdapter.onMoreClickListener = object : OnMoreClickListener {
            override fun onMoreClick(parent: ViewGroup, view: View, position: Int) {
                if (sectionContentAdapter.isCategoryList) {
                    sectionContentAdapter.getCategoryItem(position)?.let { categoryItem ->
                        (parentFragment as? BaseFragment)?.start(
                            TemplateXmlyContentListFragment.fromCategory(
                                templateApp,
                                section,
                                categoryItem.categoryId,
                                categoryItem.categoryName
                            )
                        )
                    }
                } else {
                    sectionContentAdapter.getAlbumItem(position)?.let { albumItem ->
                        (parentFragment as? BaseFragment)?.start(
                            TemplateXmlyContentListFragment.fromAlbum(
                                templateApp,
                                section,
                                albumItem.albumId,
                                albumItem.name
                            )
                        )
                    }
                }
            }
        }*/

        sectionContentAdapter.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(parent: ViewGroup, itemView: View, position: Int) {
                val albumItem = sectionContentAdapter.getAlbumItem(position)
                if (albumItem != null) {
                    val newItem = AlbumItem(
                        album = null,
                        business = templateApp?.business,
                        source = templateApp?.source,
                        type = null,
                        albumId = albumItem.albumId,
                        cover = albumItem.cover,
                        title = albumItem.name,
                        from = templateApp?.name,
                        result = null)
                    (parentFragment as? BaseFragment)?.apply {
                        start(
                            SongListFragment.newInstance(newItem, 2, 1001)
                        )
                    }
                }
            }
        }

        if (isCategoryList) {
            if (categoryList.isNotEmpty()) {
                val categoryItem = categoryList[0]
                page = 1
                var firstCover: String? = null
                categoryItem.result?.map { albumItem ->
                    if (!albumItem.cover.isNullOrEmpty() && firstCover.isNullOrEmpty()) {
                        try {
                            Uri.parse(albumItem.cover)?.let {
                                firstCover = albumItem.cover
                            }
                        } catch (t: Throwable) {

                        }
                    }
                }
                if (firstCover.isNullOrEmpty()) {
                    recyclerView?.let { recyclerView ->
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
                    //sectionContentAdapter.setCategoryList(categoryList)
                }
            } else {
                getSectionContent()
            }
        } else {
            if (albumList.isNotEmpty()) {
                val albumItem = albumList[0]
                page = 1
                var firstCover: String? = albumItem.cover
                /*albumItem.result?.map { mediaItem ->
                    if (!mediaItem.cover.isNullOrEmpty() && firstCover.isNullOrEmpty()) {
                        try {
                            Uri.parse(mediaItem.cover)?.let {
                                firstCover = mediaItem.cover
                            }
                        } catch (t: Throwable) {

                        }
                    }
                }*/
                if (firstCover.isNullOrEmpty()) {
                    recyclerView?.let { recyclerView ->
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
                    sectionContentAdapter.setAlbumList(albumList)
                } else {
                    loading?.isVisible = true
                    loading?.repeatMode = LottieDrawable.RESTART
                    loading?.repeatCount = LottieDrawable.INFINITE
                    loading?.playAnimation()

                    Glide.with(this)
                        .asBitmap()
                        .load(firstCover)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                super.onLoadFailed(errorDrawable)

                                hideLoading()

                                recyclerView?.let { recyclerView ->
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
                                sectionContentAdapter.setAlbumList(albumList)
                                sectionContentAdapter.notifyDataSetChanged()
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {

                            }

                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                val ratio =
                                    1f * resource.width / resource.height

                                sectionContentAdapter.ratio = ratio

                                setLayoutManager(ratio)

                                recyclerView?.let { recyclerView ->
                                    when {
                                        ratio <= 1f -> {
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

                                    sectionContentAdapter.setAlbumList(albumList)
                                    sectionContentAdapter.notifyDataSetChanged()

                                }
                            }
                        })
                }
            } else {
                getSectionContent()
            }
        }
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

    private fun showLoading() {
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

    private fun showError(message: String?, showRetry: Boolean? = true) {
        if (isRemoving || isDetached)
            return
        context ?: return
        recyclerView?.isVisible = false
        errorContainer?.isVisible = true
        tvError?.text = message
        errorRetry?.isVisible = showRetry == true
    }

    private fun hideError() {
        if (isRemoving || isDetached)
            return
        context ?: return
        recyclerView?.isVisible = true
        errorContainer?.isVisible = false
    }

    private fun getSectionContent() {
        val section = section ?: return

        isLoading = true

        showLoading()
        hideError()

        getAppApi()?.getXmlyIndex(section, page)?.enqueue(object :
            Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                if (isRemoving || isDetached)
                    return
                context ?: return

                isLoading = false

                hideLoading()

                if (t is UnknownHostException) {
                    showError("Ooooops，请检查网络后重试")
                } else {
                    showError("请求出错")
                }
            }

            override fun onResponse(
                call: Call<ResponseBody>, response: Response<ResponseBody>
            ) {
                if (isRemoving || isDetached)
                    return
                context ?: return

                isLoading = false

                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        val bodyString = body.string()

                        Thread {
                            try {
                                val json = JSON.parseObject(bodyString)

                                val resultArray = json.getJSONArray("result")

                                var isCategoryList = false
                                if (page == 1 && resultArray.size > 0) {
                                    val sample = resultArray.getJSONObject(0)
                                    if (sample.containsKey("category_id")) {
                                        isCategoryList = true
                                    }
                                } else {
                                    isCategoryList =
                                        this@TemplateXmlySectionContentFragment.isCategoryList
                                }

                                //sectionContentAdapter.isCategoryList = isCategoryList

                                if (isCategoryList) {
                                    val categoryList = mutableListOf<XmlyCategoryItem>()
                                    var firstCover: String? = null
                                    for (index in 0 until resultArray.size) {
                                        val jsonItem = resultArray.getJSONObject(index)
                                        val categoryItem = JSON.parseObject(
                                            jsonItem.toString(),
                                            XmlyCategoryItem::class.java
                                        )
                                        categoryItem.result?.map { albumItem ->
                                            if (firstCover.isNullOrEmpty()) {
                                                firstCover = albumItem.cover
                                            }
                                        }
                                        categoryList.add(categoryItem)
                                    }

                                    if (categoryList.size < LIMIT)
                                        isLoadComplete = true

                                    if (page == 1) {
                                        if (firstCover.isNullOrEmpty()) {
                                            /*sectionContentAdapter.setCategoryList(
                                                categoryList
                                            )*/
                                            sectionContentAdapter.notifyDataSetChanged()
                                        } else
                                            Glide.with(this@TemplateXmlySectionContentFragment)
                                                .asBitmap()
                                                .load(firstCover)
                                                .into(object : CustomTarget<Bitmap>() {
                                                    override fun onResourceReady(
                                                        resource: Bitmap,
                                                        transition: Transition<in Bitmap>?
                                                    ) {
                                                        val ratio =
                                                            1f * resource.width / resource.height

                                                        sectionContentAdapter.ratio = ratio

                                                        setLayoutManager(ratio)

                                                        recyclerView?.let { recyclerView ->
                                                            recyclerView.post {
                                                                when {
                                                                    ratio <= 1f -> {
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

                                                                /* sectionContentAdapter.setCategoryList(
                                                                     categoryList
                                                                 )*/
                                                                sectionContentAdapter.notifyDataSetChanged()
                                                            }

                                                        }
                                                    }

                                                    override fun onLoadCleared(placeholder: Drawable?) {

                                                    }
                                                })
                                    } else {
                                        //sectionContentAdapter.appendCategoryList(categoryList)
                                        recyclerView?.post {
                                            hideLoading()

                                            sectionContentAdapter.notifyDataSetChanged()
                                        }
                                    }
                                } else {
                                    val albumList = mutableListOf<XmlyAlbumItem>()

                                    var firstCover: String? = null
                                    for (index in 0 until resultArray.size) {
                                        val jsonItem = resultArray.getJSONObject(index)
                                        val albumItem = JSON.parseObject(
                                            jsonItem.toString(),
                                            XmlyAlbumItem::class.java
                                        )

                                        /*albumItem.result?.map { mediaItem ->
                                            if (firstCover.isNullOrEmpty()) {
                                                firstCover = mediaItem.cover
                                            }
                                        }*/
                                        if (firstCover.isNullOrEmpty()) {
                                            firstCover = albumItem.cover
                                        }

                                        albumList.add(albumItem)
                                    }

                                    if (page == 1) {
                                        if (albumList.isEmpty()) {
                                            showError("Ooooops，找不到结果", false)
                                        }
                                        if (firstCover.isNullOrEmpty()) {
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
                                                    sectionContentAdapter.setAlbumList(albumList)
                                                    sectionContentAdapter.notifyDataSetChanged()

                                                    hideLoading()
                                                }
                                            }
                                        } else
                                            Glide.with(this@TemplateXmlySectionContentFragment)
                                                .asBitmap()
                                                .load(firstCover)
                                                .into(object : CustomTarget<Bitmap>() {
                                                    override fun onResourceReady(
                                                        resource: Bitmap,
                                                        transition: Transition<in Bitmap>?
                                                    ) {
                                                        val ratio =
                                                            1f * resource.width / resource.height

                                                        sectionContentAdapter.ratio = ratio
                                                        setLayoutManager(ratio)

                                                        recyclerView?.let { recyclerView ->
                                                            recyclerView.post {
                                                                when {
                                                                    ratio <= 1f -> {
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

                                                                sectionContentAdapter.setAlbumList(
                                                                    albumList
                                                                )
                                                                sectionContentAdapter.notifyDataSetChanged()
                                                            }

                                                        }
                                                    }

                                                    override fun onLoadCleared(placeholder: Drawable?) {

                                                    }
                                                })
                                    } else {
                                        sectionContentAdapter.appendAlbumList(albumList)
                                        recyclerView?.post {
                                            hideLoading()

                                            sectionContentAdapter.notifyDataSetChanged()
                                        }
                                    }
                                }

                            } catch (t: Throwable) {
                                t.printStackTrace()
                            }
                        }.start()
                    }
                } else {
                    showError("请求出错")
                }
            }

        })
    }

    private fun postPlay(mediaItem: MediaItem, id: String?) {
        val audioId = mediaItem.id ?: return
        val sourceType = mediaItem.source ?: templateApp?.source ?: return
        val albumId = mediaItem.albumId ?: id ?: return

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
                    Toast.makeText(context, "播放失败", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    fun applyTheme(isDark: Boolean) {
        sectionContentAdapter.isDark = isDark
        if (sectionContentAdapter.itemCount != 0) {
            sectionContentAdapter.notifyDataSetChanged()
        }

        tvError?.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        tvRetry?.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        errorRetry?.setBackgroundResource(
            if (isDark)
                R.drawable.bg_round_border_white_36dp
            else
                R.drawable.bg_round_border_black_36dp
        )
    }

    private fun getAppApi(): AppApi? {
        val context = context ?: return null
        return CoreApplication.from(context).createApi(AppApi::class.java)
    }

    private class SectionContentAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            const val TYPE_1P0 = 0
            const val TYPE_1P6 = 1
            const val TYPE_0P75 = 2
        }

        private val albumList = mutableListOf<XmlyAlbumItem>()

        var onItemClickListener: OnItemClickListener? = null

        var isDark = false

        var ratio = 1f

        fun getAlbumItem(position: Int) = if (position < albumList.size) albumList[position] else null

        fun setAlbumList(albumList: List<XmlyAlbumItem>) {
            this.albumList.clear()
            this.albumList.addAll(albumList)
        }

        fun appendAlbumList(albumList: List<XmlyAlbumItem>) {
            this.albumList.addAll(albumList)
        }

        override fun getItemViewType(position: Int): Int {
            return when {
                ratio < 1 -> TYPE_0P75
                ratio == 1f -> TYPE_1P0
                else -> TYPE_1P6
            }
        }

        override fun getItemCount(): Int {
            return albumList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val holder = when (viewType) {
                TYPE_1P6 -> SectionContentHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_template_app_single_album_1p6,
                        parent,
                        false
                    )
                )
                TYPE_1P0 -> SectionContentHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_template_app_single_album_1p0,
                        parent,
                        false
                    )
                )
                else -> SectionContentHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_template_app_single_album_0p75,
                        parent,
                        false
                    )
                )
            }
            holder.itemView.setOnClickListener {
                onItemClickListener?.onItemClick(parent, it, holder.adapterPosition)
            }
            return holder
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is SectionContentHolder) {
                val albumItem = albumList[position]

                if (isDark) {
                    holder.titleTextView.setTextColor(Color.WHITE)
                    holder.subTitleTextView.setTextColor(Color.WHITE)
                } else {
                    holder.titleTextView.setTextColor(Color.BLACK)
                    holder.subTitleTextView.setTextColor(Color.BLACK)
                }

                holder.indexTextView.text = (position + 1).toString()

                holder.titleTextView.text = albumItem.name
                holder.subTitleTextView.isVisible = !albumItem.subtitle.isNullOrEmpty()
                holder.subTitleTextView.text = albumItem.subtitle

                if (!albumItem.cover.isNullOrEmpty()) {
                    try {
                        Uri.parse(albumItem.cover)?.let { uri ->
                            Glide.with(holder.albumImageView)
                                .load(uri)
                                .placeholder(R.drawable.bg_default_template_app_2)
                                .error(R.drawable.bg_default_template_app_2)
                                .transform(
                                    MultiTransformation(
                                        CenterCrop(), RoundedCornersTransformation(
                                            holder.albumImageView.resources.getDimensionPixelSize(
                                                R.dimen.dp_6
                                            ), 0
                                        )
                                    )
                                )
                                .into(holder.albumImageView)
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        holder.albumImageView.setImageResource(R.drawable.bg_default_template_app_2)
                    }
                } else {
                    holder.albumImageView.setImageResource(R.drawable.bg_default_template_app_2)
                }
            }
        }
    }

    private class SectionContentHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val albumImageView = itemView.findViewById<ImageView>(R.id.album_media_0)
        val indexTextView = itemView.findViewById<TextView>(R.id.index_media_0)
        val titleTextView = itemView.findViewById<TextView>(R.id.title_media_0)
        val subTitleTextView = itemView.findViewById<TextView>(R.id.subtitle_media_0)
    }

    private class XmlySectionContentAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            const val TYPE_1P0 = 0
            const val TYPE_1P6 = 1
            const val TYPE_0P75 = 2
        }

        var isCategoryList = false

        private val categoryList = mutableListOf<XmlyCategoryItem>()
        private val albumList = mutableListOf<XmlyAlbumItem>()

        var isDark = false

        var onMoreClickListener: OnMoreClickListener? = null

        var onSubItemClickListener: OnSubItemClickListener? = null
        private val innerOnSubItemClickListener = object : OnSubItemClickListener {
            override fun onSubItemClick(
                parent: ViewGroup,
                view: View,
                position: Int,
                subPosition: Int
            ) {
                onSubItemClickListener?.onSubItemClick(
                    parent, view, position, subPosition
                )
            }

        }

        var ratio = 1f

        fun setCategoryList(categoryList: List<XmlyCategoryItem>) {
            clear()
            this.categoryList.addAll(categoryList)
        }

        fun appendCategoryList(categoryList: List<XmlyCategoryItem>) {
            this.categoryList.addAll(categoryList)
        }

        fun getCategoryItem(position: Int): XmlyCategoryItem? {
            return if (categoryList.size > position) {
                categoryList[position]
            } else {
                null
            }
        }

        fun getAlbumItem(position: Int): XmlyAlbumItem? {
            return if (albumList.size > position) {
                albumList[position]
            } else {
                null
            }
        }

        fun setAlbumList(albumList: List<XmlyAlbumItem>) {
            clear()
            this.albumList.addAll(albumList)
        }

        fun appendAlbumList(albumList: List<XmlyAlbumItem>) {
            this.albumList.addAll(albumList)
        }

        fun clear() {
            categoryList.clear()
            albumList.clear()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val holder = when (viewType) {
                TYPE_1P6 -> AlbumItemViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_template_app_album_1p6,
                        parent,
                        false
                    )
                )
                TYPE_1P0 -> AlbumItemViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_template_app_album_1p0,
                        parent,
                        false
                    )
                )
                else -> AlbumItemViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_template_app_album_0p75,
                        parent,
                        false
                    )
                )
            }
            holder.childList.mapIndexed { index, item ->
                item.ivAlbumMedia?.setOnClickListener {
                    innerOnSubItemClickListener.onSubItemClick(
                        parent,
                        holder.itemView,
                        holder.adapterPosition,
                        index
                    )
                }
            }
            holder.seeMore?.setOnClickListener {
                onMoreClickListener?.onMoreClick(parent, it, holder.adapterPosition)
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
            return if (isCategoryList) {
                categoryList.size
            } else {
                albumList.size
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder !is AlbumItemViewHolder) return

            val childSize = holder.fullChildSize()
            var indexSize = 0
            for (i in 0 until position) {
                indexSize += if (isCategoryList)
                    min(childSize, categoryList[i].result?.size ?: 0)
                else
                    min(childSize, albumList[i].result?.size ?: 0)
            }

            if (isCategoryList) {
                val categoryItem = categoryList[position]

                holder.setupCategory(categoryItem, isDark, indexSize)
            } else {
                val albumItem = albumList[position]

                holder.setupAlbum(albumItem, isDark, indexSize)
            }
        }

    }

    private class AlbumItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAlbumTitle: TextView? = itemView.findViewById(R.id.album_title)
        val seeMore: View? = itemView.findViewById(R.id.album_more)
        val tvMore: TextView? = itemView.findViewById(R.id.tv_more)
        val ivMore: ImageView? = itemView.findViewById(R.id.iv_more)

        val childList = listOf(
            MediaGroup(
                itemView,
                R.id.album_media_0,
                R.id.title_media_0,
                R.id.subtitle_media_0,
                R.id.index_media_0
            ),
            MediaGroup(
                itemView,
                R.id.album_media_1,
                R.id.title_media_1,
                R.id.subtitle_media_1,
                R.id.index_media_1
            ),
            MediaGroup(
                itemView,
                R.id.album_media_2,
                R.id.title_media_2,
                R.id.subtitle_media_2,
                R.id.index_media_2
            ),
            MediaGroup(
                itemView,
                R.id.album_media_3,
                R.id.title_media_3,
                R.id.subtitle_media_3,
                R.id.index_media_3
            ),
            MediaGroup(
                itemView,
                R.id.album_media_4,
                R.id.title_media_4,
                R.id.subtitle_media_4,
                R.id.index_media_4
            )
        )

        fun setupCategory(categoryItem: XmlyCategoryItem, isDark: Boolean, indexFirst: Int) {
            val childSize = fullChildSize()

            tvAlbumTitle?.text = categoryItem.categoryName
            seeMore?.isVisible =
                !categoryItem.result.isNullOrEmpty() && categoryItem.result.size > childSize

            if (isDark) {
                tvAlbumTitle?.setTextColor(Color.WHITE)
                tvMore?.setTextColor(Color.WHITE)
                ivMore?.imageTintList = ColorStateList.valueOf(Color.WHITE)
            } else {
                tvAlbumTitle?.setTextColor(Color.BLACK)
                tvMore?.setTextColor(Color.BLACK)
                ivMore?.imageTintList = ColorStateList.valueOf(Color.BLACK)
            }

            for (i in 0 until childSize) {
                if (i < categoryItem.result?.size ?: 0) {
                    categoryItem.result?.get(i)?.let { albumItem ->
                        childList[i].setupAlbumItem(
                            albumItem,
                            isDark,
                            (indexFirst + i + 1).toString()
                        )
                        childList[i].setVisible(true)
                    } ?: run {
                        childList[i].setVisible(false)
                    }
                } else {
                    childList[i].setVisible(false)
                }
            }
        }

        fun setupAlbum(albumItem: XmlyAlbumItem, isDark: Boolean, indexFirst: Int) {
            val childSize = fullChildSize()

            tvAlbumTitle?.text = albumItem.name
            seeMore?.isVisible =
                !albumItem.result.isNullOrEmpty() && albumItem.result.size > childSize

            if (isDark) {
                tvAlbumTitle?.setTextColor(Color.WHITE)
                tvMore?.setTextColor(Color.WHITE)
                ivMore?.imageTintList = ColorStateList.valueOf(Color.WHITE)
            } else {
                tvAlbumTitle?.setTextColor(Color.BLACK)
                tvMore?.setTextColor(Color.BLACK)
                ivMore?.imageTintList = ColorStateList.valueOf(Color.BLACK)
            }

            for (i in 0 until childSize) {
                if (i < albumItem.result?.size ?: 0) {
                    albumItem.result?.get(i)?.let { mediaItem ->
                        childList[i].setupMediaItem(
                            mediaItem,
                            isDark,
                            (indexFirst + i + 1).toString()
                        )
                        childList[i].setVisible(true)
                    } ?: run {
                        childList[i].setVisible(false)
                    }
                } else {
                    childList[i].setVisible(false)
                }
            }
        }

        fun fullChildSize() = if (childList[3].exists()) 5 else 3
    }

    private class MediaGroup(
        itemView: View,
        albumId: Int,
        titleId: Int,
        subtitleId: Int,
        indexId: Int
    ) {
        val ivAlbumMedia: ImageView? = itemView.findViewById(albumId)
        val tvAlbumTitle: TextView? = itemView.findViewById(titleId)
        val tvAlbumSubtitle: TextView? = itemView.findViewById(subtitleId)
        val tvAlbumIndex: TextView? = itemView.findViewById(indexId)

        fun exists() = ivAlbumMedia != null

        fun setVisible(isVisible: Boolean) {
            ivAlbumMedia?.isVisible = isVisible
            tvAlbumTitle?.isVisible = isVisible
            tvAlbumSubtitle?.isVisible = isVisible
            tvAlbumIndex?.isVisible = isVisible
        }

        fun setupAlbumItem(albumItem: XmlyAlbumItem, isDark: Boolean, indexValue: String) {
            tvAlbumTitle?.text = albumItem.name
            tvAlbumSubtitle?.text = albumItem.subtitle
            tvAlbumSubtitle?.isVisible = !albumItem.subtitle.isNullOrEmpty()
            tvAlbumIndex?.text = indexValue

            if (isDark) {
                tvAlbumTitle?.setTextColor(Color.WHITE)
                tvAlbumSubtitle?.setTextColor(Color.WHITE)
            } else {
                tvAlbumTitle?.setTextColor(Color.BLACK)
                tvAlbumSubtitle?.setTextColor(Color.BLACK)
            }

            ivAlbumMedia?.let { imageView ->
                if (!albumItem.cover.isNullOrEmpty()) {
                    try {
                        Uri.parse(albumItem.cover)?.let { uri ->
                            Glide.with(imageView)
                                .load(uri)
                                .placeholder(R.drawable.bg_default_template_app_2)
                                .error(R.drawable.bg_default_template_app_2)
                                .transform(
                                    MultiTransformation(
                                        CenterCrop(), RoundedCornersTransformation(
                                            imageView.resources.getDimensionPixelSize(
                                                R.dimen.dp_6
                                            ), 0
                                        )
                                    )
                                )
                                .into(imageView)
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        imageView.setImageResource(R.drawable.bg_default_template_app_2)
                    }
                } else {
                    imageView.setImageResource(R.drawable.bg_default_template_app_2)
                }
            }
        }

        fun setupMediaItem(mediaItem: MediaItem, isDark: Boolean, indexValue: String) {
            tvAlbumTitle?.text = mediaItem.title
            tvAlbumSubtitle?.text = mediaItem.subtitle
            tvAlbumSubtitle?.isVisible = !mediaItem.subtitle.isNullOrEmpty()
            tvAlbumIndex?.text = indexValue

            if (isDark) {
                tvAlbumTitle?.setTextColor(Color.WHITE)
                tvAlbumSubtitle?.setTextColor(Color.WHITE)
            } else {
                tvAlbumTitle?.setTextColor(Color.BLACK)
                tvAlbumSubtitle?.setTextColor(Color.BLACK)
            }

            ivAlbumMedia?.let { imageView ->
                if (!mediaItem.cover.isNullOrEmpty()) {
                    try {
                        Uri.parse(mediaItem.cover)?.let { uri ->
                            Glide.with(imageView)
                                .load(uri)
                                .placeholder(R.drawable.bg_default_template_app_2)
                                .error(R.drawable.bg_default_template_app_2)
                                .transform(
                                    MultiTransformation(
                                        CenterCrop(), RoundedCornersTransformation(
                                            imageView.resources.getDimensionPixelSize(
                                                R.dimen.dp_6
                                            ), 0
                                        )
                                    )
                                )
                                .into(imageView)
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        imageView.setImageResource(R.drawable.bg_default_template_app_2)
                    }
                } else {
                    imageView.setImageResource(R.drawable.bg_default_template_app_2)
                }
            }
        }
    }

    private interface OnMoreClickListener {
        fun onMoreClick(parent: ViewGroup, view: View, position: Int)
    }

    private interface OnSubItemClickListener {
        fun onSubItemClick(parent: ViewGroup, view: View, position: Int, subPosition: Int)
    }
}