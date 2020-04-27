package com.iflytek.cyber.iot.show.core.adapter

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.drakeet.multitype.ItemViewBinder
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.Banner
import com.iflytek.cyber.iot.show.core.model.ContentStorage
import com.iflytek.cyber.iot.show.core.model.MainTemplate
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.widget.BannerDescTextView
import com.iflytek.cyber.iot.show.core.widget.FadeInPageTransformer
import com.iflytek.cyber.iot.show.core.widget.InterceptRecyclerView
import com.iflytek.cyber.iot.show.core.widget.ShadowFrameLayout
import org.greenrobot.eventbus.EventBus
import kotlin.math.abs

class MainViewBinder : ItemViewBinder<MainTemplate, MainViewBinder.MainViewHolder>() {

    data class ScrollAction(val action: Int)

    interface MainViewItemClickListener {
        fun onIqiyiFrameClick()

        fun onFavFrameClick()

        fun onMusicFrameClick()

        fun onBannerItemClick(banner: Banner)
    }

    private var isViewDetached = false

    private var recyclerView: InterceptRecyclerView? = null

    private var mainViewItemClickListener: MainViewItemClickListener? = null

    private var rotateAnimation: RotateAnimation? = null
    private var linearInterpolator = LinearInterpolator()

    fun setMainViewItemClickListener(mainViewItemClickListener: MainViewItemClickListener) {
        this.mainViewItemClickListener = mainViewItemClickListener
    }

    fun setRecyclerView(recyclerView: InterceptRecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): MainViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_main, parent, false)
        val holder = MainViewHolder(view)
        return holder
    }

    fun updateMusicCard(recyclerView: RecyclerView) {
        if (recyclerView.childCount == 0) {
            return
        }
        val mainItem = recyclerView.layoutManager?.findViewByPosition(0) as? ViewGroup
        if (mainItem == null || mainItem.id != R.id.main_item) {
            return
        }
        val card = mainItem.getChildAt(mainItem.childCount - 1) as? ViewGroup
        val artistTextView = card?.findViewById<AppCompatTextView>(R.id.artist_text)
        val albumImage = card?.findViewById<ImageView>(R.id.album_image)
        val playingImage = card?.findViewById<LottieAnimationView>(R.id.playing_image)
        val musicContent = card?.findViewById<FrameLayout>(R.id.music_content)

        val playerInfo = ContentStorage.get().playerInfo
        if (playerInfo == null) {
            artistTextView?.text = "未在播放"
        } else {
            artistTextView?.text = playerInfo.content?.musicTitle
        }

        val transformer: MultiTransformation<Bitmap> = MultiTransformation(
            CenterCrop(),
            RoundedCornersTransformation(
                recyclerView.context.resources.getDimensionPixelSize(R.dimen.dp_16), 0
            )
        )
        if (albumImage != null) {
            Glide.with(recyclerView.context)
                .load(playerInfo?.content?.imageUrl)
                .transform(transformer)
                .placeholder(R.drawable.ic_default_album_cover)
                .into(albumImage)
        }

        val pivot = recyclerView.context.resources.getDimensionPixelSize(R.dimen.dp_86) / 2f
        if (rotateAnimation == null) {
            rotateAnimation = RotateAnimation(0f, 360f, pivot, pivot)
            rotateAnimation?.interpolator = linearInterpolator
            rotateAnimation?.repeatCount = Animation.INFINITE
            rotateAnimation?.duration = 10 * 1000
            musicContent?.animation = rotateAnimation
        }

        if (ContentStorage.get().isMusicPlaying) {
            playingImage?.playAnimation()
            musicContent?.startAnimation(rotateAnimation)
        } else {
            playingImage?.pauseAnimation()
            rotateAnimation?.cancel()
        }
    }

    fun requestScroll(recyclerView: RecyclerView) {
        if (recyclerView.childCount == 0 || isViewDetached) {
            return
        }

        val mainItem = recyclerView.layoutManager?.findViewByPosition(0) ?: return
        mainItem as ViewGroup
        if (mainItem.childCount == 0) {
            return
        }

        val viewPager = mainItem.getChildAt(0) as ViewPager2

        val state = viewPager.scrollState
        if (state == ViewPager2.SCROLL_STATE_IDLE) {
            val animator = ValueAnimator.ofFloat(0f, 1f)
            animator.addUpdateListener(object :
                ValueAnimator.AnimatorUpdateListener {
                private var cacheValue = 0f
                override fun onAnimationUpdate(animation: ValueAnimator) {
                    val value = animation.animatedValue as Float

                    viewPager.let { viewPager ->
                        if (viewPager.isFakeDragging && viewPager.isAttachedToWindow)
                            viewPager.fakeDragBy(-viewPager.width * abs(value - cacheValue))
                    }

                    cacheValue = value
                }
            })
            animator.duration = 2000
            animator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    viewPager.endFakeDrag()
                    if (viewPager.tag == animation) {
                        viewPager.tag = null
                    }
                }

                override fun onAnimationCancel(animation: Animator?) {
                    viewPager.endFakeDrag()
                    if (viewPager.tag == animation) {
                        viewPager.tag = null
                    }
                }

                override fun onAnimationStart(animation: Animator?) {
                    viewPager.beginFakeDrag()
                }
            })
            animator.start()

            viewPager.tag = animator
        }
    }

    fun getViewPager2(recyclerView: RecyclerView): ViewPager2? {
        if (recyclerView.childCount == 0 || isViewDetached) {
            return null
        }

        val mainItem = recyclerView.layoutManager?.findViewByPosition(0) ?: return null
        mainItem as ViewGroup
        if (mainItem.childCount == 0) {
            return null
        }

        return mainItem.getChildAt(0) as? ViewPager2
    }

    fun getBannerCount(recyclerView: RecyclerView): Int {
        if (recyclerView.childCount == 0 || isViewDetached) {
            return 0
        }

        val mainViewHolder = recyclerView.findViewHolderForLayoutPosition(0) as MainViewHolder
        return mainViewHolder.adapter.itemCount
    }

    override fun onBindViewHolder(holder: MainViewHolder, item: MainTemplate) {
        holder.videoFrame.setOnClickListener {
            mainViewItemClickListener?.onIqiyiFrameClick()
        }

        holder.favImage.setOnClickListener {
            mainViewItemClickListener?.onFavFrameClick()
        }

        holder.musicImage.setOnClickListener {
            mainViewItemClickListener?.onMusicFrameClick()
        }

        holder.setupViewPager(item.data)

        val playerInfo = ContentStorage.get().playerInfo
        if (playerInfo == null) {
            holder.artistTextView.text = "未在播放"
        } else {
            holder.artistTextView.text = playerInfo.content?.musicTitle
        }
        if (ContentStorage.get().isMusicPlaying) {
            holder.playingImage.playAnimation()
        } else {
            holder.playingImage.pauseAnimation()
        }
    }

    override fun onViewAttachedToWindow(holder: MainViewHolder) {
        super.onViewAttachedToWindow(holder)
        val context = holder.itemView.context
        Glide.with(context)
            .load(R.drawable.img_bg_card_iqiyi)
            .transform(holder.transformer)
            .into(holder.iqiyiImage)

        Glide.with(context)
            .load(R.drawable.img_bg_card_my_collection)
            .transform(holder.transformer)
            .into(holder.favImage)

        isViewDetached = false
        EventBus.getDefault().post(ScrollAction(2))
    }

    override fun onViewDetachedFromWindow(holder: MainViewHolder) {
        super.onViewDetachedFromWindow(holder)
        isViewDetached = true
    }

    inner class MainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val viewPager = itemView.findViewById<ViewPager2>(R.id.view_pager)
        val iqiyiImage = itemView.findViewById<ImageView>(R.id.iqiyi_image)
        val favImage = itemView.findViewById<ImageView>(R.id.fav_image)
        val musicImage = itemView.findViewById<ImageView>(R.id.music_image)
        val artistTextView = itemView.findViewById<AppCompatTextView>(R.id.artist_text)
        val currentPageTextView = itemView.findViewById<AppCompatTextView>(R.id.current_page_text)
        val pageCountTextView = itemView.findViewById<AppCompatTextView>(R.id.page_count_text)
        val playingImage = itemView.findViewById<LottieAnimationView>(R.id.playing_image)
        val videoFrame = itemView.findViewById<ShadowFrameLayout>(R.id.video_frame)
        val favFrame = itemView.findViewById<ShadowFrameLayout>(R.id.fav_frame)

        val adapter = BannerAdapter()

        val transformer: MultiTransformation<Bitmap> = MultiTransformation(
            CenterCrop(),
            RoundedCornersTransformation(
                itemView.context.resources.getDimensionPixelSize(R.dimen.dp_16), 0
            )
        )

        fun setupViewPager(banners: List<Banner>) {
            adapter.setItems(banners)
            viewPager.adapter = adapter

            currentPageTextView.text = (viewPager.currentItem + 1).toString()
            pageCountTextView.text = "/${banners.size}"

            if (viewPager.isFakeDragging) {
                viewPager.endFakeDrag()
            }
            viewPager.setCurrentItem(1, false)

            EventBus.getDefault().post(ScrollAction(0))
        }

        private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    val position = viewPager?.currentItem

                    // 无限滚动
                    if (position == 0) {
                        viewPager?.setCurrentItem((adapter.itemCount) - 2, false)
                    } else if (position == (adapter.itemCount) - 1) {
                        viewPager?.setCurrentItem(1, false)
                    }

                    currentPageTextView.text = viewPager.currentItem.toString()
                }

                EventBus.getDefault().post(ScrollAction(0))
            }
        }

        init {
            viewPager.setPageTransformer(FadeInPageTransformer())
            viewPager.registerOnPageChangeCallback(pageChangeCallback)
        }
    }

    inner class BannerAdapter : RecyclerView.Adapter<BannerAdapter.BannerHolder>() {

        private val items = ArrayList<Banner>()

        fun setItems(items: List<Banner>) {
            this.items.clear()
            this.items.addAll(items)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_video_banner, parent, false)
            return BannerHolder(view)
        }

        override fun getItemCount(): Int {
            return if (items.isNotEmpty()) {
                items.size + 2
            } else {
                0
            }
        }

        override fun onBindViewHolder(holder: BannerHolder, position: Int) {
            val context = holder.itemView.context
            val index: Int = when (position) {
                0 -> items.size - 1
                itemCount - 1 -> 0
                else -> position - 1
            }

            val item = items[index]
            holder.mediaTitleTextView.text = item.title
            if (item.descriptions?.size ?: 0 > 0) {
                holder.mediaDescTextView.setDescText(item.descriptions?.get(0))
                holder.mediaDescTextView.setDescs(item.descriptions?.toCollection(ArrayList()))
            } else {
                holder.mediaDescTextView.text = null
            }

            val transformer: MultiTransformation<Bitmap> = MultiTransformation(
                CenterCrop(),
                RoundedCornersTransformation(
                    context.resources.getDimensionPixelSize(R.dimen.dp_16), 0
                )
            )

            Glide.with(context)
                .load(item.cover)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transition(
                    DrawableTransitionOptions.with(
                        DrawableCrossFadeFactory.Builder()
                            .setCrossFadeEnabled(true).build()
                    )
                )
                .transform(transformer)
                .into(holder.mediaImage)

            holder.itemView.setOnClickListener {
                mainViewItemClickListener?.onBannerItemClick(item)
            }
        }

        override fun onViewDetachedFromWindow(holder: BannerHolder) {
            super.onViewDetachedFromWindow(holder)
            holder.mediaDescTextView.stopScroll()
        }

        inner class BannerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val mediaImage = itemView.findViewById<ImageView>(R.id.media_image)
            val mediaTitleTextView = itemView.findViewById<AppCompatTextView>(R.id.media_title)
            val mediaDescTextView = itemView.findViewById<BannerDescTextView>(R.id.media_desc)
        }
    }
}