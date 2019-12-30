package com.iflytek.cyber.evs.sdk.agent.impl

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.content.*
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Handler
import android.os.Looper
import com.iflytek.cyber.evs.sdk.agent.Alarm
import com.iflytek.cyber.evs.sdk.utils.Log
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.*

internal class AlarmImpl(private val context: Context) : Alarm() {
    private val dataHelper = AlarmDataHelper(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val alarmPlayer = AlarmPlayerInstance(context)

    private var activeAlarmId: String? = null

    private var packageName = ""

    companion object {
        private const val TAG = "AlarmImpl"

        const val NAME_DB_ALARMS = "alarms"
        const val NAME_TABLE_ALARMS = "alarms"

        const val ACTION_ALARM_ARRIVED = "com.iflytek.cyber.evs.sdk.agent.action.ARRIVED"
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "$packageName&$ACTION_ALARM_ARRIVED" -> {
                    val alarmId = intent.getStringExtra(KEY_ALARM_ID)
                    val url = intent.getStringExtra(KEY_URL)
                    val timestamp = intent.getLongExtra(KEY_TIMESTAMP, -1L)

                    Log.d(TAG, "alarm arrived, {alarm_id=$alarmId, url=$url}.")

                    val current = System.currentTimeMillis() / 1000
                    if (current - timestamp > 5000) {
                        Log.w(
                            TAG,
                            "alarm {alarm_id=$alarmId} expired. Now is $current, alarm time is $timestamp"
                        )
                    } else {
                        activeAlarmId = alarmId
                        alarmPlayer.play(url)
                    }

                    dataHelper.deleteAlarm(alarmId)

                    onAlarmUpdatedListener?.onAlarmUpdated()
                }
            }
        }
    }

    init {
        packageName = context.packageName

        // 注册广播接收
        val intentFilter = IntentFilter()
        intentFilter.addAction("$packageName&$ACTION_ALARM_ARRIVED")
        context.registerReceiver(receiver, intentFilter)

        // 初始化闹钟服务
        val currentTimestamp = System.currentTimeMillis() / 1000
        dataHelper.queryLocalAlarms().map {
            if (it.timestamp <= currentTimestamp) {
                // 过时闹钟应被删除
                dataHelper.deleteAlarm(it.alarmId)

                onAlarmUpdatedListener?.onAlarmUpdated()
            } else {
                // 重新设置闹钟到服务中
                val intent = Intent("$packageName&$ACTION_ALARM_ARRIVED")
                intent.putExtra(KEY_ALARM_ID, it.alarmId)
                intent.putExtra(KEY_TIMESTAMP, it.timestamp)
                intent.putExtra(KEY_URL, it.url)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    it.alarmId.hashCode(),
                    intent,
                    FLAG_CANCEL_CURRENT // 更新已存在的闹钟
                )
                alarmManager.set(AlarmManager.RTC_WAKEUP, it.timestamp * 1000, pendingIntent)
            }
        }

        alarmPlayer.setOnAlarmStateChangeListener(object :
            AlarmPlayerInstance.OnAlarmStateChangeListener {
            override fun onStarted() {
                activeAlarmId?.let { alarmId ->
                    this@AlarmImpl.onAlarmStarted(alarmId)
                }
            }

            override fun onStopped() {
                activeAlarmId?.let { alarmId ->
                    this@AlarmImpl.onAlarmStopped(alarmId)
                    activeAlarmId = null
                }
            }

        })
    }

    override fun stop() {
        super.stop()
        alarmPlayer.stop()
    }

    override fun setAlarm(alarm: Item) {
        super.setAlarm(alarm)
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        val time = fmt.format(Date(alarm.timestamp * 1000))

        Log.d(TAG, "set alarm, {alarm_id=${alarm.alarmId}, time=$time}.")

        val intent = Intent("$packageName&$ACTION_ALARM_ARRIVED")
        intent.putExtra(KEY_ALARM_ID, alarm.alarmId)
        intent.putExtra(KEY_TIMESTAMP, alarm.timestamp)
        intent.putExtra(KEY_URL, alarm.url)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.alarmId.hashCode(),
            intent,
            FLAG_CANCEL_CURRENT // 更新已存在的闹钟
        )
        alarmManager.set(AlarmManager.RTC_WAKEUP, alarm.timestamp * 1000, pendingIntent)

        dataHelper.addAlarm(alarm)
    }

    private fun cancelAlarm(alarmId: String) {
        val alarm = dataHelper.queryAlarm(alarmId)
        alarm?.let {
            Log.d(TAG, "cancel unarrived alarm, {alarm_id=$alarmId}.")

            val intent = Intent("$packageName&$ACTION_ALARM_ARRIVED")
            intent.putExtra(KEY_ALARM_ID, it.alarmId)
            intent.putExtra(KEY_TIMESTAMP, it.timestamp)
            intent.putExtra(KEY_URL, it.url)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.hashCode(),
                intent,
                FLAG_CANCEL_CURRENT // 更新已存在的闹钟
            )

            alarmManager.cancel(pendingIntent)
        }

        if (activeAlarmId == alarmId) {
            Log.d(TAG, "stop active alarm, {alarm_id=$alarmId}.")

            Handler(Looper.getMainLooper()).post {
                alarmPlayer.stop()
            }
        }
    }

    override fun deleteAlarm(alarmId: String) {
        super.deleteAlarm(alarmId)
        cancelAlarm(alarmId)

        Log.d(TAG, "delete alarm, {alarm_id=$alarmId}.")
        dataHelper.deleteAlarm(alarmId)
    }

    override fun getLocalAlarms(): List<Item> {
        return dataHelper.queryLocalAlarms()
    }

    override fun getActiveAlarmId(): String? {
        return activeAlarmId
    }

    override fun destroy() {
        context.unregisterReceiver(receiver)
    }

    private class AlarmDataHelper(
        context: Context?
    ) : SQLiteOpenHelper(context, NAME_DB_ALARMS, null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE $NAME_TABLE_ALARMS (id INTEGER PRIMARY KEY AUTOINCREMENT, $KEY_ALARM_ID TEXT, $KEY_URL TEXT, $KEY_TIMESTAMP TEXT)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $NAME_TABLE_ALARMS")
            onCreate(db)
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            onUpgrade(db, oldVersion, newVersion)
        }

        fun queryLocalAlarms(): List<Item> {
            val list = mutableListOf<Item>()

            val db = readableDatabase

            val cursor = db.query(
                NAME_TABLE_ALARMS,  // The table to query
                null,               // The array of columns to return (pass null to get all)
                null,               // The columns for the WHERE clause
                null,               // The values for the WHERE clause
                null,               // don't group the rows
                null,               // don't filter by row groups
                null                // The sort order
            )

            with(cursor) {
                while (moveToNext()) {
                    val alarmId = getString(getColumnIndexOrThrow(KEY_ALARM_ID))
                    val timestamp = getString(getColumnIndexOrThrow(KEY_TIMESTAMP))
                    val url = getString(getColumnIndexOrThrow(KEY_URL))

                    val timestampValue = try {
                        timestamp.toLong()
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                        -1L
                    }

                    if (timestampValue > 0) {
                        list.add(Item(alarmId, timestampValue, url))
                    }
                }
            }

            cursor.close()

            return list.toList()
        }

        fun queryAlarm(alarmId: String): Item? {
            val list = mutableListOf<Item>()

            val db = readableDatabase

            val cursor = db.query(
                NAME_TABLE_ALARMS,  // The table to query
                null,               // The array of columns to return (pass null to get all)
                "$KEY_ALARM_ID = ?",               // The columns for the WHERE clause
                arrayOf(alarmId),               // The values for the WHERE clause
                null,               // don't group the rows
                null,               // don't filter by row groups
                null                // The sort order
            )

            with(cursor) {
                while (moveToNext()) {
                    val alarmId = getString(getColumnIndexOrThrow(KEY_ALARM_ID))
                    val timestamp = getString(getColumnIndexOrThrow(KEY_TIMESTAMP))
                    val url = getString(getColumnIndexOrThrow(KEY_URL))

                    val timestampValue = try {
                        timestamp.toLong()
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                        -1L
                    }

                    if (timestampValue > 0) {
                        list.add(Item(alarmId, timestampValue, url))
                    }
                }
            }

            cursor.close()

            if (list.isEmpty()) {
                return null
            }

            return list[0]
        }

        fun deleteAlarm(alarmId: String) {
            val db = writableDatabase

            db.delete(NAME_TABLE_ALARMS, "$KEY_ALARM_ID = ?", arrayOf(alarmId))
        }

        fun addAlarm(item: Item) {
            val db = writableDatabase

            val values = ContentValues().apply {
                put(KEY_ALARM_ID, item.alarmId)
                put(KEY_TIMESTAMP, item.timestamp.toString())
                put(KEY_URL, item.url)
            }

            db.insert(NAME_TABLE_ALARMS, null, values)
        }
    }
}