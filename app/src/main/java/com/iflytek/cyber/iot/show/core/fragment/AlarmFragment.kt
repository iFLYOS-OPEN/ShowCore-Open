package com.iflytek.cyber.iot.show.core.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.evs.sdk.agent.Alarm
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.adapter.AlarmAdapter
import com.iflytek.cyber.iot.show.core.api.AlarmApi
import com.iflytek.cyber.iot.show.core.impl.alarm.EvsAlarm
import com.iflytek.cyber.iot.show.core.model.Alert
import com.iflytek.cyber.iot.show.core.model.Message
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AlarmFragment : BaseFragment(), PageScrollable {

    private lateinit var tvSetAlarmTips: TextView
    private lateinit var tvEmptyTips: TextView
    private lateinit var alarmList: RecyclerView

    private lateinit var alarmAdapter: AlarmAdapter

    private var shouldUpdateAlarm = false
    private var backCount = 0

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

        tvSetAlarmTips = view.findViewById(R.id.tv_set_alarm_tips)
        tvEmptyTips = view.findViewById(R.id.tv_empty_tips)
        alarmList = view.findViewById(R.id.alarm_list)

        view.findViewById<View>(R.id.iv_back).setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            setFragmentResult(0, bundleOf(Pair("shouldUpdateAlarm", shouldUpdateAlarm)))
            pop()
        }

        val button = view.findViewById<Button>(R.id.add_alarm)
        button.setOnClickListener {
            start(EditAlarmFragment())
        }

        alarmAdapter = AlarmAdapter({
            start(EditAlarmFragment.instance(it))
        }, { alert, position ->
            AlertDialog.Builder(alarmList.context)
                .setMessage("您确定删除闹钟吗？")
                .setPositiveButton("确定") { dialog, which ->
                    deleteAlarm(alert, position)
                }
                .setNegativeButton("取消", null)
                .show()
        })
        alarmList.adapter = alarmAdapter

        getAlerts()

        setFragmentResult(0, bundleOf(Pair("shouldUpdateAlarm", false)))
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
                if (response.isSuccessful) {
                    alarmAdapter.alerts.removeAt(position)
                    alarmAdapter.notifyItemRemoved(position)
                    alarmAdapter.notifyItemRangeRemoved(position, alarmAdapter.alerts.size)
                    Toast.makeText(alarmList.context, "删除成功", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun getAlerts() {
        getAlarmApi()?.getAlerts()?.enqueue(object : Callback<ArrayList<Alert>> {
            override fun onFailure(call: Call<ArrayList<Alert>>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(
                call: Call<ArrayList<Alert>>,
                response: Response<ArrayList<Alert>>
            ) {
                if (response.isSuccessful) {
                    val items = response.body()
                    items?.let {
                        if (it.isNullOrEmpty()) {
                            tvSetAlarmTips.isVisible = false
                            tvEmptyTips.isVisible = true
                            alarmList.isVisible = false
                        } else {
                            alarmList.isVisible = true
                            tvSetAlarmTips.isVisible = true
                            tvEmptyTips.isVisible = false
                        }
                        alarmAdapter.alerts.clear()
                        alarmAdapter.alerts.addAll(it)
                        alarmAdapter.notifyDataSetChanged()
                    }
                }
            }
        })
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
            getAlerts()
            shouldUpdateAlarm = true
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