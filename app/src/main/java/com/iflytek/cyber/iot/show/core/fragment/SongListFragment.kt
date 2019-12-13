package com.iflytek.cyber.iot.show.core.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.google.gson.Gson
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.evs.sdk.socket.Result
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.EngineService
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.SongListAdapter
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.model.Error
import com.iflytek.cyber.iot.show.core.model.MusicBody
import com.iflytek.cyber.iot.show.core.model.SongItem
import com.iflytek.cyber.iot.show.core.model.SongList
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.utils.onLoadMore
import com.iflytek.cyber.iot.show.core.widget.ShadowLayout
import com.iflytek.cyber.iot.show.core.widget.StyledQRCodeDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.UnknownHostException
import kotlin.math.max
import kotlin.math.min

class SongListFragment : BaseFragment(), PageScrollable {

    companion object {

        const val LIMIT = 20

        fun instance(id: String, title: String, typeName: String?): SongListFragment {
            return SongListFragment().apply {
                arguments = bundleOf(
                    Pair("id", id),
                    Pair("title", title),
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
    }

    private lateinit var songList: RecyclerView
    private lateinit var ivCover: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvSource: TextView
    private lateinit var squareContent: ShadowLayout
    private lateinit var rectangleContent: ShadowLayout
    private lateinit var rectangleCover: ImageView

    private var songListAdapter = SongListAdapter {
        playMusic(it)
    }

    private var page = 1
    private var isLoading = false
    private var hasMoreResult = true
    private var audioId: String? = null
    private var name: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_song_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.back).setOnClickListener { pop() }
        view.findViewById<View>(R.id.play_all).setOnClickListener { playAll() }

        songList = view.findViewById(R.id.song_list)
        ivCover = view.findViewById(R.id.iv_cover)
        tvTitle = view.findViewById(R.id.tv_title)
        tvSource = view.findViewById(R.id.tv_source)
        squareContent = view.findViewById(R.id.square_content)
        rectangleContent = view.findViewById(R.id.rectangle_content)
        rectangleCover = view.findViewById(R.id.rectangle_cover)

        name = arguments?.getString("name")

        songListAdapter = SongListAdapter {
            playMusic(it)
        }
        songList.adapter = songListAdapter

        val songListData = arguments?.getParcelable<SongList>("song_list")
        if (songListData != null) {
            audioId = songListData.id
            songList.onLoadMore {
                if (!isLoading && hasMoreResult && songListAdapter.itemCount >= LIMIT) {
                    page += 1
                    songListData.id?.let { id -> getSongList(id, false) }
                }
            }
            setupUI(songListData)

            tvTitle.text = songListData.name

            if (songListAdapter.itemCount > 1 && songListData.items.size == 0) {
                hasMoreResult = false
            }

            songListAdapter.items.clear()
            songListAdapter.items.addAll(songListData.items)

            if (songListAdapter.items.size < LIMIT) {
                songListAdapter.loadingFinish(true)
            }
        } else {
            val id = arguments?.getString("id")
            val title = arguments?.getString("title")
            tvTitle.text = title

            if (TextUtils.equals(name, "视频")) {
                squareContent.isVisible = false
                rectangleContent.isInvisible = true
            } else {
                squareContent.isInvisible = true
                rectangleContent.isVisible = false
            }

            songList.postDelayed(200) {
                id?.let { getSongList(id, true) }
            }
            songList.onLoadMore {
                if (!isLoading && hasMoreResult && songListAdapter.itemCount >= LIMIT) {
                    page += 1
                    id?.let { getSongList(id, false) }
                }
            }
        }

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

    private fun getSongList(id: String, clear: Boolean) {
        isLoading = true
        getMediaApi()?.getSongList(id, page, LIMIT)?.enqueue(object : Callback<SongList> {
            override fun onFailure(call: Call<SongList>, t: Throwable) {
                isLoading = false
                t.printStackTrace()
            }

            override fun onResponse(call: Call<SongList>, response: Response<SongList>) {
                isLoading = false
                if (response.isSuccessful) {
                    val songList = response.body()
                    songList?.let {
                        audioId = songList.id
                        if (page == 1) {
                            setupUI(it)
                        }

                        if (songListAdapter.itemCount > 1 && songList.items.size == 0) {
                            hasMoreResult = false
                        }

                        if (clear) {
                            songListAdapter.items.clear()
                            songListAdapter.items.addAll(songList.items)
                        } else {
                            if (songList.items.size > 0) {
                                songListAdapter.items.addAll(songList.items)
                            } else {
                                songListAdapter.loadingFinish(true)
                            }
                        }

                        if (songListAdapter.items.size < LIMIT) {
                            songListAdapter.loadingFinish(true)
                        }

                        songListAdapter.notifyDataSetChanged()
                    }
                }
            }
        })
    }

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
                    songListAdapter.notifyDataSetChanged()
                    Toast.makeText(context, "播放${item.name}", Toast.LENGTH_SHORT).show()
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
                    songListAdapter.notifyDataSetChanged()
                    if (!songListAdapter.items.isNullOrEmpty()) {
                        val item = songListAdapter.items[0]
                        Toast.makeText(context, "播放${item.name}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showError(response.errorBody()?.string())
                }
            }
        })
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        songListAdapter.notifyDataSetChanged()
    }

    private fun getMediaApi(): MediaApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(MediaApi::class.java)
        } else {
            null
        }
    }

    override fun scrollToNext(): Boolean {
        songList.let { recyclerView ->
            val lastItem =
                (recyclerView.layoutManager as? LinearLayoutManager)?.findLastCompletelyVisibleItemPosition()
            val itemCount = songListAdapter.itemCount
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
            val itemCount = songListAdapter.itemCount
            if (scrollY == 0 || itemCount == 0) {
                return false
            } else {
                recyclerView.smoothScrollBy(0, -recyclerView.height)
            }
        }
        return true
    }
}