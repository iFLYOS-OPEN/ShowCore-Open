package com.iflytek.cyber.iot.show.core.fragment

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.os.postDelayed
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.gson.Gson
import com.iflytek.cyber.evs.sdk.agent.PlaybackController
import com.iflytek.cyber.evs.sdk.agent.VideoPlayer
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.VideoListAdapter
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.impl.speaker.EvsSpeaker
import com.iflytek.cyber.iot.show.core.impl.template.EvsTemplate
import com.iflytek.cyber.iot.show.core.impl.videoplayer.EvsVideoPlayer
import com.iflytek.cyber.iot.show.core.model.ContentStorage
import com.iflytek.cyber.iot.show.core.model.MusicBody
import com.iflytek.cyber.iot.show.core.model.PlayList
import com.iflytek.cyber.iot.show.core.model.Song
import com.iflytek.cyber.iot.show.core.task.SleepWorker
import com.iflytek.cyber.iot.show.core.utils.BrightnessUtils
import com.iflytek.cyber.iot.show.core.utils.InsetDividerDecoration
import com.iflytek.cyber.iot.show.core.utils.dp2Px
import com.iflytek.cyber.iot.show.core.widget.BoxedVertical
import com.iflytek.cyber.iot.show.core.widget.ProgressFrameLayout
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.abs

class VideoFragment : BaseFragment(), View.OnClickListener {

    companion object {
        private const val VOLUME_0 = 0f
        private const val VOLUME_1 = 0.34444f
        private const val VOLUME_2 = 0.66667f
        private const val VOLUME_3 = 1f
    }

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var surfaceView: SurfaceView
    private lateinit var videoList: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var seekBar: IndicatorSeekBar
    private lateinit var playPause: ImageView
    private lateinit var mainContent: RelativeLayout
    private lateinit var alphaCover: View
    private lateinit var tvVideoTitle: TextView
    private lateinit var rootCover: View
    private lateinit var brightnessProgress: ProgressFrameLayout
    private lateinit var slideBar: FrameLayout
    private lateinit var indicatorSlideBar: BoxedVertical
    private lateinit var indicatorIcon: LottieAnimationView
    private lateinit var volumeBar: FrameLayout
    private lateinit var volumeSlideBar: BoxedVertical
    private lateinit var volumeIcon: LottieAnimationView
    private lateinit var volumeProgress: ProgressFrameLayout
    private lateinit var ivPlayList: ImageView

    private var videoListAdapter: VideoListAdapter? = null

    private var seekDragging = false

    private var handler = Handler(Looper.getMainLooper())

    private var volumeAnimator: Animator? = null
    private var animatingVolumeTo = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcher?.getService()?.getVideoPlayer()?.addListener(videoStateChangeListener)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_video, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        drawerLayout = view.findViewById(R.id.drawer)
        brightnessProgress = view.findViewById(R.id.brightness_progress)
        slideBar = view.findViewById(R.id.slide_bar)
        indicatorSlideBar = view.findViewById(R.id.indicator_slide_bar)
        indicatorIcon = view.findViewById(R.id.indicator_icon)
        volumeBar = view.findViewById(R.id.volume_bar)
        volumeProgress = view.findViewById(R.id.volume_progress)
        volumeSlideBar = view.findViewById(R.id.volume_slide_bar)
        volumeIcon = view.findViewById(R.id.volume_icon)
        rootCover = view.findViewById(R.id.root_cover)
        mainContent = view.findViewById(R.id.main_content)
        tvVideoTitle = view.findViewById(R.id.tv_video_title)
        alphaCover = view.findViewById(R.id.iv_alpha_cover)
        videoList = view.findViewById(R.id.video_list)
        progressBar = view.findViewById(R.id.progress_bar)
        seekBar = view.findViewById(R.id.seek_bar)
        ivPlayList = view.findViewById(R.id.iv_play_list)
        ivPlayList.setOnClickListener(this)
        playPause = view.findViewById(R.id.iv_play_pause)
        playPause.setOnClickListener(this)
        val previous = view.findViewById<ImageView>(R.id.iv_previous)
        previous.setOnClickListener(this)
        val next = view.findViewById<ImageView>(R.id.iv_next)
        next.setOnClickListener(this)
        val back = view.findViewById<View>(R.id.back)

        surfaceView = view.findViewById(R.id.surface_view)
        surfaceView.setOnClickListener(this)

        val videoPlayerImpl = launcher?.getService()?.getVideoPlayer() as? EvsVideoPlayer
        videoPlayerImpl?.setVideoSurfaceView(surfaceView)

        seekBar.setProgress(videoPlayerImpl?.getOffset()?.toFloat() ?: 0f)

        back.setOnClickListener {
            pop()
        }

        val duration = launcher?.getService()?.getVideoPlayer()?.getDuration()
        progressBar.isVisible = duration ?: 0 <= 0

        seekBar.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams?) {
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {
                seekDragging = true
            }

            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
                seekDragging = false
                val player = launcher?.getService()?.getVideoPlayer()
                player?.seekTo(seekBar.progress.toLong())
            }
        }

        mainContent.isVisible = false
        alphaCover.isVisible = false

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }

            override fun onDrawerClosed(drawerView: View) {
            }

            override fun onDrawerOpened(drawerView: View) {
                autoCloseDrawer()
            }
        })

        videoList.setOnTouchListener { v, event ->
            autoCloseDrawer()
            false
        }

        val contentResolver = launcher?.contentResolver
        val currentBrightness = Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
        )
        brightnessProgress.setProgress(currentBrightness * 100 / 255f)
        val mode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
        brightnessProgress.setOnTouchProgressChangeListener(object : ProgressFrameLayout.OnTouchProgressChangeListener {
            override fun onTouchProgressChanged(progress: Int) {
                if (!slideBar.isVisible) {
                    slideBar.isVisible = true
                }
                indicatorSlideBar.isEnable = mode != Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                indicatorSlideBar.setValue(progress)
                indicatorIcon.progress = progress / 100f

                BrightnessUtils.setBrightness(context, progress)
            }

            override fun onStopTouch() {
                slideBar.isVisible = false
            }

            override fun onClick() {
                updateControlUI()
            }
        })

        val currentVolume = EvsSpeaker.get(context).getCurrentVolume()
        volumeProgress.setProgress(currentVolume.toFloat())
        volumeProgress.setOnTouchProgressChangeListener(object : ProgressFrameLayout.OnTouchProgressChangeListener {
            override fun onTouchProgressChanged(progress: Int) {
                if (!volumeBar.isVisible) {
                    volumeBar.isVisible = true
                }
                volumeSlideBar.setValue(progress)
                val speaker = EvsSpeaker.get(context)
                speaker.setVolumeLocally(progress)
                val volume = speaker.getCurrentVolume()
                when {
                    volume == 0 -> {
                        animateVolumeTo(volumeIcon.progress, VOLUME_0)
                    }
                    volume in 1..32 -> {
                        animateVolumeTo(volumeIcon.progress, VOLUME_1)
                    }
                    volume in 33..66 -> {
                        animateVolumeTo(volumeIcon.progress, VOLUME_2)
                    }
                    volume >= 67 -> {
                        animateVolumeTo(volumeIcon.progress, VOLUME_3)
                    }
                }
            }

            override fun onStopTouch() {
                volumeBar.isVisible = false
            }

            override fun onClick() {
                updateControlUI()
            }
        })

        loadPlayList()

        launcher?.registerCallback(simpleRenderCallback)
    }

    private val simpleRenderCallback = object : EvsTemplate.SimpleRenderCallback() {
        override fun renderVideoPlayerInfo(payload: String) {
            videoListAdapter?.notifyDataSetChanged()
        }
    }

    private fun animateVolumeTo(from: Float, progress: Float) {
        if (from == progress)
            return
        if (volumeAnimator?.isStarted == true) {
            if (animatingVolumeTo == progress) {
                // ignore
            } else {
                volumeAnimator?.cancel()
            }
        } else {
            val animator = ValueAnimator.ofFloat(from, progress)
            animatingVolumeTo = progress
            animator.addUpdateListener {
                val value = it.animatedValue as Float
                volumeIcon.progress = value
            }
            animator.duration = (500 * abs(from - progress)).toLong()
            animator.start()
            volumeAnimator = animator
        }
    }

    private fun autoCloseDrawer() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(20000) {
            drawerLayout.closeDrawer(GravityCompat.END)
        }
    }

    private val videoStateChangeListener = object : VideoPlayer.VideoStateChangedListener {
        override fun onStarted(player: VideoPlayer, resourceId: String) {
            progressBar.isVisible = true
            loadPlayList()
        }

        override fun onResumed(player: VideoPlayer, resourceId: String) {
            playPause.setImageResource(R.drawable.ic_music_pause)
        }

        override fun onPaused(player: VideoPlayer, resourceId: String) {
            playPause.setImageResource(R.drawable.ic_music_play)
        }

        override fun onStopped(player: VideoPlayer, resourceId: String) {
            playPause.setImageResource(R.drawable.ic_music_play)
        }

        override fun onCompleted(player: VideoPlayer, resourceId: String) {
            playPause.setImageResource(R.drawable.ic_music_play)
        }

        override fun onPositionUpdated(player: VideoPlayer, resourceId: String, position: Long) {
            if (position > 0) {
                progressBar.isVisible = false
                rootCover.isVisible = false
                playPause.setImageResource(R.drawable.ic_music_pause)
                SleepWorker.get(seekBar.context).doTouchWork(seekBar.context) //video playing, do not show sleep view
            }
            if (!seekDragging) {
                seekBar.max = player.getDuration().toFloat()
                seekBar.setProgress(position.toFloat())
                if (position in 1L..999) {
                    seekBar.showMusicPressState(true)
                } else if (position >= 5000) {
                    seekBar.showMusicPressState(false)
                    seekBar.resetDraggerTimeFlag()
                }
            }
        }

        override fun onError(player: VideoPlayer, resourceId: String, errorCode: String) {
            Log.e("VideoFragment", "play video error:" + resourceId + " errorCode: " + errorCode)
        }
    }

    private fun setupRecyclerView(items: ArrayList<Song>) {
        videoListAdapter = VideoListAdapter(items) {
            playVideo(it)
        }
        val decoration = InsetDividerDecoration(
                1.dp2Px(),
                ContextCompat.getColor(videoList.context, R.color.divider_line),
                0)
        videoList.addItemDecoration(decoration)
        videoList.adapter = videoListAdapter

        val playingResource = ContentStorage.get().video?.resourceId
        for (item in items) {
            if (TextUtils.equals(playingResource, item.stream.token)) {
                tvVideoTitle.text = item.metadata.title
            }
        }
    }

    private fun loadPlayList() {
        getMediaApi()?.getPlayList("video")?.enqueue(object : Callback<PlayList> {
            override fun onFailure(call: Call<PlayList>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(call: Call<PlayList>, response: Response<PlayList>) {
                if (response.isSuccessful) {
                    val items = response.body()
                    items?.let {
                        if (it.playlist != null) {
                            if (it.playlist.isEmpty()) {
                                ivPlayList.isVisible = false
                                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                            } else {
                                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                                ivPlayList.isVisible = true
                            }

                            setupRecyclerView(it.playlist)
                        }
                    }
                }
            }
        })
    }

    private fun playVideo(song: Song) {
        val body = MusicBody(null, song.stream.token, song.stream)
        getMediaApi()?.playMusic(body)?.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    tvVideoTitle.text = song.metadata.title
                } else {
                    showError(response.errorBody()?.string())
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    private fun showError(body: String?) {
        try {
            val error = Gson().fromJson(body, Error::class.java)
            Toast.makeText(context, error?.message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_play_list -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END)
                } else {
                    drawerLayout.openDrawer(GravityCompat.END)
                }
            }
            R.id.iv_next -> {
                val playback = launcher?.getService()?.getPlaybackController()
                playback?.sendCommand(PlaybackController.Command.Next)
            }
            R.id.iv_play_pause -> {
                val player = launcher?.getService()?.getVideoPlayer()
                if (player?.state == VideoPlayer.STATE_RUNNING) {
                    player.pause()
                } else {
                    player?.resume()
                }
            }
            R.id.iv_previous -> {
                val playback = launcher?.getService()?.getPlaybackController()
                playback?.sendCommand(PlaybackController.Command.Previous)
            }
            R.id.surface_view -> {
                updateControlUI()
            }
        }
    }

    private fun updateControlUI() {
        if (mainContent.isVisible) {
            mainContent.isVisible = false
            alphaCover.isVisible = false
        } else {
            mainContent.isVisible = true
            alphaCover.isVisible = true
        }
    }

    private fun getMediaApi(): MediaApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(MediaApi::class.java)
        } else {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val videoPlayerImpl = launcher?.getService()?.getVideoPlayer() as? EvsVideoPlayer
        videoPlayerImpl?.realStop()
        launcher?.getService()?.getVideoPlayer()?.removeListener(videoStateChangeListener)
        launcher?.unregisterCallback(simpleRenderCallback)
    }
}