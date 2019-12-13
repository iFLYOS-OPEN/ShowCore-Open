package com.iflytek.cyber.iot.show.core.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.evs.sdk.socket.Result
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.EngineService
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.MusicAdapter
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.model.Group
import com.iflytek.cyber.iot.show.core.model.GroupItem
import com.iflytek.cyber.iot.show.core.model.SongList
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.UnknownHostException

class MusicFragment : BaseFragment() {

    companion object {
        fun instance(id: String): MusicFragment {
            return MusicFragment().apply {
                arguments = bundleOf(Pair("id", id))
            }
        }
    }

    private lateinit var sectionList: RecyclerView
    private var errorLayout: View? = null
    private var tvError: TextView? = null
    private var placeholderView: View? = null

    private var requestingGroupId: String? = null

    private var musicAdapter: MusicAdapter? = null

    private var group: Group? = null
    private var groupId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_music, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sectionList = view.findViewById(R.id.section_list)

        errorLayout = view.findViewById(R.id.error_container)
        tvError = view.findViewById(R.id.error_text)
        placeholderView = view.findViewById(R.id.placeholder)

        groupId = arguments?.getString("id")
        groupId?.let { getMediaSection(it) }

        view.findViewById<View>(R.id.refresh)?.setOnClickListener {
            showPlaceholder()
            groupId?.let { getMediaSection(it) }
        }
    }

    private fun setupRecyclerView(items: List<Pair<String?, List<GroupItem>>>) {
        musicAdapter = MusicAdapter(items, {
            requestingGroupId = it.id
            getMediaApi()?.getSongList(it.id, 1, SongListFragment.LIMIT)?.enqueue(object :
                Callback<SongList> {
                override fun onFailure(call: Call<SongList>, t: Throwable) {
                    if (requestingGroupId != it.id)
                        return

                    if (t is UnknownHostException) {
                        val intent = Intent(EngineService.ACTION_SEND_REQUEST_FAILED)
                        intent.putExtra(
                            EngineService.EXTRA_RESULT,
                            Result(Result.CODE_DISCONNECTED, null)
                        )
                        context?.sendBroadcast(intent)
                    }
                }

                override fun onResponse(call: Call<SongList>, response: Response<SongList>) {
                    if (requestingGroupId != it.id)
                        return
                    if (response.isSuccessful) {
                        val songList = response.body() ?: return

                        (parentFragment as BaseFragment).start(
                            SongListFragment.instance(songList, null)
                        )
                    } else {
                        val disconnectNotification =
                            Intent(context, FloatingService::class.java)
                        disconnectNotification.action = FloatingService.ACTION_SHOW_NOTIFICATION
                        disconnectNotification.putExtra(
                            FloatingService.EXTRA_MESSAGE,
                            "请求出错，请稍后再试"
                        )
                        disconnectNotification.putExtra(
                            FloatingService.EXTRA_ICON_RES,
                            R.drawable.ic_default_error_white_40dp
                        )
                        disconnectNotification.putExtra(
                            FloatingService.EXTRA_POSITIVE_BUTTON_TEXT,
                            getString(R.string.i_got_it)
                        )
                        context?.startService(disconnectNotification)
                    }
                }

            })
        }, {
            group?.let { group ->
                (parentFragment as BaseFragment)
                    .start(MediaSectionListFragment.instance(group, it))
            }
        })
        sectionList.adapter = musicAdapter
    }

    private fun showPlaceholder() {
        placeholderView?.let { placeholder ->
            placeholder.isVisible = true
            placeholder.animate().alpha(1f).setDuration(350)
                .start()
        }
    }

    private fun hidePlaceholder() {
        placeholderView?.let { placeholder ->
            placeholder.animate().alpha(0f).setDuration(350)
                .withEndAction {
                    placeholder.isVisible = false
                }
                .start()
        }
    }

    private fun getMediaSection(id: String) {
        getMediaApi()?.getMediaSection(id)?.enqueue(object : Callback<Group> {
            override fun onFailure(call: Call<Group>, t: Throwable) {
                t.printStackTrace()
                hidePlaceholder()

                if (t is UnknownHostException) {
                    tvError?.text = getString(R.string.network_error)
                } else {
                    tvError?.text = getString(R.string.server_error_message)
                }
                errorLayout?.isVisible = true
            }

            override fun onResponse(call: Call<Group>, response: Response<Group>) {
                hidePlaceholder()
                if (response.isSuccessful) {
                    group = response.body()
                    val itemMap = group?.items?.groupBy { it.categoryName }
                    itemMap?.let { setupRecyclerView(it.toList()) }

                    errorLayout?.isVisible = false
                } else {
                    if (response.code() == 401) {
                        tvError?.text = getString(R.string.message_evs_auth_expired)
                    } else {
                        tvError?.text = getString(R.string.request_params_error)
                    }
                    errorLayout?.isVisible = true
                }
            }
        })
    }

    private fun getMediaApi(): MediaApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(MediaApi::class.java)
        } else {
            null
        }
    }
}