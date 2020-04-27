package com.iflytek.cyber.evs.sdk.agent

import androidx.annotation.CallSuper
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.RequestManager
import com.iflytek.cyber.evs.sdk.focus.VisualFocusManager
import com.iflytek.cyber.evs.sdk.model.Constant

/**
 * 模块展示模块。详细介绍见https://doc.iflyos.cn/device/evs/reference/template.html#%E6%A8%A1%E6%9D%BF%E5%B1%95%E7%A4%BA
 */
abstract class Template {

    val version = "1.2"

    var clearTimeout: Int = 0
        get() {
            return if (isTemplatePermanent()) {
                -1
            } else {
                field
            }
        }

    companion object {
        const val NAME_STATIC_TEMPLATE = "${Constant.NAMESPACE_TEMPLATE}.static_template"
        const val NAME_PLAYING = "${Constant.NAMESPACE_TEMPLATE}.playing_template"
        const val NAME_CUSTOM_TEMPLATE = "${Constant.NAMESPACE_TEMPLATE}.custom_template"
        const val NAME_ELEMENT_SELECTED = "${Constant.NAMESPACE_TEMPLATE}.element_selected"
        const val NAME_EXIT = "${Constant.NAMESPACE_TEMPLATE}.exit"

        const val TYPE_PLAYER_INFO = "player_info_template"
        const val TYPE_BODY_1 = "body_template_1"
        const val TYPE_BODY_2 = "body_template_2"
        const val TYPE_BODY_3 = "body_template_3"
        const val TYPE_LIST_1 = "list_template_1"
        const val TYPE_OPTION_1 = "option_template_1"
        const val TYPE_OPTION_2 = "option_template_2"
        const val TYPE_OPTION_3 = "option_template_3"
        const val TYPE_WEATHER = "weather_template"
        const val TYPE_CUSTOM = "custom_template"

        const val KEY_TYPE = "type"
        const val KEY_TEMPLATE_ID = "template_id"
        const val KEY_ELEMENT_ID = "element_id"
        const val KEY_SUPPORTED_CUSTOM_TEMPLATE = "supported_custom_template"
        const val KEY_FOCUSED = "focused"
        const val KEY_TEMPLATE_TYPE = "template_type"
        const val KEY_SHOULD_POPUP = "should_popup"
        const val KEY_HTML_SOURCE_CODE = "html_source_code"
        /**
         * 可取值：LONG,SHORT,FOREVER。具体显示时长由设备端决定，一般来说，我们建议设备LONG=显示30s，SHORT=显示10s，FOREVER代表一直显示，直到用户手动关闭
         */
        const val KEY_SHOWING_DURATION = "showing_duration"
    }

    open fun isFocused(): Boolean {
        return false
    }

    open fun getFocusTemplateType(): String? {
        return null
    }

    /**
     * 是否支持展示 custom_template（用于展示数据为 HTML 的 Template 页面）
     */
    open fun isSupportedCustomTemplate(): Boolean {
        return false
    }

    /**
     * 请求渲染自定义模板
     */
    @Deprecated(
        replaceWith = ReplaceWith("renderCustomTemplate(type, templateId, htmlSourceCode)"),
        message = "Deprecated. Use renderCustomTemplate(type, templateId, htmlSourceCode) please."
    )
    open fun renderCustomTemplate(payload: String) {
        VisualFocusManager.requestActive(
            VisualFocusManager.CHANNEL_OVERLAY_TEMPLATE, VisualFocusManager.TYPE_CUSTOM_TEMPLATE
        )
    }

    open fun renderCustomTemplate(
        type: String,
        templateId: String,
        showingDuration: String?,
        htmlSourceCode: String
    ) {
        VisualFocusManager.requestActive(
            VisualFocusManager.CHANNEL_OVERLAY_TEMPLATE, VisualFocusManager.TYPE_CUSTOM_TEMPLATE
        )
    }

    /**
     * 请求渲染模板
     */
    @CallSuper
    open fun renderStaticTemplate(payload: String) {
        VisualFocusManager.requestActive(
            VisualFocusManager.CHANNEL_OVERLAY_TEMPLATE, VisualFocusManager.TYPE_STATIC_TEMPLATE
        )
    }

    fun requestClearFocus(type: String = VisualFocusManager.TYPE_STATIC_TEMPLATE) {
        VisualFocusManager.requestAbandon(
            VisualFocusManager.CHANNEL_OVERLAY_TEMPLATE, type
        )
    }

    fun currentActiveType(): String? {
        if (VisualFocusManager.getForegroundChannel() == VisualFocusManager.CHANNEL_OVERLAY_TEMPLATE) {
            return VisualFocusManager.getForegroundChannel()
        }
        return null
    }

    /**
     * 通知播放信息已更新，但未必是已经需要渲染。一般情况下只有当 [renderPlayerInfo] 调用时需要开发者在 UI 上渲染播放信息
     */
    @CallSuper
    open fun notifyPlayerInfoUpdated(resourceId: String, payload: String) {

    }

    /**
     * 请求渲染播放信息模板
     */
    @CallSuper
    open fun renderPlayerInfo(payload: String) {
        val payloadJson = JSON.parseObject(payload)
        if (payloadJson.getBoolean(KEY_SHOULD_POPUP) == true)
            VisualFocusManager.requestActive(
                VisualFocusManager.CHANNEL_OVERLAY_TEMPLATE,
                VisualFocusManager.TYPE_PLAYING_TEMPLATE
            )
    }

    fun sendElementSelected(templateId: String, elementId: String) {
        val payload = JSONObject()
        payload[KEY_TEMPLATE_ID] = templateId
        payload[KEY_ELEMENT_ID] = elementId
        RequestManager.sendRequest(NAME_ELEMENT_SELECTED, payload, null, true)
    }

    /**
     * 请求关闭静态渲染模板
     */
    @CallSuper
    open fun exitStaticTemplate(type: String?) {
        requestClearFocus()
    }

    /**
     * 请求关闭播放信息
     */
    @CallSuper
    open fun exitPlayerInfo() {

    }

    /**
     * 请求关闭自定义静态渲染模板
     */
    @CallSuper
    open fun exitCustomTemplate() {
        requestClearFocus(VisualFocusManager.TYPE_CUSTOM_TEMPLATE)
    }

    /**
     * 请求渲染视频信息模板
     */
    @CallSuper
    open fun renderVideoPlayerInfo(payload: String) {

    }

    /**
     * 模板是否常驻
     *
     * @return true 代表常驻，SDK 不会调用 [clearStaticTemplate]
     */
    open fun isTemplatePermanent() = false

    /**
     * 播放信息是否常驻
     *
     * @return true 代表常驻，SDK 不会调用 [clearPlayerInfo]
     */
    open fun isPlayerInfoPermanent() = false

}