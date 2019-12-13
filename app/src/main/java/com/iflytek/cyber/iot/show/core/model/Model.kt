package com.iflytek.cyber.iot.show.core.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

data class Song(
    var metadata: Metadata,
    var provider: Provider,
    var stream: Stream
)

data class Metadata(
    var title: String?, var subtitle: String?,
    var source: String?, var art: Art?
)

data class Art(
    var contentDescription: String,
    var sources: ArrayList<Source>?
)

data class Source(var url: String)

data class Stream(
    var offsetInMilliseconds: Long,
    var token: String,
    var type: String,
    var url: String
)

data class MusicBody(
    @SerializedName("media_id") var mediaId: Int? = null,
    @SerializedName("audio_id") val audioId: String?,
    val stream: Stream?
)

@Parcelize
data class SkillSection(
    val id: String,
    val skills: ArrayList<Skill>,
    val title: String
) : Parcelable

@Parcelize
data class Skill(
    val description: String,
    val examples: ArrayList<String>,
    val icon: String?,
    val name: String,
    val id: String
) : Parcelable

@Parcelize
data class SkillDetail(
    @SerializedName("account_link_url") val accountLink: String?,
    @SerializedName("account_linked") val accountLinked: Boolean?,
    @SerializedName("category_name") val categoryName: String,
    val description: String,
    val developer: String,
    val examples: ArrayList<String>,
    val icon: String,
    val id: String,
    val name: String,
    @SerializedName("updated_time") val updatedTime: String,
    val version: String
) : Parcelable

data class Video(
    @SerializedName("resource_id") val resourceId: String,
    val offset: Long,
    val behavior: String,
    val url: String,
    val control: String
)

@Parcelize
data class Alert(
    val id: String,
    val time: String,
    var content: String?,
    val description: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("repeat_type") val repeatType: String,
    @SerializedName("repeat_weekdays") val repeatWeekdays: ArrayList<Int>?,
    val timestamp: Long,
    @SerializedName("repeat_date_number") val repeatDateNumber: Int,
    @SerializedName("repeat_date") val repeatDate: String
) : Parcelable {

    companion object {
        const val TYPE_DAILY = "DAILY" //每日
        const val TYPE_WORKDAY = "WORKDAY" //工作日
        const val TYPE_RESTDAY = "RESTDAY" //休息日
        const val TYPE_WEEKLY = "WEEKLY" //每周
        const val TYPE_WEEKDAY = "WEEKDAY" //周一至周五
        const val TYPE_WEEKEND = "WEEKEND" //周末
        const val TYPE_MONTHLY = "MONTHLY" //每月
        const val TYPE_YEARLY = "YEARLY" //每年
        const val TYPE_NONE = "NONE" //不重复
        const val TYPE_CUSTOM = "CUSTOM" //自定义
    }
}

data class RepeatType(val type: String, val name: String) {

    companion object {
        fun getName(type: String): String {
            when (type) {
                Alert.TYPE_DAILY -> return "每天"
                Alert.TYPE_WORKDAY -> return "法定工作日"
                Alert.TYPE_RESTDAY -> return "法定节假日"
                Alert.TYPE_WEEKDAY -> return "周一至周五"
                Alert.TYPE_WEEKEND -> return "周末"
                Alert.TYPE_MONTHLY -> return "每月"
                Alert.TYPE_YEARLY -> return "每年"
                Alert.TYPE_NONE -> return "不重复"
                Alert.TYPE_CUSTOM -> return "自定义"
            }
            return ""
        }
    }
}

class AlertBody {
    var id: String? = null
    @SerializedName("alert_type")
    var alertType: String? = null
    var time: String? = null
    var timestamp: Long? = null
    var content: String? = null
    @SerializedName("repeat_type")
    var repeatType: String? = null
    @SerializedName("repeat_weekdays")
    var repeatWeekdays: List<Int>? = null
    @SerializedName("repeat_date_number")
    var repeatDateNumber: Int? = null
    @SerializedName("repeat_date")
    var repeatDate: String? = null
}

data class Message(val code: String, val message: String)

data class UserInfo(
    val avatar: String?,
    val birthday: String?,
    val gender: String?,
    val nickname: String?,
    val phone: String?
)

@Parcelize
data class Group(
    val name: String,
    val id: String,
    @SerializedName("has_more") val hasMore: Boolean?,
    @SerializedName("display_mode") val displayMode: String?,
    val abbr: String?,
    val items: ArrayList<GroupItem>?, val descriptions: ArrayList<String>
) : Parcelable

@Parcelize
data class GroupItem(
    val name: String,
    val id: String,
    val description: String?,
    val from: String?,
    val image: String,
    val url: String?,
    @SerializedName("category_name") val categoryName: String?
) : Parcelable

@Parcelize
data class SongList(
    val id: String?,
    val from: String?,
    @SerializedName("from_type") val fromType: String?,
    val image: String,
    val name: String?,
    val items: ArrayList<SongItem>
): Parcelable

@Parcelize
data class SongItem(
    val artist: String?,
    val available: Boolean,
    @SerializedName("can_like") val canLike: Boolean,
    val id: String,
    val image: String?,
    val liked: Boolean,
    val name: String,
    @SerializedName("source_type") val sourceType: String
): Parcelable

data class Error(
    val message: String,
    @SerializedName("redirect_url") val redirectUrl: String?
)

data class PlayList(
    @SerializedName("list_loop") val listLoop: Boolean?,
    val playlist: ArrayList<Song>?
)

@Parcelize
data class Property(
    @SerializedName("property_id") val propertyId: String?,
    val name: String
) : Parcelable

@Parcelize
data class ChatConfig(
    val volume: Int?,
    val vcn: String?,
    val speed: Double?,
    val language: String?,
    @SerializedName("interaction_mode_id") val interactionModeId: Int?,
    val ent: String?,
    @SerializedName("property_id") val propertyId: String?
) : Parcelable

@Parcelize
data class InteractionModeSpeaker(
    @SerializedName("voice_name") val voiceName: String?,
    @SerializedName("voice_id") val voiceId: String?,
    val vcn: String?,
    @SerializedName("source_type") val sourceType: String?,
    @SerializedName("property_ids") val propertyIds: List<String>?,
    val image: String?,
    @SerializedName("listen_url") val listenUrl: String?
) : Parcelable

@Parcelize
data class InteractionMode(
    val name: String?,
    val speakers: List<InteractionModeSpeaker>?,
    @SerializedName("interaction_mode_id") val interactionModeId: Int?
) : Parcelable

@Parcelize
data class ChatConfigData(
    val property: List<Property>?,
    val config: ChatConfig?,
    @SerializedName("interaction_modes") val interactionModes: List<InteractionMode>?
) : Parcelable
