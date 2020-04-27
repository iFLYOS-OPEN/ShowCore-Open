package com.iflytek.cyber.iot.show.core.fragment

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.MultiTypeAdapter
import com.google.gson.Gson
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.AudioTypeViewBinder
import com.iflytek.cyber.iot.show.core.adapter.VideoTypeViewBinder
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.model.*
import com.iflytek.cyber.iot.show.core.utils.ACache
import com.iflytek.cyber.iot.show.core.utils.KeyboardUtils
import com.zhy.view.flowlayout.FlowLayout
import com.zhy.view.flowlayout.TagAdapter
import com.zhy.view.flowlayout.TagFlowLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchFragment : BaseFragment(), View.OnClickListener {

    companion object {
        private const val KEY_TAGS = "key_tags"
    }

    data class HistoryTag(
        val tags: ArrayList<String>
    )

    private lateinit var recommendTagLayout: TagFlowLayout
    private lateinit var historyTagLayout: TagFlowLayout
    private lateinit var scrollView: NestedScrollView
    private lateinit var mainContent: FrameLayout
    private lateinit var edtQuery: EditText
    private lateinit var clear: ImageView
    private lateinit var deleteTag: ImageView
    private lateinit var tvSearchTag: TextView
    private lateinit var tvRecommend: TextView
    private lateinit var resultList: RecyclerView
    //private lateinit var hintList: RecyclerView
    //private lateinit var hintContent: LinearLayout
    private lateinit var toolbar: FrameLayout
    private lateinit var tvSearchEmpty: TextView
    private lateinit var retryContent: LinearLayout
    private lateinit var retry: TextView

    //private var hintResultAdapter: HintResultAdapter? = null
    private var resultAdapter: MultiTypeAdapter? = null
    private lateinit var audioTypeViewBinder: AudioTypeViewBinder
    private lateinit var videoTypeViewBinder: VideoTypeViewBinder

    private var historyTags = ArrayList<String>()
    private var isSearch = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar = view.findViewById(R.id.toolbar)
        recommendTagLayout = view.findViewById(R.id.recommend_tag_layout)
        historyTagLayout = view.findViewById(R.id.history_tag_layout)
        resultList = view.findViewById(R.id.result_list)
        //hintList = view.findViewById(R.id.hint_list)
        //hintContent = view.findViewById(R.id.hint_content)
        scrollView = view.findViewById(R.id.scroll_view)
        mainContent = view.findViewById(R.id.main_content)
        deleteTag = view.findViewById(R.id.delete)
        deleteTag.setOnClickListener(this)
        tvSearchTag = view.findViewById(R.id.tv_search_tag)
        tvRecommend = view.findViewById(R.id.tv_recommend)
        edtQuery = view.findViewById(R.id.edt_query)
        clear = view.findViewById(R.id.clear)
        tvSearchEmpty = view.findViewById(R.id.tv_search_empty)
        retryContent = view.findViewById(R.id.retry_content)
        retry = view.findViewById(R.id.tv_retry)
        retry.setOnClickListener(this)
        clear.setOnClickListener(this)

        view.findViewById<View>(R.id.back).setOnClickListener { pop() }

        edtQuery.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    clear.isVisible = false
                    scrollView.isVisible = true
                    resultList.isVisible = false
                    toolbar.setBackgroundColor(Color.WHITE)
                    mainContent.setBackgroundColor(Color.WHITE)
                } else {
                    clear.isVisible = true
                }
            }
        })

        /*edtQuery.textChanges()
            .map { s ->
                isSearch = false
                if (s.isNotEmpty()) {
                    mainContent.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.grey_100
                        )
                    )
                    scrollView.isVisible = false
                    clear.isVisible = true
                } else {
                    mainContent.setBackgroundColor(Color.WHITE)
                    scrollView.isVisible = true
                    clear.isVisible = false
                }
                return@map s
            }
            .debounce(400, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .subscribeOn(AndroidSchedulers.mainThread())
            .filter { s -> return@filter s.isNotEmpty() }
            .switchMap { value ->
                if (!TextUtils.isEmpty(value.toString())) {
                    search(value.toString())
                }
                return@switchMap Observable.just(value)
            }
            .subscribe()*/

        edtQuery.setOnKeyListener { v, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener true
            }

            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                isSearch = true
                if (!edtQuery.text.isNullOrEmpty()) {
                    KeyboardUtils.closeKeyboard(edtQuery)
                    search(edtQuery.text.toString())
                }
            } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                pop()
            }
            return@setOnKeyListener false
        }

        loadRecommendTag()

        loadHistoryTag()

        videoTypeViewBinder = VideoTypeViewBinder({
            playSearchMedia(it)
        }, {
            start(
                SearchResultFragment.newInstance(
                    edtQuery.text.toString(),
                    SearchResultFragment.TYPE_VIDEO, it
                )
            )
        })
        audioTypeViewBinder = AudioTypeViewBinder({
            playSearchMedia(it)
        }, {
            start(
                SearchResultFragment.newInstance(
                    edtQuery.text.toString(),
                    SearchResultFragment.TYPE_AUDIO, it
                )
            )
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.clear -> {
                edtQuery.text = null
                mainContent.setBackgroundColor(Color.WHITE)
                toolbar.setBackgroundColor(Color.WHITE)
                scrollView.isVisible = true
                resultList.isVisible = false
                tvSearchEmpty.isVisible = false
                retryContent.isVisible = false
            }
            R.id.delete -> {
                ACache.get(launcher!!).put(KEY_TAGS, "")
                historyTags.clear()
                setupHistoryTag(historyTags)
            }
            R.id.tv_retry -> {
                if (!edtQuery.text.isNullOrEmpty()) {
                    search(edtQuery.text.toString())
                }
            }
        }
    }

    private fun setupRecommendTag(tags: ArrayList<String>) {
        recommendTagLayout.adapter = object : TagAdapter<String>(tags) {
            override fun getView(parent: FlowLayout?, position: Int, tag: String?): View {
                val button = LayoutInflater.from(launcher!!)
                    .inflate(R.layout.item_recommend_tag, parent, false) as TextView
                button.text = tag
                return button
            }
        }
        recommendTagLayout.setOnTagClickListener { view, position, parent ->
            KeyboardUtils.closeKeyboard(edtQuery)
            val tag = tags[position]
            edtQuery.setText(tag)
            edtQuery.setSelection(tag.length)
            addNewTag(tag)
            search(tag)
            return@setOnTagClickListener true
        }
    }

    private fun setupHistoryTag(tags: ArrayList<String>) {
        if (tags.size == 0) {
            tvSearchTag.isVisible = false
            deleteTag.isVisible = false
        } else {
            tvSearchTag.isVisible = true
            deleteTag.isVisible = true
        }
        historyTagLayout.adapter = object : TagAdapter<String>(tags) {
            override fun getView(parent: FlowLayout?, position: Int, tag: String?): View {
                val button = LayoutInflater.from(launcher!!)
                    .inflate(R.layout.item_history_tag, parent, false) as Button
                button.text = tag
                return button
            }
        }
        historyTagLayout.setOnTagClickListener { view, position, parent ->
            KeyboardUtils.closeKeyboard(edtQuery)
            val tag = tags[position]
            edtQuery.setText(tag)
            edtQuery.setSelection(tag.length)
            search(tag)
            return@setOnTagClickListener true
        }
    }

    private fun loadRecommendTag() {
        getMediaApi()?.loadRecommendTag("showcore")?.enqueue(object : Callback<RecommendTag> {
            override fun onResponse(call: Call<RecommendTag>, response: Response<RecommendTag>) {
                if (response.isSuccessful) {
                    val recommendTag = response.body()
                    if (recommendTag != null && !recommendTag.tags.isNullOrEmpty()) {
                        tvRecommend.isVisible = true
                        setupRecommendTag(recommendTag.tags)
                    } else {
                        tvRecommend.isVisible = false
                    }
                }
            }

            override fun onFailure(call: Call<RecommendTag>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    private fun loadHistoryTag() {
        val value = ACache.get(launcher!!).getAsString(KEY_TAGS)
        if (!value.isNullOrEmpty()) {
            val result = Gson().fromJson(value, HistoryTag::class.java)
            historyTags = result.tags
            setupHistoryTag(result.tags)
        } else {
            tvSearchTag.isVisible = false
            deleteTag.isVisible = false
        }
    }

    private fun search(keyword: String) {
        addNewTag(keyword)
        val body = SearchBody(keyword, limit = 10)
        getMediaApi()?.search(body)?.enqueue(object : Callback<ArrayList<SearchResult>> {
            override fun onResponse(
                call: Call<ArrayList<SearchResult>>,
                response: Response<ArrayList<SearchResult>>
            ) {
                retryContent.isVisible = false
                scrollView.isVisible = true
                if (response.isSuccessful) {
                    resultList.scrollToPosition(0)
                    val result = response.body()
                    tvSearchEmpty.isVisible = result.isNullOrEmpty()
                    scrollView.isVisible = !result.isNullOrEmpty()
                    resultList.isVisible = !result.isNullOrEmpty()
                    setupResultAdapter(result, keyword)
                }
            }

            override fun onFailure(call: Call<ArrayList<SearchResult>>, t: Throwable) {
                t.printStackTrace()
                if (isAdded && context != null) {
                    retryContent.isVisible = true
                    scrollView.isVisible = false
                    tvSearchEmpty.isVisible = false
                    resultList.isVisible = false
                }
            }
        })
    }

    private fun addNewTag(keyword: String) {
        val index = historyTags.indexOf(keyword)
        if (index == -1) {
            if (historyTags.size > 10) {
                historyTags.removeAt(historyTags.size - 1)
            }
            historyTags.add(0, keyword)
            setupHistoryTag(historyTags)
            val historyTag = HistoryTag(historyTags)
            val result = Gson().toJson(historyTag)
            ACache.get(launcher!!).put(KEY_TAGS, result)
        }
    }

    private fun getMediaApi(): MediaApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(MediaApi::class.java)
        } else {
            null
        }
    }

    private fun setupResultAdapter(items: ArrayList<SearchResult>?, keyword: String) {
        if (items.isNullOrEmpty()) {
            return
        }

        val newItems = ArrayList<SearchResult>(items)
        items.forEach { searchResult ->
            if (searchResult.results.isNullOrEmpty()) {
                newItems.remove(searchResult)
            }
        }

        mainContent.setBackgroundColor(
            ContextCompat.getColor(
                launcher!!,
                R.color.grey_100
            )
        )

        toolbar.setBackgroundColor(ContextCompat.getColor(launcher!!, R.color.grey_100))
        scrollView.isVisible = false
        //hintContent.isVisible = false
        resultList.isVisible = true

        audioTypeViewBinder.setKeyword(keyword)
        videoTypeViewBinder.setKeyword(keyword)

        if (resultAdapter == null) {
            resultAdapter = MultiTypeAdapter()

            resultAdapter?.register(SearchResult::class.java)?.to(
                videoTypeViewBinder,
                audioTypeViewBinder
            )?.withKotlinClassLinker { _, item ->
                when (item.type) {
                    2 -> VideoTypeViewBinder::class
                    else -> AudioTypeViewBinder::class
                }
            }

            resultAdapter?.items = newItems
            resultList.adapter = resultAdapter
        } else {
            resultAdapter?.items = newItems
            resultAdapter?.notifyDataSetChanged()
        }
    }

    private fun playSearchMedia(result: Result) {
        val body = PlayBody(result.id, result.business, result.source)
        getMediaApi()?.playSearchMedia(body)?.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    Toast.makeText(launcher!!, "播放${result.title}", Toast.LENGTH_SHORT).show()
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

    /*private fun setupHintAdapter(items: ArrayList<SearchResult>?) {
        if (items.isNullOrEmpty()) {
            return
        }

        toolbar.setBackgroundColor(Color.WHITE)

        val newItems = ArrayList<Result>()

        items.forEachIndexed { _, searchResult ->
            searchResult.results?.forEachIndexed { i, result ->
                if (i < 5) {
                    newItems.add(result)
                }
            }
        }

        if (hintResultAdapter == null) {
            hintResultAdapter = HintResultAdapter(newItems)
            hintList.adapter = hintResultAdapter
        } else {
            hintResultAdapter?.items = newItems
            hintResultAdapter?.notifyDataSetChanged()
        }
    }*/

    /*inner class HintResultAdapter(var items: ArrayList<Result>) :
        RecyclerView.Adapter<HintResultAdapter.HintResultHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HintResultHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_hint_result, parent, false)
            return HintResultHolder(view)
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(holder: HintResultHolder, position: Int) {
            val item = items[position]
            holder.text.text = item.text
            holder.divider.isVisible = position != items.size - 1
            holder.itemView.setOnClickListener {
                playSearchMedia(item)
            }
        }

        inner class HintResultHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val text = itemView.findViewById<TextView>(R.id.text)
            val divider = itemView.findViewById<View>(R.id.divider)
        }
    }*/
}
