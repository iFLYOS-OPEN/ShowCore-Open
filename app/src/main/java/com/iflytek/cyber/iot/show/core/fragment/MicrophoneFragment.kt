package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.gson.JsonParser
import com.iflytek.cyber.evs.sdk.agent.Recognizer
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.DeviceApi
import com.iflytek.cyber.iot.show.core.model.ChatConfigData
import com.iflytek.cyber.iot.show.core.utils.ConfigUtils
import com.iflytek.cyber.iot.show.core.widget.StyledSwitch
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.UnknownHostException
import kotlin.math.max
import kotlin.math.min

class MicrophoneFragment : BaseFragment(), PageScrollable {
    companion object {
        private const val REQUEST_PROFILE_CODE = 10101
    }

    private var microphoneSwitch: StyledSwitch? = null
    private var voiceButtonSwitch: StyledSwitch? = null
    private var voiceButtonCover: View? = null

    private var dialectView: View? = null
    private var speakerView: View? = null
    private var languageAndSpeakerView: View? = null
    private var languageAndSpeakerValueView: TextView? = null
    private var tvSpeakerName: TextView? = null
    private var continousView: View? = null
    private var continousSwitch: StyledSwitch? = null
    private var backgroundRecognizeCover: View? = null
    private var backgroundRecognizeSwitch: StyledSwitch? = null
    private var responseSoundSwitch: StyledSwitch? = null
    private var wakeUpSoundSwitch: StyledSwitch? = null

    private var tvRecognizerProfile: TextView? = null

    private var scrollView: ScrollView? = null
    private var contentContainer: LinearLayout? = null

    private var continousMode = false

    private var chatConfigData: ChatConfigData? = null

    private var backCount = 0

    private val configChangedListener = object : ConfigUtils.OnConfigChangedListener {
        override fun onConfigChanged(key: String, value: Any?) {
            when (key) {
                ConfigUtils.KEY_RECOGNIZER_PROFILE -> {
                    val isCloseTalk = value == Recognizer.Profile.CloseTalk.toString()
                    if (isCloseTalk)
                        tvRecognizerProfile?.text = getString(R.string.message_near_field)
                    else
                        tvRecognizerProfile?.text = getString(R.string.message_far_field)
                }
                ConfigUtils.KEY_VOICE_WAKEUP_ENABLED -> {
                    val microphoneEnabled = value == true
                    if (microphoneSwitch?.isChecked != microphoneEnabled)
                        microphoneSwitch?.isChecked = microphoneEnabled
                    voiceButtonCover?.isVisible = !microphoneEnabled
                }
                ConfigUtils.KEY_VOICE_BUTTON_ENABLED -> {
                    val voiceButtonEnabled = value == true
                    if (voiceButtonSwitch?.isChecked != voiceButtonEnabled)
                        voiceButtonSwitch?.isChecked = voiceButtonEnabled
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ConfigUtils.registerOnConfigChangedListener(configChangedListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_microphone, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        microphoneSwitch = view.findViewById(R.id.microphone_switch)
        voiceButtonSwitch = view.findViewById(R.id.voice_button_switch)
        voiceButtonCover = view.findViewById(R.id.voice_button_cover)

        scrollView = view.findViewById(R.id.scroll_view)
        contentContainer = view.findViewById(R.id.content_container)

        continousView = view.findViewById(R.id.continous_mode)
        continousSwitch = view.findViewById(R.id.continous_switch)
       // speakerView = view.findViewById(R.id.speaker)
        languageAndSpeakerView = view.findViewById(R.id.language_and_speaker)
        languageAndSpeakerValueView = view.findViewById(R.id.language_and_speaker_value)
        backgroundRecognizeCover = view.findViewById(R.id.background_recognize_cover)
        backgroundRecognizeSwitch = view.findViewById(R.id.background_recognize_switch)
        responseSoundSwitch = view.findViewById(R.id.response_sound_switch)
        wakeUpSoundSwitch = view.findViewById(R.id.wake_up_sound_switch)

        tvRecognizerProfile = view.findViewById(R.id.recognize_profile_value)

        view.findViewById<View>(R.id.back).setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            pop()
        }
        view.findViewById<View>(R.id.recognize_profile).setOnClickListener {
            val titles =
                arrayOf(
                    getString(R.string.title_close_talk),
                    getString(R.string.title_far_field)
                )
            val messages =
                arrayOf(
                    getString(R.string.summary_close_talk),
                    getString(R.string.summary_far_field)
                )
            val profile = ConfigUtils.getString(
                ConfigUtils.KEY_RECOGNIZER_PROFILE,
                Recognizer.Profile.FarField.toString()
            )
            val selected = if (profile == Recognizer.Profile.FarField.toString()) {
                1
            } else {
                0
            }
            val fragment =
                SingleChoiceFragment.newInstance(
                    getString(R.string.recognize_profile),
                    getString(R.string.message_recognize_profile),
                    titles,
                    messages,
                    selected,
                    true
                )
            startForResult(fragment, REQUEST_PROFILE_CODE)
        }
        view.findViewById<View>(R.id.background_recognize_clickable).setOnClickListener {
            backgroundRecognizeSwitch?.isChecked = backgroundRecognizeSwitch?.isChecked != true
        }
        view.findViewById<View>(R.id.language_and_speaker).setOnClickListener {
            start(LanguageAndSpeakerFragment.newInstance(chatConfigData))
        }

        view.findViewById<View>(R.id.microphone).setOnClickListener {
            microphoneSwitch?.isChecked = microphoneSwitch?.isChecked != true
        }
        view.findViewById<View>(R.id.voice_button).setOnClickListener {
            voiceButtonSwitch?.isChecked = voiceButtonSwitch?.isChecked != true
        }
        view.findViewById<View>(R.id.response_sound_clickable).setOnClickListener {
            responseSoundSwitch?.isChecked = responseSoundSwitch?.isChecked != true
        }
        view.findViewById<View>(R.id.wake_up_sound_clickable).setOnClickListener {
            wakeUpSoundSwitch?.isChecked = wakeUpSoundSwitch?.isChecked != true
        }
        responseSoundSwitch?.setOnCheckedChangeListener(object :
            StyledSwitch.OnCheckedChangeListener {
            override fun onCheckedChange(switch: StyledSwitch, isChecked: Boolean) {
                val responseSoundEnabled =
                    ConfigUtils.getBoolean(ConfigUtils.KEY_RESPONSE_SOUND, true)
                if (isChecked) {
                    if (!responseSoundEnabled) {
                        ConfigUtils.putBoolean(ConfigUtils.KEY_RESPONSE_SOUND, true)
                    }
                } else {
                    if (responseSoundEnabled) {
                        ConfigUtils.putBoolean(ConfigUtils.KEY_RESPONSE_SOUND, false)
                    }
                }
            }
        })
        wakeUpSoundSwitch?.setOnCheckedChangeListener(object :
            StyledSwitch.OnCheckedChangeListener {
            override fun onCheckedChange(switch: StyledSwitch, isChecked: Boolean) {
                val responseSoundEnabled =
                    ConfigUtils.getBoolean(ConfigUtils.KEY_WAKE_UP_SOUND, true)
                if (isChecked) {
                    if (!responseSoundEnabled) {
                        ConfigUtils.putBoolean(ConfigUtils.KEY_WAKE_UP_SOUND, true)
                    }
                } else {
                    if (responseSoundEnabled) {
                        ConfigUtils.putBoolean(ConfigUtils.KEY_WAKE_UP_SOUND, false)
                    }
                }
            }
        })
        responseSoundSwitch?.isChecked =
            ConfigUtils.getBoolean(ConfigUtils.KEY_RESPONSE_SOUND, true)
        wakeUpSoundSwitch?.isChecked =
            ConfigUtils.getBoolean(ConfigUtils.KEY_WAKE_UP_SOUND, true)
        microphoneSwitch?.setOnCheckedChangeListener(object : StyledSwitch.OnCheckedChangeListener {
            override fun onCheckedChange(switch: StyledSwitch, isChecked: Boolean) {
                val microphoneEnabled =
                    ConfigUtils.getBoolean(ConfigUtils.KEY_VOICE_WAKEUP_ENABLED, true)
                if (isChecked) {
                    if (!microphoneEnabled) {
                        ConfigUtils.putBoolean(ConfigUtils.KEY_VOICE_WAKEUP_ENABLED, true)
                    }
                } else {
                    if (microphoneEnabled) {
                        ConfigUtils.putBoolean(ConfigUtils.KEY_VOICE_WAKEUP_ENABLED, false)
                    }
                }
            }
        })
        voiceButtonSwitch?.setOnCheckedChangeListener(object :
            StyledSwitch.OnCheckedChangeListener {
            override fun onCheckedChange(switch: StyledSwitch, isChecked: Boolean) {
                val voiceButtonEnabled =
                    ConfigUtils.getBoolean(ConfigUtils.KEY_VOICE_BUTTON_ENABLED, true)
                if (isChecked) {
                    if (!voiceButtonEnabled) {
                        ConfigUtils.putBoolean(ConfigUtils.KEY_VOICE_BUTTON_ENABLED, true)
                    }
                } else {
                    if (voiceButtonEnabled) {
                        ConfigUtils.putBoolean(ConfigUtils.KEY_VOICE_BUTTON_ENABLED, false)
                    }
                }
            }
        })
        view.findViewById<View>(R.id.continous_mode_clickable)?.setOnClickListener {
            continousSwitch?.isChecked = continousSwitch?.isChecked != true
        }

        continousSwitch?.setOnCheckedChangeListener(object : StyledSwitch.OnCheckedChangeListener {
            override fun onCheckedChange(switch: StyledSwitch, isChecked: Boolean) {
                backgroundRecognizeCover?.isVisible = !isChecked
                if (isChecked == continousMode)
                    return
                if (isChecked) {
                    val requestBody =
                        RequestBody.create(
                            MediaType.parse("application/json"), """
                            {"continous_mode": true}
                        """.trimIndent()
                        )
                    getDeviceApi()?.put(requestBody)?.enqueue(object : Callback<ResponseBody> {
                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            continousSwitch?.isChecked = false
                            if (t is UnknownHostException) {
                                toastError(R.string.network_error)
                            }
                        }

                        override fun onResponse(
                            call: Call<ResponseBody>,
                            response: Response<ResponseBody>
                        ) {
                            if (!response.isSuccessful) {
                                continousSwitch?.isChecked = false
                                toastError(R.string.request_params_error)
                            } else {
                                continousMode = true
                            }
                        }

                    })
                } else {
                    val requestBody =
                        RequestBody.create(
                            MediaType.parse("application/json"), """
                            {"continous_mode": false}
                        """.trimIndent()
                        )
                    getDeviceApi()?.put(requestBody)?.enqueue(object : Callback<ResponseBody> {
                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            continousSwitch?.isChecked = true
                            if (t is UnknownHostException) {
                                toastError(R.string.network_error)
                            }
                        }

                        override fun onResponse(
                            call: Call<ResponseBody>,
                            response: Response<ResponseBody>
                        ) {
                            if (!response.isSuccessful) {
                                continousSwitch?.isChecked = true
                                toastError(R.string.request_params_error)
                            } else {
                                continousMode = false
                            }
                        }

                    })
                }
            }
        })
        backgroundRecognizeSwitch?.setOnCheckedChangeListener(object :
            StyledSwitch.OnCheckedChangeListener {
            override fun onCheckedChange(switch: StyledSwitch, isChecked: Boolean) {
                val backgroundRecognizeEnabled =
                    ConfigUtils.getBoolean(ConfigUtils.KEY_BACKGROUND_RECOGNIZE, false)
                if (isChecked == backgroundRecognizeEnabled) {
                    return
                }
                ConfigUtils.putBoolean(ConfigUtils.KEY_BACKGROUND_RECOGNIZE, isChecked)
            }
        })

        // init ui value
        val profile = ConfigUtils.getString(
            ConfigUtils.KEY_RECOGNIZER_PROFILE,
            Recognizer.Profile.FarField.toString()
        )
        val isCloseTalk = profile == Recognizer.Profile.CloseTalk.toString()
        if (isCloseTalk)
            tvRecognizerProfile?.text = getString(R.string.message_near_field)
        else
            tvRecognizerProfile?.text = getString(R.string.message_far_field)

        val backgroundRecognizeEnabled =
            ConfigUtils.getBoolean(ConfigUtils.KEY_BACKGROUND_RECOGNIZE, false)
        backgroundRecognizeSwitch?.isChecked = backgroundRecognizeEnabled

        val microphoneEnabled = ConfigUtils.getBoolean(ConfigUtils.KEY_VOICE_WAKEUP_ENABLED, true)
        microphoneSwitch?.isChecked = microphoneEnabled

        val voiceButtonEnabled = ConfigUtils.getBoolean(ConfigUtils.KEY_VOICE_BUTTON_ENABLED, true)
        voiceButtonSwitch?.isChecked = voiceButtonEnabled

        voiceButtonCover?.isVisible = !microphoneEnabled
    }

    private fun toastError(stringResId: Int) {
        if (isSupportVisible) {
            Toast.makeText(context, stringResId, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportVisible() {
        super.onSupportVisible()

        getDeviceApi()?.let { api ->
            api.get().enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    t.printStackTrace()
                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        val body = responseBody?.string()
                        val json = JsonParser().parse(body).asJsonObject

                        if (json.has("continous_mode")) {
                            continousMode = json.get("continous_mode").asBoolean
                            if (continousView?.isVisible != true) {
                                continousView?.isVisible = true

                                continousSwitch?.setChecked(continousMode, false)
                            } else {
                                continousSwitch?.isChecked = continousMode
                            }
                            backgroundRecognizeCover?.isVisible = continousSwitch?.isChecked != true
                        } else {
                            continousView?.isVisible = false
                        }
                        responseBody?.close()
                    }
                }

            })
            api.getChatConfig().enqueue(object : Callback<ChatConfigData> {
                override fun onFailure(call: Call<ChatConfigData>, t: Throwable) {
                    t.printStackTrace()
                }

                override fun onResponse(
                    call: Call<ChatConfigData>,
                    response: Response<ChatConfigData>
                ) {
                    if (response.isSuccessful) {
                        val configData = response.body()

                        languageAndSpeakerView?.isVisible = true
                        var languageName: String? = null
                        var propertyName: String? = null
                        var vcnName: String? = null

                        val currentConfig = configData?.config
                        if (configData?.interactionModes?.isNotEmpty() == true) {
                            for (mode in configData.interactionModes) {
                                if (mode.interactionModeId == currentConfig?.interactionModeId) {
                                    languageName = mode.name

                                    if (mode.speakers?.isNotEmpty() == true) {
                                        for (speaker in mode.speakers) {
                                            if (speaker.vcn == currentConfig?.vcn) {
                                                vcnName = speaker.voiceName
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (configData?.property?.isNotEmpty() == true) {
                            for (property in configData.property) {
                                if (property.propertyId == currentConfig?.propertyId) {
                                    propertyName = property.name
                                }
                            }
                        }

                        val value = StringBuilder()
                        if (!languageName.isNullOrEmpty()) {
                            value.append(languageName)
                        }
                        if (!propertyName.isNullOrEmpty()) {
                            value.append(", ")
                            value.append(propertyName)
                        }
                        if (!vcnName.isNullOrEmpty()) {
                            value.append(", ")
                            value.append(vcnName)
                        }
                        languageAndSpeakerValueView?.text = value

                        chatConfigData = configData
                    } else {
                        chatConfigData = null
                        languageAndSpeakerView?.isVisible = false
                    }
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        ConfigUtils.unregisterOnConfigChangedListener(configChangedListener)
    }

    override fun onFragmentResult(requestCode: Int, resultCode: Int, data: Bundle) {
        super.onFragmentResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PROFILE_CODE) {
            if (resultCode == 0) {
                val selectedItem = data.getInt("selected_item")
                if (selectedItem == 0) {
                    ConfigUtils.putString(
                        ConfigUtils.KEY_RECOGNIZER_PROFILE,
                        Recognizer.Profile.CloseTalk.toString()
                    )
                } else {
                    ConfigUtils.putString(
                        ConfigUtils.KEY_RECOGNIZER_PROFILE,
                        Recognizer.Profile.FarField.toString()
                    )
                }
            }
        }
    }

    override fun scrollToNext(): Boolean {
        scrollView?.let { scrollView ->
            val pageHeight = scrollView.height
            val scrollY = scrollView.scrollY
            val contentHeight = contentContainer?.height ?: 0
            if (scrollY == contentHeight - pageHeight) {
                return false
            }
            val target = min(contentHeight - pageHeight, scrollY + pageHeight)
            smoothScrollTo(target)
            return true
        } ?: run {
            return false
        }
    }

    override fun scrollToPrevious(): Boolean {
        scrollView?.let { scrollView ->
            val pageHeight = scrollView.height
            val scrollY = scrollView.scrollY
            if (scrollY == 0) {
                return false
            }
            val target = max(0, scrollY - pageHeight)
            smoothScrollTo(target)
            return true
        } ?: run {
            return false
        }
    }

    private fun smoothScrollTo(scrollY: Int) {
        scrollView?.isSmoothScrollingEnabled = true
        scrollView?.smoothScrollTo(0, scrollY)
    }

    private fun getDeviceApi(): DeviceApi? {
        val context = context ?: return null
        return CoreApplication.from(context).createApi(DeviceApi::class.java)
    }
}