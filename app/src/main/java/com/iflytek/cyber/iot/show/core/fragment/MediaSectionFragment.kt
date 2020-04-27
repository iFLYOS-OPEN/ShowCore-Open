package com.iflytek.cyber.iot.show.core.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.evs.sdk.socket.Result
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.EngineService
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.MediaSectionAdapter
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.model.Group
import com.iflytek.cyber.iot.show.core.model.SongList
import kotlinx.android.synthetic.main.item_app.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.UnknownHostException

class MediaSectionFragment : BaseFragment() {

    companion object {
        fun newInstance(name: String?, groups: List<Group>): MediaSectionFragment {
            return MediaSectionFragment().apply {
                arguments = bundleOf(
                    Pair("name", name),
                    Pair("groups", groups)
                )
            }
        }
    }

    private lateinit var sectionList: RecyclerView
    private var placeholderView: View? = null

    private var mediaSectionAdapter: MediaSectionAdapter? = null

    private var requestingGroupId: String? = null


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

        placeholderView = view.findViewById(R.id.placeholder)

        sectionList.postDelayed({
            hidePlaceholder()
        }, 200)
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        val name = arguments?.getString("name")
        val groups = arguments?.getParcelableArrayList<Group>("groups")

        if (name.isNullOrEmpty() || groups == null) {
            return
        }

        mediaSectionAdapter = MediaSectionAdapter(name, groups, {
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
                            SongListFragment.instance(songList, name)
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
        }, { position ->
            val groupId = groups[position].id
            requestingGroupId = groupId

            getMediaApi()?.getMediaSection(groupId)?.enqueue(object :
                Callback<Group> {
                override fun onFailure(call: Call<Group>, t: Throwable) {
                    if (requestingGroupId != groupId)
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

                override fun onResponse(call: Call<Group>, response: Response<Group>) {
                    if (requestingGroupId != groupId)
                        return
                    if (response.isSuccessful) {
                        val group = response.body() ?: return

                        (parentFragment as BaseFragment).start(
                            MediaSectionListFragment.instance(group, null, name)
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
        })
        sectionList.adapter = mediaSectionAdapter
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

    private fun getMediaApi(): MediaApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(MediaApi::class.java)
        } else {
            null
        }
    }
}