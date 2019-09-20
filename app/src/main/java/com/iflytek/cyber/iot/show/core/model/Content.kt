/*
 * Copyright (C) 2019 iFLYTEK CO.,LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iflytek.cyber.iot.show.core.model

import com.google.gson.annotations.SerializedName

class Content {

    var title: String? = null
    var titleSubtext1: String? = null
    var titleSubtext2: String? = null
    var header: String? = null
    var headerSubtext1: String? = null
    var mediaLengthInMilliseconds: String? = null
    var art: Image? = null
    var provider: Provider? = null

    @SerializedName("secondary_text")
    var musicArtist: String? = null
    @SerializedName("primary_text")
    var musicTitle: String? = null
    @SerializedName("image_url")
    var imageUrl: String? = null

    override fun toString(): String {
        return "Content(title=$title, titleSubtext1=$titleSubtext1, titleSubtext2=$titleSubtext2, header=$header, headerSubtext1=$headerSubtext1, mediaLengthInMilliseconds=$mediaLengthInMilliseconds, art=$art, provider=$provider, musicArtist=$musicArtist, musicTitle=$musicTitle, imageUrl=$imageUrl)"
    }
}

class Provider {
    var name: String? = null
    var logo: String? = null
    @SerializedName("logo_url") var logoUrl: String? = null

    override fun toString(): String {
        return "Provider{" +
                "name='" + name + '\''.toString() +
                ", logo=" + logo +
                '}'.toString()
    }
}
