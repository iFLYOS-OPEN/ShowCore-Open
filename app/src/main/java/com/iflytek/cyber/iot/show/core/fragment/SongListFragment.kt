package com.iflytek.cyber.iot.show.core.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.alibaba.fastjson.JSONObject
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.google.gson.Gson
import com.iflytek.cyber.evs.sdk.agent.AudioPlayer
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.evs.sdk.socket.Result
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.EngineService
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.SongListAdapter
import com.iflytek.cyber.iot.show.core.adapter.SongListAdapter2
import com.iflytek.cyber.iot.show.core.api.AppApi
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.model.*
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.utils.clickWithTrigger
import com.iflytek.cyber.iot.show.core.utils.onLoadMore
import com.iflytek.cyber.iot.show.core.widget.ShadowLayout
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.UnknownHostException

class SongListFragment : BaseFragment(), PageScrollable, AudioPlayer.MediaStateChangedListener {

    companion object {

        const val LIMIT = 20

        fun instance(id: String, title: String, typeName: String?): SongListFragment {
            return SongListFragment().apply {
                arguments = bundleOf(
                    Pair("id", id),
                    Pair("text", title),
                    Pair("name", typeName)
                )
            }
        }

        fun instance(songList: SongList, typeName: String?): SongListFragment {
            return SongListFragment().apply {
                arguments = bundleOf(
                    Pair("song_list", songList),
                    Pair("name", typeName)
                )
            }
        }

        fun newInstance(song: CollectionSong, type: Int): SongListFragment {
            return SongListFragment().apply {
                arguments = bundleOf(
                    Pair("song", song),
                    Pair("type", type)
                )
            }
        }

        fun newInstance(sourceItem: SourceItem, type: Int, albumType: Int): SongListFragment {
            return SongListFragment().apply {
                arguments = bundleOf(
                    Pair("sourceItem", sourceItem),
                    Pair("type", type),
                    Pair("albumType", albumType)
                )
            }
        }

        fun newInstance(albumItem: AlbumItem, type: Int, albumType: Int): SongListFragment {
            return SongListFragment().apply {
                arguments = bundleOf(
                    Pair("albumItem", albumItem),
                    Pair("type", type),
                    Pair("albumType", albumType)
                )
            }
        }
    }

    private lateinit var songList: RecyclerView
    private lateinit var ivCover: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvSource: TextView
    private lateinit var squareContent: ShadowLayout
    private lateinit var rectangleContent: ShadowLayout
    private lateinit var rectangleCover: ImageView
    private lateinit var loadingContainer: FrameLayout
    private lateinit var loadingImageView: LottieAnimationView
    private lateinit var mainContent: LinearLayout

    private var songListAdapter: SongListAdapter? = null
    private var songListAdapter2: SongListAdapter2? = null

    private var page = 1
    private var isLoading = false
    private var hasMoreResult = true
    private var audioId: String? = null
    private var name: String? = null
    private var song: CollectionSong? = null
    private var type: Int? = null // type: 1 收藏列表， type: 2 专辑列表
    private var albumType: Int? = null //1001: 喜马拉雅专辑， 1002: 其他信源专辑
    private var sourceItem: SourceItem? = null
    private var albumItem: AlbumItem? = null
    private var appShowResult: AppShowResult? = null

    private var backCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_song_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launcher?.getService()?.getAudioPlayer()?.addListener(this)

        view.findViewById<View>(R.id.back).clickWithTrigger {
            pop()
        }
        view.findViewById<View>(R.id.play_all).setOnClickListener {
            if (type != null && type == 1) {
                playAllCollection()
            } else if (type == 2) {
                playAllAlbum()
            } else {
                playAll()
            }
        }

        songList = view.findViewById(R.id.song_list)
        ivCover = view.findViewById(R.id.iv_cover)
        tvTitle = view.findViewById(R.id.tv_title)
        tvSource = view.findViewById(R.id.tv_source)
        squareContent = view.findViewById(R.id.square_content)
        rectangleContent = view.findViewById(R.id.rectangle_content)
        rectangleCover = view.findViewById(R.id.rectangle_cover)
        loadingContainer = view.findViewById(R.id.loading_container)
        loadingImageView = view.findViewById(R.id.loading_image)
        mainContent = view.findViewById(R.id.main_content)

        name = arguments?.getString("name")
        type = arguments?.getInt("type", -1)
        albumType = arguments?.getInt("albumType", -1)
        song = arguments?.getParcelable("song")
        sourceItem = arguments?.getParcelable("sourceItem")
        albumItem = arguments?.getParcelable("albumItem")

        if (type == 2) {
            songListAdapter2 = SongListAdapter2 {
                playAlbumMusic(it)
            }
            songList.adapter = songListAdapter2
        } else {
            songListAdapter = SongListAdapter {
                if (type != null && type == 1) {
                    playCollectionMusic(it)
                } else {
                    playMusic(it)
                }
            }
            songList.adapter = songListAdapter
        }

        val id = arguments?.getString("id")
        val title = arguments?.getString("title")

        view.findViewById<View>(R.id.refresh)?.setOnClickListener {
            when (type) {
                1 -> getCollectionList(true)
                2 -> getAlbumList(true)
                else -> id?.let { getSongList(id, true) }
            }
        }

        val songListData = arguments?.getParcelable<SongList>("song_list")
        if (songListData != null) {
            mainContent.isVisible = true
            audioId = songListData.id
            songList.onLoadMore {
                if (!isLoading && hasMoreResult && songListAdapter?.itemCount ?: 0 >= LIMIT) {
                    page += 1
                    songListData.id?.let { id -> getSongList(id, false) }
                }
            }
            setupUI(songListData)

            tvTitle.text = songListData.name

            if (songListAdapter?.itemCount ?: 0 > 1 && songListData.items.size == 0) {
                hasMoreResult = false
            }

            songListAdapter?.items?.clear()
            songListAdapter?.items?.addAll(songListData.items)

            if (songListAdapter?.items?.size ?: 0 < LIMIT) {
                songListAdapter?.loadingFinish(true)
            }
        } else {
            mainContent.isVisible = false
            if (type != null && type == 1) {
                tvTitle.text = song?.name
            } else {
                tvTitle.text = title
            }

            if (TextUtils.equals(name, "视频")) {
                squareContent.isVisible = false
                rectangleContent.isInvisible = true
            } else {
                squareContent.isInvisible = true
                rectangleContent.isVisible = false
            }

            showLoading()

            songList.postDelayed(200) {
                when (type) {
                    1 -> getCollectionList(true)
                    2 -> getAlbumList(true)
                    else -> id?.let { getSongList(id, true) }
                }
            }
            songList.onLoadMore {
                if (!isLoading && hasMoreResult && songListAdapter?.itemCount ?: 0 >= LIMIT && type != 2) {
                    page += 1
                    when (type) {
                        1 -> getCollectionList(false)
                        else -> id?.let { getSongList(id, false) }
                    }
                } else if (!isLoading && hasMoreResult && songListAdapter2?.itemCount ?: 0 >= LIMIT && type == 2) {
                    page += 1
                    getAlbumList(false)
                }
            }
        }
    }

    override fun onStarted(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            songListAdapter?.notifyDataSetChanged()
            songListAdapter2?.notifyDataSetChanged()
        }
    }

    override fun onResumed(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            songListAdapter?.notifyDataSetChanged()
            songListAdapter2?.notifyDataSetChanged()
        }
    }

    override fun onPaused(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            songListAdapter?.notifyDataSetChanged()
            songListAdapter2?.notifyDataSetChanged()
        }
    }

    override fun onStopped(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            songListAdapter?.notifyDataSetChanged()
            songListAdapter2?.notifyDataSetChanged()
        }
    }

    override fun onCompleted(player: AudioPlayer, type: String, resourceId: String) {
    }

    override fun onPositionUpdated(
        player: AudioPlayer,
        type: String,
        resourceId: String,
        position: Long
    ) {
    }

    override fun onError(player: AudioPlayer, type: String, resourceId: String, errorCode: String) {
    }

    private fun setupUI(songList: SongList) {
        val context = context ?: return
        if ((context as? Activity)?.isDestroyed == true)
            return
        if (TextUtils.equals(name, "视频")) {
            squareContent.isVisible = false
            rectangleContent.isVisible = true
        } else {
            squareContent.isVisible = true
            rectangleContent.isVisible = false
        }

        tvSource.text = songList.from
        val transformer = MultiTransformation(
            CenterCrop(),
            RoundedCornersTransformation(
                ivCover.context.resources.getDimensionPixelSize(R.dimen.dp_6), 0
            )
        )

        Glide.with(context)
            .load(songList.image)
            .transform(transformer)
            .into(ivCover)

        Glide.with(context)
            .load(songList.image)
            .transform(transformer)
            .into(rectangleCover)
    }

    private fun getSongList(id: String, clear: Boolean) { //获取歌单列表
        isLoading = true
        getMediaApi()?.getSongList(id, page, LIMIT)?.enqueue(object : Callback<SongList> {
            override fun onFailure(call: Call<SongList>, t: Throwable) {
                isLoading = false
                dismissLoading()
                t.printStackTrace()
                showError(t)
            }

            override fun onResponse(call: Call<SongList>, response: Response<SongList>) {
                isLoading = false
                dismissLoading()
                if (response.isSuccessful) {
                    val songList = response.body()
                    songList?.let {
                        audioId = songList.id
                        if (page == 1) {
                            setupUI(it)
                        }

                        if (songListAdapter?.itemCount ?: 0 > 1 && songList.items.size == 0) {
                            hasMoreResult = false
                        }

                        if (clear) {
                            songListAdapter?.items?.clear()
                            songListAdapter?.items?.addAll(songList.items)
                        } else {
                            if (songList.items.size > 0) {
                                songListAdapter?.items?.addAll(songList.items)
                            } else {
                                songListAdapter?.loadingFinish(true)
                            }
                        }

                        if (songListAdapter?.items?.size ?: 0 < LIMIT) {
                            songListAdapter?.loadingFinish(true)
                        }

                        songListAdapter?.notifyDataSetChanged()
                    }
                } else {
                    if (clear) {
                        mainContent.isVisible = false
                        loadingContainer.isVisible = false
                        view?.findViewById<View>(R.id.error_container)?.isVisible = true
                    }
                }
            }
        })
    }

    private fun showLoading() {
        mainContent.isVisible = false
        loadingContainer.isVisible = true
        loadingImageView.playAnimation()
    }

    private fun dismissLoading() {
        mainContent.isVisible = true
        loadingContainer.isVisible = false
        loadingImageView.pauseAnimation()
    }

    private fun showError(t: Throwable) {
        if (t is UnknownHostException) {
            mainContent.isVisible = false
            loadingContainer.isVisible = false
            view?.findViewById<View>(R.id.error_container)?.let { errorContainer ->
                errorContainer.isVisible = true

                val errorText = errorContainer.findViewById<TextView>(R.id.error_text)
                errorText?.text = "网络出了点小差，请检查网络后重试"
            }
        } else {
            mainContent.isVisible = false
            loadingContainer.isVisible = false
            view?.findViewById<View>(R.id.error_container)?.let { errorContainer ->
                errorContainer.isVisible = true

                val errorText = errorContainer.findViewById<TextView>(R.id.error_text)
                errorText?.text = "加载失败，请重试"
            }
        }
    }

    private fun getCollectionList(clear: Boolean) { //获取收藏列表
        isLoading = true
        if (song == null || song?.albumId.isNullOrEmpty()) {
            isLoading = false
            return
        }

        getMediaApi()?.getAlbumList(song!!.albumId!!, song?.sourceType, song?.business, page, LIMIT)
            ?.enqueue(object : Callback<SongList> {
                override fun onFailure(call: Call<SongList>, t: Throwable) {
                    isLoading = false
                    dismissLoading()
                    t.printStackTrace()
                    showError(t)
                }

                override fun onResponse(call: Call<SongList>, response: Response<SongList>) {
                    isLoading = false
                    dismissLoading()
                    if (response.isSuccessful) {
                        view?.findViewById<View>(R.id.error_container)?.isVisible = false
                        val songList = response.body()
                        songList?.let {
                            audioId = songList.id
                            if (page == 1) {
                                setupUI(it)
                            }

                            if (songListAdapter?.itemCount ?: 0 > 1 && songList.items.size == 0) {
                                hasMoreResult = false
                            }

                            if (clear) {
                                songListAdapter?.items?.clear()
                                songListAdapter?.items?.addAll(songList.items)
                            } else {
                                if (songList.items.size > 0) {
                                    songListAdapter?.items?.addAll(songList.items)
                                } else {
                                    songListAdapter?.loadingFinish(true)
                                }
                            }

                            if (songListAdapter?.items?.size ?: 0 < LIMIT) {
                                songListAdapter?.loadingFinish(true)
                            }

                            songListAdapter?.notifyDataSetChanged()
                        }
                    } else {
                        page -= 1
                        songListAdapter?.loadingFinish(true)
                        if (clear) {
                            mainContent.isVisible = false
                            loadingContainer.isVisible = false
                            view?.findViewById<View>(R.id.error_container)?.isVisible = true
                        }
                    }
                }
            })
    }

    private fun setupAlbumUI() {
        val context = context ?: return
        if ((context as? Activity)?.isDestroyed == true)
            return
        squareContent.isVisible = true
        rectangleContent.isVisible = false

        if (albumType == 1001) {
            tvTitle.text = albumItem?.title
            tvSource.text = albumItem?.from
        } else {
            tvTitle.text = sourceItem?.title
            tvSource.text = appShowResult?.source
        }

        val transformer = MultiTransformation(
            CenterCrop(),
            RoundedCornersTransformation(
                ivCover.context.resources.getDimensionPixelSize(R.dimen.dp_6), 0
            )
        )

        val cover = if (albumType == 1001) {
            albumItem?.cover
        } else {
            sourceItem?.cover
        }

        Glide.with(context)
            .load(cover)
            .transform(transformer)
            .into(ivCover)

        Glide.with(context)
            .load(cover)
            .transform(transformer)
            .into(rectangleCover)
    }

    private fun getAlbumList(clear: Boolean = true) {
        isLoading = true

        if (albumType != null && albumType == 1001) {
            val json = JSONObject()
            if (albumType != null && albumType == 1001) {
                json["album_id"] = albumItem?.albumId
                json["page"] = page
                json["limit"] = LIMIT
            }

            val body = RequestBody.create(
                MediaType.parse("application/json"),
                json.toString()
            )

            loadXmlyAlbumList(body, clear)
        } else {
            loadAlbumList(sourceItem, clear)
        }
    }

    private fun loadXmlyAlbumList(body: RequestBody, clear: Boolean) {
        getAppApi()?.getXmlyShow(body)?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                isLoading = false
                dismissLoading()
                if (response.isSuccessful) {
                    view?.findViewById<View>(R.id.error_container)?.isVisible = false
                    val appShowResult = Gson().fromJson<AppShowResult>(
                        response.body()?.string(),
                        AppShowResult::class.java
                    )
                    if (appShowResult != null) {
                        setupSongListAdpater2(clear, appShowResult.result)
                    }
                } else {
                    page -= 1
                    songListAdapter2?.loadingFinish(true)
                    if (clear) {
                        mainContent.isVisible = false
                        loadingContainer.isVisible = false
                        showErrorText(response.errorBody()?.string())
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                page -= 1
                isLoading = false
                dismissLoading()
                t.printStackTrace()
                showError(t)
            }
        })
    }

    private fun showErrorText(errorJson: String?) {
        try {
            view?.findViewById<View>(R.id.error_container)?.isVisible = true
            val error = Gson().fromJson<Message>(errorJson, Message::class.java)
            view?.findViewById<TextView>(R.id.error_text)?.text = error.error
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadAlbumList(sourceItem: SourceItem?, clear: Boolean) {
        val mediaType = if (sourceItem?.metadata?.type == 1) {
            "audio"
        } else {
            "video"
        }
        getAppApi()?.getAlbumList(
            sourceItem?.source,
            sourceItem?.metadata?.business,
            sourceItem?.metadata?.album,
            mediaType,
            page
        )?.enqueue(object : Callback<AppShowResult> {
            override fun onResponse(call: Call<AppShowResult>, response: Response<AppShowResult>) {
                isLoading = false
                dismissLoading()
                if (response.isSuccessful) {
                    view?.findViewById<View>(R.id.error_container)?.isVisible = false
                    val songList = response.body()
                    this@SongListFragment.appShowResult = songList
                    if (songList != null) {
                        setupSongListAdpater2(clear, songList.items)
                    }
                } else {
                    page -= 1
                    songListAdapter2?.loadingFinish(true)
                    if (clear) {
                        mainContent.isVisible = false
                        loadingContainer.isVisible = false
                        if (isAdded && context != null) {
                            showError(response.errorBody()?.string())
                        }
                    }
                }
            }

            override fun onFailure(call: Call<AppShowResult>, t: Throwable) {
                page -= 1
                isLoading = false
                dismissLoading()
                t.printStackTrace()
                showError(t)
            }
        })
    }

    private fun setupSongListAdpater2(clear: Boolean, result: List<MediaItem>) {
        if (page == 1) {
            //setupUI(it)
            setupAlbumUI()
        }

        if (songListAdapter2?.itemCount ?: 0 > 1 && result.isEmpty()) {
            hasMoreResult = false
        }

        if (clear) {
            songListAdapter2?.items?.clear()
            songListAdapter2?.items?.addAll(result)
        } else {
            if (result.isNotEmpty()) {
                songListAdapter2?.items?.addAll(result)
            } else {
                songListAdapter2?.loadingFinish(true)
            }
        }

        if (songListAdapter2?.items?.size ?: 0 < LIMIT) {
            songListAdapter2?.loadingFinish(true)
        }

        songListAdapter2?.notifyDataSetChanged()
    }

    private fun playAlbumMusic(item: MediaItem) {
        val json = JSONObject()
         if (albumType != null && albumType == 1002) {
             json["audio_id"] = item.id
             json["source"] = item.source
             json["business"] = sourceItem?.metadata?.business
             json["album"] = sourceItem?.metadata?.album
             json["media_type"] = item.mediaType
         } else {
             json["audio_id"] = item.id
             json["source_type"] = item.source
             json["album_id"] = albumItem?.albumId
         }

        val body = RequestBody.create(
            MediaType.parse("application/json"),
            json.toString()
        )

        if (albumType == 1001) {
            postPlayXmly(body, item)
        } else {
            postPlayAlbum(body, item)
        }
    }

    private fun postPlayXmly(body: RequestBody, item: MediaItem) {
        getAppApi()?.postPlayMedia(body)?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    songListAdapter2?.notifyDataSetChanged()
                    if (isAdded && context != null) {
                        Toast.makeText(context, "播放${item.title}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showError(response.errorBody()?.string())
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    private fun postPlayAlbum(body: RequestBody, item: MediaItem) {
        getAppApi()?.postPlayAlbum(body)?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    songListAdapter2?.notifyDataSetChanged()
                    if (isAdded && context != null) {
                        Toast.makeText(context, "播放${item.title}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showError(response.errorBody()?.string())
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    /**
     * 播放歌单歌曲
     */
    private fun playMusic(item: SongItem) {
        if (audioId == null) {
            return
        }
        val body = MusicBody(audioId!!.toInt(), item.id, null)
        getMediaApi()?.playMusic(body)?.enqueue(object : Callback<String> {
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

            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    songListAdapter?.notifyDataSetChanged()
                    if (isAdded && context != null) {
                        Toast.makeText(context, "播放${item.name}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showError(response.errorBody()?.string())
                }
            }
        })
    }

    /**
     * 播放收藏歌曲，从第一首开始
     */
    private fun playAllCollection() {
        if (song == null || song?.albumId.isNullOrEmpty() ||
            song?.sourceType.isNullOrEmpty() ||
            song?.business.isNullOrEmpty()
        ) {
            return
        }
        val body = PlayAlbumBody(song?.albumId!!, song?.sourceType!!, null, song?.business!!)
        getMediaApi()?.playAlbum(body)?.enqueue(object : Callback<PlayResult> {
            override fun onFailure(call: Call<PlayResult>, t: Throwable) {
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

            override fun onResponse(call: Call<PlayResult>, response: Response<PlayResult>) {
                if (response.isSuccessful) {
                    songListAdapter?.notifyDataSetChanged()
                    if (isAdded && context != null) {
                        Toast.makeText(context, "播放${song?.name}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showError(response.errorBody()?.string())
                }
            }
        })
    }

    private fun showError(errorStr: String?) {
        try {
            val error = Gson().fromJson(errorStr, Error::class.java)
            if (error.redirectUrl.isNullOrEmpty()) {
                val intent = Intent(context, FloatingService::class.java)
                intent.action = FloatingService.ACTION_SHOW_NOTIFICATION
                intent.putExtra(FloatingService.EXTRA_MESSAGE, error.message)
                intent.putExtra(FloatingService.EXTRA_POSITIVE_BUTTON_TEXT, "我知道了")
                context?.startService(intent)
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

                start(
                    KugouQRCodeFragment.newInstance(
                        error.title,
                        error.message,
                        error.qrCodeMessage,
                        error.redirectUrl
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 播放收藏歌曲
     */
    private fun playCollectionMusic(item: SongItem) {
        val body = PlayAlbumBody(item.albumId, item.sourceType, item.id, item.business)
        getMediaApi()?.playAlbum(body)?.enqueue(object : Callback<PlayResult> {
            override fun onFailure(call: Call<PlayResult>, t: Throwable) {
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

            override fun onResponse(call: Call<PlayResult>, response: Response<PlayResult>) {
                if (response.isSuccessful) {
                    songListAdapter?.notifyDataSetChanged()
                    if (isAdded && context != null) {
                        Toast.makeText(context, "播放${item.name}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showError(response.errorBody()?.string())
                }
            }
        })
    }

    private fun playAllAlbum() {
        val items = songListAdapter2?.items
        if (items != null && items.size > 0) {
            val first = items[0]
            playAlbumMusic(first)
        }
    }

    /**
     * 播放歌单，从第一首开始
     */
    private fun playAll() {
        val audioId = audioId ?: return
        val body = MusicBody(audioId.toInt(), null, null)
        getMediaApi()?.playMusic(body)?.enqueue(object : Callback<String> {
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

            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    songListAdapter?.notifyDataSetChanged()
                    if (!songListAdapter?.items.isNullOrEmpty()) {
                        val item = songListAdapter?.items?.get(0)
                        if (isAdded && context != null) {
                            Toast.makeText(context, "播放${item?.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else if (isAdded && context != null) {
                    showError(response.errorBody()?.string())
                }
            }
        })
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        songListAdapter?.notifyDataSetChanged()
        songListAdapter2?.notifyDataSetChanged()
    }

    private fun getMediaApi(): MediaApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(MediaApi::class.java)
        } else {
            null
        }
    }

    private fun getAppApi(): AppApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(AppApi::class.java)
        } else {
            null
        }
    }

    override fun scrollToNext(): Boolean {
        songList.let { recyclerView ->
            val lastItem =
                (recyclerView.layoutManager as? LinearLayoutManager)?.findLastCompletelyVisibleItemPosition()
            val itemCount = songListAdapter?.itemCount ?: 0
            if (lastItem == itemCount - 1 || itemCount == 0
            ) {
                return false
            } else {
                recyclerView.smoothScrollBy(0, recyclerView.height)
            }
        }
        return true
    }

    override fun scrollToPrevious(): Boolean {
        songList.let { recyclerView ->
            val scrollY = recyclerView.computeVerticalScrollOffset()
            val itemCount = songListAdapter?.itemCount ?: 0
            if (scrollY == 0 || itemCount == 0) {
                return false
            } else {
                recyclerView.smoothScrollBy(0, -recyclerView.height)
            }
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        launcher?.getService()?.getAudioPlayer()?.removeListener(this)
    }
}