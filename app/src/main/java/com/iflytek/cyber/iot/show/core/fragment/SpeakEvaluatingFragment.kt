package com.iflytek.cyber.iot.show.core.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.core.os.bundleOf
import androidx.core.os.postDelayed
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import com.airbnb.lottie.LottieAnimationView
import com.google.gson.Gson
import com.iflytek.cyber.evs.sdk.agent.Alarm
import com.iflytek.cyber.evs.sdk.agent.AudioPlayer
import com.iflytek.cyber.evs.sdk.agent.Recognizer
import com.iflytek.cyber.evs.sdk.agent.System.Companion.PAYLOAD_CODE
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.impl.alarm.EvsAlarm
import com.iflytek.cyber.iot.show.core.impl.prompt.PromptManager
import com.iflytek.cyber.iot.show.core.impl.recognizer.EvsRecognizer
import com.iflytek.cyber.iot.show.core.impl.system.EvsSystem
import com.iflytek.cyber.iot.show.core.model.ReadWord
import com.iflytek.cyber.iot.show.core.record.GlobalRecorder
import com.iflytek.cyber.iot.show.core.utils.*
import com.iflytek.cyber.iot.show.core.widget.ShadowFrameLayout
import com.iflytek.cyber.iot.show.core.widget.StyledAlertDialog
import com.kk.taurus.playerbase.utils.NetworkUtils
import kotlinx.android.synthetic.main.fragment_speak_evaluating.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject

class SpeakEvaluatingFragment : BaseFragment(), View.OnClickListener,
    EvsRecognizer.OnEvaluateResultCallback,
    GlobalRecorder.Observer, Alarm.AlarmStateChangedListener {


    companion object {
        const val CHINESE_SINGLE_WORD_TYPE = 0 //中文单字
        const val CHINESE_WORDS_TYPE = 1 //中文词语
        const val CHINESE_SENTENCE_TYPE = 2 //中文短句
        const val ENGLISH_WORD_TYPE = 3 //英文单词
        const val ENGLISH_SENTENCE_TYPE = 4 //英文短句
        const val ENGLISH_ARTICLE_TYPE = 5 //英文篇章

        fun newInstance(type: Int): SpeakEvaluatingFragment {
            return SpeakEvaluatingFragment().apply {
                arguments = bundleOf(Pair("type", type))
            }
        }
    }

    private lateinit var evaluatingProgress: ProgressBar
    private lateinit var progressContainer: LinearLayout
    private lateinit var tvWord: TextView
    private lateinit var tvSentence: TextView
    private lateinit var progressTextView: TextView
    private lateinit var tvTitle: TextView
    private lateinit var recording: LottieAnimationView
    private lateinit var chineseSentence: FrameLayout
    private lateinit var englishSentence: FrameLayout
    private lateinit var recordButton: ImageView
    private lateinit var tvEnglishArticle: TextView
    private lateinit var tvRecordingText: TextView
    private lateinit var recordingContainer: LinearLayout
    private lateinit var tvRecordTips: TextView
    private lateinit var tvEnglishTips: TextView
    private lateinit var startRecordTips: LinearLayout
    private lateinit var recordToolContent: View
    private lateinit var restartRecord: TextView
    private lateinit var finishRecord: TextView
    private lateinit var recordButtonContent: FrameLayout
    private lateinit var centerRecord: LottieAnimationView

    private val evaluation = EvaluationUtils()
    private var currentProgress = 0
    private var textList = ArrayList<String>()
    private var sentenceText: String? = null
    private var animateCount = 0
    private var isEndTipsShow = false
    private var shouldEnableWakeUp = false //是否应该恢复之前的唤醒按钮
    private var retryCount = 0 //max 3
    private var isRecording = false
    private var shouldShowMidErrorView = false
    private var shouldPromptTts = true
    private var startRecordTime = 0L
    private var isNetworkError = false

    private var readSentence: ReadWord? = null
    private var readSyllableList = ArrayList<ReadWord>()
    private var punctuationList = ArrayList<String>()

    private var handler = Handler(Looper.getMainLooper())

    private var testType: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_speak_evaluating, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (launcher?.getService()?.getRecognizer() as? EvsRecognizer)?.registerEvaluateResultCallback(
            this
        )
        (launcher?.getService()?.getAlarm() as? EvsAlarm)?.addListener(this)

        EvsSystem.get().onEvsErrorListener = onEvsErrorListener

        GlobalRecorder.registerObserver(this)

        launcher?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        view.findViewById<View>(R.id.back).clickWithTrigger {
            pop()
        }

        progressContainer = view.findViewById(R.id.progress_container)
        evaluatingProgress = view.findViewById(R.id.progress)
        tvWord = view.findViewById(R.id.tv_word)
        tvTitle = view.findViewById(R.id.tv_title)
        tvSentence = view.findViewById(R.id.tv_sentence)
        progressTextView = view.findViewById(R.id.tv_progress)
        recording = view.findViewById(R.id.recording)
        recording.setOnClickListener(this)
        recordingContainer = view.findViewById(R.id.recording_container)
        chineseSentence = view.findViewById(R.id.chinese_sentence)
        englishSentence = view.findViewById(R.id.english_sentence)
        tvEnglishArticle = view.findViewById(R.id.tv_english_article)
        recordButton = view.findViewById(R.id.record_button)
        recordButton.setOnClickListener(this)
        tvRecordingText = view.findViewById(R.id.tv_recording_text)
        tvRecordTips = view.findViewById(R.id.tv_record_tips)
        tvEnglishTips = view.findViewById(R.id.tv_english_tips)
        startRecordTips = view.findViewById(R.id.start_record_tips)
        startRecordTips.translationX = 40f.dp2Px()
        recordButtonContent = view.findViewById(R.id.record_button_content)
        recordToolContent = view.findViewById(R.id.record_tool_content)
        centerRecord = view.findViewById(R.id.center_recording)
        restartRecord = view.findViewById(R.id.restart_record)
        finishRecord = view.findViewById(R.id.finish_record)
        restartRecord.clickWithTrigger {
            if (restartRecord.isSelected) {
                onRestartRecord()
            }
        }

        finishRecord.clickWithTrigger {
            if (finishRecord.isSelected) {
                onFinishRecord()
            }
        }

        testType = arguments?.getInt("type")
        when (testType) {
            CHINESE_SINGLE_WORD_TYPE -> {
                tvRecordTips.text = "请在开始录音后读出以下汉字"
                progressContainer.isVisible = true
                chineseSentence.isVisible = true
                startRecordTips.isVisible = true
                recordToolContent.isVisible = false
                progressTextView.text = "1/10"
                textList = evaluation.getChineseSingleList()
                tvWord.text = evaluation.filterSentence(textList[0])
            }
            CHINESE_WORDS_TYPE -> {
                tvRecordTips.text = "请在开始录音后读出以下词语"
                progressContainer.isVisible = true
                chineseSentence.isVisible = true
                startRecordTips.isVisible = true
                recordToolContent.isVisible = false
                progressTextView.text = "1/10"
                textList = evaluation.getChineseWordsList()
                tvWord.text = textList[0]
            }
            CHINESE_SENTENCE_TYPE -> {
                tvRecordTips.text = "请在开始录音后读出以下句子"
                tvTitle.isVisible = true
                tvSentence.isVisible = true
                chineseSentence.isVisible = true
                sentenceText = evaluation.getChineseSentence()
                punctuationList = evaluation.getPunctuationList(sentenceText!!)
                tvSentence.text = evaluation.filterSentence(sentenceText)
            }
            ENGLISH_WORD_TYPE -> {
                tvRecordTips.text = "请在开始录音后读出以下单词"
                progressTextView.text = "1/10"
                progressContainer.isVisible = true
                chineseSentence.isVisible = true
                startRecordTips.isVisible = true
                recordToolContent.isVisible = false
                textList = evaluation.getEnglishSingleWordList()
                tvWord.text = textList[0]
            }
            ENGLISH_SENTENCE_TYPE -> {
                tvRecordTips.text = "请在开始录音后读出以下句子"
                tvTitle.isVisible = true
                tvSentence.isVisible = true
                chineseSentence.isVisible = true
                sentenceText = evaluation.getEnglishSentence()
                punctuationList = evaluation.getPunctuationList(sentenceText!!)
                tvSentence.text = sentenceText
            }
            ENGLISH_ARTICLE_TYPE -> {
                tvEnglishTips.text = "请在开始录音后60s读完文章"
                tvTitle.isVisible = true
                tvTitle.setText(R.string.article_evaluation)
                englishSentence.isVisible = true
                sentenceText = evaluation.getEnglishArticle()
                punctuationList = evaluation.getPunctuationList(sentenceText!!)
                tvEnglishArticle.text = sentenceText
            }
        }

        EventBus.getDefault().register(this)

        val intent = Intent(context, FloatingService::class.java).apply {
            action = FloatingService.ACTION_EVALUATING
            putExtra("isEvaluating", true)
        }
        context?.startService(intent)

        val dismissMicErrorIntent = Intent(context, FloatingService::class.java).apply {
            action = FloatingService.ACTION_DISMISS_MIC_ERROR
        }
        context?.startService(dismissMicErrorIntent)

        val isMicrophoneEnabled =
            ConfigUtils.getBoolean(ConfigUtils.KEY_VOICE_WAKEUP_ENABLED, true)
        if (isMicrophoneEnabled) {
            shouldPromptTts = true
        } else {
            shouldPromptTts = false
            shouldShowMidErrorView = true
        }

        handler.postDelayed(100) {
            if (isMicrophoneEnabled) {
                ConfigUtils.putBoolean(ConfigUtils.KEY_VOICE_WAKEUP_ENABLED, false)
                shouldEnableWakeUp = true
            }
        }
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        if (animateCount < 3) {
            view?.postDelayed(500) {
                setupStartRecordTipsAnimation()
            }
        }

        ConnectivityUtils.checkIvsAvailable({
            post {
                isNetworkError = false
            }
        }, { _, _ ->
            post {
                isNetworkError = true
                if (testType == CHINESE_SENTENCE_TYPE ||
                    testType == ENGLISH_SENTENCE_TYPE ||
                    testType == ENGLISH_ARTICLE_TYPE) {
                    onRestartRecord()
                } else {
                    recordButtonContent.isVisible = true
                    recordingContainer.isVisible = false
                    recording.pauseAnimation()
                    networkUnavailable()
                }
            }
        })
    }

    override fun onAlarmStateChanged(alarmId: String, state: Alarm.AlarmState) {
        if (state == Alarm.AlarmState.Stopped) {
            if (testType == CHINESE_SENTENCE_TYPE ||
                testType == ENGLISH_SENTENCE_TYPE ||
                testType == ENGLISH_ARTICLE_TYPE
            ) {
                val alertDialog = StyledAlertDialog.Builder()
                    .setMessage("评测被打断，是否重新录音？")
                    .setPositiveButton("重录该题", View.OnClickListener {
                        onRecordButtonClick()
                    })
                    .setNegativeButton("退出", View.OnClickListener {
                        startWithPopTo(
                            SpeakEvaluationFragment(),
                            SpeakEvaluationFragment::class.java,
                            true
                        )
                    })
                    .show(childFragmentManager)
                alertDialog.isCancelable = false
            } else {
                onRecordingClick()
            }
        }
    }

    private val onEvsErrorListener = object : EvsSystem.OnEvsErrorListener {
        override fun onError(payload: com.alibaba.fastjson.JSONObject) {
            val code = payload.getIntValue(PAYLOAD_CODE)
            if (code == 500) {
                if (retryCount < 3) {
                    showErrorDialog()
                } else if (isAdded && context != null) {
                    Toast.makeText(context, "系统异常，请稍后再开始评测", Toast.LENGTH_SHORT).show()
                    startWithPopTo(
                        SpeakEvaluationFragment(),
                        SpeakEvaluationFragment::class.java,
                        true
                    )
                }
            }
        }
    }

    private fun showErrorDialog() {
        if (!isAdded || context == null) {
            return
        }
        val alertDialog = StyledAlertDialog.Builder()
            .setMessage("系统出了点问题,你可以选择重录该题")
            .setPositiveButton("重录该题", View.OnClickListener {
                retryCount += 1
                onRecordButtonClick()
            })
            .setNegativeButton("退出", View.OnClickListener {
                startWithPopTo(SpeakEvaluationFragment(), SpeakEvaluationFragment::class.java, true)
            })
            .show(childFragmentManager)
        alertDialog.isCancelable = false
    }

    private fun setupStartRecordTipsAnimation() {
        startRecordTips.translationX = 40f.dp2Px()
        startRecordTips.animate()
            .translationX(0f)
            .setDuration(1000)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    if (animateCount < 3) {
                        setupStartRecordTipsAnimation()
                    }
                    animateCount += 1
                }
            })
            .start()
    }

    override fun onAudioData(array: ByteArray, offset: Int, length: Int) {
        val volume = RecordVolumeUtils.calculateVolume(array, length)
        val progress = volume / 100
        //recording.progress = progress
    }

    override fun onWakeUp(angle: Int, beam: Int, params: String?) {
        if (shouldPromptTts) {
            warnUserWorkingWithEvaluation()
        }
    }

    private fun warnUserWorkingWithEvaluation() {
        handler.removeCallbacksAndMessages(null)
        launcher?.getService()?.getRecognizer()?.requestCancel()
        PromptManager.play(PromptManager.EVALUATION_WARN)
        if (isRecording) {
            handler.postDelayed(5500) {
                onRecordButtonClick()
            }
        }
    }

    private fun sendEvaluate(
        language: String,
        category: String,
        text: String,
        enableVad: Boolean = true
    ) {
        isRecording = true
        launcher?.getService()?.getRecognizer()?.sendEvaluate(
            language,
            category,
            text,
            enableVad
        )
    }

    private fun showCodeErrorDialog() {
        if (!isAdded || context == null) {
            return
        }
        val alertDialog = StyledAlertDialog.Builder()
            .setMessage("系统出了点问题,你可以选择重录该题")
            .setPositiveButton("重录该题", View.OnClickListener {
                onRecordButtonClick()
            })
            .setNegativeButton("退出", View.OnClickListener {
                startWithPopTo(SpeakEvaluationFragment(), SpeakEvaluationFragment::class.java, true)
            })
            .show(childFragmentManager)
        alertDialog.isCancelable = false
    }

    override fun onEvaluateResult(payload: String) {
        val json = JSONObject(payload)
        val code = json.getInt("code")
        if (code != 0) {
            launcher?.getService()?.getRecognizer()?.requestCancel()
            pauseAudioPlayer()
            showCodeErrorDialog()
            return
        }
        retryCount = 0
        if (testType == CHINESE_SINGLE_WORD_TYPE) {
            extraWordPayload(payload, "read_syllable")
            if (currentProgress < 9) {
                currentProgress += 1
                updateEvaluateProgress((currentProgress + 1) * 10)
                progressTextView.text = "${currentProgress + 1}/10"
                val word = textList[currentProgress]
                tvWord.text = evaluation.filterSentence(word)
                sendEvaluate(
                    Recognizer.LANGUAGE_ZH_CN,
                    Recognizer.EVALUATE_CATEGORY_READ_SYLLABLE,
                    word
                )
            } else if (currentProgress == 9) {
                progressTextView.text = "10/10"
                isRecording = false
                startWithPop(SpeakEvaluationResultFragment.newInstance(testType, readSyllableList))
            }
        }

        if (testType == CHINESE_WORDS_TYPE) {
            extraWordPayload(payload, "read_word")
            if (currentProgress < 9) {
                currentProgress += 1
                updateEvaluateProgress((currentProgress + 1) * 10)
                progressTextView.text = "${currentProgress + 1}/10"
                val word = textList[currentProgress]
                tvWord.text = word
                sendEvaluate(
                    Recognizer.LANGUAGE_ZH_CN,
                    Recognizer.EVALUATE_CATEGORY_READ_WORD,
                    "[word]\n${word}"
                )
            } else if (currentProgress == 9) {
                isRecording = false
                progressTextView.text = "10/10"
                startWithPop(SpeakEvaluationResultFragment.newInstance(testType, readSyllableList))
            }
        }

        if (testType == CHINESE_SENTENCE_TYPE) {
            isRecording = false
            extraSentencePayload(payload, "read_sentence")
            startWithPop(
                SpeakEvaluationResultFragment.newInstance(
                    testType,
                    readSentence,
                    punctuationList
                )
            )
        }

        if (testType == ENGLISH_WORD_TYPE) {
            extraWordPayload(payload, "read_word")
            if (currentProgress < 9) {
                currentProgress += 1
                updateEvaluateProgress((currentProgress + 1) * 10)
                progressTextView.text = "${currentProgress + 1}/10"
                val word = textList[currentProgress]
                tvWord.text = word
                sendEvaluate(
                    Recognizer.LANGUAGE_EN_US,
                    Recognizer.EVALUATE_CATEGORY_READ_WORD,
                    "[word]\n${word}"
                )
            } else if (currentProgress == 9) {
                isRecording = false
                progressTextView.text = "10/10"
                startWithPop(SpeakEvaluationResultFragment.newInstance(testType, readSyllableList))
            }
        }

        if (testType == ENGLISH_SENTENCE_TYPE) {
            isRecording = false
            extraSentencePayload(payload, "read_chapter")
            startWithPop(
                SpeakEvaluationResultFragment.newInstance(
                    testType,
                    readSentence,
                    punctuationList
                )
            )
        }

        if (testType == ENGLISH_ARTICLE_TYPE) {
            isRecording = false
            extraSentencePayload(payload, "read_chapter")
            startWithPop(
                SpeakEvaluationResultFragment.newInstance(
                    testType,
                    readSentence,
                    punctuationList
                )
            )
        }
    }

    private fun updateEvaluateProgress(end: Int) {
        val start = currentProgress * 10
        val animator = ValueAnimator.ofInt(start, end)
        animator.addUpdateListener {
            val value = it.animatedValue as Int
            evaluatingProgress.progress = value
        }
        animator.duration = 300
        animator.start()
    }

    private fun networkUnavailable() {
        if (isAdded && launcher != null) {
            PromptManager.play(PromptManager.NETWORK_LOST)
            val networkErrorNotification =
                Intent(launcher!!, FloatingService::class.java)
            networkErrorNotification.action =
                FloatingService.ACTION_SHOW_NOTIFICATION
            networkErrorNotification.putExtra(
                FloatingService.EXTRA_MESSAGE, "网络连接异常，请重新设置"
            )
            networkErrorNotification.putExtra(
                FloatingService.EXTRA_TAG, "network_error"
            )
            networkErrorNotification.putExtra(
                FloatingService.EXTRA_POSITIVE_BUTTON_TEXT, "设置网络"
            )
            networkErrorNotification.putExtra(
                FloatingService.EXTRA_POSITIVE_BUTTON_ACTION,
                MainFragment2.ACTION_OPEN_WIFI
            )
            networkErrorNotification.putExtra(
                FloatingService.EXTRA_ICON_RES,
                R.drawable.ic_wifi_error_white_40dp
            )
            launcher?.startService(networkErrorNotification)
        }
    }

    /**
     * 手动点击唤醒按钮回调
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWakeUpButtonClick(evaluationAction: EvaluationAction) {
        if (evaluationAction.action == 0) {
            warnUserWorkingWithEvaluation()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.record_button -> {
                if (isAdded && context != null && (!NetworkUtils.isNetConnected(context) || isNetworkError)) {
                    networkUnavailable()
                    return
                }
                onRecordButtonClick()
            }
            R.id.recording -> {
                onRecordingClick()
            }
        }
    }

    private fun scaleRecordButton() {
        recordButton.setBackgroundResource(R.drawable.ic_music_pause)
        recordButtonContent.animate()
            .setInterpolator(AccelerateDecelerateInterpolator())
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(300)
            .start()
    }

    private fun onRecordButtonClick() {
        //recordButton.isVisible = false
        //recordingContainer.isVisible = true
        startRecordTime = System.currentTimeMillis()
        startRecordTips.isVisible = false
        pauseAudioPlayer()
        val isMicrophoneEnabled =
            ConfigUtils.getBoolean(ConfigUtils.KEY_VOICE_WAKEUP_ENABLED, true)
        if (isMicrophoneEnabled) {
            ConfigUtils.putBoolean(ConfigUtils.KEY_VOICE_WAKEUP_ENABLED, false)
            shouldEnableWakeUp = true
        }
        if (testType == CHINESE_SINGLE_WORD_TYPE) {
            tvRecordTips.text = "请继续…读出以下汉字"
            tvRecordingText.isVisible = true
            recordButtonContent.isVisible = false
            recordingContainer.isVisible = true
            recording.playAnimation()
            sendEvaluate(
                Recognizer.LANGUAGE_ZH_CN,
                Recognizer.EVALUATE_CATEGORY_READ_SYLLABLE,
                textList[currentProgress]
            )
        } else if (testType == CHINESE_WORDS_TYPE) {
            tvRecordTips.text = "请继续…读出以下词语"
            tvRecordingText.isVisible = true
            recordButtonContent.isVisible = false
            recordingContainer.isVisible = true
            recording.playAnimation()
            sendEvaluate(
                Recognizer.LANGUAGE_ZH_CN,
                Recognizer.EVALUATE_CATEGORY_READ_WORD,
                "[word]\n${textList[currentProgress]}"
            )
        } else if (testType == CHINESE_SENTENCE_TYPE && !sentenceText.isNullOrEmpty()) {
            tvRecordingText.isVisible = true
            restartRecord.isSelected = true
            finishRecord.isSelected = true
            centerRecord.playAnimation()
            recordButtonContent.isVisible = false
            sendEvaluate(
                Recognizer.LANGUAGE_ZH_CN,
                Recognizer.EVALUATE_CATEGORY_READ_SENTENCE,
                sentenceText!!,
                false
            )
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed(20 * 1000) {
                launcher?.getService()?.getRecognizer()?.requestEnd()
            }
        } else if (testType == ENGLISH_WORD_TYPE) {
            tvRecordTips.text = "请继续…读出以下单词"
            tvRecordingText.isVisible = true
            recordButtonContent.isVisible = false
            recordingContainer.isVisible = true
            recording.playAnimation()
            sendEvaluate(
                Recognizer.LANGUAGE_EN_US,
                Recognizer.EVALUATE_CATEGORY_READ_WORD,
                "[word]\n${textList[currentProgress]}"
            )
        } else if (testType == ENGLISH_SENTENCE_TYPE && !sentenceText.isNullOrEmpty()) {
            tvRecordingText.isVisible = true
            restartRecord.isSelected = true
            finishRecord.isSelected = true
            centerRecord.playAnimation()
            recordButtonContent.isVisible = false
            sendEvaluate(
                Recognizer.LANGUAGE_EN_US,
                Recognizer.EVALUATE_CATEGORY_READ_SENTENCE,
                sentenceText!!,
                false
            )
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed(20 * 1000) {
                launcher?.getService()?.getRecognizer()?.requestEnd()
            }
        } else if (testType == ENGLISH_ARTICLE_TYPE && !sentenceText.isNullOrEmpty()) {
            tvEnglishTips.text = "请继续…读完以下文章"
            tvRecordingText.isVisible = false
            centerRecord.playAnimation()
            recordButtonContent.isVisible = false
            restartRecord.isSelected = true
            finishRecord.isSelected = true
            sendEvaluate(
                Recognizer.LANGUAGE_EN_US,
                Recognizer.EVALUATE_CATEGORY_READ_CHAPTER,
                sentenceText!!,
                false
            )
        }
    }

    private fun onRestartRecord() {
        if (testType == ENGLISH_ARTICLE_TYPE ||
            testType == CHINESE_SENTENCE_TYPE ||
            testType == ENGLISH_SENTENCE_TYPE
        ) {
            centerRecord.pauseAnimation()
            recordButtonContent.isVisible = true
            recordButton.setBackgroundResource(R.drawable.ic_record_white_40)
            restartRecord.isSelected = false
            finishRecord.isSelected = false

            launcher?.getService()?.getRecognizer()?.requestCancel()
        }
    }

    private fun onFinishRecord() {
        if (!isAdded || context == null) {
            return
        }
        val endRecordTime = System.currentTimeMillis()
        if (endRecordTime - startRecordTime <= 1000) {
            Toast.makeText(context, "录音时间过短", Toast.LENGTH_SHORT).show()
        }
        launcher?.getService()?.getRecognizer()?.requestEnd()
    }

    private fun onRecordingClick() {
        if (!isAdded || context == null) {
            return
        }

        if (testType == ENGLISH_ARTICLE_TYPE ||
            testType == CHINESE_SENTENCE_TYPE ||
            testType == ENGLISH_SENTENCE_TYPE
        ) {
            val alertDialog = StyledAlertDialog.Builder()
                .setTitle("是否结束并提交录音？")
                .setNegativeButton("继续录音", View.OnClickListener {
                })
                .setNegativeButton2("重录", View.OnClickListener {
                    launcher?.getService()?.getRecognizer()?.requestCancel()
                    onRecordButtonClick()
                })
                .setPositiveButton("提交录音", View.OnClickListener {
                    launcher?.getService()?.getRecognizer()?.requestEnd()
                })
                .show(childFragmentManager)
            alertDialog.isCancelable = false
        } else {
            val alertDialog = StyledAlertDialog.Builder()
                .setTitle("是否结束并提交录音？")
                .setMessage("还没录完，只有录完才可以获得较高分数哦")
                .setNegativeButton2("重录", View.OnClickListener {
                    restartRecord()
                })
                .setPositiveButton("继续录音", View.OnClickListener {
                    launcher?.getService()?.getRecognizer()?.requestCancel()
                    onRecordButtonClick()
                })
                .show(childFragmentManager)
            alertDialog.isCancelable = false
        }
    }

    private fun restartRecord() {
        if (testType == CHINESE_SINGLE_WORD_TYPE ||
            testType == CHINESE_WORDS_TYPE ||
            testType == ENGLISH_WORD_TYPE
        ) {
            currentProgress = 0
            progressTextView.text = "1/10"
            evaluatingProgress.progress = 10
            tvWord.text = textList[0]
            readSyllableList.clear()
            launcher?.getService()?.getRecognizer()?.requestCancel()
            onRecordButtonClick()
        } else if (testType == CHINESE_SENTENCE_TYPE ||
            testType == ENGLISH_SENTENCE_TYPE
        ) {
            launcher?.getService()?.getRecognizer()?.requestCancel()
            onRecordButtonClick()
        }
    }

    private fun extraWordPayload(payload: String, objectName: String) {
        val json = JSONObject(payload)
        val readSyllableJson = json.getJSONObject("data").getJSONObject(objectName)
        val readSyllable =
            Gson().fromJson<ReadWord>(readSyllableJson.toString(), ReadWord::class.java)
        readSyllableList.add(readSyllable)
    }

    private fun extraSentencePayload(payload: String, objectName: String) {
        val json = JSONObject(payload)
        val readSentenceJson = json.getJSONObject("data").getJSONObject(objectName)
        val readWord =
            Gson().fromJson<ReadWord>(readSentenceJson.toString(), ReadWord::class.java)
        this.readSentence = readWord
    }

    private fun pauseAudioPlayer() {
        val audioPlayer = launcher?.getService()?.getAudioPlayer()
        if (audioPlayer?.playbackState == AudioPlayer.PLAYBACK_STATE_PLAYING) {
            audioPlayer.stop(AudioPlayer.TYPE_TTS)
            audioPlayer.stop(AudioPlayer.TYPE_RING)
            audioPlayer.stop(AudioPlayer.TYPE_PLAYBACK)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false

        launcher?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PromptManager.stop()
        handler.removeCallbacksAndMessages(null)

        EvsSystem.get().onEvsErrorListener = null

        val intent = Intent(context, FloatingService::class.java).apply {
            action = FloatingService.ACTION_EVALUATING
            putExtra("isEvaluating", false)
        }
        context?.startService(intent)

        if (shouldShowMidErrorView) {
            val showMicErrorIntent = Intent(context, FloatingService::class.java).apply {
                action = FloatingService.ACTION_SHOW_MISC_ERROR
            }
            context?.startService(showMicErrorIntent)
        }

        EventBus.getDefault().unregister(this)
        handler.postDelayed(100) {
            if (shouldEnableWakeUp) {
                ConfigUtils.putBoolean(ConfigUtils.KEY_VOICE_WAKEUP_ENABLED, true)
            }
        }
        launcher?.getService()?.getRecognizer()?.requestCancel()
        (launcher?.getService()?.getAlarm() as? EvsAlarm)?.removeListener(this)
        currentProgress = 0
        GlobalRecorder.unregisterObserver(this)
    }

    data class EvaluationAction(val action: Int)
}