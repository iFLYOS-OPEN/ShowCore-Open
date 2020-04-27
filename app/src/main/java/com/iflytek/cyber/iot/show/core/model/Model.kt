package com.iflytek.cyber.iot.show.core.model

import android.content.SyncResult
import android.os.Parcelable
import com.alibaba.fastjson.JSONObject
import com.google.gson.Gson
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
    @SerializedName("category_name") val categoryName: String?,
    val description: String?,
    val developer: String?,
    val examples: ArrayList<String?>?,
    val icon: String?,
    val id: String?,
    val name: String?,
    @SerializedName("updated_time") val updatedTime: String?,
    val version: String?
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

data class Message(val code: String, val message: String, val error: String?)

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
) : Parcelable

@Parcelize
data class SongItem(
    val artist: String?,
    val available: Boolean,
    @SerializedName("can_like") val canLike: Boolean,
    val id: String,
    val image: String?,
    val liked: Boolean,
    val name: String,
    @SerializedName("album_id") val albumId: String,
    val business: String,
    @SerializedName("source_type") val sourceType: String
) : Parcelable

data class Error(
    val message: String?,
    val title: String?,
    @SerializedName("qrcode_message") val qrCodeMessage: String?,
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

data class WakeWord(
    val id: String?,
    val name: String?,
    val score: Int?,
    val default: Boolean?,
    val enable: Boolean?,
    val status: String?
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }
}

data class WakeWordsBody(
    @SerializedName("wakewords") val wakeWords: List<WakeWord>?
)

data class Weather(
    val temperature: String?,
    val lifestyle: String?,
    val icon: String?,
    val description: String?
)

data class RecommendTag(
    val id: Int,
    val name: String?,
    @SerializedName("search_word") val tags: ArrayList<String>?,
    val rank: Int
)

data class SearchBody(
    val name: String,
    val limit: Int = 20,
    val page: Int = 1
)

data class SearchResult(
    val type: Int,
    val total: String?,
    val page: String?,
    val limit: String?,
    val results: ArrayList<Result>?
)

@Parcelize
data class Result(
    val id: String,
    val title: String,
    val cover: String?,
    val business: String?,
    val source: String,
    val author: String?
) : Parcelable

data class PlayBody(
    @SerializedName("audio_id") val audioId: String,
    val business: String?,
    @SerializedName("source_type") val sourceType: String
)

data class MainTemplate(
    val type: Int,
    val data: List<Banner>
)

data class CollectionTag(val tags: ArrayList<Tags>)

data class Tags(val id: Int, val name: String, val value: String?)

@Parcelize
data class CollectionSong(
    val name: String,
    val id: String,
    @SerializedName("error_reason") val errorReason: String?,
    val available: Boolean,
    val artist: String?,
    @SerializedName("music_img") val musicImg: String?,
    @SerializedName("source_type") val sourceType: String?,
    val business: String?,
    @SerializedName("album_id") val albumId: String?
) : Parcelable

data class CollectionV2(
    val total: Int,
    val name: String,
    var collections: ArrayList<CollectionSong>
)

data class CollectionV3(
    val single: CollectionV2,
    val album: CollectionV2,
    @SerializedName("kugou_sync_status") val syncResult: SyncResult?
)

data class PlayResult(val music: MusicResult?)

data class MusicResult(val name: String?)

data class PlaySingleBody(
    @SerializedName("tag_id") val tagId: Int,
    @SerializedName("media_id") val mediaId: String
)

data class PlayAlbumBody(
    @SerializedName("album_id") val albumId: String,
    @SerializedName("source_type") val sourceType: String,
    @SerializedName("media_id") val mediaId: String?,
    val business: String
)

data class TemplateApp(
    val source: String?,
    val name: String?,
    val icon: String?,
    val img: String?,
    val template: Int?,
    val business: String?,
    val isDark: Boolean?,
    val type: String? = null,
    val url: String? = null,
    val textIn: String? = null
) {
    companion object {
        const val TEMPLATE_XMLR = 0         // 喜马拉雅
        const val TEMPLATE_TEMPLATE_1 = 1   // 模板一
        const val TEMPLATE_TEMPLATE_2 = 2   // 模板二
        const val TEMPLATE_TEMPLATE_3 = 3   // 模板三
    }
}

data class TemplateApp2(
    val source: String?,
    val name: String?,
    val img: String?,
    val business: String?
)

data class TemplateMediaItem(
    val type: Int,
    val total: Int,
    val template: Int,
    val page: Int,
    val limit: Int,
    val source: String?,
    val items: ArrayList<SourceItem>?
)

@Parcelize
data class SourceItem(
    val cover: String?,
    val source: String?,
    val subtitle: String?,
    val title: String?,
    val type: Int,
    val metadata: SourceMetaData?
) : Parcelable

@Parcelize
data class SourceMetaData(
    val business: String?,
    val resourceId: String?,
    val album: String?,
    val source: String?,
    val topLevel: String?,
    val secondLevel: String?,
    val thirdLevel: String?,
    val type: Int?
) : Parcelable

@Parcelize
data class AlbumItem(
    @SerializedName("album") val album: String?,
    @SerializedName("album_id") val albumId: String?,
    @SerializedName("business") val business: String?,
    @SerializedName("cover") val cover: String?,
    @SerializedName("source") val source: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("type") val type: String?,
    var from: String?,
    val result: List<MediaItem>?
) : Parcelable

@Parcelize
data class MediaItem(
    @SerializedName("album_id") val albumId: String?,
    @SerializedName("audio_type") val audioType: String?,
    @SerializedName("business") val business: String?,
    @SerializedName("cover") val cover: String?,
    @SerializedName("id") val id: String?,
    @SerializedName("media_type") val mediaType: String?,
    @SerializedName("source") val source: String?,
    @SerializedName("subtitle") val subtitle: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("variable") val variable: Boolean?
) : Parcelable

data class XmlyAlbumItem(
    @SerializedName("id") val id: String?,
    @SerializedName("album_id") val albumId: String?,
    @SerializedName("name") val name: String?,
    val title: String?,
    @SerializedName("subtitle") val subtitle: String?,
    @SerializedName("cover") val cover: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("from") val from: String?,
    val result: List<MediaItem>?
)

data class XmlyCategoryItem(
    @SerializedName("category_name") val categoryName: String?,
    @SerializedName("category_id") val categoryId: String?,
    val result: List<XmlyAlbumItem>?
)

data class AppShowResult(
    val total: Int?,
    val limit: Int?,
    val page: Int?,
    val album: String?,
    val source: String?,
    val description: String?,
    val items: List<MediaItem>,
    val result: List<MediaItem>
)

data class XmlyQueryResponse(
    @SerializedName("albumId") val albumId: String?,
    val name: String?,
    val result: List<MediaItem>?
)

data class DeskRecommend(
    val id: Int?,
    var type: Int?,
    var title: String?,
    var titleColor: String?,
    var background: String?,
    var more: DeskRecommendMore?,
    val items: ArrayList<DeskRecommendItem>?
)

data class DeskRecommendMore(
    val type: Int?,
    val text: String?,
    val textColor: String?,
    val metadata: JSONObject?
)

data class DeskRecommendItem(
    var type: Int?,
    var title: String?,
    var titleColor: String?,
    var subtitle: String?,
    var subtitleColor: String?,
    var background: String?,
    var cover: String?,
    var metadata: JSONObject?,
    var shouldHide: Boolean = false
)

@Parcelize
data class ReadWord(
    @SerializedName("fluency_score") val fluencyScore: Float?,
    @SerializedName("phone_score") val phoneScore: Float?,
    @SerializedName("total_score") val totalScore: Float?,
    @SerializedName("tone_score") val toneScore: Float?,
    @SerializedName("accuracy_score") val accurecyScore: Float?,
    @SerializedName("integrity_score") val integrityScore: Float?,
    @SerializedName("standard_score") val standardScore: Float?,
    @SerializedName("except_info") val exceptInfo: String?,
    val content: String?,
    val sentences: ArrayList<Sentence>
) : Parcelable

@Parcelize
data class Sentence(
    val content: String,
    val words: ArrayList<Word>?,
    val word: Word,
    @SerializedName("fluency_score") val fluencyScore: Float?,
    @SerializedName("phone_score") val phoneScore: Float?,
    @SerializedName("total_score") val totalScore: Float?,
    @SerializedName("tone_score") val toneScore: Float?
) : Parcelable

@Parcelize
data class Word(
    val symbol: String?,
    val syllables: ArrayList<Syllable>?,
    val content: String?,
    @SerializedName("total_score") val totalScore: Float?,
    @SerializedName("dp_message") val dpMessage: String?
) : Parcelable

@Parcelize
data class Syllable(
    val symbol: String?,
    val content: String?,
    @SerializedName("dp_message") val dpMessage: String?
) : Parcelable

data class Brand(
    val name: String?,
    val id: Int,
    @SerializedName("icon_url") val iconUrl: String?,
    val describe: String?,
    @SerializedName("auth_state") val authState: AuthState?
)

data class AuthState(
    val invalid: Boolean
)

data class DeviceList(
    val devices: ArrayList<Device>?
)

data class Device(
    val brand: String?,
    @SerializedName("client_name") val clientName: String?,
    @SerializedName("device_name") val deviceName: String?,
    @SerializedName("device_type") val deviceType: DeviceType,
    @SerializedName("device_type_id") val deviceTypeId: Int?,
    @SerializedName("friendly_name") val friendlyName: String?,
    val icon: String?,
    val id: Int,
    @SerializedName("infrared_id") val infraredId: String?,
    @SerializedName("iot_info") val iotInfo: IotInfo?,
    @SerializedName("iot_info_id") val iotInfoId: Int?,
    val reacheable: Boolean?,
    val zone: String?,
    val actions: ArrayList<Action>?,
    @SerializedName("device_id") val deviceId: String?
)

data class DeviceType(
    val describe: String?,
    val id: Int,
    val name: String?
)

data class IotInfo(
    @SerializedName("icon_url") val iconUrl: String?,
    val id: Int?,
    val name: String?
)

data class Action(
    val name: String?,
    val values: ArrayList<String>
)

data class AuthUrl(
    @SerializedName("auth_url") val authUrl: String
)

data class DeviceAuthState(
    @SerializedName("revoked_yet") val revokedYet: Boolean
)

// 音视频媒体推荐实体类
@Parcelize
data class MediaEntity(
    @SerializedName("image") val image: String?,
    @SerializedName("url") var url: String?,
    @SerializedName("name") var name: String?
) : Parcelable
