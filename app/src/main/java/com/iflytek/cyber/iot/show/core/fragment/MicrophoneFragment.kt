package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.iflytek.cyber.evs.sdk.agent.Recognizer
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.utils.ConfigUtils
import com.iflytek.cyber.iot.show.core.widget.CircleCheckBox
import com.iflytek.cyber.iot.show.core.widget.StyledSwitch

class MicrophoneFragment : BaseFragment() {
    private var farFieldCheckBox: CircleCheckBox? = null
    private var closeTalkCheckBox: CircleCheckBox? = null

    private var microphoneSwitch: StyledSwitch? = null
    private var voiceButtonSwitch: StyledSwitch? = null

    private val configChangedListener = object : ConfigUtils.OnConfigChangedListener {
        override fun onConfigChanged(key: String, value: Any?) {
            when (key) {
                ConfigUtils.KEY_MICROPHONE_ENABLED -> {
                    val microphoneEnabled = value == true
                    microphoneSwitch?.isChecked = microphoneEnabled
                    if (microphoneEnabled) {
                        view?.findViewById<View>(R.id.voice_button)?.alpha = 1f
                        view?.findViewById<View>(R.id.voice_button_disable)?.isVisible = false
                    } else {
                        view?.findViewById<View>(R.id.voice_button)?.alpha = 0.5f
                        view?.findViewById<View>(R.id.voice_button_disable)?.isVisible = true
                    }
                }
                ConfigUtils.KEY_VOICE_BUTTON_ENABLED -> {
                    voiceButtonSwitch?.isChecked = value == true
                }
                ConfigUtils.KEY_RECOGNIZER_PROFILE -> {
                    val profile = ConfigUtils.getString(ConfigUtils.KEY_RECOGNIZER_PROFILE, null)
                    farFieldCheckBox?.isChecked = profile == Recognizer.Profile.FarField.toString()
                    closeTalkCheckBox?.isChecked =
                        profile == Recognizer.Profile.CloseTalk.toString()
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

        farFieldCheckBox = view.findViewById(R.id.far_field_checkbox)
        closeTalkCheckBox = view.findViewById(R.id.close_talk_checkbox)
        microphoneSwitch = view.findViewById(R.id.microphone_switch)
        voiceButtonSwitch = view.findViewById(R.id.voice_button_switch)

        view.findViewById<View>(R.id.back).setOnClickListener {
            pop()
        }
        view.findViewById<View>(R.id.far_field).setOnClickListener {
            farFieldCheckBox?.isChecked = true
        }
        view.findViewById<View>(R.id.close_talk).setOnClickListener {
            closeTalkCheckBox?.isChecked = true
        }
        farFieldCheckBox?.setOnCheckedChangeListener { _, isChecked ->
            val profile = ConfigUtils.getString(ConfigUtils.KEY_RECOGNIZER_PROFILE, null)
            if (isChecked) {
                if (profile != Recognizer.Profile.FarField.toString()) {
                    ConfigUtils.putString(
                        ConfigUtils.KEY_RECOGNIZER_PROFILE,
                        Recognizer.Profile.FarField.toString()
                    )
                } else {
                    configChangedListener.onConfigChanged(
                        ConfigUtils.KEY_RECOGNIZER_PROFILE,
                        profile
                    )
                }
            } else {
                if (profile == Recognizer.Profile.FarField.toString()) {
                    ConfigUtils.putString(
                        ConfigUtils.KEY_RECOGNIZER_PROFILE,
                        Recognizer.Profile.CloseTalk.toString()
                    )
                } else {
                    configChangedListener.onConfigChanged(
                        ConfigUtils.KEY_RECOGNIZER_PROFILE,
                        profile
                    )
                }
            }
        }
        closeTalkCheckBox?.setOnCheckedChangeListener { _, isChecked ->
            val profile = ConfigUtils.getString(ConfigUtils.KEY_RECOGNIZER_PROFILE, null)
            if (isChecked) {
                if (profile != Recognizer.Profile.CloseTalk.toString()) {
                    ConfigUtils.putString(
                        ConfigUtils.KEY_RECOGNIZER_PROFILE,
                        Recognizer.Profile.CloseTalk.toString()
                    )
                } else {
                    configChangedListener.onConfigChanged(
                        ConfigUtils.KEY_RECOGNIZER_PROFILE,
                        profile
                    )
                }
            } else {
                if (profile == Recognizer.Profile.CloseTalk.toString()) {
                    ConfigUtils.putString(
                        ConfigUtils.KEY_RECOGNIZER_PROFILE,
                        Recognizer.Profile.FarField.toString()
                    )
                } else {
                    configChangedListener.onConfigChanged(
                        ConfigUtils.KEY_RECOGNIZER_PROFILE,
                        profile
                    )
                }
            }
        }

        view.findViewById<View>(R.id.microphone).setOnClickListener {
            microphoneSwitch?.isChecked = microphoneSwitch?.isChecked != true
        }
        view.findViewById<View>(R.id.voice_button).setOnClickListener {
            voiceButtonSwitch?.isChecked = voiceButtonSwitch?.isChecked != true
        }
        microphoneSwitch?.setOnCheckedChangeListener(object : StyledSwitch.OnCheckedChangeListener {
            override fun onCheckedChange(switch: StyledSwitch, isChecked: Boolean) {
                val microphoneEnabled =
                    ConfigUtils.getBoolean(ConfigUtils.KEY_MICROPHONE_ENABLED, true)
                if (isChecked) {
                    if (!microphoneEnabled) {
                        ConfigUtils.putBoolean(ConfigUtils.KEY_MICROPHONE_ENABLED, true)
                    }
                } else {
                    if (microphoneEnabled) {
                        ConfigUtils.putBoolean(ConfigUtils.KEY_MICROPHONE_ENABLED, false)
                    }
                }
            }
        })
        voiceButtonSwitch?.setOnCheckedChangeListener(object : StyledSwitch.OnCheckedChangeListener {
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

        // init ui value
        val profile = ConfigUtils.getString(ConfigUtils.KEY_RECOGNIZER_PROFILE, null)
        closeTalkCheckBox?.isChecked = profile == Recognizer.Profile.CloseTalk.toString()
        farFieldCheckBox?.isChecked = profile == Recognizer.Profile.FarField.toString()

        val microphoneEnabled = ConfigUtils.getBoolean(ConfigUtils.KEY_MICROPHONE_ENABLED, true)
        microphoneSwitch?.isChecked = microphoneEnabled
        if (microphoneEnabled) {
            view.findViewById<View>(R.id.voice_button).alpha = 1f
            view.findViewById<View>(R.id.voice_button_disable).isVisible = false
        } else {
            view.findViewById<View>(R.id.voice_button).alpha = 0.5f
            view.findViewById<View>(R.id.voice_button_disable).isVisible = true
        }

        val voiceButtonEnabled = ConfigUtils.getBoolean(ConfigUtils.KEY_VOICE_BUTTON_ENABLED, true)
        voiceButtonSwitch?.isChecked = voiceButtonEnabled
    }

    override fun onDestroy() {
        super.onDestroy()

        ConfigUtils.unregisterOnConfigChangedListener(configChangedListener)
    }
}