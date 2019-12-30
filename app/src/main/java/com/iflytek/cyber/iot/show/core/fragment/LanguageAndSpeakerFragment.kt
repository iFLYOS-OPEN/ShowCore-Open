package com.iflytek.cyber.iot.show.core.fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.util.keyIterator
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.DeviceApi
import com.iflytek.cyber.iot.show.core.impl.prompt.PromptManager
import com.iflytek.cyber.iot.show.core.model.ChatConfigData
import com.iflytek.cyber.iot.show.core.model.InteractionMode
import com.iflytek.cyber.iot.show.core.widget.StyledProgressDialog
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.UnknownHostException
import kotlin.Comparator

class LanguageAndSpeakerFragment : BaseFragment() {
    companion object {
        fun newInstance(chatConfigData: ChatConfigData?): LanguageAndSpeakerFragment {
            val fragment = LanguageAndSpeakerFragment()
            fragment.chatConfigData = chatConfigData
            return fragment
        }
    }

    private var viewPager: ViewPager2? = null
    private var tabLayout: TabLayout? = null
    private var tvNextStep: TextView? = null
    private var progressDialog: StyledProgressDialog? = null

    private var adapter: StepPageAdapter? = null

    private var chatConfigData: ChatConfigData? = null

    private var backCount = 0

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            if (position == getPageCount() - 1) {
                tvNextStep?.setText(R.string.save)
            } else {
                tvNextStep?.setText(R.string.next_step)
            }
        }
    }
    private val onItemSelectedListener = object : SingleChoicePageFragment.OnItemSelectedListener {
        override fun onItemSelect(fragment: SingleChoicePageFragment, position: Int) {
            if (viewPager?.currentItem != getPageCount() - 1) {
                tvNextStep?.performClick()
            }
            when (fragment.tag.toString()) {
                "language" -> {
                    chatConfigData?.interactionModes?.get(position)?.let { mode ->
                        val list = mutableListOf<String>()
                        var selectedItem = 0
                        val icons = mutableListOf<String>()
                        if (mode.speakers?.isNotEmpty() == true) {
                            for (j in mode.speakers.indices) {
                                val speaker = mode.speakers[j]
                                list.add(speaker.voiceName ?: "")
                                icons.add(speaker.image ?: "")
                                if (speaker.vcn == chatConfigData?.config?.vcn) {
                                    selectedItem = j
                                }
                            }
                        }

                        val speakerFragment =
                            if (chatConfigData?.property.isNullOrEmpty()) {
                                adapter?.fragments?.get(1)
                            } else {
                                adapter?.fragments?.get(2)
                            }
                        speakerFragment?.updateData(
                            list.toTypedArray(),
                            icons.toTypedArray(),
                            selectedItem
                        )
                    }
                }
                "speaker" -> {
                    getCurrentInteractionMode()?.let {
                        val speakerSize = it.speakers?.size ?: 0
                        if (speakerSize <= position)
                            return
                        val speakerName = fragment.titles[position]
                        it.speakers ?: return
                        for (speaker in it.speakers) {
                            if (speaker.voiceName == speakerName)
                                speaker.listenUrl?.let { url -> PromptManager.playUrl(url) }
                        }
                    }
                }
                "property" -> {
                    chatConfigData?.interactionModes?.get(
                        adapter?.fragments?.get(0)?.selectedItem ?: 0
                    )?.let { mode ->
                        val speakers = mode.speakers?.toMutableList() ?: return
                        val list = mutableListOf<String>()
                        var selectedItem = 0
                        val icons = mutableListOf<String>()

                        val selectedProperty = chatConfigData?.property?.get(position)
                        val selectedPropertyId = selectedProperty?.propertyId

                        speakers.sortWith(Comparator { o1, o2 ->
                            when {
                                o1.propertyIds?.contains(selectedPropertyId) == true -> -1
                                o2.propertyIds?.contains(selectedPropertyId) == true -> 1
                                else -> 0
                            }
                        })

                        for (j in speakers.indices) {
                            val speaker = speakers[j]
                            list.add(speaker.voiceName ?: "")
                            icons.add(speaker.image ?: "")
                            if (speaker.vcn == chatConfigData?.config?.vcn) {
                                selectedItem = j
                            }
                        }

                        val speakerFragment =
                            if (chatConfigData?.property.isNullOrEmpty()) {
                                adapter?.fragments?.get(1)
                            } else {
                                adapter?.fragments?.get(2)
                            }
                        speakerFragment?.updateData(
                            list.toTypedArray(),
                            icons.toTypedArray(),
                            selectedItem
                        )
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_language_and_speaker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvNextStep = view.findViewById(R.id.next_step)
        val viewPager: ViewPager2 = view.findViewById(R.id.view_pager)
        val tabLayout: TabLayout = view.findViewById(R.id.tab_layout)

        adapter = StepPageAdapter(this)
        viewPager.adapter = adapter

        tabLayout.tabRippleColor = null
        tabLayout.tabMode = TabLayout.MODE_FIXED
        tabLayout.setSelectedTabIndicatorColor(Color.TRANSPARENT)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.setCustomView(R.layout.item_step_tab)
            tab.view.setPadding(0)
            val num = tab.customView?.findViewById<TextView>(R.id.num)
            num?.text = (position + 1).toString()

            val title = tab.customView?.findViewById<TextView>(R.id.title)
            title?.text = when (position) {
                0 -> {
                    getString(R.string.language)
                }
                1 -> {
                    if (chatConfigData?.property?.isNotEmpty() == true) {
                        getString(R.string.human_property)
                    } else {
                        getString(R.string.voice_property)
                    }
                }
                2 -> {
                    getString(R.string.voice_property)
                }
                else -> {
                    null
                }
            }

            tab.customView?.findViewById<View>(R.id.divider_start)?.alpha =
                if (position != 0) 1f else 0f
            tab.customView?.findViewById<View>(R.id.divider_end)?.alpha =
                if (position != getPageCount() - 1)
                    1f
                else 0f
        }.attach()

        viewPager.offscreenPageLimit = 3
        viewPager.registerOnPageChangeCallback(onPageChangeCallback)

        this.viewPager = viewPager
        this.tabLayout = tabLayout

        tvNextStep?.setOnClickListener { nextStep ->
            if (viewPager.currentItem < getPageCount() - 1) {
                viewPager.currentItem = viewPager.currentItem + 1
            } else {
                tvNextStep?.isEnabled = false

                progressDialog = StyledProgressDialog.Builder()
                    .setTitle(getString(R.string.saving))
                    .setMessage(getString(R.string.saving_language_and_speaker))
                    .setCancelable(false)
                    .show(fragmentManager)

                var interactionModeId: Int? = null
                var propertyId: String? = null
                var vcn: String? = null

                adapter?.fragments?.let { fragments ->
                    for (key in fragments.keyIterator()) {
                        val fragment = fragments[key]
                        val tag = fragment.tag
                        val selectedItem = fragment.selectedItem
                        if (tag == "language") {
                            chatConfigData?.interactionModes?.get(selectedItem)?.let {
                                interactionModeId = it.interactionModeId
                            }
                        } else if (tag == "property") {
                            chatConfigData?.property?.get(selectedItem)?.let {
                                propertyId = it.propertyId
                            }
                        } else if (tag == "speaker") {
                            val speakerName = fragment.titles[selectedItem]
                            val selectedModeId =
                                chatConfigData?.interactionModes?.get(fragments[0].selectedItem)
                                    ?.interactionModeId
                            val modes = chatConfigData?.interactionModes
                            if (modes?.isNotEmpty() == true) {
                                for (mode in modes) {
                                    if (mode.interactionModeId == selectedModeId) {
                                        mode.speakers?.let { speakers ->
                                            for (speaker in speakers) {
                                                if (speaker.voiceName == speakerName) {
                                                    vcn = speaker.vcn
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val json = JsonObject()
                interactionModeId?.let {
                    json.addProperty("interaction_mode_id", it)
                }
                propertyId?.let {
                    json.addProperty("property_id", it)
                }
                vcn?.let {
                    json.addProperty("vcn", it)
                }

                getDeviceApi()?.putChatConfig(
                    RequestBody.create(
                        MediaType.parse("application/json"),
                        json.toString()
                    )
                )?.enqueue(object : Callback<ResponseBody> {
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        if (t is UnknownHostException) {
                            Toast.makeText(activity, "网络异常，请检查网络后重试", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(activity, "请求出现错误，请检查网络后重试", Toast.LENGTH_SHORT).show()
                        }
                        progressDialog?.dismiss()
                        progressDialog = null

                        tvNextStep?.isEnabled = true
                    }

                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            Toast.makeText(activity, "保存成功", Toast.LENGTH_SHORT).show()
                            if (!isDetached) {
                                pop()
                            }
                        } else {
                            val errorBody = response.errorBody()
                            val body = errorBody?.string() ?: "{}"
                            val errorJson = JsonParser().parse(body).asJsonObject
                            if (errorJson.has("message")) {
                                Toast.makeText(
                                    activity,
                                    errorJson.get("message").asString,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            errorBody?.close()
                        }
                        progressDialog?.dismiss()
                        progressDialog = null

                        tvNextStep?.isEnabled = true
                    }

                })
            }
        }

        view.findViewById<View>(R.id.back).setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            pop()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        viewPager?.unregisterOnPageChangeCallback(onPageChangeCallback)
    }

    private fun getPageCount(): Int {
        chatConfigData?.let {
            if (it.property?.isNotEmpty() == true) {
                return 3
            }
            return 2
        }
        return 0
    }

    private fun getCurrentInteractionMode(): InteractionMode? {
        chatConfigData?.let {
            if (it.interactionModes?.isNotEmpty() == true) {
                val fragment = adapter?.fragments?.get(0) ?: return null
                return it.interactionModes[fragment.selectedItem]
            }
        }
        return null
    }

    private fun getDeviceApi(): DeviceApi? {
        val context = context ?: return null
        return CoreApplication.from(context).createApi(DeviceApi::class.java)
    }

    private inner class StepPageAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        val fragments = SparseArray<SingleChoicePageFragment>()

        override fun getItemCount(): Int {
            return getPageCount()
        }

        override fun createFragment(position: Int): Fragment {
            val fragment = when (position) {
                0 -> {
                    val list = mutableListOf<String>()
                    var selectedItem = 0
                    chatConfigData?.let {
                        if (it.interactionModes?.isNotEmpty() == true)
                            for (i in it.interactionModes.indices) {
                                val mode = it.interactionModes[i]
                                list.add(mode.name ?: "")
                                if (mode.interactionModeId == it.config?.interactionModeId) {
                                    selectedItem = i
                                }
                            }
                    }
                    SingleChoicePageFragment.newInstance(
                        list.toTypedArray(),
                        null,
                        selectedItem,
                        "language"
                    )
                }
                1 -> {
                    val list = mutableListOf<String>()
                    var selectedItem = 0
                    val icons = mutableListOf<String>()
                    var tag: Any? = null
                    chatConfigData?.let {
                        if (it.property?.isNotEmpty() == true) {
                            for (i in it.property.indices) {
                                val property = it.property[i]
                                list.add(property.name)
                                if (it.config?.propertyId == property.propertyId) {
                                    selectedItem = i
                                }
                            }
                            tag = "property"
                        } else {
                            if (it.interactionModes?.isNotEmpty() == true)
                                for (i in it.interactionModes.indices) {
                                    val mode = it.interactionModes[i]
                                    if (mode.interactionModeId == it.config?.interactionModeId) {
                                        if (mode.speakers?.isNotEmpty() == true) {
                                            for (j in mode.speakers.indices) {
                                                val speaker = mode.speakers[j]
                                                list.add(speaker.voiceName ?: "")
                                                icons.add(speaker.image ?: "")
                                                if (speaker.vcn == it.config?.vcn) {
                                                    selectedItem = j
                                                }
                                            }
                                        }
                                    }
                                }
                            tag = "speaker"
                        }
                    }
                    SingleChoicePageFragment.newInstance(
                        list.toTypedArray(),
                        icons.toTypedArray(),
                        selectedItem,
                        tag
                    )
                }
                2 -> {
                    val tag = "speaker"
                    var selectedModeItem = 0
                    chatConfigData?.let {
                        if (it.interactionModes?.isNotEmpty() == true)
                            for (i in it.interactionModes.indices) {
                                val mode = it.interactionModes[i]
                                if (mode.interactionModeId == it.config?.interactionModeId) {
                                    selectedModeItem = i
                                }
                            }
                    }
                    chatConfigData?.interactionModes?.get(selectedModeItem)?.let { mode ->
                        val speakers = mode.speakers?.toMutableList()
                        val list = mutableListOf<String>()
                        var selectedItem = 0
                        val icons = mutableListOf<String>()

                        val selectedPropertyId = chatConfigData?.config?.propertyId
                            ?: chatConfigData?.property?.get(0)?.propertyId ?: ""

                        speakers?.sortWith(Comparator { o1, o2 ->
                            when {
                                o1.propertyIds?.contains(selectedPropertyId) == true -> -1
                                o2.propertyIds?.contains(selectedPropertyId) == true -> 1
                                else -> 0
                            }
                        })

                        if (speakers?.isNotEmpty() == true)
                            for (j in speakers.indices) {
                                val speaker = speakers[j]
                                list.add(speaker.voiceName ?: "")
                                icons.add(speaker.image ?: "")
                                if (speaker.vcn == chatConfigData?.config?.vcn) {
                                    selectedItem = j
                                }
                            }

                        SingleChoicePageFragment.newInstance(
                            list.toTypedArray(),
                            icons.toTypedArray(),
                            selectedItem,
                            tag
                        )
                    } ?: run {
                        SingleChoicePageFragment()
                    }
                }
                else -> {
                    SingleChoicePageFragment()
                }
            }
            fragment.onItemSelectedListener = onItemSelectedListener
            fragments.put(position, fragment)
            return fragment
        }
    }
}