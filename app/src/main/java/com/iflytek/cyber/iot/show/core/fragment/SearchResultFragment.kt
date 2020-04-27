package com.iflytek.cyber.iot.show.core.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.AudioSearchResultAdapter
import com.iflytek.cyber.iot.show.core.adapter.SpacesItemDecoration
import com.iflytek.cyber.iot.show.core.adapter.VideoSearchResultAdapter
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.model.Error
import com.iflytek.cyber.iot.show.core.model.PlayBody
import com.iflytek.cyber.iot.show.core.model.Result
import com.iflytek.cyber.iot.show.core.model.SearchBody
import com.iflytek.cyber.iot.show.core.model.SearchResult
import com.iflytek.cyber.iot.show.core.utils.InsetDividerDecoration
import com.iflytek.cyber.iot.show.core.utils.dp2Px
import com.iflytek.cyber.iot.show.core.utils.onLoadMore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchResultFragment : BaseFragment() {

    private lateinit var audioList: RecyclerView
    private lateinit var videoList: RecyclerView

    private var results: ArrayList<Result>? = null
    private var keyword: String? = null
    private var listType: Int? = -1
    private var page = 1
    private var isLoading = false

    private var audioSearchResultAdapter: AudioSearchResultAdapter? = null
    private var videoSearchResultAdapter: VideoSearchResultAdapter? = null

    companion object {

        const val TYPE_AUDIO = 10322
        const val TYPE_VIDEO = 10323

        fun newInstance(
            keyword: String,
            type: Int,
            results: ArrayList<Result>
        ): SearchResultFragment {
            return SearchResultFragment().apply {
                arguments = bundleOf(
                    Pair("keyword", keyword),
                    Pair("results", results),
                    Pair("type", type)
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_result, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        audioList = view.findViewById(R.id.audio_list)
        videoList = view.findViewById(R.id.video_list)
        val title = view.findViewById<TextView>(R.id.title)

        view.findViewById<View>(R.id.back).setOnClickListener { pop() }

        keyword = arguments?.getString("keyword")
        listType = arguments?.getInt("type")
        results = arguments?.getParcelableArrayList<Result>("results")

        if (listType == TYPE_AUDIO) {
            title.text = "“${keyword}” 音频搜索结果"
            audioList.isVisible = true
            setupAudioList()
        } else if (listType == TYPE_VIDEO) {
            title.text = "“${keyword}” 视频搜索结果"
            videoList.isVisible = true
            setupVideoList()
        }
    }

    private fun setupAudioList() {
        val itemDecoration = InsetDividerDecoration(
            1.dp2Px(),
            ContextCompat.getColor(requireContext(), R.color.dividerLight), 1
        )
        itemDecoration.startPadding = resources.getDimensionPixelOffset(R.dimen.dp_32)
        itemDecoration.endPadding = resources.getDimensionPixelOffset(R.dimen.dp_32)
        audioList.addItemDecoration(itemDecoration)
        if (audioSearchResultAdapter == null && results != null) {
            audioSearchResultAdapter = AudioSearchResultAdapter(results!!) {
                playSearchMedia(it)
            }
           /* if (results!!.size < 10) {
                audioSearchResultAdapter?.loadingFinish(true)
            }*/
            audioList.adapter = audioSearchResultAdapter
        }

        audioList.onLoadMore {
            if (!isLoading) {
                page += 1
                loadMore()
            }
        }
    }

    private fun setupVideoList() {
        val layoutManager = GridLayoutManager(requireContext(), 4)
        videoList.layoutManager = layoutManager

        val decoration = SpacesItemDecoration(0)
        decoration.top = resources.getDimensionPixelSize(R.dimen.dp_12)
        decoration.bottom = resources.getDimensionPixelSize(R.dimen.dp_12)
        videoList.addItemDecoration(decoration)

        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (videoSearchResultAdapter != null &&
                    videoSearchResultAdapter?.getItemViewType(position) == R.layout.item_loading
                ) {
                    4
                } else {
                    1
                }
            }
        }

        if (videoSearchResultAdapter == null) {
            videoSearchResultAdapter = VideoSearchResultAdapter {
                playSearchMedia(it)
            }
            if (results != null) {
                videoSearchResultAdapter?.setResults("", results!!)
            }
            /*if (results!!.size < 10) {
                videoSearchResultAdapter?.loadingFinish(true)
            }*/
            videoList.adapter = videoSearchResultAdapter
        }

        videoList.onLoadMore {
            if (!isLoading) {
                page += 1
                loadMore()
            }
        }
    }

    private fun loadMore() {
        if (keyword.isNullOrEmpty()) {
            return
        }
        isLoading = true
        val body = SearchBody(keyword!!, 20, page)
        getMediaApi()?.search(body)?.enqueue(object : Callback<ArrayList<SearchResult>> {
            override fun onResponse(
                call: Call<ArrayList<SearchResult>>,
                response: Response<ArrayList<SearchResult>>
            ) {
                isLoading = false
                if (response.isSuccessful) {
                    val searchResult = response.body()
                    if (searchResult.isNullOrEmpty()) {
                        if (listType == TYPE_AUDIO) {
                            audioSearchResultAdapter?.loadingFinish(true)
                        } else {
                            videoSearchResultAdapter?.loadingFinish(true)
                        }
                    } else {
                        searchResult.forEach {
                            if (listType == TYPE_AUDIO && it.type == 1) {
                                if (it.results.isNullOrEmpty()) {
                                    audioSearchResultAdapter?.loadingFinish(true)
                                } else {
                                    results?.addAll(it.results)
                                }
                            } else if (listType == TYPE_VIDEO && it.type == 2) {
                                if (it.results.isNullOrEmpty()) {
                                    videoSearchResultAdapter?.loadingFinish(true)
                                } else {
                                    results?.addAll(it.results)
                                }
                            }
                        }
                    }
                    if (listType == TYPE_AUDIO) {
                        audioSearchResultAdapter?.notifyDataSetChanged()
                    } else if (listType == TYPE_VIDEO && results != null) {
                        videoSearchResultAdapter?.setResults("", results!!)
                        videoSearchResultAdapter?.notifyDataSetChanged()
                    }
                } else {
                    page -= 1
                    if (listType == TYPE_AUDIO) {
                        audioSearchResultAdapter?.loadingFinish(true)
                    } else if (listType == TYPE_VIDEO) {
                        videoSearchResultAdapter?.loadingFinish(true)
                    }
                }
            }

            override fun onFailure(call: Call<ArrayList<SearchResult>>, t: Throwable) {
                page -= 1
                isLoading = false
                t.printStackTrace()
            }
        })
    }

    private fun playSearchMedia(result: Result) {
        val body = PlayBody(result.id, result.business, result.source)
        getMediaApi()?.playSearchMedia(body)?.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "播放${result.title}", Toast.LENGTH_SHORT).show()
                } else {
                    showError(response.errorBody()?.string())
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
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

    private fun getMediaApi(): MediaApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(MediaApi::class.java)
        } else {
            null
        }
    }
}

