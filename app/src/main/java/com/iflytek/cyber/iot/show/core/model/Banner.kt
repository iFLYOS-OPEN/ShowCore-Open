package com.iflytek.cyber.iot.show.core.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Banner(
    val id: String?,
    val cover: String?,
    val title: String?,
    val descriptions: Array<String>?,
    val target: String?,
    val content: String?
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Banner

        if (id != other.id) return false
        if (cover != other.cover) return false
        if (title != other.title) return false
        if (descriptions != null) {
            if (other.descriptions == null) return false
            if (!descriptions.contentEquals(other.descriptions)) return false
        } else if (other.descriptions != null) return false
        if (target != other.target) return false
        if (content != other.content) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (cover?.hashCode() ?: 0)
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (descriptions?.contentHashCode() ?: 0)
        result = 31 * result + (target?.hashCode() ?: 0)
        result = 31 * result + (content?.hashCode() ?: 0)
        return result
    }
}