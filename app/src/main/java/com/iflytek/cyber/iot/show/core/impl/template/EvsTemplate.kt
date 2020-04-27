package com.iflytek.cyber.iot.show.core.impl.template

import com.iflytek.cyber.evs.sdk.agent.Template

class EvsTemplate private constructor() : Template() {

    companion object {
        private var instance: EvsTemplate? = null

        fun get(): EvsTemplate {
            instance?.let {
                return it
            } ?: run {
                val template = EvsTemplate()
                instance = template
                return template
            }
        }
    }

    var renderCallback: RenderCallback? = null

    var isPlayerInfoFocused = false
    var isOtherTemplateFocused = false

    var templateType: String? = null
    private val isTemplateFocused: Boolean
        get() = isPlayerInfoFocused || isOtherTemplateFocused

    override fun isFocused(): Boolean {
        return isTemplateFocused
    }

    override fun getFocusTemplateType(): String? {
        return if (isPlayerInfoFocused && !isOtherTemplateFocused) TYPE_PLAYER_INFO else templateType
    }

    override fun isSupportedCustomTemplate(): Boolean {
        return true
    }

    override fun exitStaticTemplate(type: String?) {
        super.exitStaticTemplate(type)
        renderCallback?.exitStaticTemplate(type)
    }

    override fun exitPlayerInfo() {
        super.exitPlayerInfo()
        renderCallback?.exitPlayerInfo()
    }

    override fun exitCustomTemplate() {
        super.exitCustomTemplate()
        renderCallback?.exitCustomTemplate()
    }

    override fun renderStaticTemplate(payload: String) {
        super.renderStaticTemplate(payload)
        renderCallback?.renderTemplate(payload)
    }

    override fun renderCustomTemplate(type: String, templateId: String, showingDuration: String?, htmlSourceCode: String) {
        super.renderCustomTemplate(type, templateId, showingDuration, htmlSourceCode)
        renderCallback?.renderCustomTemplate(type, templateId, showingDuration, htmlSourceCode)
    }

    override fun notifyPlayerInfoUpdated(resourceId: String, payload: String) {
        super.notifyPlayerInfoUpdated(resourceId, payload)
        renderCallback?.notifyPlayerInfoUpdated(resourceId, payload)
    }

    override fun renderPlayerInfo(payload: String) {
        super.renderPlayerInfo(payload)
        renderCallback?.renderPlayerInfo(payload)
    }

    override fun renderVideoPlayerInfo(payload: String) {
        super.renderVideoPlayerInfo(payload)
        renderCallback?.renderVideoPlayerInfo(payload)
    }

    override fun isTemplatePermanent(): Boolean {
        return true
    }

    interface RenderCallback {
        fun renderTemplate(payload: String)
        fun notifyPlayerInfoUpdated(resourceId: String, payload: String)
        fun renderPlayerInfo(payload: String)
        fun exitStaticTemplate(type: String?)
        fun exitPlayerInfo()
        fun exitCustomTemplate()
        fun renderVideoPlayerInfo(payload: String)
        fun renderCustomTemplate(type: String, templateId: String, showingDuration: String?, htmlSourceCode: String)
    }

    abstract class SimpleRenderCallback : RenderCallback {
        override fun renderPlayerInfo(payload: String) {
        }

        override fun notifyPlayerInfoUpdated(resourceId: String, payload: String) {
        }

        override fun renderTemplate(payload: String) {
        }

        override fun exitCustomTemplate() {
        }

        override fun exitPlayerInfo() {
        }

        override fun exitStaticTemplate(type: String?) {
        }

        override fun renderVideoPlayerInfo(payload: String) {
        }

        override fun renderCustomTemplate(
            type: String,
            templateId: String,
            showingDuration: String?,
            htmlSourceCode: String
        ) {

        }
    }
}