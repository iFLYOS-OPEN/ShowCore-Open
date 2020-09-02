package com.iflytek.cyber.iot.show.core.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.os.bundleOf
import androidx.core.os.postDelayed
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.google.gson.Gson
import com.iflytek.cyber.evs.sdk.agent.AudioPlayer
import com.iflytek.cyber.evs.sdk.agent.VideoPlayer
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.LatestListAdapter
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.model.*
import com.iflytek.cyber.iot.show.core.utils.InsetDividerDecoration
import com.iflytek.cyber.iot.show.core.utils.ScreenUtils
import com.iflytek.cyber.iot.show.core.utils.dp2Px
import com.iflytek.cyber.iot.show.core.utils.onLoadMore
import com.iflytek.cyber.iot.show.core.widget.CircleCheckBox
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LatestMediaFragment : BaseFragment(), View.OnClickListener,
    AudioPlayer.MediaStateChangedListener {

    companion object {

        fun newInstance(tag: Tags): LatestMediaFragment {
            return LatestMediaFragment().apply {
                arguments = bundleOf(Pair("tag", tag))
            }
        }
    }

    private var recyclerView: RecyclerView? = null
    private var emptyContent: LinearLayout? = null
    private var latestTag: TextView? = null
    private var loadingImageView: LottieAnimationView? = null
    private var retryView: LinearLayout? = null
    private var retryButton: TextView? = null
    private var headerView: View? = null
    private var selectView: View? = null
    private var cancelButton: Button? = null
    private var selectAllButton: CircleCheckBox? = null
    private var deleteButton: Button? = null
    private var popupWindow: PopupWindow? = null
    private var ivMultiSelect: ImageView? = null
    private var tvMultiSelect: TextView? = null
    private var tvPlayAll: TextView? = null
    private var ivPlayAllIcon: ImageView? = null
    private var playAllView: View? = null
    private var multiSelectView: View? = null

    private var adapter: LatestListAdapter? = null

    private var handler = Handler(Looper.getMainLooper())

    private var currentPage = 1
    private var currentTag: Tags? = null
    private var isLoading = false
    private var isDeleting = false
    private var totalCount = 0
    private var currentVideoResourceId: String? = null
    private var isVideoPlaying = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_latest_media, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launcher?.getService()?.getAudioPlayer()?.addListener(this)
        launcher?.getService()?.getVideoPlayer()?.addListener(videoStateChangedListener)

        currentTag = arguments?.getParcelable("tag")

        recyclerView = view.findViewById(R.id.recycler_view)
        emptyContent = view.findViewById(R.id.empty_content)
        loadingImageView = view.findViewById(R.id.loading_image)
        retryView = view.findViewById(R.id.retry_view)
        retryButton = view.findViewById(R.id.retry)
        latestTag = view.findViewById(R.id.latest_tag)
        headerView = view.findViewById(R.id.header_view)
        selectView = view.findViewById(R.id.select_view)
        cancelButton = view.findViewById(R.id.btn_cancel)
        selectAllButton = view.findViewById(R.id.btn_select_all)
        deleteButton = view.findViewById(R.id.btn_delete)
        latestTag?.text = "最近播放${currentTag?.name}"
        ivMultiSelect = view.findViewById(R.id.iv_multi_select)
        tvMultiSelect = view.findViewById(R.id.tv_multi_select)
        tvPlayAll = view.findViewById(R.id.play_all)
        ivPlayAllIcon = view.findViewById(R.id.iv_play_all_icon)
        playAllView = view.findViewById(R.id.play_all_view)
        multiSelectView = view.findViewById(R.id.multi_select)

        retryButton?.setOnClickListener(this)
        deleteButton?.setOnClickListener(this)
        view.findViewById<View>(R.id.multi_select).setOnClickListener(this)
        view.findViewById<View>(R.id.btn_select_all).setOnClickListener(this)
        playAllView?.setOnClickListener(this)

        cancelButton?.setOnClickListener(this)

        adapter = LatestListAdapter({ item, position ->
            playMedia(item, position)
        }, { anchorView, latestRecord ->
            showContextMenu(anchorView, latestRecord)
        }, { size ->
            if (size > 0) {
                deleteButton?.text = "删除(${size})"
                deleteButton?.isSelected = true
                deleteButton?.isClickable = true
            } else {
                deleteButton?.text = "删除"
                deleteButton?.isSelected = false
                deleteButton?.isClickable = false
            }
            val itemCount = adapter?.itemCount
            val isSelectAll = size == itemCount
            if (isSelectAll) {
                selectAllButton?.isChecked = true
                selectAllButton?.text = "取消全选"
            } else {
                selectAllButton?.isChecked = false
                selectAllButton?.text = "全选"
            }
        })
        val decoration = InsetDividerDecoration(
            0.5f.dp2Px().toInt(),
            Color.parseColor("#e6e6e6"),
            1
        )
        decoration.startPadding = 32.dp2Px()
        decoration.endPadding = 32.dp2Px()
        recyclerView?.addItemDecoration(decoration)
        if (currentTag?.id == 3) { //视频
            adapter?.isShowCover(true)
        }
        recyclerView?.adapter = adapter

        /*recyclerView?.onLoadMore {
            if (!isLoading && adapter?.itemCount ?: 0 < totalCount && adapter?.isSelected() == false) {
                currentTag?.let {
                    currentPage += 1
                    loadLatestRecords(it.id, false)
                }
            }
        }*/

        currentTag?.let {
            showLoading()
            loadLatestRecords(it.id)
        }
    }

    private val videoStateChangedListener = object : VideoPlayer.VideoStateChangedListener {
        override fun onStarted(player: VideoPlayer, resourceId: String) {
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed(200) {
                if (currentVideoResourceId != resourceId) {
                    currentVideoResourceId = resourceId
                    isVideoPlaying = true
                    stickLatestPlayingItem()
                }
            }
        }

        override fun onResumed(player: VideoPlayer, resourceId: String) {
            recyclerView?.post {
                isVideoPlaying = true
                stickLatestPlayingItem()
            }
        }

        override fun onPaused(player: VideoPlayer, resourceId: String) {
            recyclerView?.post {
                isVideoPlaying = false
                stickLatestPlayingItem()
            }
        }

        override fun onStopped(player: VideoPlayer, resourceId: String) {
            recyclerView?.post {
                isVideoPlaying = false
                stickLatestPlayingItem()
            }
        }

        override fun onCompleted(player: VideoPlayer, resourceId: String) {
            recyclerView?.post {
                isVideoPlaying = false
                stickLatestPlayingItem()
            }
        }

        override fun onPositionUpdated(player: VideoPlayer, resourceId: String, position: Long) {
        }

        override fun onError(player: VideoPlayer, resourceId: String, errorCode: String) {
        }
    }

    override fun onStarted(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            recyclerView?.postDelayed(500) {
                //adapter?.notifyDataSetChanged()
                stickLatestPlayingItem()
            }
        }
    }

    override fun onResumed(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            recyclerView?.postDelayed(300) {
                //adapter?.notifyDataSetChanged()
                stickLatestPlayingItem()
            }
        }
    }

    override fun onPaused(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            recyclerView?.postDelayed(300) {
                //adapter?.notifyDataSetChanged()
                stickLatestPlayingItem()
            }
        }
    }

    override fun onStopped(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            recyclerView?.postDelayed(300) {
                //adapter?.notifyDataSetChanged()
                stickLatestPlayingItem()
            }
        }
    }

    override fun onCompleted(player: AudioPlayer, type: String, resourceId: String) {
        if (type == AudioPlayer.TYPE_PLAYBACK) {
            recyclerView?.postDelayed(300) {
                //adapter?.notifyDataSetChanged()
                stickLatestPlayingItem()
            }
        }
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

    private fun setupList(items: ArrayList<LatestRecord>?, clear: Boolean) {
        if (!items.isNullOrEmpty()) {
            adapter?.apply {
                setItems(items, clear)
                notifyDataSetChanged()
            }
            if (clear) {
                recyclerView?.scrollToPosition(0)
            }
        }
    }

    private fun loadLatestRecords(tagId: Int, clear: Boolean = true) {
        isLoading = true
        getMediaApi()?.getLatestRecords(tagId)
            ?.enqueue(object : Callback<LatestResult> {
                override fun onFailure(call: Call<LatestResult>, t: Throwable) {
                    t.printStackTrace()
                    if (context == null || !isAdded) {
                        return
                    }
                    retryView?.isVisible = true
                    if (!clear) {
                        currentPage -= 1
                    }
                    dismissLoading()
                }

                override fun onResponse(
                    call: Call<LatestResult>,
                    response: Response<LatestResult>
                ) {
                    if (context == null || !isAdded) {
                        return
                    }

                    dismissLoading()
                    if (response.isSuccessful) {
                        retryView?.isVisible = false
                        val result = response.body()
                        result?.let {
                            totalCount = it.total
                            setUIClickable(it.result.isNullOrEmpty())
                            if (it.result.isNullOrEmpty()) {
                                emptyContent?.isVisible = true
                            } else {
                                emptyContent?.isVisible = false
                                setupList(it.result, clear)
                            }
                        }
                    } else {
                        retryView?.isVisible = true
                        if (!clear) {
                            currentPage -= 1
                        }
                    }
                }
            })
    }

    private fun setUIClickable(isEmpty: Boolean) {
        if (isEmpty) {
            tvMultiSelect?.isEnabled = false
            ivMultiSelect?.isEnabled = false
            tvPlayAll?.isEnabled = false
            ivPlayAllIcon?.setColorFilter(Color.parseColor("#CAD0D6"))
            playAllView?.setBackgroundResource(R.drawable.bg_play_all_disable_border)
            multiSelectView?.isClickable = false
            playAllView?.isClickable = false
        } else {
            tvMultiSelect?.isEnabled = true
            ivMultiSelect?.isEnabled = true
            tvPlayAll?.isEnabled = true
            ivPlayAllIcon?.setColorFilter(Color.parseColor("#262626"))
            playAllView?.setBackgroundResource(R.drawable.bg_play_all_black_border)
            playAllView?.isClickable = true
            multiSelectView?.isClickable = true
        }
    }

    private fun switchSelectedState() {
        if (adapter?.isSelected() == true) {
            headerView?.isVisible = true
            selectView?.isVisible = false
            deleteButton?.text = "删除"
            deleteButton?.isSelected = false
            deleteButton?.isClickable = false
        } else {
            headerView?.isVisible = false
            selectView?.isVisible = true
        }
        selectAllButton?.text = "全选"
        selectAllButton?.isChecked = false
        adapter?.switchSelectedState()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.multi_select, R.id.btn_cancel -> {
                switchSelectedState()
            }
            R.id.retry -> {
                currentTag?.let {
                    showLoading()
                    loadLatestRecords(it.id)
                }
            }
            R.id.btn_select_all -> {
                if (adapter?.isSelectAll() == true) {
                    selectAllButton?.isChecked = false
                    selectAllButton?.text = "全选"
                } else {
                    selectAllButton?.isChecked = true
                    selectAllButton?.text = "取消全选"
                }
                adapter?.selectAll()
                val length = adapter?.getSelectIds()?.size ?: 0
                if (length > 0) {
                    deleteButton?.text = "删除(${length})"
                    deleteButton?.isSelected = true
                    deleteButton?.isClickable = true
                } else {
                    deleteButton?.text = "删除"
                    deleteButton?.isSelected = false
                    deleteButton?.isClickable = false
                }
            }
            R.id.btn_delete -> {
                val ids = adapter?.getSelectIds()
                val itemCount = adapter?.itemCount
                if (!ids.isNullOrEmpty()) {
                    val isSelectAll = ids.size == itemCount
                    deleteRecord(ids, isSelectAll)
                }
            }
            R.id.play_all_view -> {
                val item = adapter?.getItem(0)
                if (item != null) {
                    playMedia(item, 0)
                }
            }
        }
    }

    private fun showLoading() {
        retryView?.isVisible = false
        loadingImageView?.isVisible = true
        loadingImageView?.playAnimation()
    }

    private fun dismissLoading() {
        isLoading = false
        loadingImageView?.isVisible = false
        loadingImageView?.pauseAnimation()
    }

    override fun onSupportInvisible() {
        super.onSupportInvisible()
        popupWindow?.dismiss()
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        adapter?.notifyDataSetChanged()
    }

    private fun stickLatestPlayingItem() {
        val playerInfo = ContentStorage.get().playerInfo
        val items = adapter?.getItems() ?: return
        var changeItem: LatestRecord? = null
        for (item in items) {
            if (item.musicId == playerInfo?.resourceId ||
                item.musicId == currentVideoResourceId
            ) {
                changeItem = item
                break
            }
        }
        if (changeItem != null) {
            items.remove(changeItem)
            items.add(0, changeItem)
            adapter?.notifyDataSetChanged()
            recyclerView?.scrollToPosition(0)
        } else {
            currentTag?.let {
                if (!isLoading) {
                    currentPage = 1
                    loadLatestRecords(it.id)
                }
            }
        }
    }

    private fun showContextMenu(anchorView: View, item: LatestRecord) {
        if (context == null || !isAdded) {
            return
        }
        popupWindow = PopupWindow(context)
        popupWindow?.width = ViewGroup.LayoutParams.WRAP_CONTENT
        popupWindow?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        popupWindow?.isOutsideTouchable = true
        popupWindow?.isFocusable = true
        popupWindow?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val view = LayoutInflater.from(context).inflate(R.layout.layout_latest_popup, null, false)
        val deleteButton = view.findViewById<TextView>(R.id.btn_delete)
        deleteButton.setOnClickListener {
            popupWindow?.dismiss()
            val ids = arrayListOf(item.id)
            deleteRecord(ids, false)
        }
        val openAlbumButton = view.findViewById<TextView>(R.id.btn_open_album)
        openAlbumButton.setOnClickListener {
            popupWindow?.dismiss()
            (parentFragment as BaseFragment).start(SongListFragment.newInstance(item, 3))
        }
        popupWindow?.contentView = view
        val loc = intArrayOf(0, 0)
        anchorView.getLocationInWindow(loc)
        var xOff = loc[0]
        var yOff = loc[1]
        val screenHeight = ScreenUtils.getHeight(context)
        val limit = screenHeight - yOff - 24.dp2Px()
        var viewHeight = 136.dp2Px()
        val viewWidth = 144.dp2Px()
        if (item.albumId.isNullOrEmpty()) {
            openAlbumButton.isVisible = false
            viewHeight = 98.dp2Px()
        } else {
            openAlbumButton.isVisible = true
        }
        if (limit > viewHeight) {
            xOff -= viewWidth - anchorView.width / 2 - 16.dp2Px()
            yOff += anchorView.height / 2 + 8.dp2Px()
        } else {
            xOff -= viewWidth - anchorView.width / 2 - 16.dp2Px()
            yOff -= viewHeight - anchorView.height / 2
        }
        popupWindow?.showAtLocation(anchorView, Gravity.NO_GRAVITY, xOff, yOff)
    }

    private fun deleteRecord(ids: ArrayList<Long>, isSelectAll: Boolean) {
        isDeleting = true
        val json = JSONObject()
        if (!isSelectAll) {
            val array = JSONArray()
            ids.forEach {
                array.add(it)
            }
            json["ids"] = array
        } else {
            json["tag_id"] = currentTag?.id
        }
        val body = RequestBody.create(
            MediaType.parse("application/json"),
            json.toString()
        )
        getMediaApi()?.deleteRecord(body)?.enqueue(object : Callback<Message> {
            override fun onFailure(call: Call<Message>, t: Throwable) {
                t.printStackTrace()
                isDeleting = false
            }

            override fun onResponse(call: Call<Message>, response: Response<Message>) {
                isDeleting = false
                if (context == null || !isAdded) {
                    return
                }
                if (response.isSuccessful) {
                    adapter?.updateItems(ids)
                    if (selectView?.isVisible == true) {
                        switchSelectedState()
                    } else {
                        adapter?.notifyDataSetChanged()
                    }
                    if (isSelectAll) {
                        emptyContent?.isVisible = true
                    }
                    setUIClickable(adapter?.itemCount == 0)
                } else {
                    showToast("删除失败，请稍后重试")
                }
            }
        })
    }

    private fun playMedia(item: LatestRecord, position: Int) {
        val json = JSONObject()
        json["source_type"] = item.sourceType
        json["media_id"] = item.musicId
        json["tag_id"] = currentTag?.id
        val body = RequestBody.create(
            MediaType.parse("application/json"),
            json.toString()
        )
        getMediaApi()?.playRecord(body)?.enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (context == null || !isAdded) {
                    return
                }
                if (response.isSuccessful) {
                    /*currentTag?.let {
                        currentPage = 1
                        loadLatestRecords(it.id)
                    }*/
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

                ((parentFragment) as BaseFragment).start(
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

    private fun showToast(message: String) {
        if (context == null || !isAdded) {
            return
        }
        Toast.makeText(launcher!!, message, Toast.LENGTH_SHORT).show()
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
        launcher?.getService()?.getVideoPlayer()?.removeListener(videoStateChangedListener)
        launcher?.getService()?.getAudioPlayer()?.removeListener(this)
    }
}