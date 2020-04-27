package com.iflytek.cyber.iot.show.core.fragment

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.bumptech.glide.request.transition.Transition
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.AlbumSectionContentAdapter
import com.iflytek.cyber.iot.show.core.api.AppApi
import com.iflytek.cyber.iot.show.core.model.SourceItem
import com.iflytek.cyber.iot.show.core.model.TemplateApp
import com.iflytek.cyber.iot.show.core.model.TemplateMediaItem
import com.iflytek.cyber.iot.show.core.utils.OnItemClickListener
import com.iflytek.cyber.iot.show.core.utils.onLoadMore
import com.iflytek.cyber.iot.show.core.widget.GradientTextView
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.UnknownHostException
import kotlin.math.abs

class TemplateApp1Fragment : BaseFragment(), PageScrollable {

    companion object {
        private const val TAG = "TemplateApp1Fragment"

        fun newInstance(templateApp: TemplateApp): TemplateApp1Fragment {
            val fragment = TemplateApp1Fragment()
            fragment.templateApp = templateApp
            return fragment
        }

        fun newInstance(templateApp: TemplateApp, sourceItem: SourceItem?): TemplateApp1Fragment {
            val fragment = TemplateApp1Fragment().apply {
                arguments = bundleOf(Pair("sourceItem", sourceItem))
            }
            fragment.templateApp = templateApp
            return fragment
        }
    }

    private var templateApp: TemplateApp? = null
    private var sourceItem: SourceItem? = null

    private var ivBack: ImageView? = null
    private var ivIcon: ImageView? = null
    private var ivBackground: ImageView? = null
    private var tabLayout: RecyclerView? = null
    private var recyclerView: RecyclerView? = null
    private var viewPager: ViewPager2? = null
    private var tvTitle: TextView? = null
    private var loadingContainer: FrameLayout? = null
    private var loadingImageView: LottieAnimationView? = null
    private var loadingBottom: LottieAnimationView? = null
    private var loadingCenter: LottieAnimationView? = null

    private var errorContainer: View? = null
    private var tvError: TextView? = null
    private var errorRetry: View? = null
    private var tvRetry: TextView? = null

    private val tabAdapter = TabAdapter()
    private var pagerAdapter: CategoryContentAdapter? = null
    private val albumAdapter = AlbumSectionContentAdapter()

    private var clickCount = 0
    private var page = 1
    private var isLoading = false
    private var totalListSize = 0
    private var templateType = -1

    private val tabScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            (recyclerView.layoutManager as? LinearLayoutManager)?.let { linearLayoutManager ->
                val lastComplete = linearLayoutManager.findLastCompletelyVisibleItemPosition()
                val lastVisible = linearLayoutManager.findLastVisibleItemPosition()

                if (lastComplete == lastComplete
                    && linearLayoutManager.findFirstCompletelyVisibleItemPosition()
                    == linearLayoutManager.findFirstVisibleItemPosition()
                    && tabAdapter.itemCount == lastComplete + 1
                )
                    return@let

                /*val targetViewSet = mutableSetOf<View>()
                var lastViewWidth = 0
                recyclerView.findViewHolderForAdapterPosition(lastVisible)?.let { holder ->
                    if (holder is TabViewHolder) {
                        val itemView = holder.itemView

                        val offset = itemView.width - itemView.right + recyclerView.width

                        holder.tvTitle?.let { gradientTextView ->
                            if (!gradientTextView.isGradient)
                                gradientTextView.isGradient = true

                            gradientTextView.startColor =
                                Color.argb(
                                    255 * offset / itemView.width,
                                    255,
                                    255,
                                    255
                                )
                            if (recyclerView.adapter?.itemCount == lastVisible + 1) {
                                gradientTextView.endColor =
                                    Color.argb(
                                        255 * offset / itemView.width,
                                        255,
                                        255,
                                        255
                                    )
                            } else {
                                if (gradientTextView.endColor != Color.TRANSPARENT)
                                    gradientTextView.endColor =
                                        Color.TRANSPARENT
                            }
                        }

                        lastViewWidth = itemView.width

                        targetViewSet.add(itemView)
                    }
                }*/

                /*if (lastComplete != lastVisible) {
                    recyclerView.findViewHolderForAdapterPosition(lastComplete)?.let { holder ->
                        if (holder is TabViewHolder) {
                            val itemView = holder.itemView

                            val offset =
                                lastViewWidth - (itemView.right + lastViewWidth - recyclerView.width)

                            holder.tvTitle?.let { gradientTextView ->
                                if (!gradientTextView.isGradient)
                                    gradientTextView.isGradient = true

                                if (gradientTextView.startColor != Color.WHITE)
                                    gradientTextView.startColor = Color.WHITE
                                gradientTextView.endColor =
                                    Color.argb(
                                        255 * offset / lastViewWidth,
                                        255,
                                        255,
                                        255
                                    )
                            }

                            targetViewSet.add(itemView)
                        }
                    }
                }*/

                /*// 从后往前遍历
                for (i in 0 until recyclerView.childCount) {
                    val child = recyclerView.getChildAt(recyclerView.childCount - 1 - i)

                    if (child !in targetViewSet) {
                        (recyclerView.getChildViewHolder(child) as? TabViewHolder)?.let {
                            it.tvTitle?.isGradient = false
                        }
                    }
                }*/
            }
        }
    }
    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)

            val oldPosition = tabAdapter.selectedItem
            tabAdapter.selectedItem = position

            tabAdapter.notifyItemChanged(oldPosition)
            tabAdapter.notifyItemChanged(position)

            if (position < tabAdapter.itemCount)
                tabLayout?.smoothScrollToPosition(position)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_template_app_1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.back)?.setOnClickListener {
            pop()
        }

        ivBack = view.findViewById(R.id.iv_back)
        ivIcon = view.findViewById(R.id.iv_icon)
        ivBackground = view.findViewById(R.id.background)
        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.view_pager)
        recyclerView = view.findViewById(R.id.recycler_view)
        tvRetry = view.findViewById(R.id.tv_retry)
        errorRetry = view.findViewById(R.id.error_retry)
        tvError = view.findViewById(R.id.tv_error)
        errorContainer = view.findViewById(R.id.error_container)
        tvTitle = view.findViewById(R.id.title)
        loadingContainer = view.findViewById(R.id.loading_container)
        loadingImageView = view.findViewById(R.id.loading_image)
        loadingBottom = view.findViewById(R.id.loading_bottom)
        loadingCenter = view.findViewById(R.id.loading_center)

        tvTitle?.text = templateApp?.name
        tvTitle?.isVisible = false

        errorRetry?.setOnClickListener {
            if (!isLoading) {
                page = 1
                //getFirstTemplate()
                getTemplateList()
            }
        }

        viewPager?.registerOnPageChangeCallback(onPageChangeCallback)

        tabLayout?.adapter = tabAdapter
        tabLayout?.addOnScrollListener(tabScrollListener)
        tabLayout?.itemAnimator = null
        tabAdapter.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(parent: ViewGroup, itemView: View, position: Int) {
                setCurrentPageItem(position)
            }
        }
        albumAdapter.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(parent: ViewGroup, itemView: View, position: Int) {
                val albumItem = albumAdapter.getAlbumItem(position)
                val templateApp = templateApp ?: return
                if (albumItem != null) {
                    if (albumItem.type == 1) {
                        start(newInstance(templateApp, albumItem))
                    } else if (albumItem.type == 2) {
                        if (templateType == 2) {
                            start(newInstance(templateApp, albumItem))
                        } else {
                            start(TemplateApp2Fragment.newInstance(templateApp))
                        }
                    } else {
                        if (!albumItem.metadata?.resourceId.isNullOrEmpty()) {
                            postPlay(albumItem)
                        } else {
                            start(SongListFragment.newInstance(albumItem, 2, 1002))
                        }
                    }
                }
            }
        }
        setLayoutManager(1.0f)

        sourceItem = arguments?.getParcelable("sourceItem")

        applyTheme(templateApp?.isDark != false)

        loadBackground()

        loadAppIcon()

        //getFirstTemplate()
        getTemplateList()

        recyclerView?.onLoadMore {
            if (!isLoading && albumAdapter.itemCount < totalListSize) {
                page += 1
                //getFirstTemplate()
                getTemplateList()
            }
        }
    }

    override fun onSupportVisible() {
        super.onSupportVisible()

        clickCount = 0
    }

    override fun onDestroy() {
        super.onDestroy()

        recyclerView?.removeOnScrollListener(tabScrollListener)

        viewPager?.unregisterOnPageChangeCallback(onPageChangeCallback)
    }

    private fun applyTheme(isDark: Boolean) {
        if (isDark) {
            loadingImageView?.setAnimation(R.raw.animation_loading_l_white)
        } else {
            loadingImageView?.setAnimation(R.raw.animation_loading_l)
        }
        ivBack?.setColorFilter(if (isDark) Color.WHITE else Color.BLACK)

        tabAdapter.isDark = isDark
        if (tabAdapter.itemCount != 0) {
            tabAdapter.notifyDataSetChanged()
        }

        albumAdapter.isDark = isDark
        if (albumAdapter.itemCount != 0) {
            albumAdapter.notifyDataSetChanged()
        }

        pagerAdapter?.isDark = isDark

        tvTitle?.setTextColor(if (isDark) Color.WHITE else Color.BLACK)

        tvError?.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        tvRetry?.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        errorRetry?.setBackgroundResource(
            if (isDark) R.drawable.bg_round_border_white_36dp
            else R.drawable.bg_round_border_black_36dp
        )
    }

    private fun setCurrentPageItem(position: Int) {
        val viewPager = viewPager ?: return
        if (pagerAdapter?.itemCount ?: 0 <= position) return
        val smooth = abs(position - viewPager.currentItem) == 1
        viewPager.setCurrentItem(position, smooth)
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

    private fun showLoading() {
        if (page == 1) {
            loadingContainer?.isVisible = true
            loadingImageView?.playAnimation()
        } else {
            loadingBottom?.isVisible = true
            loadingBottom?.playAnimation()
        }
    }

    private fun dismissLoading() {
        loadingContainer?.post {
            loadingImageView?.pauseAnimation()
            loadingContainer?.isVisible = false

            if (loadingBottom?.isVisible == true) {
                loadingBottom?.pauseAnimation()
                loadingBottom?.isVisible = false
            }
        }
    }

    private fun showError(message: String?, showRetry: Boolean = true) {
        if (isRemoving || isDetached)
            return
        context ?: return
        viewPager?.isVisible = false
        recyclerView?.isVisible = false
        errorContainer?.isVisible = true
        errorRetry?.isVisible = showRetry
        tvError?.text = message
    }

    private fun hideError() {
        if (isRemoving || isDetached)
            return
        context ?: return
        errorContainer?.isVisible = false
    }

    private fun getTemplateList() {
        val templateApp = templateApp ?: return
        isLoading = true
        hideError()
        showLoading()

        val metaData = sourceItem?.metadata

        getAppApi()?.getTemplateList(
            templateApp.name,
            metaData?.topLevel,
            metaData?.secondLevel,
            metaData?.thirdLevel,
            page
        )?.enqueue(object : Callback<TemplateMediaItem> {
            override fun onResponse(
                call: Call<TemplateMediaItem>,
                response: Response<TemplateMediaItem>
            ) {
                isLoading = false
                if (response.isSuccessful) {
                    val templateMediaItem = response.body()
                    if (templateMediaItem != null) {
                        totalListSize = templateMediaItem.total
                        //templateType = templateMediaItem.type
                        val type = if (sourceItem != null && sourceItem?.type != null) {
                            sourceItem!!.type
                        } else if (templateMediaItem.items?.size ?: 0 > 0) {
                            templateMediaItem.items?.get(0)?.type
                        } else {
                            templateMediaItem.type
                        }
                        if (type != null) {
                            handleResult(templateApp, type, templateMediaItem)
                        }
                    }
                } else {
                    dismissLoading()
                    if (albumAdapter.itemCount == 0) {
                        showError("请求出错，请稍后重试")
                    }
                }
            }

            override fun onFailure(call: Call<TemplateMediaItem>, t: Throwable) {
                if (isRemoving || isDetached)
                    return
                context ?: return
                isLoading = false
                dismissLoading()
                if (t is UnknownHostException) {
                    showError("Ooooops，请检查网络后重试")
                } else {
                    showError("请求出错，请稍后重试")
                }
            }
        })
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

    private fun handleResult(
        templateApp: TemplateApp,
        type: Int,
        templateMediaItem: TemplateMediaItem
    ) {
        if (!isAdded || context == null) {
            return
        }

        if ((templateApp.template == 2 && type == 1) || (templateApp.template == 1 && type == 2)) {
            dismissLoading()
            viewPager?.isVisible = true
            recyclerView?.isVisible = false
            pagerAdapter = CategoryContentAdapter(templateApp, templateMediaItem.items, this)
            viewPager?.adapter = pagerAdapter
            tabAdapter.setCategories(templateMediaItem.items)
            tabAdapter.notifyDataSetChanged()
        } else if (type > 1 && templateMediaItem.items != null) {
            viewPager?.isVisible = false
            recyclerView?.isVisible = true
            val firstCover = getFirstItemCover(templateMediaItem.items)
            if (firstCover.isNullOrEmpty()) {
                dismissLoading()
                setupAlbumList(1f, templateMediaItem.items)
            } else {
                setupAlbumAdapter(firstCover, templateMediaItem.items)
            }
        }
    }

    private fun setupAlbumAdapter(firstCover: String?, items: ArrayList<SourceItem>) {
        Glide.with(this)
            .asBitmap()
            .load(firstCover)
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {
                    isLoading = false
                    dismissLoading()
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    dismissLoading()
                    showError("请求出错，请稍后重试")
                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    dismissLoading()
                    val ratio = 1f * resource.width / resource.height
                    setupAlbumList(ratio, items)
                }
            })
    }

    private fun setupAlbumList(ratio: Float, items: ArrayList<SourceItem>) {
        if (page == 1) {
            setRecyclerViewPadding(ratio)
            setLayoutManager(ratio)
            albumAdapter.ratio = ratio
            albumAdapter.setAlbumList(items)
            recyclerView?.adapter = albumAdapter
        } else {
            albumAdapter.appendAlbumList(items)
            albumAdapter.notifyDataSetChanged()
        }
    }

    private fun setRecyclerViewPadding(ratio: Float) {
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

    private fun postPlay(mediaItem: SourceItem) {
        val audioId = mediaItem.metadata?.resourceId
        val sourceType = mediaItem.source
        val business = mediaItem.metadata?.business

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
                clickCount = 0
                Toast.makeText(context, "请求出错", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (isRemoving || isDetached)
                    return
                context ?: return

                loadingCenter?.isVisible = false
                loadingCenter?.pauseAnimation()

                clickCount = 0
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

    override fun scrollToNext(): Boolean {
        if (viewPager?.isVisible == false) {
            return false
        }

        val currentItem = viewPager?.currentItem ?: return false

        if (currentItem < (viewPager?.adapter?.itemCount ?: 0) - 1 || currentItem < 0) {
            viewPager?.post {
                viewPager?.setCurrentItem(currentItem + 1, true)
            }
            return true
        }
        return false
    }

    override fun scrollToPrevious(): Boolean {
        if (viewPager?.isVisible == false) {
            return false
        }

        val currentItem = viewPager?.currentItem ?: return false

        if (currentItem > 0) {
            viewPager?.post {
                viewPager?.setCurrentItem(currentItem - 1, true)
            }
            return true
        }
        return false
    }

    // Setup ViewPager

    private class CategoryContentAdapter(
        val templateApp: TemplateApp,
        val items: ArrayList<SourceItem>?,
        fragment: Fragment
    ) :
        FragmentStateAdapter(fragment) {

        var isDark = false
            set(value) {
                field = value
                fragments.map {
                    (it.value as? TemplateCategoryContentFragment)?.applyTheme(value)
                }
            }

        private val fragments = mutableMapOf<Int, Fragment>() // position -> fragment

        override fun getItemCount(): Int {
            return items?.size ?: 0
        }

        override fun createFragment(position: Int): Fragment {
            val sourceItem = items?.get(position)
            val fragment = TemplateCategoryContentFragment.newInstance(templateApp, sourceItem)
            fragments[position] = fragment
            return fragment
        }
    }

    // Setup Tab

    private class TabAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val categories = mutableListOf<SourceItem>()
        var selectedItem = 0
        var onItemClickListener: OnItemClickListener? = null

        var isDark = false

        fun setCategories(categories: ArrayList<SourceItem>?) {
            if (categories != null) {
                this.categories.clear()
                this.categories.addAll(categories)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val holder = TabViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_app_tab, parent, false)
            )
            holder.itemView.setOnClickListener {
                onItemClickListener?.onItemClick(parent, it, holder.adapterPosition)
            }
            return holder
        }

        override fun getItemCount(): Int {
            return categories.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is TabViewHolder) {
                if (selectedItem == position) {
                    if (holder.indicator?.isVisible != true) {
                        holder.indicator?.isVisible = true
                    }
                    if (holder.tvTitle?.alpha != 1f) {
                        holder.tvTitle?.alpha = 1f
                    }
                } else {
                    if (holder.indicator?.isVisible != false) {
                        holder.indicator?.isVisible = false
                    }
                    if (holder.tvTitle?.alpha != 0.7f) {
                        holder.tvTitle?.alpha = 0.7f
                    }
                }

                holder.tvTitle?.text = categories[position].title

                if (isDark) {
                    holder.indicator?.background?.setColorFilter(
                        Color.WHITE,
                        PorterDuff.Mode.SRC_IN
                    )
                    holder.tvTitle?.setTextColor(Color.WHITE)
                } else {
                    holder.indicator?.background?.setColorFilter(
                        Color.BLACK,
                        PorterDuff.Mode.SRC_IN
                    )
                    holder.tvTitle?.setTextColor(Color.BLACK)
                }
            }
        }
    }

    private class TabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: GradientTextView? = itemView.findViewById(R.id.title)
        val indicator: View? = itemView.findViewById(R.id.indicator)
    }

}