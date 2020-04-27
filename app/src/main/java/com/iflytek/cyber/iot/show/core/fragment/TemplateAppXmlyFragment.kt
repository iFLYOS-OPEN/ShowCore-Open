package com.iflytek.cyber.iot.show.core.fragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.alibaba.fastjson.JSON
import com.bumptech.glide.Glide
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.AppApi
import com.iflytek.cyber.iot.show.core.model.TemplateApp
import com.iflytek.cyber.iot.show.core.model.XmlyAlbumItem
import com.iflytek.cyber.iot.show.core.model.XmlyCategoryItem
import com.iflytek.cyber.iot.show.core.utils.OnItemClickListener
import com.iflytek.cyber.iot.show.core.widget.GradientTextView
import com.iflytek.cyber.iot.show.core.widget.StyledProgressDialog
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.UnknownHostException
import kotlin.math.abs

class TemplateAppXmlyFragment : BaseFragment(), PageScrollable {

    companion object {
        private const val TAG = "TemplateAppXmlyFragment"

        fun newInstance(templateApp: TemplateApp): TemplateAppXmlyFragment {
            val fragment = TemplateAppXmlyFragment()
            fragment.templateApp = templateApp
            return fragment
        }
    }

    private var templateApp: TemplateApp? = null

    private var ivBack: ImageView? = null
    private var ivIcon: ImageView? = null
    private var ivBackground: ImageView? = null
    private var tabLayout: RecyclerView? = null
    private var recyclerView: RecyclerView? = null
    private var viewPager: ViewPager2? = null
    private var errorContainer: View? = null
    private var tvError: TextView? = null
    private var errorRetry: View? = null
    private var tvRetry: TextView? = null
    private var loadingContainer: FrameLayout? = null
    private var loadingImageView: LottieAnimationView? = null

    private val tabAdapter = TabAdapter()
    private var pagerAdapter: SectionContentAdapter? = null

    //private var progressDialog: StyledProgressDialog? = null

    private var clickCount = 0

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

                val targetViewSet = mutableSetOf<View>()
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
                }

                if (lastComplete != lastVisible) {
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
                }

                // 从后往前遍历
                for (i in 0 until recyclerView.childCount) {
                    val child = recyclerView.getChildAt(recyclerView.childCount - 1 - i)

                    if (child !in targetViewSet) {
                        (recyclerView.getChildViewHolder(child) as? TabViewHolder)?.let {
                            it.tvTitle?.isGradient = false
                        }
                    }
                }
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
        return inflater.inflate(R.layout.fragment_template_app_xmly, container, false)
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
        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.view_pager)
        recyclerView = view.findViewById(R.id.recycler_view)
        tvRetry = view.findViewById(R.id.tv_retry)
        errorRetry = view.findViewById(R.id.error_retry)
        tvError = view.findViewById(R.id.tv_error)
        errorContainer = view.findViewById(R.id.error_container)
        loadingContainer = view.findViewById(R.id.loading_container)
        loadingImageView = view.findViewById(R.id.loading_image)


        errorRetry?.setOnClickListener {
            getFirstSection()
        }

        viewPager?.registerOnPageChangeCallback(onPageChangeCallback)

        tabLayout?.adapter = tabAdapter
        tabLayout?.itemAnimator = null
        tabLayout?.addOnScrollListener(tabScrollListener)
        tabAdapter.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(parent: ViewGroup, itemView: View, position: Int) {
                setCurrentPageItem(position)
            }

        }

        applyTheme(templateApp?.isDark != false)

        loadBackground()

        loadAppIcon()

        getFirstSection()
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


    private fun setCurrentPageItem(position: Int) {
        val viewPager = viewPager ?: return
        if (pagerAdapter?.itemCount ?: 0 <= position) return
        val smooth = abs(position - viewPager.currentItem) == 1
        viewPager.setCurrentItem(position, smooth)
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

    private fun applyTheme(isDark: Boolean) {
        if (isDark) {
            loadingImageView?.setAnimation(R.raw.animation_loading_l_white)
        } else {
            loadingImageView?.setAnimation(R.raw.animation_loading_l)
        }
        ivBack?.imageTintList = ColorStateList.valueOf(if (isDark) Color.WHITE else Color.BLACK)

        tabAdapter.isDark = isDark
        if (tabAdapter.itemCount != 0) {
            tabAdapter.notifyDataSetChanged()
        }

        pagerAdapter?.isDark = isDark
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
        loadingContainer?.isVisible = true
        loadingImageView?.playAnimation()
    }

    private fun dismissLoading() {
        loadingContainer?.post {
            loadingImageView?.pauseAnimation()
            loadingContainer?.isVisible = false
        }
    }

    private fun getFirstSection() {
        /*progressDialog = StyledProgressDialog.Builder()
            .setTitle(getString(R.string.loading))
            .setMessage(getString(R.string.please_wait))
            .show(fragmentManager)*/
        showLoading()

        hideError()

        getAppApi()?.getXmlyIndex()?.enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                if (isRemoving || isDetached)
                    return
                context ?: return
                //progressDialog?.dismiss()
                dismissLoading()

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
                            try {
                                val json = JSON.parseObject(bodyString)

                                val sections = json.getJSONArray("section")
                                val sectionArray = if (sections.isNullOrEmpty()) {
                                    listOf("热门推荐")
                                } else {
                                    sections.map { it.toString() }
                                }

                                tabAdapter.setSections(sectionArray)

                                val result = json.getJSONArray("result")
                                val sample = result.getJSONObject(0)
                                var isCategoryList = false
                                if (sample.containsKey("category")) {
                                    isCategoryList = true
                                }

                                pagerAdapter =
                                    SectionContentAdapter(templateApp, this@TemplateAppXmlyFragment)
                                pagerAdapter?.isDark = templateApp?.isDark != false
                                pagerAdapter?.setSections(sectionArray)

                                if (isCategoryList) {
                                    val categoryList = mutableListOf<XmlyCategoryItem>()
                                    for (i in 0 until result.size) {
                                        val jsonItem = result.getJSONObject(i)
                                        val category = JSON.parseObject(
                                            jsonItem.toString(),
                                            XmlyCategoryItem::class.java
                                        )
                                        categoryList.add(category)
                                    }

                                    pagerAdapter?.initialCategoryList = categoryList
                                    pagerAdapter?.initialAlbumList = null

                                } else {
                                    val albumList = mutableListOf<XmlyAlbumItem>()
                                    for (i in 0 until result.size) {
                                        val jsonItem = result.getJSONObject(i)
                                        val album = JSON.parseObject(
                                            jsonItem.toString(),
                                            XmlyAlbumItem::class.java
                                        )
                                        albumList.add(album)
                                    }

                                    pagerAdapter?.initialCategoryList = null
                                    pagerAdapter?.initialAlbumList = albumList
                                }

                                post {
                                    viewPager?.adapter = pagerAdapter

                                    tabAdapter.notifyDataSetChanged()

                                    //progressDialog?.dismiss()
                                    dismissLoading()
                                }
                            } catch (t: Throwable) {
                                t.printStackTrace()
                            }
                        }.start()
                    }
                } else {
                    //progressDialog?.dismiss()
                    dismissLoading()
                }

            }

        })
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
        viewPager?.isVisible = true
        recyclerView?.isVisible = true
        errorContainer?.isVisible = false
    }

    private fun getAppApi(): AppApi? {
        val context = context ?: return null
        return CoreApplication.from(context).createApi(AppApi::class.java)
    }

    // Setup ViewPager

    private class SectionContentAdapter(val templateApp: TemplateApp?, fragment: Fragment) :
        FragmentStateAdapter(fragment) {
        private val sections = mutableListOf<String>()

        var initialCategoryList: List<XmlyCategoryItem>? = null
        var initialAlbumList: List<XmlyAlbumItem>? = null

        var isDark = false
            set(value) {
                field = value
                fragments.map {
                    (it.value as? TemplateXmlySectionContentFragment)?.applyTheme(value)
                }
            }

        private val fragments = mutableMapOf<Int, Fragment>() // position -> fragment

        fun setSections(sections: List<String>) {
            this.sections.clear()
            this.sections.addAll(sections)
        }

        override fun getItemCount(): Int {
            return sections.size
        }

        override fun createFragment(position: Int): Fragment {
            val initialCategoryList = initialCategoryList
            val initialAlbumList = initialAlbumList
            val fragment = if (position == 0) {
                if (initialAlbumList.isNullOrEmpty()) {
                    TemplateXmlySectionContentFragment.newInstance(
                        templateApp,
                        sections[position]
                    )
                } else {
                    if (initialCategoryList.isNullOrEmpty()) {
                        TemplateXmlySectionContentFragment.fromAlbumList(
                            templateApp,
                            sections[position],
                            initialAlbumList
                        )
                    } else {
                        TemplateXmlySectionContentFragment.fromCategoryList(
                            templateApp,
                            sections[position],
                            initialCategoryList
                        )
                    }
                }
            } else {
                TemplateXmlySectionContentFragment.newInstance(
                    templateApp,
                    sections[position]
                )
            }
            fragments[position] = fragment
            return fragment
        }

        fun getFragment(position: Int) = fragments[position]
    }


    // Setup Tab

    private class TabAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val sections = mutableListOf<String>()
        var selectedItem = 0
        var onItemClickListener: OnItemClickListener? = null

        var isDark = false

        fun setSections(categories: List<String>) {
            this.sections.clear()
            this.sections.addAll(categories)
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
            return sections.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is TabViewHolder) {
                holder.tvTitle?.text = sections[position]

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

                if (isDark) {
                    holder.indicator?.background?.setColorFilter(
                        Color.WHITE,
                        PorterDuff.Mode.SRC_IN
                    )
                    holder.tvTitle?.setTextColor(Color.WHITE)
                } else {
                    holder.indicator?.background?.setColorFilter(
                        Color.WHITE,
                        PorterDuff.Mode.SRC_IN
                    )
                    holder.tvTitle?.setTextColor(Color.WHITE)
                }
            }
        }
    }

    private class TabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: GradientTextView? = itemView.findViewById(R.id.title)
        val indicator: View? = itemView.findViewById(R.id.indicator)
    }
}