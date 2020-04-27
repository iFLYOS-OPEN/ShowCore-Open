package com.iflytek.cyber.iot.show.core.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.iflytek.cyber.evs.sdk.agent.AudioPlayer
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.CollectionSingleAdapter
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.model.CollectionSong
import com.iflytek.cyber.iot.show.core.model.Error
import com.iflytek.cyber.iot.show.core.model.PlayResult
import com.iflytek.cyber.iot.show.core.model.PlaySingleBody
import com.kk.taurus.playerbase.utils.NetworkUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CollectionSingleFragment : BaseFragment(), AudioPlayer.MediaStateChangedListener {

    private lateinit var emptyContent: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private var adapter: CollectionSingleAdapter? = null

    private var items = ArrayList<CollectionSong>()
    private var tagId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragement_collection_media, container, false)
    }

    fun setItems(items: ArrayList<CollectionSong>, tagId: Int) {
        this.tagId = tagId
        this.items.clear()
        this.items.addAll(items)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recycler_view)
        emptyContent = view.findViewById(R.id.empty_content)
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        launcher?.getService()?.getAudioPlayer()?.addListener(this)

        adapter = CollectionSingleAdapter {
            play(it)
        }
        adapter?.items = items
        recyclerView.adapter = adapter

        if (items.isEmpty()) {
            emptyContent.isVisible = true
            recyclerView.isVisible = false
        } else {
            emptyContent.isVisible = false
            recyclerView.isVisible = true
        }
    }

    override fun onStarted(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            adapter?.notifyDataSetChanged()
        }
    }

    override fun onResumed(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            adapter?.notifyDataSetChanged()
        }
    }

    override fun onPaused(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            adapter?.notifyDataSetChanged()
        }
    }

    override fun onStopped(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            adapter?.notifyDataSetChanged()
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

    override fun onSupportVisible() {
        super.onSupportVisible()
        adapter?.notifyDataSetChanged()
    }

    private fun play(song: CollectionSong) {
        if (tagId == -1) {
            return
        }
        val body = PlaySingleBody(tagId, song.id)
        getMediaApi()?.playSingleCollection(body)?.enqueue(object : Callback<PlayResult> {
            override fun onResponse(call: Call<PlayResult>, response: Response<PlayResult>) {
                if (!response.isSuccessful) {
                    showError(response.errorBody()?.string())
                }
            }

            override fun onFailure(call: Call<PlayResult>, t: Throwable) {
                if (isAdded && context != null && !NetworkUtils.isNetConnected(context)) {
                    Toast.makeText(context, "网络连接异常，请重新设置", Toast.LENGTH_SHORT).show()
                }
                t.printStackTrace()
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

                (parentFragment?.parentFragment as? BaseFragment)?.start(
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

    private fun getMediaApi(): MediaApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(MediaApi::class.java)
        } else {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        launcher?.getService()?.getAudioPlayer()?.removeListener(this)
    }
}