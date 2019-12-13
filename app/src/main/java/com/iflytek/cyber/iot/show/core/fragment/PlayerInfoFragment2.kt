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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.os.postDelayed
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.C
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
import com.iflytek.cyber.iot.show.core.adapter.SongsAdapter
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.impl.audioplayer.EvsAudioPlayer
import com.iflytek.cyber.iot.show.core.impl.template.EvsTemplate
import com.iflytek.cyber.iot.show.core.model.*
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.widget.StyledQRCodeDialog
import com.iflytek.cyber.iot.show.core.widget.lrc.LrcView
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import jp.wasabeef.blurry.Blurry
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
    private lateinit var tvDuration: TextView
    private lateinit var tvOnlyTitle: TextView
    private lateinit var ivPlayList: ImageView

    private var seekBarDragging = false
    private var currentPosition = 0L
    private var currentResourceId: String? = ""
    private var isAnimatingScreenLrc = false

    private var retryCount = 0

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
        ivBack.setOnClickListener(this)
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

        ivBlurCover = view.findViewById(R.id.iv_cover_blur)
        ivLogo = view.findViewById(R.id.iv_logo)
        musicCover = view.findViewById(R.id.iv_cover)
        musicTitle = view.findViewById(R.id.tv_title)
        musicArtist = view.findViewById(R.id.tv_artist)

        if (launcher?.getService()?.getAudioPlayer()?.playbackState == AudioPlayer.PLAYBACK_STATE_PLAYING) {
            playPause.setImageResource(R.drawable.ic_music_pause)
        } else {
            playPause.setImageResource(R.drawable.ic_music_play)
        }

        val topView = LayoutInflater.from(view.context).inflate(R.layout.indicator_top_view, null)
        tvDuration = topView.findViewById(R.id.tv_duration)
        seekBar.indicator.addTopContentView(topView)

        seekBar.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams) {
                val duration =
                    format(seekParams.progress.toLong()) + "-" + format(seekBar.max.toLong())
                tvDuration.text = duration
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
        if (audioPlayer.playbackState == AudioPlayer.PLAYBACK_STATE_PLAYING)
            seekBar.isVisible = mediaType == C.TYPE_OTHER
    }

    private fun autoCloseDrawer() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(20000) {
            drawer.closeDrawer(GravityCompat.END)
        }
    }

    private fun setupTitleVisible(playerInfo: PlayerInfoPayload?) {
        if (playerInfo?.content?.musicArtist.isNullOrEmpty() &&
            playerInfo?.lyric?.url.isNullOrEmpty()
        ) {
            musicTitle.isVisible = false
            musicArtist.isVisible = false
            tvOnlyTitle.isVisible = true
        } else {
            musicTitle.isVisible = true
            musicArtist.isVisible = true
            tvOnlyTitle.isVisible = false
        }
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
                        .from(getBitmapFromVectorDrawable(R.drawable.cover_default))
                        .into(imageView)
                }
            }
        }
        val transformer = MultiTransformation(
            CenterCrop(),
            RoundedCornersTransformation(dp4, 0)
        )
        Glide.with(musicCover.context)
            .asBitmap()
            .load(playerInfo?.content?.imageUrl)
            .transform(transformer)
            .apply(
                RequestOptions
                    .placeholderOf(R.drawable.cover_default)
                    .error(R.drawable.cover_default)
            )
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    musicCover.setImageBitmap(resource)
                    Blurry.with(musicCover.context)
                        .sampling(4)
                        .color(Color.parseColor("#66212121"))
                        .from(resource)
                        .into(ivBlurCover)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    musicCover.setImageResource(R.drawable.cover_default)
                }
            })
        val dp8 = musicCover.context.resources.getDimensionPixelSize(R.dimen.dp_8)
        Glide.with(ivLogo.context)
            .load(playerInfo?.provider?.logoUrl)
            .apply(
                RequestOptions()
                    .transform(RoundedCornersTransformation(dp8, 0))
            )
            .into(ivLogo)
        musicTitle.text = playerInfo?.content?.musicTitle
        musicArtist.text = playerInfo?.content?.musicArtist
        tvOnlyTitle.text = playerInfo?.content?.musicTitle
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
            loadPlayList()
            setupMusic(playerInfo)
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
        } else {
            playNext.alpha = 1f
            playNext.isEnabled = true
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

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_play_list -> {
                if (drawer.isDrawerOpen(GravityCompat.END)) {
                    drawer.closeDrawer(GravityCompat.END)
                } else {
                    drawer.openDrawer(GravityCompat.END)
                }
            }
            R.id.iv_next -> {
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
            R.id.iv_play_pause -> {
                val audioPlayer = EvsAudioPlayer.get(context)
                if (audioPlayer.playbackResourceId.isNullOrEmpty()) {
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
            R.id.back -> {
                launcher?.onBackPressed()
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
            playPause.setImageResource(R.drawable.ic_music_pause)
            seekBar.max = player.getDuration(type).toFloat()

            setupSeekBar()
        }
    }

    override fun onResumed(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            playPause.setImageResource(R.drawable.ic_music_pause)
        }
    }

    override fun onPaused(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            playPause.setImageResource(R.drawable.ic_music_play)
        }
    }

    override fun onStopped(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            playPause.setImageResource(R.drawable.ic_music_play)
        }
    }

    override fun onCompleted(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            playPause.setImageResource(R.drawable.ic_music_play)
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
                screenLyricView.updateTime(position)
                seekBar.setProgress(position.toFloat())
                currentPosition = position
            }
            if (currentPosition <= position && position in 0L..999) {
                setupSeekBar()
                showMusicPress()
                playPause.setImageResource(R.drawable.ic_music_pause)
            } else if (position >= 5000) {
                hideMusicPress()
                currentPosition = position
            }
        }
    }

    override fun onError(player: AudioPlayer, type: String, resourceId: String, errorCode: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            playPause.setImageResource(R.drawable.ic_music_play)
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
                    } ?: run {
                        ivPlayList.isVisible = false
                    }
                } else {
                    ivPlayList.isVisible = false
                }
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

    private fun showMusicPress() {
        if (isSupportVisible && mainContent.isVisible && !isAnimatingScreenLrc)
            seekBar.showMusicPressState(true)
    }

    private fun hideMusicPress() {
        seekBar.showMusicPressState(false)
    }

    private fun getLyric() {
        lrcView.clearLrc()
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
            hideMusicPress()
            showScreenLyric()
        }
    }

    private fun showScreenLyric() {
        isAnimatingScreenLrc = true

        hideMusicPress()

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
        hideMusicPress()

        EvsTemplate.get().isPlayerInfoFocused = false
    }

    override fun onDestroy() {
        super.onDestroy()
        EvsTemplate.get().requestClearFocus(VisualFocusManager.TYPE_PLAYING_TEMPLATE)
        launcher?.getService()?.getAudioPlayer()?.removeListener(this)
        launcher?.unregisterCallback(simpleRenderCallback)
    }
}