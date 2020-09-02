package com.iflytek.cyber.iot.show.core

import android.Manifest
import android.content.*
import android.content.res.Configuration
import android.media.AudioManager
import android.os.*
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import com.iflytek.cyber.evs.sdk.agent.AudioPlayer
import com.iflytek.cyber.evs.sdk.agent.ExternalPlayer
import com.iflytek.cyber.iot.show.core.impl.haotu.HaotuExternalPlayerImpl
import com.iflytek.cyber.iot.show.core.model.DemoConstant
import com.iflytek.cyber.iot.show.core.utils.DataUtil
import com.yilan.sdk.entity.MediaInfo
import com.yilan.sdk.player.PlayerView
import com.yilan.sdk.player.UserCallback
import com.yilan.sdk.player.core.IMediaPlayer
import com.yilan.sdk.player.entity.PlayData
import com.yilan.sdk.player.utils.Constant
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.reflect.Field

class HaotuVideoActivity : AppCompatActivity() {
    companion object {
        const val TAG: String = "HaotuVideo"
    }

    private var mHaotuPlayer: PlayerView? = null
    private val mMediaInfoList: MutableList<MediaInfo> = ArrayList()

    private var mCurPlayIndex: Int = 0
    private var mCurResourceId: String = ""

    private var mIsPaused = false

    private val mPlayCallback = object : UserCallback {
        override fun event(type: Int, data: PlayData?, playHash: Int): Boolean {
            Log.d("HaotuEvent", "event, type=${haotuEventTypeToString(type)}, hash=$playHash")

            when (type) {
                Constant.STATE_NORMAL -> {

                }
                Constant.STATE_PREPARED -> {

                }
                Constant.STATE_PLAYING -> {
                    mEngineService?.getExternalPlayer()?.let {
                        it.requestFocus()
                    }

                    mIsPaused = false
                }
                Constant.STATE_PAUSED -> {
                    mIsPaused = true
                    mEngineService?.getExternalPlayer()?.let {
                        it.abandonFocus()
                    }
                }
                Constant.STATE_COMPLETE -> {
                    mEngineService?.getExternalPlayer()?.let {
                        it.abandonFocus()
                    }

                    playNext()
                }
                Constant.STATE_INFO -> {
                    data?.playList?.let {
                        for (play in it) {
                            Log.d(TAG, "${play.uri}, ${play.name}")
                        }
                    }
                }
                Constant.ACTION_BACK -> {
                    mHaotuPlayer?.setUserCallback(null)

                    Handler(Looper.getMainLooper()).post {
                        onBackPressed()
                    }
                }
                Constant.STATE_ERROR -> {

                }
            }

            return true
        }
    }

    private var mEngineService: EngineService? = null

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is EngineService.EngineServiceBinder) {
                mEngineService = service.getService()
            }

            if (mMediaInfoList.isNotEmpty()) {
                mCurPlayIndex = 0
                mHaotuPlayer?.play(mMediaInfoList[mCurPlayIndex])
                mIsPaused = false
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mEngineService = null
        }
    }
    private val haotuCallback = HaotuExternalPlayerImpl.HaotuCallback { list ->
        mMediaInfoList.clear()

        getMediaInfo(list)

        if (mMediaInfoList.isNotEmpty()) {
            mCurPlayIndex = 0
            mHaotuPlayer?.play(mMediaInfoList[mCurPlayIndex])
            mIsPaused = false
        }
    }
    private val mCommandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                DemoConstant.ACTION_HAOTU_PLAY -> {

                }
                DemoConstant.ACTION_HAOTU_PAUSE -> {
                    mHaotuPlayer?.pause()

                    mEngineService?.getExternalPlayer()?.let {
                        it.replyCommandResult(true)
                    }
                }
                DemoConstant.ACTION_HAOTU_RESUME -> {
                    mHaotuPlayer?.resume()

                    mEngineService?.getExternalPlayer()?.let {
                        it.replyCommandResult(true)
                    }
                }
                DemoConstant.ACTION_HAOTU_PREVIOUS -> {
                    playPrevious()
                }
                DemoConstant.ACTION_HAOTU_NEXT -> {
                    playNext()
                }
                DemoConstant.ACTION_HAOTU_MOVE_TO_BACKGROUND -> {
                    getIMediaPlayer()?.let {
                        it.setVolume(0.1f, 0.1f)
                    }
                }
                DemoConstant.ACTION_HAOTU_MOVE_TO_FOREGROUND -> {
                    getIMediaPlayer()?.let {
                        it.setVolume(1f, 1f)
                    }
                }
                DemoConstant.ACTION_HAOTU_VOL_UP -> {
                    mCurVolume = getVolume()
                    val max = getMaxVolume()

                    if (mCurVolume + mVolumeDelta <= max) {
                        mCurVolume += mVolumeDelta
                    } else {
                        mCurVolume = max
                    }

                    setVolume(mCurVolume)

                    mEngineService?.getExternalPlayer()?.let {
                        it.replyCommandResult(true)
                    }
                }
                DemoConstant.ACTION_HAOTU_VOL_DOWN -> {
                    mCurVolume = getVolume()
                    val min = getMinVolume()

                    if (mCurVolume - mVolumeDelta >= min) {
                        mCurVolume -= mVolumeDelta
                    } else {
                        mCurVolume = min
                    }

                    setVolume(mCurVolume)

                    mEngineService?.getExternalPlayer()?.let {
                        it.replyCommandResult(true)
                    }
                }
                DemoConstant.ACTION_HAOTU_SET_OFFSET -> {
                    val offset = intent.getIntExtra("offset", -1)
                    if (offset != -1) {
                        getIMediaPlayer()?.let {
                            if (offset.toLong() >= 0 && offset.toLong() <= it.duration) {
                                it.seekTo(offset.toLong())

                                mEngineService?.getExternalPlayer()?.let {
                                    it.replyCommandResult(true)
                                }
                            } else {
                                mEngineService?.getExternalPlayer()?.let {
                                    it.replyCommandResult(
                                        false, ExternalPlayer.CODE_COMMAND_UNSUPPORTED,
                                        "进度超出范围"
                                    )
                                }
                            }
                        }
                    }
                }
                DemoConstant.ACTION_HAOTU_FAST_FORWARD -> {
                    val offset = intent.getIntExtra("offset", -1)
                    if (offset != -1) {
                        getIMediaPlayer()?.let {
                            val curPos = it.currentPosition
                            if (curPos + offset <= it.duration) {
                                it.seekTo(curPos + offset)

                                mEngineService?.getExternalPlayer()?.let {
                                    it.replyCommandResult(true)
                                }
                            } else {
                                mEngineService?.getExternalPlayer()?.let {
                                    it.replyCommandResult(
                                        false, ExternalPlayer.CODE_COMMAND_UNSUPPORTED,
                                        "进度超出范围"
                                    )
                                }
                            }
                        }
                    }
                }
                DemoConstant.ACTION_HAOTU_FAST_BACKWARD -> {
                    val offset = intent.getIntExtra("offset", -1)
                    if (offset != -1) {
                        getIMediaPlayer()?.let {
                            val curPos = it.currentPosition
                            if (curPos - offset >= 0) {
                                it.seekTo(curPos - offset)

                                mEngineService?.getExternalPlayer()?.let {
                                    it.replyCommandResult(true)
                                }
                            } else {
                                mEngineService?.getExternalPlayer()?.let {
                                    it.replyCommandResult(
                                        false, ExternalPlayer.CODE_COMMAND_UNSUPPORTED,
                                        "进度超出范围"
                                    )
                                }
                            }
                        }
                    }
                }
                DemoConstant.ACTION_HAOTU_EXIT -> {
                    finish()
                }
            }
        }
    }

    private fun playNext() {
        if (mCurPlayIndex + 1 >= mMediaInfoList.size) {
            mEngineService?.getExternalPlayer()?.let {
                it.replyCommandResult(
                    false, ExternalPlayer.CODE_COMMAND_UNSUPPORTED,
                    "已经是最后一个了哦"
                )
            }
        } else {
            mCurPlayIndex++
            mHaotuPlayer?.play(mMediaInfoList[mCurPlayIndex])
            mIsPaused = false

            mEngineService?.getExternalPlayer()?.let {
                it.replyCommandResult(true)
            }
        }
    }

    private fun playPrevious() {
        if (mCurPlayIndex - 1 < 0) {
            mEngineService?.getExternalPlayer()?.let {
                it.replyCommandResult(
                    false, ExternalPlayer.CODE_COMMAND_UNSUPPORTED,
                    "没有上一个"
                )
            }
        } else {
            mCurPlayIndex--
            mHaotuPlayer?.play(mMediaInfoList[mCurPlayIndex])
            mIsPaused = false

            mEngineService?.getExternalPlayer()?.let {
                it.replyCommandResult(true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_haotu_video)
        supportActionBar?.hide()

        val arrayJsonStr = intent.getStringExtra(DemoConstant.KEY_MEDIA_LIST)
        getMediaInfo(arrayJsonStr)

        initUI()

        val intent = Intent(this, EngineService::class.java)
        startService(intent)
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)

        val filter = IntentFilter()
        filter.addAction(DemoConstant.ACTION_HAOTU_PAUSE)
        filter.addAction(DemoConstant.ACTION_HAOTU_RESUME)
        filter.addAction(DemoConstant.ACTION_HAOTU_PREVIOUS)
        filter.addAction(DemoConstant.ACTION_HAOTU_NEXT)
        filter.addAction(DemoConstant.ACTION_HAOTU_MOVE_TO_BACKGROUND)
        filter.addAction(DemoConstant.ACTION_HAOTU_MOVE_TO_FOREGROUND)
        filter.addAction(DemoConstant.ACTION_HAOTU_VOL_UP)
        filter.addAction(DemoConstant.ACTION_HAOTU_VOL_DOWN)
        filter.addAction(DemoConstant.ACTION_HAOTU_SET_OFFSET)
        filter.addAction(DemoConstant.ACTION_HAOTU_FAST_FORWARD)
        filter.addAction(DemoConstant.ACTION_HAOTU_FAST_BACKWARD)
        filter.addAction(DemoConstant.ACTION_HAOTU_EXIT)

        registerReceiver(mCommandReceiver, filter)

        mCurVolume = getVolume()
        mVolumeDelta = (getMaxVolume() - getMinVolume()) / 5

        HaotuExternalPlayerImpl.getInstance(this).haotuCallback = haotuCallback
    }

    private fun initUI() {
        mHaotuPlayer = findViewById(R.id.ht_player)
        mHaotuPlayer?.setUserCallback(mPlayCallback)
        mHaotuPlayer?.setControllerStyle(Constant.PLAYER_STYLE_NORMAL)

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent?.let {
            val jsonStr = it.getStringExtra(DemoConstant.KEY_MEDIA_LIST)

            mMediaInfoList.clear()
            getMediaInfo(jsonStr)

            if (mMediaInfoList.isNotEmpty()) {
                mCurPlayIndex = 0
                mHaotuPlayer?.play(mMediaInfoList[mCurPlayIndex])
                mIsPaused = false
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        super.onDestroy()

        mHaotuPlayer?.release()

        unbindService(mServiceConnection)

        unregisterReceiver(mCommandReceiver)

        HaotuExternalPlayerImpl.getInstance(this).haotuCallback = null
    }

    override fun onBackPressed() {
        super.onBackPressed()

        mEngineService?.let {
            it.getRecognizer().stopCapture()
            it.getAudioPlayer().stop(AudioPlayer.TYPE_TTS)
        }
    }

    private fun getMediaInfo(arrayJsonStr: String) {
        try {
            val arrayJson = JSONArray(arrayJsonStr)

            for (i in 0 until arrayJson.length()) {
                val mediaJson = arrayJson.getJSONObject(i)
                val info = MediaInfo()

                val urlStr = mediaJson.getString("url")
                val base64 = urlStr.substring("haotu://play?result=".length)
                val jsonStr = String(DataUtil.decodeBase64(base64))

                val infoJson = JSONObject(jsonStr)
                info.video_id = infoJson.getString("id")
                info.h5_url = infoJson.getString("url")
                mCurResourceId = info.video_id

                mMediaInfoList.add(info)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun haotuEventTypeToString(type: Int): String {
        return when (type) {
            Constant.STATE_NORMAL -> {
                "STATE_NORMAL"
            }
            Constant.STATE_PREPARED -> {
                "STATE_PREPARED"
            }
            Constant.STATE_PLAYING -> {
                "STATE_PLAYING"
            }
            Constant.STATE_PAUSED -> {
                "STATE_PAUSED"
            }
            Constant.STATE_PLAYING_BUFFERING_START -> {
                "STATE_PLAYING_BUFFERING_START"
            }
            Constant.STATE_PLAYING_BUFFERING_END -> {
                "STATE_PLAYING_BUFFERING_END"
            }
            Constant.STATE_COMPLETE -> {
                "STATE_COMPLETE"
            }
            Constant.STATE_ERROR -> {
                "STATE_ERROR"
            }
            Constant.STATE_MOBILE -> {
                "STATE_MOBILE"
            }
            Constant.STATE_SIZECHANGE -> {
                "STATE_SIZECHANGE"
            }
            Constant.STATE_INFO -> {
                "STATE_INFO"
            }
            Constant.LAYOUT_LANDSCAPE -> {
                "LAYOUT_LANDSCAPE"
            }
            Constant.LAYOUT_PORTRAIT -> {
                "LAYOUT_PORTRAIT"
            }
            Constant.ERROR_EMPTY_NET -> {
                "ERROR_EMPTY_NET"
            }
            Constant.ERROR_INNER -> {
                "ERROR_INNER"
            }
            Constant.ERROR_NO_DATA -> {
                "ERROR_NO_DATA"
            }
            Constant.ACTION_BACK -> {
                "ACTION_BACK"
            }
            Constant.ACTION_REPLAY -> {
                "ACTION_REPLAY"
            }
            Constant.ACTION_PLAY_RELATE -> {
                "ACTION_PLAY_RELATE"
            }
            else -> {
                "UNKNOWN"
            }
        }
    }

    override fun onResume() {
        super.onResume()

        HaotuExternalPlayerImpl.getInstance(this).isActive = true

        setupStatusBarFlag()
    }

    override fun onPause() {
        super.onPause()

        HaotuExternalPlayerImpl.getInstance(this).pause()

        HaotuExternalPlayerImpl.getInstance(this).isActive = false

        finish()
    }

    private fun setupStatusBarFlag() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    private fun hasPermission(): Boolean {
        return PermissionChecker.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PermissionChecker.PERMISSION_GRANTED
    }

    private fun getIMediaPlayer(): IMediaPlayer? {
        mHaotuPlayer?.let {
            val playerField: Field = PlayerView::class.java.getDeclaredField("mPlayer")
            playerField.isAccessible = true
            val player = playerField.get(it)

            if (player != null) {
                return player as IMediaPlayer
            }
        }

        return null
    }

    private var mCurVolume: Int = 0
    private var mVolumeDelta = 10

    private fun getMaxVolume(): Int {
        (this.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.let { audioManager ->
            return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        }

        return 0
    }

    private fun getMinVolume(): Int {
        (this.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.let { audioManager ->
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
            else 0
        }

        return 0
    }


    private fun setVolume(volume: Int) {
        (this.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.let { audioManager ->
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                volume,
                AudioManager.FLAG_SHOW_UI
            )
        }
    }

    private fun getVolume(): Int {
        (this.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.let { audioManager ->
            return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        }

        return 0
    }

}