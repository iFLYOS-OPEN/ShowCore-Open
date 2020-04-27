package com.iflytek.cyber.iot.show.core.fragment

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.AppApi
import com.iflytek.cyber.iot.show.core.model.SourceItem
import com.iflytek.cyber.iot.show.core.model.TemplateApp
import com.iflytek.cyber.iot.show.core.model.TemplateApp2
import com.iflytek.cyber.iot.show.core.model.TemplateMediaItem
import com.iflytek.cyber.iot.show.core.utils.OnItemClickListener
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.utils.clickWithTrigger
import com.iflytek.cyber.iot.show.core.utils.dp2Px
import com.iflytek.cyber.iot.show.core.widget.StyledProgressDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.UnknownHostException

class TemplateApp2Fragment : BaseFragment() {

    companion object {
        fun newInstance(templateApp: TemplateApp): TemplateApp2Fragment {
            val fragment = TemplateApp2Fragment()
            fragment.templateApp = templateApp
            return fragment
        }
    }

    private val adapter = TemplateAppAdapter()

    private var templateApp: TemplateApp? = null

    private var ivBack: ImageView? = null
    private var ivIcon: ImageView? = null
    private var ivBackground: ImageView? = null
    private var recyclerView: RecyclerView? = null
    private var tvError: TextView? = null
    private var tvRetry: TextView? = null
    private var errorRetry: View? = null
    private var errorContainer: View? = null
    private var loadingContainer: FrameLayout? = null
    private var loadingImageView: LottieAnimationView? = null

    private var progressDialog: StyledProgressDialog? = null

    private var clickCount = 0

    private var page = 1
    private var isLoading = false
    private var totalListSize = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_template_app_2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.back)?.setOnClickListener {
            pop()
        }

        loadingContainer = view.findViewById(R.id.loading_container)
        loadingImageView = view.findViewById(R.id.loading_image)

        ivBack = view.findViewById(R.id.iv_back)
        ivIcon = view.findViewById(R.id.iv_icon)
        ivBackground = view.findViewById(R.id.background)
        recyclerView = view.findViewById(R.id.recycler_view)
        tvRetry = view.findViewById(R.id.tv_retry)
        tvError = view.findViewById(R.id.tv_error)
        errorContainer = view.findViewById(R.id.error_container)
        errorRetry = view.findViewById(R.id.error_retry)
        errorRetry?.setOnClickListener {
            if (!isLoading) {
                //getTemplate()
                getTemplateList()
            }
        }

        recyclerView?.adapter = adapter
        recyclerView?.itemAnimator = DefaultItemAnimator()
        recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (isLoading) {
                    return
                }

                val itemCount = recyclerView.adapter?.itemCount
                (recyclerView.layoutManager as? LinearLayoutManager)?.let {
                    if (it.findLastCompletelyVisibleItemPosition() == it.findLastVisibleItemPosition() &&
                        itemCount == it.findLastVisibleItemPosition() + 1 &&
                        itemCount < totalListSize
                    ) {
                        page++
                        getTemplateList()
                    }
                }
            }
        })

        adapter.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(parent: ViewGroup, itemView: View, position: Int) {
                val templateApp = templateApp ?: return
                val app = adapter.getAppItem(position)
                start(
                    TemplateApp1Fragment.newInstance(templateApp, app)
                )
            }
        }

        loadAppIcon()

        //getTemplate()
        getTemplateList()

        loadBackground()

        applyTheme(templateApp?.isDark != false)
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
        loadingContainer?.isVisible = true
        loadingImageView?.playAnimation()
    }

    private fun dismissLoading() {
        loadingContainer?.post {
            loadingImageView?.pauseAnimation()
            loadingContainer?.isVisible = false
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

    private fun getTemplateList() {
        val templateApp = templateApp ?: return
        isLoading = true
        hideError()
        showLoading()

        getAppApi()?.getTemplateList(
            templateApp.name,
            null, null,
            null, page
        )?.enqueue(object : Callback<TemplateMediaItem> {
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

            override fun onResponse(
                call: Call<TemplateMediaItem>,
                response: Response<TemplateMediaItem>
            ) {
                if (isRemoving || isDetached || context == null)
                    return
                isLoading = false
                dismissLoading()

                if (response.isSuccessful) {
                    val templateMediaItem = response.body()
                    if (templateMediaItem?.items != null) {
                        totalListSize = templateMediaItem.total
                        if (page == 1) {
                            setRecyclerViewLayout(templateMediaItem.items)
                            adapter.setAppList(templateMediaItem.items)
                        } else {
                            adapter.appendList(templateMediaItem.items)
                        }
                        adapter.notifyDataSetChanged()
                    }
                } else {
                    isLoading = false
                    dismissLoading()
                    showError("请求出错，请稍后重试")
                }
            }
        })
    }

    private fun setRecyclerViewLayout(appList: ArrayList<SourceItem>) {
        if (appList.size < 3) {
            recyclerView?.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            recyclerView?.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        } else {
            recyclerView?.layoutManager =  GridLayoutManager(context, 5)
            recyclerView?.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                if (appList.size > 5)
                    FrameLayout.LayoutParams.MATCH_PARENT
                else
                    FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (appList.size > 5) {
                    topMargin = recyclerView?.resources?.getDimensionPixelOffset(
                        R.dimen.dp_56
                    ) ?: 56.dp2Px()
                } else {
                    gravity = Gravity.CENTER
                    topMargin = 0
                }
            }
            if (appList.size > 5) {
                val paddingBottom =
                    recyclerView?.resources?.getDimensionPixelSize(R.dimen.dp_32)
                recyclerView?.paddingBottom != paddingBottom
                recyclerView?.setPadding(
                    recyclerView?.paddingLeft ?: 0,
                    recyclerView?.paddingTop ?: 0,
                    recyclerView?.paddingRight ?: 0,
                    paddingBottom ?: 0
                )
            } else {
                recyclerView?.paddingBottom != 0
                recyclerView?.setPadding(
                    recyclerView?.paddingLeft ?: 0,
                    recyclerView?.paddingTop ?: 0,
                    recyclerView?.paddingRight ?: 0,
                    0
                )
            }
        }
    }

    override fun onSupportVisible() {
        super.onSupportVisible()

        clickCount = 0
    }

    private fun applyTheme(isDark: Boolean) {
        val tintColor = if (isDark) Color.WHITE else Color.BLACK
        ivBack?.setColorFilter(tintColor)
        adapter.titleTextColor = tintColor
        if (adapter.itemCount > 0) {
            adapter.notifyDataSetChanged()
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

    private class TemplateAppAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        companion object {
            const val HORIZONTAL = 0
            const val VERTICAL = 1
        }

        var onItemClickListener: OnItemClickListener? = null
        private val appList = mutableListOf<SourceItem>()

        var titleTextColor: Int? = null

        fun setAppList(appList: List<SourceItem>) {
            this.appList.clear()
            this.appList.addAll(appList)
        }

        fun appendList(appList: List<SourceItem>) {
            this.appList.addAll(appList)
        }

        override fun getItemViewType(position: Int): Int {
            return if (itemCount < 3) {
                HORIZONTAL
            } else {
                VERTICAL
            }
        }

        fun getAppItem(position: Int) = appList[position]

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val holder =
                when (viewType) {
                    HORIZONTAL -> TemplateAppViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(R.layout.item_template_app_2_horizontal, parent, false)
                    )
                    else -> TemplateAppViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(R.layout.item_template_app_2_vertical, parent, false)
                    )
                }
            holder.itemView.findViewById<View>(R.id.clickable_view).clickWithTrigger {
                onItemClickListener?.onItemClick(parent, holder.itemView, holder.adapterPosition)
            }
            return holder
        }

        override fun getItemCount(): Int {
            return appList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is TemplateAppViewHolder) {
                val app = appList[position]

                holder.tvIndex?.text = (position + 1).toString()

                if (!app.cover.isNullOrEmpty()) {
                    try {
                        Uri.parse(app.cover)?.let { uri ->
                            holder.ivImage?.let { imageView ->
                                val transformer = MultiTransformation(
                                    CenterCrop(),
                                    RoundedCornersTransformation(
                                        imageView.context.resources.getDimensionPixelSize(R.dimen.dp_6),
                                        0
                                    )
                                )

                                Glide.with(imageView)
                                    .load(uri)
                                    .placeholder(R.drawable.bg_default_template_app_2)
                                    .error(R.drawable.bg_default_template_app_2)
                                    .transform(transformer)
                                    .transition(
                                        DrawableTransitionOptions.with(
                                            DrawableCrossFadeFactory.Builder()
                                                .setCrossFadeEnabled(true).build()
                                        )
                                    )
                                    .into(imageView)
                            }
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        holder.ivImage?.setImageResource(R.drawable.bg_default_template_app_2)
                    }
                } else {
                    holder.ivImage?.setImageResource(R.drawable.bg_default_template_app_2)
                }

                holder.tvName?.text = app.title
                titleTextColor?.let { textColor ->
                    holder.tvName?.setTextColor(textColor)
                }
            }
        }

    }

    private class TemplateAppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvName: TextView? = itemView.findViewById(R.id.name)
        var tvIndex: TextView? = itemView.findViewById(R.id.tv_index)
        var ivImage: ImageView? = itemView.findViewById(R.id.image)
    }
}