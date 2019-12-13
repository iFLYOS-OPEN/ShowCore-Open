package com.iflytek.cyber.iot.show.core.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.os.postDelayed
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.AlarmApi
import com.iflytek.cyber.iot.show.core.model.*
import com.iflytek.cyber.iot.show.core.utils.DateUtils.Companion.dateToStr
import com.iflytek.cyber.iot.show.core.utils.DateUtils.Companion.getDateList
import com.iflytek.cyber.iot.show.core.utils.DateUtils.Companion.getHourList
import com.iflytek.cyber.iot.show.core.utils.DateUtils.Companion.getMinList
import com.iflytek.cyber.iot.show.core.utils.DateUtils.Companion.getMonthList
import com.iflytek.cyber.iot.show.core.utils.DateUtils.Companion.stringToDate
import com.iflytek.cyber.iot.show.core.widget.CircleCheckBox
import com.zyyoona7.wheel.WheelView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList

class EditAlarmFragment : BaseFragment(), View.OnClickListener {

    companion object {
        private const val REQUEST_EDIT_CODE = 13023

        fun instance(alert: Alert?): EditAlarmFragment {
            return EditAlarmFragment().apply {
                arguments = bundleOf(Pair("alert", alert))
            }
        }
    }

    private lateinit var drawer: DrawerLayout
    private lateinit var noneWheel: WheelView<DateEntity>
    private lateinit var dailyWheel: WheelView<String>
    private lateinit var hourWheel: WheelView<String>
    private lateinit var minuteWheel: WheelView<String>
    private lateinit var tvRepeatType: TextView
    private lateinit var tvAlarmDesc: TextView
    private lateinit var tvRepeatSettings: TextView
    private lateinit var tvTitle: TextView

    private lateinit var customList: RecyclerView
    private var customAdapter: CustomAdapter? = null
    private lateinit var repeatList: RecyclerView
    private var repeatTypeAdapter: RepeatTypeAdapter? = null
    private var repeatTypeList = ArrayList<RepeatType>()
    private var currentDesc: String? = null

    private var dateList = ArrayList<DateEntity>()

    private var alert: Alert? = null
    private var year = 0
    private var currentRepeatType: String? = null

    private var handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_edit_alarm, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.iv_back).setOnClickListener { pop() }
        view.findViewById<View>(R.id.done).setOnClickListener(this)

        drawer = view.findViewById(R.id.drawer)
        repeatList = view.findViewById(R.id.repeat_list)
        customList = view.findViewById(R.id.custom_list)
        noneWheel = view.findViewById(R.id.none_wheel)
        dailyWheel = view.findViewById(R.id.daily_wheel)
        hourWheel = view.findViewById(R.id.hour_wheel)
        minuteWheel = view.findViewById(R.id.minute_wheel)
        tvRepeatType = view.findViewById(R.id.tv_repeat_type)
        tvAlarmDesc = view.findViewById(R.id.tv_alarm_desc)
        tvRepeatSettings = view.findViewById(R.id.tv_repeat_settings)
        tvTitle = view.findViewById(R.id.tv_title)

        view.findViewById<View>(R.id.repeat_type).setOnClickListener(this)
        view.findViewById<View>(R.id.alarm_desc_content).setOnClickListener(this)

        alert = arguments?.getParcelable("alert")

        currentDesc = alert?.content

        hourWheel.data = getHourList()
        minuteWheel.data = getMinList()

        if (alert == null) { //add new alarm
            tvTitle.setText(R.string.add_new_alarm)
            tvAlarmDesc.text = "无"
            currentRepeatType = Alert.TYPE_NONE
            tvRepeatType.text = RepeatType.getName(Alert.TYPE_NONE)
            setDateWheel(Alert.TYPE_NONE)
        } else { //edit current alarm
            tvTitle.setText(R.string.edit_alarm)
            currentRepeatType = alert?.repeatType
            if (TextUtils.equals(alert?.repeatType, Alert.TYPE_WEEKLY)) {
                tvRepeatType.text = alert?.description
            } else {
                tvRepeatType.text = RepeatType.getName(alert!!.repeatType)
            }
            if (alert?.content.isNullOrEmpty()) {
                tvAlarmDesc.text = "无"
            } else {
                tvAlarmDesc.text = alert!!.content
            }
            setDateWheel(alert!!.repeatType)
        }

        noneWheel.onWheelChangedListener = object : WheelView.OnWheelChangedListener {
            override fun onWheelScroll(scrollOffsetY: Int) {
            }

            override fun onWheelSelected(position: Int) {
                if (position == noneWheel.data.size - 1) {
                    year += 1
                    val calendar = Calendar.getInstance()
                    val start = "${calendar.get(Calendar.YEAR) + year}-01-01}"
                    val end = "${calendar.get(Calendar.YEAR) + year}-12-31"
                    val data = getDateList(start, end, Alert.TYPE_NONE)
                    dateList.addAll(data)
                    noneWheel.data = dateList
                }
            }

            override fun onWheelScrollStateChanged(state: Int) {
            }

            override fun onWheelItemChanged(oldPosition: Int, newPosition: Int) {
            }
        }

        drawer.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }

            override fun onDrawerClosed(drawerView: View) {
            }

            override fun onDrawerOpened(drawerView: View) {
                autoCloseDrawer()
            }
        })

        repeatList.setOnTouchListener { v, event ->
            autoCloseDrawer()
            false
        }
        customList.setOnTouchListener { v, event ->
            autoCloseDrawer()
            false
        }

        setupRepeatType()
        setupWeeks()
    }

    private fun autoCloseDrawer() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(20000) {
            drawer.closeDrawer(GravityCompat.END)
        }
    }

    private fun findCurrentDateIndex(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.time = Date(timestamp * 1000)
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val realMonth = String.format(Locale.getDefault(), "%02d", month)
        val realDay = String.format(Locale.getDefault(), "%02d", day)
        val newDate = stringToDate("yyyy-MM-dd", "$year-$realMonth-$realDay")
        for (i in 0 until noneWheel.data.size) {
            val entity = noneWheel.data[i]
            if (entity.date.time == newDate.time) {
                return i
            }
        }
        return 0
    }

    private fun setDateWheel(repeatType: String) {
        val calendar = Calendar.getInstance()
        val times = alert?.time?.split(Regex(":"))
        if (!times.isNullOrEmpty()) {
            hourWheel.setSelectedItemPosition(times[0].toInt(), false)
            minuteWheel.setSelectedItemPosition(times[1].toInt(), false)
        } else {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            hourWheel.setSelectedItemPosition(hour, false)
            minuteWheel.setSelectedItemPosition(minute, false)
        }
        when (repeatType) {
            Alert.TYPE_NONE -> {
                dailyWheel.isVisible = false
                noneWheel.isVisible = true
                val start =
                    "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(
                        Calendar.DAY_OF_MONTH
                    )}"
                val end = "${calendar.get(Calendar.YEAR)}-12-31"
                val data = getDateList(start, end, Alert.TYPE_NONE)
                noneWheel.data = data
                if (alert != null && alert!!.timestamp > 0) {
                    val index = findCurrentDateIndex(alert!!.timestamp)
                    noneWheel.setSelectedItemPosition(index, false)
                } else {
                    noneWheel.setSelectedItemPosition(0, false)
                }
                dateList.clear()
                dateList.addAll(data)
            }
            Alert.TYPE_YEARLY -> {
                dailyWheel.isVisible = false
                noneWheel.isVisible = true
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val data = getDateList("$year-01-01", "$year-12-31", Alert.TYPE_YEARLY)
                noneWheel.data = data
                if (alert?.repeatDate.isNullOrEmpty()) {
                    val realMonth = String.format(Locale.getDefault(), "%02d", month)
                    val realDay = String.format(Locale.getDefault(), "%02d", day)
                    val date = stringToDate("$year-$realMonth-$realDay")
                    val index = String.format("%tj", date).toInt() - 1
                    noneWheel.setSelectedItemPosition(index, false)
                } else if (alert != null) {
                    val date = stringToDate("$year-${alert!!.repeatDate}")
                    val index = String.format("%tj", date).toInt() - 1
                    noneWheel.selectedItemPosition = index
                }
            }
            Alert.TYPE_DAILY, Alert.TYPE_WORKDAY, Alert.TYPE_RESTDAY, Alert.TYPE_WEEKEND, Alert.TYPE_WEEKLY, Alert.TYPE_WEEKDAY -> {
                dailyWheel.isVisible = false
                noneWheel.isVisible = false
            }
            Alert.TYPE_MONTHLY -> {
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                dailyWheel.isVisible = true
                noneWheel.isVisible = false
                dailyWheel.data = getMonthList()
                val position = if (alert != null) {
                    alert!!.repeatDateNumber - 1
                } else {
                    day - 1
                }
                dailyWheel.setSelectedItemPosition(position, false)
            }
        }
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        launcher?.setImmersiveFlags()
    }

    override fun onFragmentResult(requestCode: Int, resultCode: Int, data: Bundle) {
        if (requestCode == REQUEST_EDIT_CODE) {
            val isEdited = data.getBoolean("isEdited")
            if (isEdited) {
                val desc = data.getString("desc")
                tvAlarmDesc.text = desc
                alert?.content = desc
                currentDesc = desc
            }
        }
    }

    private fun updateAlert() {
        if (alert == null) {
            return
        }

        val body = AlertBody()
        body.id = alert!!.id
        body.content = currentDesc
        var hour = hourWheel.selectedItemData.substring(0, hourWheel.selectedItemData.length - 1)
        var minute =
            minuteWheel.selectedItemData.substring(0, minuteWheel.selectedItemData.length - 1)
        if (hour.toInt() < 10) {
            hour = "0$hour"
        }
        if (minute.toInt() < 10) {
            minute = "0$minute"
        }
        if (TextUtils.equals(currentRepeatType, Alert.TYPE_NONE)) {
            body.alertType = "single"
            val calendar = Calendar.getInstance()
            calendar.time = noneWheel.selectedItemData.date
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val date = stringToDate("yyyy-MM-dd HH:mm", "$year-$month-$day $hour:$minute")
            body.timestamp = date.time / 1000
        } else {
            body.alertType = "repeat"
            body.time = "$hour:$minute"
            body.repeatType = currentRepeatType
            if (TextUtils.equals(currentRepeatType, Alert.TYPE_WEEKLY)) {
                body.repeatWeekdays = customAdapter?.selects?.toList()?.sorted()
            }
            if (TextUtils.equals(currentRepeatType, Alert.TYPE_MONTHLY)) {
                body.repeatDateNumber =
                    dailyWheel.selectedItemData.substring(0, dailyWheel.selectedItemData.length - 1)
                        .toInt()
            }
            if (TextUtils.equals(currentRepeatType, Alert.TYPE_YEARLY)) {
                body.repeatDate = dateToStr("MM-dd", noneWheel.selectedItemData.date)
            }
        }

        getAlarmApi()?.updateAlert(alert!!.id, body)?.enqueue(object : Callback<Message> {
            override fun onFailure(call: Call<Message>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(call: Call<Message>, response: Response<Message>) {
                if (response.isSuccessful) {
                    Toast.makeText(tvAlarmDesc.context, "更新成功", Toast.LENGTH_SHORT).show()
                    pop()
                } else {
                    showError(response.errorBody()?.string())
                }
            }
        })
    }

    private fun showError(errorStr: String?) {
        try {
            val error = Gson().fromJson(errorStr, Error::class.java)
            Toast.makeText(tvAlarmDesc.context, error.message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addNewAlarm() {
        val body = AlertBody()
        body.content = currentDesc
        var hour = hourWheel.selectedItemData.substring(0, hourWheel.selectedItemData.length - 1)
        var minute =
            minuteWheel.selectedItemData.substring(0, minuteWheel.selectedItemData.length - 1)
        if (hour.toInt() < 10) {
            hour = "0$hour"
        }
        if (minute.toInt() < 10) {
            minute = "0$minute"
        }
        if (TextUtils.equals(currentRepeatType, Alert.TYPE_NONE)) {
            body.alertType = "single"
            val calendar = Calendar.getInstance()
            calendar.time = noneWheel.selectedItemData.date
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val date = stringToDate("yyyy-MM-dd HH:mm", "$year-$month-$day $hour:$minute")
            body.timestamp = date.time / 1000
        } else {
            body.alertType = "repeat"
            body.time = "$hour:$minute"
            body.repeatType = currentRepeatType
            if (TextUtils.equals(currentRepeatType, Alert.TYPE_WEEKLY)) {
                body.repeatWeekdays = customAdapter?.selects?.toList()?.sorted()
            }
            if (TextUtils.equals(currentRepeatType, Alert.TYPE_MONTHLY)) {
                body.repeatDateNumber =
                    dailyWheel.selectedItemData.substring(0, dailyWheel.selectedItemData.length - 1)
                        .toInt()
            }
            if (TextUtils.equals(currentRepeatType, Alert.TYPE_YEARLY)) {
                body.repeatDate = dateToStr("MM-dd", noneWheel.selectedItemData.date)
            }
        }

        getAlarmApi()?.addNewAlarm(body)?.enqueue(object : Callback<Message> {
            override fun onFailure(call: Call<Message>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(call: Call<Message>, response: Response<Message>) {
                if (response.isSuccessful) {
                    Toast.makeText(tvAlarmDesc.context, "添加成功", Toast.LENGTH_SHORT).show()
                    pop()
                } else {
                    showError(response.errorBody()?.string())
                }
            }
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.repeat_type -> {
                if (drawer.isDrawerOpen(GravityCompat.END)) {
                    drawer.closeDrawer(GravityCompat.END)
                } else {
                    tvRepeatSettings.text = "设置重复"
                    repeatList.isVisible = true
                    customList.isVisible = false
                    drawer.openDrawer(GravityCompat.END)
                }
            }
            R.id.alarm_desc_content -> {
                val desc = if (alert?.content.isNullOrEmpty()) {
                    ""
                } else {
                    tvAlarmDesc.text.toString()
                }
                startForResult(
                    EditAlarmDescFragment.instance(desc),
                    REQUEST_EDIT_CODE
                )
            }
            R.id.done -> {
                if (alert != null) {
                    updateAlert()
                } else { //add new alarm
                    addNewAlarm()
                }
            }
        }
    }

    private fun setupRepeatType() {
        val type1 = RepeatType(Alert.TYPE_NONE, "不重复")
        repeatTypeList.add(type1)
        val type2 = RepeatType(Alert.TYPE_DAILY, "每天")
        repeatTypeList.add(type2)
        val type3 = RepeatType(Alert.TYPE_MONTHLY, "每月")
        repeatTypeList.add(type3)
        val type4 = RepeatType(Alert.TYPE_YEARLY, "每年")
        repeatTypeList.add(type4)
        val type5 = RepeatType(Alert.TYPE_WORKDAY, "法定工作日")
        repeatTypeList.add(type5)
        val type6 = RepeatType(Alert.TYPE_RESTDAY, "法定节假日")
        repeatTypeList.add(type6)
        val type7 = RepeatType(Alert.TYPE_WEEKEND, "周末")
        repeatTypeList.add(type7)
        val type8 = RepeatType(Alert.TYPE_WEEKDAY, "周一至周五")
        repeatTypeList.add(type8)
        val type9 = RepeatType(Alert.TYPE_WEEKLY, "自定义")
        repeatTypeList.add(type9)
        repeatTypeAdapter = RepeatTypeAdapter(repeatTypeList) {
            currentRepeatType = it.type
            if (it.type == Alert.TYPE_WEEKLY) {
                //custom
                tvRepeatSettings.text = "自定义"
                customAdapter?.setRepeatType()
                noneWheel.isVisible = false
                repeatList.isVisible = false
                customList.isVisible = true
            } else {
                drawer.closeDrawer(GravityCompat.END)
                tvRepeatType.text = RepeatType.getName(it.type)
                setDateWheel(it.type)
            }
        }
        repeatList.adapter = repeatTypeAdapter
    }

    private fun setupWeeks() {
        val items = arrayListOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        customAdapter = CustomAdapter(items)
        if (alert != null && alert!!.repeatWeekdays != null) {
            customAdapter?.selects?.clear()
            customAdapter?.selects?.addAll(alert!!.repeatWeekdays!!.toList())
        }
        customList.adapter = customAdapter
    }

    inner class RepeatTypeAdapter(
        private val repeatList: ArrayList<RepeatType>,
        private val onItemClickListener: (RepeatType) -> Unit
    ) : RecyclerView.Adapter<RepeatTypeAdapter.RepeatTypeHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepeatTypeHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_repeat_type, parent, false)
            return RepeatTypeHolder(view)
        }

        override fun getItemCount(): Int {
            return repeatList.size
        }

        override fun onBindViewHolder(holder: RepeatTypeHolder, position: Int) {
            val repeatType = repeatList[position]
            holder.type.text = repeatType.name
            holder.itemView.setOnClickListener {
                onItemClickListener.invoke(repeatType)
            }
        }

        inner class RepeatTypeHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val type = itemView.findViewById<TextView>(R.id.type)
        }
    }

    inner class CustomAdapter(private val items: ArrayList<String>) :
        RecyclerView.Adapter<CustomAdapter.CustomHolder>() {

        val selects = hashSetOf(1, 2, 3, 4, 5, 6, 7)
        var customText: String = ""

        private fun transferText(position: Int): String {
            when (position) {
                1 -> return "周一"
                2 -> return "周二"
                3 -> return "周三"
                4 -> return "周四"
                5 -> return "周五"
                6 -> return "周六"
                7 -> return "周日"
            }
            return ""
        }

        fun setRepeatType() {
            customText = ""
            selects.sorted().toList().forEach {
                customText = customText + transferText(it) + ", "
            }
            if (customText.length > 1) {
                tvRepeatType.text = customText.substring(0, customText.length - 1)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_custom_weekday, parent, false)
            return CustomHolder(view)
        }

        override fun getItemCount(): Int {
            return items.size
        }

        private fun onItemClick(holder: CustomHolder, position: Int) {
            if (holder.checkbox.isChecked) {
                holder.checkbox.isChecked = false
                selects.remove(position + 1)
            } else {
                holder.checkbox.isChecked = true
                selects.add(position + 1)
            }
            setRepeatType()
        }

        override fun onBindViewHolder(holder: CustomHolder, position: Int) {
            holder.tvWeek.text = items[position]
            holder.checkbox.isChecked = selects.contains(position + 1)
            holder.itemView.setOnClickListener {
                onItemClick(holder, position)
            }
            holder.checkbox.setOnClickListener {
                onItemClick(holder, position)
            }
        }

        inner class CustomHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvWeek = itemView.findViewById<TextView>(R.id.tv_week)
            val checkbox = itemView.findViewById<CircleCheckBox>(R.id.check_box)
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