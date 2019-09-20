package com.iflytek.cyber.iot.show.core.template

import android.content.Context
import android.content.Intent
import android.view.View
import com.google.gson.JsonParser
import com.iflytek.cyber.iot.show.core.EngineService
import com.iflytek.cyber.iot.show.core.template.OptionTemplateView.OptionElement

object TemplateViewBuilder {
    fun build(context: Context, payload: String, onClickBackListener: View.OnClickListener? = null): View? {
        val json = JsonParser().parse(payload).asJsonObject
        val templateId = json.get(Constant.PAYLOAD_TEMPLATE_ID).asString
        return when (json.get(Constant.PAYLOAD_TYPE).asString) {
            Constant.TYPE_BODY_TEMPLATE_1,
            Constant.TYPE_BODY_TEMPLATE_2 -> {
                BodyTemplateView1.Builder(context).payload(payload)
                    .onClickBackListener(onClickBackListener).build()
            }
            Constant.TYPE_BODY_TEMPLATE_3 -> {
                BodyTemplateView3.Builder(context).payload(payload)
                    .onClickBackListener(onClickBackListener).build()
            }
            Constant.TYPE_LIST_TEMPLATE_1 -> {
                ListTemplateView1.Builder(context).payload(payload)
                    .onClickBackListener(onClickBackListener).build()
            }
            Constant.TYPE_OPTION_TEMPLATE_1,
            Constant.TYPE_OPTION_TEMPLATE_2,
            Constant.TYPE_OPTION_TEMPLATE_3
            -> {
                OptionTemplateView.Builder(context).payload(payload)
                    .onElementSelectedListener(
                        object : OptionTemplateView.OnElementSelectedListener {
                            override fun onElementSelected(parent: OptionTemplateView,
                                                           itemView: View,
                                                           position: Int,
                                                           element: OptionElement) {
                                val intent = Intent(context, EngineService::class.java)
                                intent.action = EngineService.ACTION_SEND_TEMPLATE_ELEMENT_SELECTED
                                intent.putExtra(EngineService.EXTRA_ELEMENT_ID, element.elementId)
                                intent.putExtra(EngineService.EXTRA_TEMPLATE_ID, templateId)
                                context.startService(intent)
                            }
                        })
                    .onClickBackListener(onClickBackListener).build()
            }
            Constant.TYPE_WEATHER_TEMPLATE -> {
                WeatherTemplateView.Builder(context).payload(payload)
                    .onClickBackListener(onClickBackListener).build()
            }
            else -> {
                null
            }
        }
    }

}