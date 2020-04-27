package com.iflytek.cyber.iot.show.core.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.os.postDelayed
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.alibaba.fastjson.JSONException
import com.alibaba.fastjson.JSONObject
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.gson.Gson
import com.iflytek.cyber.evs.sdk.RequestCallback
import com.iflytek.cyber.evs.sdk.agent.AudioPlayer
import com.iflytek.cyber.evs.sdk.agent.PlaybackController
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.evs.sdk.focus.VisualFocusManager
import com.iflytek.cyber.evs.sdk.socket.Result
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.EngineService
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.accessibility.TouchAccessibility
import com.iflytek.cyber.iot.show.core.adapter.RecommendMediaAdapter
import com.iflytek.cyber.iot.show.core.adapter.SongsAdapter
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.impl.audioplayer.EvsAudioPlayer
import com.iflytek.cyber.iot.show.core.impl.template.EvsTemplate
import com.iflytek.cyber.iot.show.core.model.*
import com.iflytek.cyber.iot.show.core.recommend.RecommendAgent
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.utils.ScreenUtils
import com.iflytek.cyber.iot.show.core.utils.clickWithTrigger
import com.iflytek.cyber.iot.show.core.widget.StyledQRCodeDialog
import com.iflytek.cyber.iot.show.core.widget.TouchNestedScrollView
import com.iflytek.cyber.iot.show.core.widget.lrc.LrcView
import com.makeramen.roundedimageview.RoundedImageView
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import jp.wasabeef.blurry.Blurry
import kotlinx.android.synthetic.main.fragment_search.*
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.UnknownHostException
import java.util.*

class PlayerInfoFragment2 : BaseFragment(), View.OnClickListener,
        AudioPlayer.MediaStateChangedListener, PageScrollable {

    companion object {
        private const val MAX_RETRY_COUNT = 5
        private var toolbarHeight = 0

        private var lastPlayResourceId: String? = ""
        private var lastRecommendAudioList: List<MediaEntity>? = null
    }

    private lateinit var drawer: DrawerLayout
    private lateinit var musicList: RecyclerView
    private lateinit var listContent: LinearLayout
    private lateinit var playPrevious: ImageView
    private lateinit var playNext: ImageView
    private lateinit var playPause: ImageView
    private lateinit var musicCover: ImageView
    private lateinit var musicTitle: TextView
    private lateinit var musicArtist: TextView
    private lateinit var toolBar: RelativeLayout
    private lateinit var ivLogo: ImageView
    private lateinit var ivBlurCover: ImageView
    private lateinit var lrcView: LrcView
    private lateinit var mainContent: RelativeLayout
    private lateinit var lyricContent: FrameLayout
    private lateinit var screenLyricView: LrcView
    private lateinit var lyricClickContent: FrameLayout
    private lateinit var lrcLoading: LottieAnimationView
    private lateinit var tvLyricError: TextView
    private lateinit var seekBar: IndicatorSeekBar
    private lateinit var tvOnlyTitle: TextView
    private lateinit var ivPlayList: ImageView
    private lateinit var tvPosition: TextView
    private lateinit var tvDuration: TextView

    /** 推荐相关 */
    private lateinit var smallControl: RelativeLayout
    private lateinit var scrollView: TouchNestedScrollView
    private lateinit var musicLayout: RelativeLayout
    private lateinit var recommendLayout: LinearLayout
    private lateinit var recommendAudioView: RecyclerView
    private lateinit var smallCover: RoundedImageView
    private lateinit var smallTitle: TextView
    private lateinit var smallLrc: LrcView
    private lateinit var smallPlayPause: ImageView
    private lateinit var smallPlayNext: ImageView
    private lateinit var smallPlayList: ImageView

    private var seekBarDragging = false
    private var currentPosition = 0L
    private var currentResourceId: String? = ""
    private var isAnimatingScreenLrc = false

    private var retryCount = 0
    private var backCount = 0

    private var songsAdapter: SongsAdapter? = null

    private var handler = Handler(Looper.getMainLooper())
    private var enterInScreenLyricMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcher?.registerCallback(simpleRenderCallback)
        launcher?.getService()?.getAudioPlayer()?.addListener(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_player_info_2, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        drawer = view.findViewById(R.id.drawer)
        listContent = view.findViewById(R.id.list_content)
        seekBar = view.findViewById(R.id.seek_bar)
        musicList = view.findViewById(R.id.music_list)
        mainContent = view.findViewById(R.id.main_content)
        lyricContent = view.findViewById(R.id.lyric_content)
        lyricContent.setOnClickListener(this)
        lrcLoading = view.findViewById(R.id.lyric_loading)
        screenLyricView = view.findViewById(R.id.screen_lyric_view)
        lyricClickContent = view.findViewById(R.id.lyric_click_content)
        lyricClickContent.setOnClickListener(this)
        val ivBack = view.findViewById<View>(R.id.back)
        ivBack.clickWithTrigger {
            launcher?.onBackPressed()
        }
        tvLyricError = view.findViewById(R.id.tv_lyric_error)
        tvLyricError.setOnClickListener(this)
        lrcView = view.findViewById(R.id.lrc_view)
        lrcView.setOnClickListener(this)
        playPrevious = view.findViewById(R.id.iv_previous)
        playPrevious.setOnClickListener(this)
        playNext = view.findViewById(R.id.iv_next)
        playNext.setOnClickListener(this)
        playPause = view.findViewById(R.id.iv_play_pause)
        playPause.setOnClickListener(this)
        ivPlayList = view.findViewById(R.id.iv_play_list)
        ivPlayList.setOnClickListener(this)
        tvOnlyTitle = view.findViewById(R.id.tv_only_title)
        tvPosition = view.findViewById(R.id.tv_position)
        tvDuration = view.findViewById(R.id.tv_duration)

        ivBlurCover = view.findViewById(R.id.iv_cover_blur)
        ivLogo = view.findViewById(R.id.iv_logo)
        musicCover = view.findViewById(R.id.iv_cover)
        musicTitle = view.findViewById(R.id.tv_title)
        musicArtist = view.findViewById(R.id.tv_artist)
        toolBar = view.findViewById(R.id.toolbar)

        smallControl = view.findViewById(R.id.rlyt_small_control)
        scrollView = view.findViewById(R.id.scroll_view)
        musicLayout = view.findViewById(R.id.rlyt_music)
        recommendLayout = view.findViewById(R.id.llyt_recommend)
        recommendAudioView = view.findViewById(R.id.rcyc_recommend)

        smallCover = view.findViewById(R.id.iv_small_cover)
        smallTitle = view.findViewById(R.id.txt_small_title)
        smallLrc = view.findViewById(R.id.small_screen_lyric_view)
        smallPlayPause = view.findViewById(R.id.iv_small_play_pause)
        smallPlayNext = view.findViewById(R.id.iv_small_next)
        smallPlayList = view.findViewById(R.id.iv_small_play_list)

        smallCover.setOnClickListener(this)
        smallTitle.setOnClickListener(this)
        smallPlayPause.setOnClickListener(this)
        smallPlayNext.setOnClickListener(this)
        smallPlayList.setOnClickListener(this)

        scrollView.overScrollMode = ScrollView.OVER_SCROLL_NEVER

        if (launcher?.getService()?.getAudioPlayer()?.playbackState == AudioPlayer.PLAYBACK_STATE_PLAYING) {
            playPause.setImageResource(R.drawable.ic_music_pause)
            smallPlayPause.setImageResource(R.drawable.ic_music_pause)
        } else {
            playPause.setImageResource(R.drawable.ic_music_play)
            smallPlayPause.setImageResource(R.drawable.ic_music_play)
        }

        seekBar.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams) {
                val position = format(seekParams.progress.toLong())
                tvPosition.text = position
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {
                seekBarDragging = true
            }

            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
                seekBarDragging = false
                val audioPlayer = launcher?.getService()?.getAudioPlayer()
                audioPlayer?.seekTo(AudioPlayer.TYPE_PLAYBACK, seekBar.progress.toLong())
            }
        }

        screenLyricView.setDraggable(true, object : LrcView.OnPlayClickListener {
            override fun onPlayClick(time: Long): Boolean {
                val audioPlayer = launcher?.getService()?.getAudioPlayer()
                audioPlayer?.seekTo(AudioPlayer.TYPE_PLAYBACK, time)
                return true
            }

            override fun onContentClick() {
                lyricContent.performClick()
            }
        })

        drawer.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }

            override fun onDrawerClosed(drawerView: View) {
            }

            override fun onDrawerOpened(drawerView: View) {
                autoCloseDrawer() //20秒后自动收缩侧边栏
            }
        })

        musicList.setOnTouchListener { v, event ->
            autoCloseDrawer()
            false
        }

        val playerInfo = ContentStorage.get().playerInfo
        seekBar.isEnabled = playerInfo != null
        val player = launcher?.getService()?.getAudioPlayer()
        seekBar.max = player?.getDuration(AudioPlayer.TYPE_PLAYBACK)?.toFloat() ?: 0f
        tvDuration.text = format(seekBar.max.toLong())
        seekBar.setProgress(player?.getOffset(AudioPlayer.TYPE_PLAYBACK)?.toFloat() ?: 0f)
        currentPosition = seekBar.progress.toLong()
        setupSeekBar()

        currentResourceId = playerInfo?.resourceId

        setupTitleVisible(playerInfo)

        if (seekBar.isEnabled) {
            getLyric()
            view.post {
                loadPlayList()
            }
        }

        tryToLoadRecommend(playerInfo)

        setupMusic(playerInfo)
        setupRecyclerView()
    }

    override fun scrollToPrevious(): Boolean {
        if (EvsAudioPlayer.get(context).playbackResourceId != null) {
            if (playPrevious.isEnabled) {
                playPrevious.performClick()
                return true
            }
        }
        return false
    }

    override fun scrollToNext(): Boolean {
        if (EvsAudioPlayer.get(context).playbackResourceId != null) {
            if (playNext.isEnabled) {
                playNext.performClick()
                return true
            }
        }
        return false
    }

    private fun setupSeekBar() {
        val audioPlayer = EvsAudioPlayer.get(context)
        val mediaType = audioPlayer.getCurrentResourceMediaPlayerType()
        if (audioPlayer.playbackState == AudioPlayer.PLAYBACK_STATE_PLAYING) {
            seekBar.isVisible = mediaType == C.TYPE_OTHER
            tvPosition.isVisible = seekBar.isVisible
            tvDuration.isVisible = seekBar.isVisible
        }
    }

    private fun autoCloseDrawer() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(20000) {
            drawer.closeDrawer(GravityCompat.END)
        }
    }

    private fun setupTitleVisible(playerInfo: PlayerInfoPayload?) {
//        if (playerInfo?.content?.musicArtist.isNullOrEmpty() &&
//            playerInfo?.lyric?.url.isNullOrEmpty()
//        ) {
//            musicTitle.isVisible = false
//            musicArtist.isVisible = false
//            tvOnlyTitle.isVisible = true
//        } else {
            musicTitle.isVisible = true
            musicArtist.isVisible = true
            tvOnlyTitle.isVisible = false
//        }
    }

    private fun setupMusic(playerInfo: PlayerInfoPayload?) {
        val dp4 = musicCover.context.resources.getDimensionPixelSize(R.dimen.dp_4)
        if (playerInfo?.content?.imageUrl.isNullOrEmpty()) {
            ivBlurCover.let { imageView ->
                imageView.post {
                    Blurry.with(imageView.context)
                        .sampling(4)
                        .radius(75)
                        .color(Color.parseColor("#66212121"))
                        .from(getBitmapFromVectorDrawable(R.drawable.default_media_placeholder))
                        .into(imageView)
                }
            }
        }

        val transformer = MultiTransformation(
            CenterCrop(),
            RoundedCornersTransformation(dp4, 0)
        )

        Glide.with(this)
            .asBitmap()
            .load(playerInfo?.content?.imageUrl)
            .placeholder(R.drawable.default_media_placeholder)
            .error(R.drawable.default_media_placeholder)
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    //musicCover.setImageBitmap(resource)
                    if (isRemoving || isDetached)
                        return
                    val context = context ?: return

                    Glide.with(this@PlayerInfoFragment2)
                        .load(playerInfo?.content?.imageUrl)
                        .transform(transformer)
                        .into(musicCover)

                    Blurry.with(context)
                        .sampling(4)
                        .color(Color.parseColor("#66212121"))
                        .from(resource)
                        .into(ivBlurCover)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    if (isRemoving || isDetached)
                        return
                    context ?: return
                    musicCover.setImageResource(R.drawable.default_media_placeholder)
                }
            })
        val dp8 = musicCover.context.resources.getDimensionPixelSize(R.dimen.dp_8)
        Glide.with(this)
            .load(playerInfo?.provider?.logoUrl)
            .apply(
                RequestOptions()
                    .transform(RoundedCornersTransformation(dp8, 0))
            )
            .into(ivLogo)
        musicTitle.text = playerInfo?.content?.musicTitle
        musicArtist.text = playerInfo?.content?.musicArtist
        tvOnlyTitle.text = playerInfo?.content?.musicTitle

        if (TextUtils.isEmpty(musicArtist.text)) {
            musicArtist.text = getString(R.string.unknown)
        }
    }

    private fun setupRecyclerView() {
        songsAdapter = SongsAdapter {
            drawer.closeDrawer(GravityCompat.END)
            playMusic(it)
        }
        musicList.adapter = songsAdapter
    }

    private val simpleRenderCallback = object : EvsTemplate.SimpleRenderCallback() {
        override fun renderPlayerInfo(payload: String) {
            val playerInfo = ContentStorage.get().playerInfo
            if (!mainContent.isVisible) {
                showMainContent()
            }

            seekBar.isEnabled = playerInfo != null

            scrollToHead()

            loadPlayList()
            setupMusic(playerInfo)

            // 尝试加载推荐内容
            tryToLoadRecommend(playerInfo)

            setupTitleVisible(playerInfo)
            getLyric()

            songsAdapter?.notifyDataSetChanged()
            currentResourceId = playerInfo?.resourceId
            currentPosition = 0L
        }

        override fun exitPlayerInfo() {
            super.exitPlayerInfo()

            if (isSupportVisible) {
                pop()
            }
        }
    }

    /**
     * 第一首、最后一首是否可以点击
     */
    private fun updatePlayButton(isListLoop: Boolean) {
        val playerInfo = ContentStorage.get().playerInfo
        var firstPlayingId: String? = ""
        var endPlayingId: String? = ""
        if (!songsAdapter?.songList.isNullOrEmpty() && !isListLoop) {
            firstPlayingId = songsAdapter?.songList?.get(0)?.stream?.token
        }
        if (!songsAdapter?.songList.isNullOrEmpty() && !isListLoop) {
            endPlayingId =
                songsAdapter?.songList?.get(songsAdapter?.songList!!.size - 1)?.stream?.token
        }

        if (TextUtils.equals(playerInfo?.resourceId, firstPlayingId)) {
            playPrevious.alpha = 0.5f
            playPrevious.isEnabled = false
        } else {
            playPrevious.alpha = 1f
            playPrevious.isEnabled = true
        }
        if (TextUtils.equals(playerInfo?.resourceId, endPlayingId)) {
            playNext.alpha = 0.5f
            playNext.isEnabled = false
            smallPlayNext.alpha = 0.5f
            smallPlayNext.isEnabled = false
        } else {
            playNext.alpha = 1f
            playNext.isEnabled = true
            smallPlayNext.alpha = 1f
            smallPlayNext.isEnabled = true
        }

        if (context != null) {
            val intent = Intent(context, FloatingService::class.java).apply {
                action = FloatingService.ACTION_UPDATE_MUSIC_CONTROL_BUTTON
                putExtra("firstPlayId", firstPlayingId)
                putExtra("endPlayId", endPlayingId)
            }
            context?.startService(intent)
        }
    }

    private fun scrollToCurrentPosition() {
        var position = -1
        songsAdapter?.songList?.forEachIndexed { index, song ->
            val playingId = ContentStorage.get().playerInfo?.resourceId
            if (TextUtils.equals(playingId, song.stream.token)) {
                position = index
                return@forEachIndexed
            }
        }
        if (position != -1) {
            musicList.scrollToPosition(position)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_play_list, R.id.iv_small_play_list -> {
                scrollToCurrentPosition()
                if (drawer.isDrawerOpen(GravityCompat.END)) {
                    drawer.closeDrawer(GravityCompat.END)
                } else {
                    drawer.openDrawer(GravityCompat.END)
                }
            }
            R.id.iv_next, R.id.iv_small_next -> {
                val playback = launcher?.getService()?.getPlaybackController()
                playback?.sendCommand(PlaybackController.Command.Next, object :
                    RequestCallback {
                    override fun onResult(result: Result) {
                        if (!result.isSuccessful) {
                            val intent = Intent(EngineService.ACTION_SEND_REQUEST_FAILED)
                            intent.putExtra(EngineService.EXTRA_RESULT, result)
                            context?.sendBroadcast(intent)
                        }
                    }
                })
            }
            R.id.iv_play_pause, R.id.iv_small_play_pause -> {
                val audioPlayer = EvsAudioPlayer.get(context)
                if (audioPlayer.playbackResourceId.isNullOrEmpty() || audioPlayer.getPlayerPlaybackState(
                        AudioPlayer.TYPE_PLAYBACK
                    ) == Player.STATE_IDLE
                ) {
                    val playback = launcher?.getService()?.getPlaybackController()
                    playback?.sendCommand(PlaybackController.Command.Resume, object :
                        RequestCallback {
                        override fun onResult(result: Result) {
                            if (!result.isSuccessful) {
                                val intent = Intent(EngineService.ACTION_SEND_REQUEST_FAILED)
                                intent.putExtra(EngineService.EXTRA_RESULT, result)
                                context?.sendBroadcast(intent)
                            }
                        }
                    })
                } else {
                    if (audioPlayer.playbackState == AudioPlayer.PLAYBACK_STATE_PLAYING) {
                        audioPlayer.pause(AudioPlayer.TYPE_PLAYBACK)
                    } else {
                        audioPlayer.resume(AudioPlayer.TYPE_PLAYBACK)
                    }
                }
            }
            R.id.iv_previous -> {
                val playback = launcher?.getService()?.getPlaybackController()
                playback?.sendCommand(PlaybackController.Command.Previous, object :
                    RequestCallback {
                    override fun onResult(result: Result) {
                        if (!result.isSuccessful) {
                            val intent = Intent(EngineService.ACTION_SEND_REQUEST_FAILED)
                            intent.putExtra(EngineService.EXTRA_RESULT, result)
                            context?.sendBroadcast(intent)
                        }
                    }
                })
            }
            R.id.lyric_click_content -> {
                if (lrcView.hasLrc()) {
                    showScreenLyric()
                }
            }
            R.id.lyric_content -> {
                showMainContent()
            }
            R.id.tv_lyric_error -> {
                getLyric()
            }
            R.id.iv_small_cover, R.id.txt_small_title -> {
                scrollToHead()
            }
        }
    }

    private fun format(duration: Long): String {
        return String.format(
            Locale.getDefault(),
            "%2d:%02d",
            duration / 1000 / 60,
            duration / 1000 % 60
        )
    }

    override fun onStarted(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            songsAdapter?.notifyDataSetChanged()
            playPause.setImageResource(R.drawable.ic_music_pause)
            smallPlayPause.setImageResource(R.drawable.ic_music_pause)
            seekBar.max = player.getDuration(type).toFloat()
            if (seekBar.max > 0) {
                tvDuration.text = format(seekBar.max.toLong())
            }
            setupSeekBar()
        }
    }

    override fun onResumed(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            playPause.setImageResource(R.drawable.ic_music_pause)
            smallPlayPause.setImageResource(R.drawable.ic_music_pause)
            songsAdapter?.notifyDataSetChanged()
        }
    }

    override fun onPaused(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            playPause.setImageResource(R.drawable.ic_music_play)
            smallPlayPause.setImageResource(R.drawable.ic_music_play)
            songsAdapter?.notifyDataSetChanged()
        }
    }

    override fun onStopped(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            playPause.setImageResource(R.drawable.ic_music_play)
            smallPlayPause.setImageResource(R.drawable.ic_music_play)
            songsAdapter?.notifyDataSetChanged()
        }
    }

    override fun onCompleted(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            playPause.setImageResource(R.drawable.ic_music_play)
            smallPlayPause.setImageResource(R.drawable.ic_music_play)
            songsAdapter?.notifyDataSetChanged()
        }
    }

    override fun onPositionUpdated(
        player: AudioPlayer,
        type: String,
        resourceId: String,
        position: Long
    ) {
        if (type == AudioPlayer.TYPE_PLAYBACK && !seekBarDragging) {
            if (seekBar.max <= 100f) {
                seekBar.max = player.getDuration(type).toFloat()
            }
            if (currentPosition <= position) {
                lrcView.updateTime(position)
                smallLrc.updateTime(position)
                screenLyricView.updateTime(position)
                seekBar.setProgress(position.toFloat())
                currentPosition = position
            }
            if (currentPosition <= position && position in 0L..999) {
                setupSeekBar()
                tvDuration.text = format(seekBar.max.toLong())
                //showMusicPress()
                playPause.setImageResource(R.drawable.ic_music_pause)
                smallPlayPause.setImageResource(R.drawable.ic_music_pause)
            } else if (position >= 5000) {
                //hideMusicPress()
                currentPosition = position
            }
        }
    }

    override fun onError(player: AudioPlayer, type: String, resourceId: String, errorCode: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            playPause.setImageResource(R.drawable.ic_music_play)
            smallPlayPause.setImageResource(R.drawable.ic_music_play)
        }
    }

    private fun getBitmapFromVectorDrawable(drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(musicCover.context, drawableId)
        val bitmap = Bitmap.createBitmap(
            drawable!!.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun loadPlayList() {
        getMediaApi()?.getPlayList("audio")?.enqueue(object : Callback<PlayList> {
            override fun onResponse(call: Call<PlayList>, response: Response<PlayList>) {
                if (response.isSuccessful) {
                    val songList = response.body()
                    songList?.let {
                        songsAdapter?.songList?.clear()
                        if (it.playlist != null) {
                            var hasCurrentResourceId = false
                            it.playlist.map { song ->
                                if (song.stream.token == currentResourceId) {
                                    hasCurrentResourceId = true
                                }
                            }
                            if (!hasCurrentResourceId) {
                                if (retryCount < MAX_RETRY_COUNT) {
                                    ivPlayList.postDelayed({
                                        loadPlayList()
                                    }, 1000)
                                    retryCount++
                                }
                            } else {
                                retryCount = 0
                            }
                            songsAdapter?.songList?.addAll(it.playlist)

                            if (it.playlist.isEmpty()) {
                                ivPlayList.isVisible = false
                                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                            } else {
                                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                                ivPlayList.isVisible = true
                            }
                        }

                        songsAdapter?.notifyDataSetChanged()

                        updatePlayButton(it.listLoop == true)

                        ivPlayList.isVisible = !songList.playlist.isNullOrEmpty()

                        // 当打开推荐内容时，要打开播放列表
                        if (isNextPlayRecommend) {
                            scrollToCurrentPosition()
                            if (!drawer.isDrawerOpen(GravityCompat.END)) {
                                drawer.openDrawer(GravityCompat.END)
                            }
                        }
                    } ?: run {
                        ivPlayList.isVisible = false
                    }
                } else {
                    ivPlayList.isVisible = false
                }

                isNextPlayRecommend = false
            }

            override fun onFailure(call: Call<PlayList>, t: Throwable) {
                t.printStackTrace()

                ivPlayList.isVisible = false

                if (t is UnknownHostException) {
                    val intent = Intent(EngineService.ACTION_SEND_REQUEST_FAILED)
                    intent.putExtra(
                        EngineService.EXTRA_RESULT,
                        Result(Result.CODE_DISCONNECTED, null)
                    )
                    context?.sendBroadcast(intent)
                }

                isNextPlayRecommend = false
            }
        })
    }

    private fun playMusic(song: Song) {
        val body = MusicBody(null, song.stream.token, song.stream)
        getMediaApi()?.playMusic(body)?.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (!response.isSuccessful) {
                    showError(response.errorBody()?.string())
                } else {
                    songsAdapter?.notifyDataSetChanged()
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                t.printStackTrace()

                if (t is UnknownHostException) {
                    val intent = Intent(EngineService.ACTION_SEND_REQUEST_FAILED)
                    intent.putExtra(
                        EngineService.EXTRA_RESULT,
                        Result(Result.CODE_DISCONNECTED, null)
                    )
                    context?.sendBroadcast(intent)
                }
            }
        })
    }

    private fun setNoRecommend() {
        if (!isAdded || context == null) {
            return
        }
        if (toolbarHeight <= 0f) {
            toolBar.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    toolbarHeight = toolBar.height
                    toolBar.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val screenHeight = ScreenUtils.getHeight(launcher!!)

                    smallControl.visibility = View.GONE
                    val layoutParams = LinearLayout.LayoutParams(musicLayout.layoutParams)
                    layoutParams.height = screenHeight - toolbarHeight
                    musicLayout.layoutParams = layoutParams
                    recommendLayout.visibility = View.GONE

                    musicLayout.invalidate()
                }
            })
        } else {
            val screenHeight = ScreenUtils.getHeight(launcher!!)

            smallControl.visibility = View.GONE
            val layoutParams = LinearLayout.LayoutParams(musicLayout.layoutParams)
            layoutParams.height = screenHeight - toolbarHeight
            musicLayout.layoutParams = layoutParams
            recommendLayout.visibility = View.GONE

            musicLayout.invalidate()
        }
    }

    private fun scrollToHead() {
        scrollView.smoothScrollTo(0, 0)
    }

    private var isNextPlayRecommend = false
    private var isRecommendItemClickable = false

    private fun playRecommend(music: MediaEntity) {
        val client = CoreApplication.from(context!!).getClient()
        if (client != null) {
            Log.d("playRecommendMusic", "${music.url}")

            val request = Request.Builder().get()
                    .url(music.url!!)
                    .build()
            val call = client.newCall(request)
            try {
                val response = call.execute()
                if (response.isSuccessful) {
    //                    isNextPlayRecommend = true

                    Log.d("playRecommendMusic", "success")
                } else {
                    isNextPlayRecommend = false

                    try {
                        val result = JSONObject.parseObject(response.body()?.string())
                        val message = result.getString("message")

                        Log.d("playRecommendMusic", "failed, result=$result")

                        post {
                            Toast.makeText(context!!, message, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: JSONException) {
                        post {
                            Toast.makeText(context!!, R.string.play_recommend_fail, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e1: Exception) {
                e1.printStackTrace()

                isNextPlayRecommend = false

                post {
                    Toast.makeText(context!!, R.string.play_recommend_fail, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getSmallTitleText(playerInfo: PlayerInfoPayload): String {
        val sb = StringBuilder()
        if (!playerInfo.content?.musicTitle.isNullOrEmpty()) {
            sb.append(playerInfo.content?.musicTitle)
        }

        if (!playerInfo.content?.musicArtist.isNullOrEmpty()) {
            sb.append("-").append(playerInfo.content?.musicArtist)
        }

        return sb.toString()
    }

    private var curScrollY = 0

    private fun tryToLoadRecommend(playerInfo: PlayerInfoPayload?) {
        if (!isAdded || context == null) {
            return
        }
        if (playerInfo?.recommend?.url.isNullOrEmpty()) {
            setNoRecommend()
            return
        }

        Thread {
            val resourceId = playerInfo?.resourceId
            val audioList: List<MediaEntity>? = if (lastRecommendAudioList != null) {
                if (resourceId == lastPlayResourceId) {
                    lastRecommendAudioList
                } else {
                    RecommendAgent.getRecommendList(context!!,
                            playerInfo?.recommend?.url!!,
                            MediaEntity::class.java)
                }
            } else {
                RecommendAgent.getRecommendList(context!!,
                                playerInfo?.recommend?.url!!,
                                MediaEntity::class.java)
            }

            lastRecommendAudioList = audioList
            lastPlayResourceId = resourceId

            post {
                if (audioList.isNullOrEmpty()) {
                    setNoRecommend()
                } else {
                    // 显示推荐
                    val playerInfo = ContentStorage.get().playerInfo
                    Glide.with(launcher!!)
                            .load(playerInfo?.content?.imageUrl)
                            .placeholder(R.drawable.default_media_placeholder)
                            .error(R.drawable.default_media_placeholder)
                            .into(smallCover)

                    smallTitle.text = getSmallTitleText(playerInfo!!)

                    val screenHeight = ScreenUtils.getHeight(launcher!!)
                    toolbarHeight = toolBar.height

                    val layoutParams = LinearLayout.LayoutParams(musicLayout.layoutParams)
                    layoutParams.height = screenHeight - toolbarHeight -
                            launcher!!.resources.getDimensionPixelSize(R.dimen.dp_80)
                    musicLayout.layoutParams = layoutParams
                    recommendLayout.visibility = View.VISIBLE

                    val adapter = RecommendMediaAdapter()
                    val audio = audioList[0]

                    if (audio.image.isNullOrEmpty()) {
                        adapter.setColsType(RecommendMediaAdapter.ColsType.THREE_COLS)
                    } else {
                        adapter.setColsType(RecommendMediaAdapter.ColsType.FIVE_COLS)
                    }
                    adapter.setItems(audioList)

                    val layoutManager = GridLayoutManager(launcher!!, 5)
                    layoutManager.orientation = RecyclerView.VERTICAL

                    recommendAudioView.layoutManager = layoutManager
                    recommendAudioView.adapter = adapter
                    recommendAudioView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                    recommendAudioView.isNestedScrollingEnabled = false
                    recommendAudioView.minimumHeight = screenHeight - toolbarHeight -
                            launcher!!.resources.getDimensionPixelSize(R.dimen.dp_60)

                    adapter.setOnItemClickListener(object : RecommendMediaAdapter
                        .OnItemClickListener {

                        override fun onItemClicked(view: View, position: Int) {
                            if (isRecommendItemClickable) {
                                val music = (recommendAudioView.adapter as RecommendMediaAdapter)
                                        .getItem(position)
                                if (music != null) {
                                    Thread {
                                        playRecommend(music)
                                    }.start()
                                }
                            }
                        }
                    })

                    adapter.notifyDataSetChanged()

                    val musicLayoutHeight = layoutParams.height
                    val scrollLimit = musicLayoutHeight * 2 / 3
                    val clickableLimit = launcher!!.resources.getDimensionPixelSize(R.dimen.dp_130)

//                    scrollView.setOnTouchListener { v, event ->
//                        when (event.action) {
//                            MotionEvent.ACTION_DOWN -> {
//                                Log.d("testtest", "ACTION_DOWN")
//
//                                handler.removeCallbacksAndMessages(null)
//                            }
//                            MotionEvent.ACTION_UP -> {
//                                Log.d("testtest", "ACTION_UP")
//
//                                if (!enterInScreenLyricMode && curScrollY == 0) {
//                                    handler.removeCallbacksAndMessages(null)
//                                    handler.postDelayed(autoRunnable, 5000)
//                                }
//                            }
//                        }
//
//                        false
//                    }

                    scrollView.setOnCustomTouchListener(object: TouchNestedScrollView.OnCustomTouchListener{
                        override fun onTouchEvent(event: MotionEvent?) {
                            when (event?.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    handler.removeCallbacksAndMessages(null)
                                }
                                MotionEvent.ACTION_UP -> {
                                    if (!enterInScreenLyricMode && curScrollY == 0) {
                                        handler.removeCallbacksAndMessages(null)
                                        handler.postDelayed(autoRunnable, 5000)
                                    }
                                }
                            }
                        }
                    })

                    scrollView.setOnScrollChangeListener { v: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
                        curScrollY = scrollY

                        if (!enterInScreenLyricMode) {
                            handler.removeCallbacksAndMessages(null)
                        }

                        if (scrollY == 0) {
                            if (!enterInScreenLyricMode) {
                                handler.removeCallbacksAndMessages(null)
                                handler.postDelayed(autoRunnable, 5000)
                            }
                        }

                        isRecommendItemClickable = scrollY > clickableLimit

                        if (scrollY >= scrollLimit) {
                            if (scrollY <= musicLayoutHeight) {
                                if (smallControl.visibility == View.GONE) {
                                    smallControl.visibility = View.VISIBLE
                                    smallControl.alpha = 0.0f

                                    ivLogo.visibility = View.GONE

                                    smallCover.isClickable = false
                                    smallTitle.isClickable = false
                                    smallPlayPause.isClickable = false
                                    smallPlayNext.isClickable = false
                                    smallPlayList.isClickable = false
                                }

                                var alpha = (scrollY - scrollLimit) / ((musicLayoutHeight - scrollLimit).toFloat())
                                if (alpha > 0.9f) {
                                    alpha = 1.0f
                                } else if (alpha < 0.1) {
                                    alpha = 0.0f
                                }

                                smallControl.alpha = alpha

                                if (alpha == 1.0f && !smallCover.isClickable) {
                                    smallCover.isClickable = true
                                    smallTitle.isClickable = true
                                    smallPlayNext.isClickable = true
                                    smallPlayPause.isClickable = true
                                    smallPlayList.isClickable = true
                                }
                            } else {
                                if (smallControl.visibility == View.GONE) {
                                    smallControl.visibility = View.VISIBLE
                                    ivLogo.visibility = View.GONE
                                }

                                if (smallControl.alpha != 1.0f) {
                                    smallControl.alpha = 1.0f
                                }

                                if (!smallCover.isClickable) {
                                    smallCover.isClickable = true
                                    smallTitle.isClickable = true
                                    smallPlayNext.isClickable = true
                                    smallPlayPause.isClickable = true
                                    smallPlayList.isClickable = true
                                }
                            }
                        } else {
                            if (smallControl.visibility == View.VISIBLE) {
                                smallControl.visibility = View.GONE
                                ivLogo.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }.start()
    }

    private fun showError(body: String?) {
        try {
            val error = Gson().fromJson(body, Error::class.java)
            if (error.redirectUrl.isNullOrEmpty()) {
                Toast.makeText(lrcView.context, error?.message, Toast.LENGTH_SHORT).show()
            } else {
                val context = context ?: return
                val uri = Uri.parse(error.redirectUrl)
                val codeUrl = uri.buildUpon()
                    .appendQueryParameter(
                        "token",
                        AuthDelegate.getAuthResponseFromPref(context)?.accessToken
                    )
                    .build()
                    .toString()
                StyledQRCodeDialog.Builder()
                    .setTitle(error.message)
                    .setMessage(getString(R.string.scan_qrcode_to_continue))
                    .setCode(codeUrl)
                    .setButton(getString(R.string.close), null)
                    .show(fragmentManager)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLyric() {
        lrcView.clearLrc()
        smallLrc.clearLrc()
        screenLyricView.clearLrc()

        val playerInfo = ContentStorage.get().playerInfo
        Log.d("PlayerInfo", "lyric url: " + playerInfo?.lyric?.url)
        val url = playerInfo?.lyric?.url
        if (url.isNullOrEmpty()) {
            Log.i("PlayerInfoFragment", "lyric url is empty or null.")
            return
        }

        if (launcher == null) {
            return
        }

        tvLyricError.isVisible = false
        lrcLoading.isVisible = true
        lrcLoading.playAnimation()

        val lyricFile = File(launcher!!.filesDir, "lrc")
        val lyricPath = File(lyricFile, playerInfo.resourceId + ".lrc")

        lyricFile.mkdirs()

        if (lyricPath.exists()) {
            lrcView.post {
                loadLyric(lyricPath)
            }
            return
        }

        val request = Request.Builder().url(url).build()
        OkHttpClient().newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
                tvLyricError.post {
                    tvLyricError.isVisible = true
                    lrcLoading.pauseAnimation()
                    lrcLoading.isVisible = false
                }

                if (e is UnknownHostException) {
                    val intent = Intent(EngineService.ACTION_SEND_REQUEST_FAILED)
                    intent.putExtra(
                        EngineService.EXTRA_RESULT,
                        Result(Result.CODE_DISCONNECTED, null)
                    )
                    context?.sendBroadcast(intent)
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    tvLyricError.post { tvLyricError.isVisible = false }
                    val body = response.body() ?: return
                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(lyricPath)

                    val bytes = ByteArray(1024)

                    var len = 0
                    try {
                        while (len != -1) {
                            len = inputStream.read(bytes)
                            if (len != -1)
                                outputStream.write(bytes, 0, len)
                        }
                        outputStream.flush()

                        inputStream.close()
                        outputStream.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    loadLyric(lyricPath)
                } else {
                    tvLyricError.post {
                        tvLyricError.isVisible = true
                        lrcLoading.pauseAnimation()
                        lrcLoading.isVisible = false
                    }
                }
            }
        })
    }

    private fun loadLyric(lyric: File) {
        lrcLoading.post {
            lrcLoading.pauseAnimation()
            lrcLoading.isVisible = false
        }
        lrcView.loadLrc(lyric)
        smallLrc.loadLrc(lyric)
        screenLyricView.loadLrc(lyric)
        if (!enterInScreenLyricMode) {
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed(autoRunnable, 5000)
        }
    }

    private val autoRunnable = Runnable {
        if (lrcView.hasLrc() && lrcView.lrcLength > 3 &&
            EvsAudioPlayer.get(context).playbackState == AudioPlayer.PLAYBACK_STATE_PLAYING
        ) {
            enterInScreenLyricMode = true
            showScreenLyric()
        }
    }

    private fun showScreenLyric() {
        isAnimatingScreenLrc = true

        if (mainContent.isVisible) {
            val alpha = AlphaAnimation(1f, 0f)
            alpha.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    isAnimatingScreenLrc = false

                    mainContent.isVisible = false
                }

                override fun onAnimationStart(animation: Animation?) {
                }
            })
            alpha.duration = 300
            mainContent.startAnimation(alpha)
        }
        lyricContent.isVisible = true
        lyricContent.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(300)
            .start()
    }

    private fun showMainContent() {
        val alpha = AlphaAnimation(1f, 0f)
        alpha.duration = 300
        lyricContent.startAnimation(alpha)
        mainContent.alpha = 0f
        mainContent.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    super.onAnimationStart(animation)
                    mainContent.isVisible = true
                    lyricContent.isVisible = false
                }
            })
            .start()
    }

    private fun getMediaApi(): MediaApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(MediaApi::class.java)
        } else {
            null
        }
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        TouchAccessibility.isMainFragment = false
        EvsTemplate.get().isPlayerInfoFocused = true
    }

    override fun onSupportInvisible() {
        super.onSupportInvisible()
        //hideMusicPress()

        EvsTemplate.get().isPlayerInfoFocused = false
    }

    override fun onDestroy() {
        super.onDestroy()
        EvsTemplate.get().requestClearFocus(VisualFocusManager.TYPE_PLAYING_TEMPLATE)
        launcher?.getService()?.getAudioPlayer()?.removeListener(this)
        launcher?.unregisterCallback(simpleRenderCallback)
    }
}