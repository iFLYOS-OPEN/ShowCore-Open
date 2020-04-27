package com.iflytek.cyber.iot.show.core.launcher

import android.graphics.drawable.Drawable
import com.google.gson.annotations.SerializedName
import com.iflytek.cyber.iot.show.core.model.TemplateApp

open class AppData(
    val name: String,
    val packageName: String,
    val icon: Drawable?,
    val iconUrl: String? = null,
    val appType: Int = TYPE_PARTY
) {
    companion object {
        const val TYPE_PARTY = 1
        const val TYPE_INTERNAL = 2
        const val TYPE_TEMPLATE = 3
    }
}

class TemplateAppData(
    val source: String?,
    name: String?,
    icon: String?,
    val img: String?,
    val template: Int?,
    val business: String?,
    @SerializedName("is_dark") val isDark: Boolean?,
    val type: String?,
    val url: String?,
    val textIn: String?
) : AppData(name ?: "", source ?: "", null, icon, AppData.TYPE_TEMPLATE) {
    companion object {

        const val TYPE_TEMPLATE = "TEMPLATE"
        const val TYPE_SKILL = "SKILL"
        const val TYPE_H5_APP = "H5_APP"

        fun fromTemplateApp(templateApp: TemplateApp): TemplateAppData {
            return TemplateAppData(
                templateApp.source,
                templateApp.name,
                templateApp.icon,
                templateApp.img,
                templateApp.template,
                templateApp.business,
                templateApp.isDark,
                templateApp.type,
                templateApp.url,
                templateApp.textIn
            )
        }
    }

    fun toTemplateApp(): TemplateApp {
        return TemplateApp(
            source,
            name,
            iconUrl,
            img,
            template,
            business,
            isDark,
            type,
            url,
            textIn
        )
    }
}