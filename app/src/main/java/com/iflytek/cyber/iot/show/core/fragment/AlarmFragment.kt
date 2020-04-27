package com.iflytek.cyber.iot.show.core.fragment

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.iflytek.cyber.evs.sdk.agent.Alarm
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.AlarmAdapter
import com.iflytek.cyber.iot.show.core.api.AlarmApi
import com.iflytek.cyber.iot.show.core.impl.alarm.EvsAlarm
import com.iflytek.cyber.iot.show.core.model.Alert
import com.iflytek.cyber.iot.show.core.model.Message
import com.iflytek.cyber.iot.show.core.utils.ContextWrapper
import com.iflytek.cyber.iot.show.core.utils.InsetDividerDecoration
import com.iflytek.cyber.iot.show.core.widget.ExFrameLayout
import com.kk.taurus.playerbase.utils.NetworkUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AlarmFragment : BaseFragment(), PageScrollable {

    companion object {
        private const val REQUEST_ADD_ALARM_CODE = 10332
    }

    //private lateinit var tvSetAlarmTips: TextView
    private lateinit var tvEmptyTips: TextView
    private lateinit var alarmList: RecyclerView
    private lateinit var tvSummary: AppCompatTextView
    private lateinit var errorContainer: LinearLayout
    private lateinit var loading: LottieAnimationView

    private lateinit var alarmAdapter: AlarmAdapter

    private var shouldUpdateAlarm = false
    private var backCount = 0
    private var currentSummary = 0
    private var isLoading = false

    private val tips = arrayListOf(
        "你可以说：帮我设置工作日早上八点半的闹钟",
        "你可以说：明天中午12点提醒我和父母吃饭",
        "你可以说：提醒我明天下午两点和女朋友逛街"
    )

    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EvsAlarm.get(context).addOnAlarmUpdatedListener(onAlarmUpdatedListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_alarm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //tvSetAlarmTips = view.findViewById(R.id.tv_set_alarm_tips)
        errorContainer = view.findViewById(R.id.error_container)
        loading = view.findViewById(R.id.loading)
        view.findViewById<Button>(R.id.retry).setOnClickListener {
            errorContainer.isVisible = false
            loading.isVisible = true
            loading.playAnimation()
            getAlerts()
        }
        tvSummary = view.findViewById(R.id.tv_summary)
        tvEmptyTips = view.findViewById(R.id.tv_empty_tips)
        alarmList = view.findViewById(R.id.alarm_list)
        val exFrameLayout = view.findViewById<ExFrameLayout>(R.id.ex_frame_layout)
        exFrameLayout.setOnCloseListener(object : ExFrameLayout.OnCloseListener {
            override fun onClose() {
                pop()
            }
        })

        tvSummary.text = tips[currentSummary]

        view.findViewById<View>(R.id.iv_back).setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            setFragmentResult(0, bundleOf(Pair("shouldUpdateAlarm", shouldUpdateAlarm)))
            pop()
        }

        val button = view.findViewById<Button>(R.id.add_alarm)
        button.setOnClickListener {
            if (isAdded && context != null && !NetworkUtils.isNetConnected(context)) {
                showNetworkError()
                return@setOnClickListener
            }
            startForResult(EditAlarmFragment(), REQUEST_ADD_ALARM_CODE)
        }

        alarmAdapter = AlarmAdapter({
            start(EditAlarmFragment.instance(it))
        }, { alert, position ->
            deleteAlarm(alert, position)
        })

        val lineHeight = resources.getDimensionPixelSize(R.dimen.dp_1)
        val padding = resources.getDimensionPixelSize(R.dimen.dp_40)
        val dividerColor = ContextCompat.getColor(requireActivity(), R.color.grey_line)
        val decoration = InsetDividerDecoration(lineHeight, dividerColor, 0)
        decoration.startPadding = padding
        decoration.endPadding = padding
        alarmList.addItemDecoration(decoration)
        alarmList.adapter = alarmAdapter

        loading.isVisible = true
        loading.playAnimation()
        getAlerts()

        setFragmentResult(0, bundleOf(Pair("shouldUpdateAlarm", false)))

        if (isAdded && context != null && !NetworkUtils.isNetConnected(context)) {
            showNetworkError()
        }
    }

    private fun showNetworkError() {
        val networkErrorNotification =
            Intent(context, FloatingService::class.java)
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
        ContextWrapper.startServiceAsUser(context!!, networkErrorNotification, "CURRENT")
    }

    override fun onDestroy() {
        super.onDestroy()

        EvsAlarm.get(context).removeOnAlarmUpdatedListener(onAlarmUpdatedListener)
    }

    override fun onBackPressedSupport(): Boolean {
        setFragmentResult(0, bundleOf(Pair("shouldUpdateAlarm", shouldUpdateAlarm)))
        return super.onBackPressedSupport()
    }

    private fun deleteAlarm(alert: Alert, position: Int) {
        getAlarmApi()?.deleteAlarm(alert.id)?.enqueue(object : Callback<Message> {
            override fun onFailure(call: Call<Message>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(call: Call<Message>, response: Response<Message>) {
                if (!isAdded) {
                    return
                }
                if (response.isSuccessful) {
                    alarmAdapter.alerts.removeAt(position)
                    alarmAdapter.notifyDataSetChanged()
                    /*alarmAdapter.notifyItemRemoved(position)
                    alarmAdapter.notifyItemRangeRemoved(position, alarmAdapter.alerts.size)*/
                    Toast.makeText(alarmList.context, "删除成功", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun getAlerts() {
        if (isLoading) {
            return
        }
        isLoading = true
        getAlarmApi()?.getAlerts()?.enqueue(object : Callback<ArrayList<Alert>> {
            override fun onFailure(call: Call<ArrayList<Alert>>, t: Throwable) {
                t.printStackTrace()
                isLoading = false
                loading.isVisible = false
                loading.pauseAnimation()
                errorContainer.isVisible = true
            }

            override fun onResponse(
                call: Call<ArrayList<Alert>>,
                response: Response<ArrayList<Alert>>
            ) {
                isLoading = false
                loading.isVisible = false
                loading.pauseAnimation()

                if (response.isSuccessful) {
                    errorContainer.isVisible = false
                    val items = response.body()
                    items?.let {
                        if (it.isNullOrEmpty()) {
                            //tvSetAlarmTips.isVisible = false
                            tvEmptyTips.isVisible = true
                            alarmList.isVisible = false
                        } else {
                            alarmList.isVisible = true
                            //tvSetAlarmTips.isVisible = true
                            tvEmptyTips.isVisible = false
                        }
                        alarmAdapter.alerts.clear()
                        alarmAdapter.alerts.addAll(it)
                        alarmAdapter.notifyDataSetChanged()
                    }
                } else {
                    errorContainer.isVisible = true
                }
            }
        })
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        postNextUpdateSummary()
    }

    override fun onSupportInvisible() {
        super.onSupportInvisible()
        handler.removeCallbacksAndMessages(null)
    }

    private val updateSummaryRunnable: Runnable = Runnable {
        if (isResumed) {
            val next = (currentSummary + 1) % tips.size
            currentSummary = next
            startUpdateSummaryAnimator(tips[next])

            postNextUpdateSummary()
        }
    }

    private fun postNextUpdateSummary() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(updateSummaryRunnable, 6 * 1000)
    }

    private fun startUpdateSummaryAnimator(newText: String) {
        val animator = ValueAnimator.ofFloat(0f, 2f)
        animator.addUpdateListener {
            val value = it.animatedValue as Float
            if (value > 1) {
                if (tvSummary.text.toString() != newText) {
                    tvSummary.text = tips[currentSummary]
                }
                tvSummary.alpha = value - 1
            } else {
                tvSummary.alpha = 1 - value
            }
        }
        animator.duration = 600
        animator.start()
    }

    override fun scrollToNext(): Boolean {
        alarmList.let { recyclerView ->
            val lastItem =
                (recyclerView.layoutManager as? LinearLayoutManager)?.findLastCompletelyVisibleItemPosition()
            val itemCount = alarmAdapter.itemCount
            if (lastItem == itemCount - 1 || itemCount == 0) {
                return false
            } else {
                recyclerView.smoothScrollBy(0, recyclerView.height)
            }
        }
        return true
    }

    override fun scrollToPrevious(): Boolean {
        alarmList.let { recyclerView ->
            val scrollY = recyclerView.computeVerticalScrollOffset()
            val itemCount = alarmAdapter.itemCount
            if (scrollY == 0 || itemCount == 0) {
                return false
            } else {
                recyclerView.smoothScrollBy(0, -recyclerView.height)
            }
        }
        return true
    }

    private val onAlarmUpdatedListener = object : Alarm.OnAlarmUpdatedListener {
        override fun onAlarmUpdated() {
            alarmList.postDelayed(50) {
                getAlerts()
            }
            shouldUpdateAlarm = true
        }
    }

    override fun onFragmentResult(requestCode: Int, resultCode: Int, data: Bundle) {
        super.onFragmentResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_ALARM_CODE) {
            alarmList.postDelayed(80) {
                getAlerts()
            }
        }
    }

    private fun getAlarmApi(): AlarmApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(AlarmApi::class.java)
        } else {
            null
        }
    }
}