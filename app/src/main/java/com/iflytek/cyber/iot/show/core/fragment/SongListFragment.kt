package com.iflytek.cyber.iot.show.core.fragment

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
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
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.AlbumAdapter
import com.iflytek.cyber.iot.show.core.adapter.SongListAdapter
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.model.Error
import com.iflytek.cyber.iot.show.core.model.MusicBody
import com.iflytek.cyber.iot.show.core.model.SongItem
import com.iflytek.cyber.iot.show.core.model.SongList
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.utils.onLoadMore
import com.iflytek.cyber.iot.show.core.widget.RatioImageView
import com.iflytek.cyber.iot.show.core.widget.ShadowLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SongListFragment : BaseFragment() {

    companion object {

        private const val LIMIT = 20

        fun instance(id: String, title: String, typeName: String?): SongListFragment {
            return SongListFragment().apply {
                arguments = bundleOf(
                        Pair("id", id),
                        Pair("title", title),
                        Pair("name", typeName))
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

    private var songListAdapter: SongListAdapter? = null

    private var page = 1
    private var isLoading = false
    private var hasMoreResult = true
    private var audioId: String? = null
    private var name: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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

        val id = arguments?.getString("id")
        val title = arguments?.getString("title")
        name = arguments?.getString("name")
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

        songListAdapter = SongListAdapter {
            playMusic(it)
        }
        songList.adapter = songListAdapter

        songList.onLoadMore {
            if (!isLoading && hasMoreResult && songListAdapter!!.itemCount >= LIMIT) {
                page += 1
                id?.let { getSongList(id, false) }
            }
        }
    }

    private fun setupUI(songList: SongList) {
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
                        ivCover.context.resources.getDimensionPixelSize(R.dimen.dp_6), 0)
        )

        Glide.with(ivCover)
                .load(songList.image)
                .transform(transformer)
                .into(ivCover)

        Glide.with(rectangleCover)
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
            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    songListAdapter?.notifyDataSetChanged()
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
            val intent = Intent(context, FloatingService::class.java)
            intent.action = FloatingService.ACTION_SHOW_NOTIFICATION
            intent.putExtra(FloatingService.EXTRA_MESSAGE, error.message)
            intent.putExtra(FloatingService.EXTRA_POSITIVE_BUTTON_TEXT, "我知道了")
            context?.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playAll() {
        if (audioId == null) {
            return
        }
        val body = MusicBody(audioId!!.toInt(), null, null)
        getMediaApi()?.playMusic(body)?.enqueue(object : Callback<String> {
            override fun onFailure(call: Call<String>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    songListAdapter?.notifyDataSetChanged()
                    if ((songListAdapter?.items?.isNullOrEmpty() == false) && songListAdapter?.items?.get(0) != null) {
                        val item = songListAdapter?.items?.get(0)
                        Toast.makeText(context, "播放${item?.name}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showError(response.errorBody()?.string())
                }
            }
        })
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        songListAdapter?.notifyDataSetChanged()
    }

    private fun getMediaApi(): MediaApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(MediaApi::class.java)
        } else {
            null
        }
    }
}