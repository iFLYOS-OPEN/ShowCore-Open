package com.iflytek.cyber.iot.show.core.model

import com.google.gson.annotations.SerializedName

data class PlayerInfoPayload(@SerializedName("resource_id") var resourceId: String?,
                             @SerializedName("should_popup") var shouldPopup: Boolean,
                             @SerializedName("main_title") var mainTitle: String?,
                             @SerializedName("sub_title") var subTitle: String?,
                             var lyric: Lyric,
                             var content: Content?,
                             var provider: Provider?,
                             var recommend: Recommend?)

data class Lyric(var url: String?,
                 var format: String?)

data class Recommend(@SerializedName("url") var url: String?)