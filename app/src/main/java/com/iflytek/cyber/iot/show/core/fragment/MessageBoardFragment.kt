package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.iot.show.core.BuildConfig
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.utils.DeviceUtils
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MessageBoardFragment : BaseFragment() {
    companion object {
        private const val TAG = "MessageBoard"
    }

    private val client: OkHttpClient
    private val adapter = ListAdapter()

    private var recyclerView: RecyclerView? = null
    private var messagesData: MessagesData? = null

    private var backCount = 0

    init {
        val logger = HttpLoggingInterceptor()
        logger.level = HttpLoggingInterceptor.Level.BODY
        client = OkHttpClient.Builder()
            .addInterceptor(logger)
            .build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_message_board, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.back)?.setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            pop()
        }

        recyclerView = view.findViewById(R.id.message_list)
        recyclerView?.adapter = adapter

        post {
            getBoardDetail()

            requestMessageBoard()
        }
    }

    private fun requestMessageBoard() {
        val context = context ?: return
        Thread {
            val deviceId = DeviceUtils.getDeviceId(context)
            val clientId = BuildConfig.CLIENT_ID

            val authResponse = AuthDelegate.getAuthResponseFromPref(context)
            val request = Request.Builder()
                .url("https://staging-home.iflyos.cn/web/message_boards/42/messages?deviceId=$deviceId&clientId=$clientId")
                .addHeader("Authorization", "Bearer ${authResponse?.accessToken}")
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body()?.string()
                val messagesData = Gson().fromJson<MessagesData>(body, MessagesData::class.java)

                this.messagesData = messagesData
            } else {
            }

            post {
                adapter.notifyDataSetChanged()

                markAsRead()
            }
        }.start()
    }

    private fun getBoardDetail() {
        val context = context ?: return
        Thread {
            val deviceId = DeviceUtils.getDeviceId(context)
            val clientId = BuildConfig.CLIENT_ID

            val authResponse = AuthDelegate.getAuthResponseFromPref(context)
            val request = Request.Builder()
                .url("https://staging-home.iflyos.cn/web/message_boards/42?deviceId=$deviceId&clientId=$clientId")
                .addHeader("Authorization", "Bearer ${authResponse?.accessToken}")
                .put(RequestBody.create(MediaType.get("application/json"), "{}"))
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {

            }
        }.start()
    }

    private fun markAsRead() {
        val context = context ?: return
        Thread {
            val deviceId = DeviceUtils.getDeviceId(context)
            val clientId = BuildConfig.CLIENT_ID

            val authResponse = AuthDelegate.getAuthResponseFromPref(context)
            val request = Request.Builder()
                .url("https://staging-home.iflyos.cn/web/message_boards/42/messages/mark_read?deviceId=$deviceId&clientId=$clientId")
                .addHeader("Authorization", "Bearer ${authResponse?.accessToken}")
                .put(RequestBody.create(MediaType.get("application/json"), "{}"))
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {

            }
        }.start()
    }

    private inner class ListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val viewTypeSystem = 0
        private val viewTypeMessage = 1
        private val viewTypeRecord = 2
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            when (viewType) {
                viewTypeSystem -> {
                    return SystemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false))
                }
                viewTypeMessage -> {
                    return MessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false))
                }
                viewTypeRecord -> {
                    return RecordViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false))
                }
            }
            return SystemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false))
        }

        override fun getItemViewType(position: Int): Int {
            messagesData?.data?.get(position)?.let {
                when (it.msgType) {
                    0 -> {
                        return viewTypeSystem
                    }
                    1 -> {
                        return viewTypeMessage
                    }
                    2 -> {
                        return viewTypeRecord
                    }
                    else -> {
                        // ignore
                    }
                }
            }
            return super.getItemViewType(position)
        }

        override fun getItemCount(): Int {
            return messagesData?.data?.size ?: 0
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            messagesData?.data?.get(position)?.let { message ->
                when (holder) {
                    is MessageViewHolder -> {
                        val time = message.createTime ?: 0
                        val shouldShowTime =
                            if (position > 0) {
                                messagesData?.data?.get(Math.max(0, position - 1))?.let { preMessage ->
                                    val preTime = preMessage.createTime ?: 0
                                    time - preTime > TimeUnit.MINUTES.toMillis(5)
                                } ?: true
                            } else {
                                true
                            }
                        if (shouldShowTime) {
                            holder.tvTime?.visibility = View.VISIBLE

                            val nowDate = Calendar.getInstance()
                            val now = nowDate.timeInMillis
                            val timeDate = Calendar.getInstance()
                            timeDate.timeInMillis = time

                            if (nowDate.get(Calendar.DATE) - 1 == timeDate.get(Calendar.DATE)) {
                                holder.tvTime?.text = SimpleDateFormat("昨天 HH:mm", Locale.getDefault()).format(timeDate.time)
                            } else if (now - time < TimeUnit.DAYS.toMicros(7) &&
                                nowDate.get(Calendar.DATE) - 1 > timeDate.get(Calendar.DATE)) {
                                val weekday = when (timeDate.get(Calendar.DAY_OF_WEEK)) {
                                    Calendar.SUNDAY -> "星期天"
                                    Calendar.MONDAY -> "星期一"
                                    Calendar.TUESDAY -> "星期二"
                                    Calendar.WEDNESDAY -> "星期三"
                                    Calendar.THURSDAY -> "星期四"
                                    Calendar.FRIDAY -> "星期五"
                                    Calendar.SATURDAY -> "星期六"
                                    else -> ""
                                }
                                holder.tvTime?.text = SimpleDateFormat("$weekday HH:mm", Locale.getDefault()).format(timeDate.time)
                            } else {
                                holder.tvTime?.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(timeDate.time)
                            }
                        } else {
                            holder.tvTime?.visibility = View.GONE
                        }
                        holder.tvMessage?.text = message.msg
                        if (message.isOwner == true) {
                            holder.ivAvatar?.visibility = View.GONE
                            (holder.messageArea?.layoutParams as? FrameLayout.LayoutParams)?.let {
                                if (it.gravity != Gravity.CENTER_VERTICAL or Gravity.END) {
                                    it.gravity = Gravity.CENTER_VERTICAL or Gravity.END
                                    holder.messageArea.layoutParams = it
                                }
                            }
                        } else {
                            holder.ivAvatar?.let { avatar ->
                                avatar.visibility = View.VISIBLE
                                Glide.with(avatar)
                                    .asDrawable()
                                    .apply(RequestOptions
                                        .placeholderOf(R.drawable.cover_default)
                                        .transform(RoundedCornersTransformation(avatar.width / 4, 0)))
                                    .load(message.profilePicture)
                                    .into(avatar)
                            }
                            (holder.messageArea?.layoutParams as? FrameLayout.LayoutParams)?.let {
                                if (it.gravity != Gravity.CENTER_VERTICAL) {
                                    it.gravity = Gravity.CENTER_VERTICAL
                                    holder.messageArea.layoutParams = it
                                }
                            }
                        }
                    }
                    is SystemViewHolder -> {
                        val time = message.createTime ?: 0
                        val shouldShowTime =
                            if (position > 0) {
                                messagesData?.data?.get(Math.max(0, position - 1))?.let { preMessage ->
                                    val preTime = preMessage.createTime ?: 0
                                    time - preTime > TimeUnit.MINUTES.toMillis(5)
                                } ?: true
                            } else {
                                true
                            }
                        if (shouldShowTime) {
                            holder.tvTime?.visibility = View.VISIBLE

                            val nowDate = Calendar.getInstance()
                            val now = nowDate.timeInMillis
                            val timeDate = Calendar.getInstance()
                            timeDate.timeInMillis = time

                            if (nowDate.get(Calendar.DATE) - 1 == timeDate.get(Calendar.DATE)) {
                                holder.tvTime?.text = SimpleDateFormat("昨天 HH:mm", Locale.getDefault()).format(timeDate.time)
                            } else if (now - time < TimeUnit.DAYS.toMicros(7) &&
                                nowDate.get(Calendar.DATE) - 1 > timeDate.get(Calendar.DATE)) {
                                val weekday = when (timeDate.get(Calendar.DAY_OF_WEEK)) {
                                    Calendar.SUNDAY -> "星期天"
                                    Calendar.MONDAY -> "星期一"
                                    Calendar.TUESDAY -> "星期二"
                                    Calendar.WEDNESDAY -> "星期三"
                                    Calendar.THURSDAY -> "星期四"
                                    Calendar.FRIDAY -> "星期五"
                                    Calendar.SATURDAY -> "星期六"
                                    else -> ""
                                }
                                holder.tvTime?.text = SimpleDateFormat("$weekday HH:mm", Locale.getDefault()).format(timeDate.time)
                            } else {
                                holder.tvTime?.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(timeDate.time)
                            }
                        } else {
                            holder.tvTime?.visibility = View.GONE
                        }
                        holder.tvMessage?.text = message.msg
                    }
                    is RecordViewHolder -> {
                        val time = message.createTime ?: 0
                        val shouldShowTime =
                            if (position > 0) {
                                messagesData?.data?.get(Math.max(0, position - 1))?.let { preMessage ->
                                    val preTime = preMessage.createTime ?: 0
                                    time - preTime > TimeUnit.MINUTES.toMillis(5)
                                } ?: true
                            } else {
                                true
                            }
                        if (shouldShowTime) {
                            holder.tvTime?.visibility = View.VISIBLE

                            val nowDate = Calendar.getInstance()
                            val now = nowDate.timeInMillis
                            val timeDate = Calendar.getInstance()
                            timeDate.timeInMillis = time

                            if (nowDate.get(Calendar.DATE) - 1 == timeDate.get(Calendar.DATE)) {
                                holder.tvTime?.text = SimpleDateFormat("昨天 HH:mm", Locale.getDefault()).format(timeDate.time)
                            } else if (now - time < TimeUnit.DAYS.toMicros(7) &&
                                nowDate.get(Calendar.DATE) - 1 > timeDate.get(Calendar.DATE)) {
                                val weekday = when (timeDate.get(Calendar.DAY_OF_WEEK)) {
                                    Calendar.SUNDAY -> "星期天"
                                    Calendar.MONDAY -> "星期一"
                                    Calendar.TUESDAY -> "星期二"
                                    Calendar.WEDNESDAY -> "星期三"
                                    Calendar.THURSDAY -> "星期四"
                                    Calendar.FRIDAY -> "星期五"
                                    Calendar.SATURDAY -> "星期六"
                                    else -> ""
                                }
                                holder.tvTime?.text = SimpleDateFormat("$weekday HH:mm", Locale.getDefault()).format(timeDate.time)
                            } else {
                                holder.tvTime?.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(timeDate.time)
                            }
                        } else {
                            holder.tvTime?.visibility = View.GONE
                        }
                        if (message.isOwner == true) {
                            holder.ivAvatar?.visibility = View.GONE
                            (holder.messageArea?.layoutParams as? FrameLayout.LayoutParams)?.let {
                                if (it.gravity != Gravity.CENTER_VERTICAL or Gravity.END) {
                                    it.gravity = Gravity.CENTER_VERTICAL or Gravity.END
                                    holder.messageArea.layoutParams = it
                                }
                            }
                        } else {
                            holder.ivAvatar?.let { avatar ->
                                avatar.visibility = View.VISIBLE
                                Glide.with(avatar)
                                    .asDrawable()
                                    .apply(RequestOptions
                                        .placeholderOf(R.drawable.cover_default)
                                        .transform(RoundedCornersTransformation(avatar.width / 4, 0)))
                                    .load(message.profilePicture)
                                    .into(avatar)
                            }
                            (holder.messageArea?.layoutParams as? FrameLayout.LayoutParams)?.let {
                                if (it.gravity != Gravity.CENTER_VERTICAL) {
                                    it.gravity = Gravity.CENTER_VERTICAL
                                    holder.messageArea.layoutParams = it
                                }
                            }
                        }
                    }
                    else -> {

                    }
                }
            }
        }
    }

    private class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage: TextView? = itemView.findViewById(R.id.message)
        val messageArea: View? = itemView.findViewById(R.id.message_area)
        val ivAvatar: ImageView? = itemView.findViewById(R.id.avatar)
        val tvTime: TextView? = itemView.findViewById(R.id.time)
    }

    private class SystemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage: TextView? = itemView.findViewById(R.id.message)
        val tvTime: TextView? = itemView.findViewById(R.id.time)
    }

    private class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageArea: View? = itemView.findViewById(R.id.message_area)
        val ivAvatar: ImageView? = itemView.findViewById(R.id.avatar)
        val tvTime: TextView? = itemView.findViewById(R.id.time)
    }

    data class MessageBoard(
        @SerializedName("title") val title: String?,
        @SerializedName("ownerUserId") val ownerUserId: String?,
        @SerializedName("icon") val icon: String?,
        @SerializedName("createtime") val createtime: String?
    )

    data class MessagesData(
        val code: Int?,
        val data: List<MessageItem>?,
        val desc: String?,
        val flag: Boolean?
    )

    data class MessageItem(
        @SerializedName("client_id") val clientId: String?,
        @SerializedName("createtime") val createTime: Long?,
        @SerializedName("device_id") val deviceId: String?,
        @SerializedName("id") val id: String?,
        @SerializedName("isOwner") val isOwner: Boolean?,
        @SerializedName("length") val length: Int?,
        @SerializedName("memberType") val memberType: Int?,
        @SerializedName("msg") val msg: String?,
        @SerializedName("msgType") val msgType: Int?,
        @SerializedName("profilePicture") val profilePicture: String?,
        @SerializedName("user_id") val userId: String?,
        @SerializedName("voiceUrl") val voiceUrl: String?
    )
}