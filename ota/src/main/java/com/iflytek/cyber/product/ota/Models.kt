package com.iflytek.cyber.product.ota

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.util.*

data class ReportEntity(val pids: List<Long>) {
    companion object {
        fun from(manifests: List<ManifestEntity>): ReportEntity {
            val pids = ArrayList<Long>()
            manifests.map {
                pids.add(it.id)
            }
            return ReportEntity(pids)
        }

        fun from(manifest: ManifestEntity): ReportEntity {
            return ReportEntity(listOf(manifest.id))
        }
    }
}

data class PackageEntity(val id: Long, val revision: Long, val identity: String, val url: String)

@Parcelize
data class PackageEntityNew(
    val id: Long?,
    val identity: String?,
    @SerializedName("version_id") val versionId: String?,
    @SerializedName("version_name") val versionName: String?,
    val description: String?,
    val url: String?
) : Parcelable

data class ManifestEntity(val id: Long, val revision: Long, val identity: String) {
    companion object {
        fun from(pkg: PackageEntity): ManifestEntity {
            return ManifestEntity(pkg.id, pkg.revision, pkg.identity)
        }
    }
}

data class VersionCodeAndId(val versionCode: Int, val pid: Long)

data class VersionCodeMap(val set: Set<VersionCodeAndId>)