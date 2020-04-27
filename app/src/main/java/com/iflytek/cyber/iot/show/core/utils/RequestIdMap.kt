package com.iflytek.cyber.iot.show.core.utils

object RequestIdMap {
    private const val TAG = "RequestIdMap"
    private val requestTemplateIdMap = object : LinkedHashMap<String, String>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > 100
        }
    } // requestId -> templateId
    private val requestTtsIdMap = object : LinkedHashMap<String, String>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > 100
        }
    } // requestId -> resourceId

    fun putRequestTemplate(requestId: String, templateId: String) {
        synchronized(requestTemplateIdMap) {
            requestTemplateIdMap.iterator().run {
                while (hasNext()) {
                    val next = next()
                    if (next.value == templateId)
                        remove()
                }
            }
            requestTemplateIdMap.put(requestId, templateId)
        }
    }

    fun putRequestTts(requestId: String, resourceId: String) {
        synchronized(requestTtsIdMap) {
            requestTtsIdMap.iterator().run {
                while (hasNext()) {
                    val next = next()
                    if (next.value == resourceId)
                        remove()
                }
            }
            requestTtsIdMap.put(requestId, resourceId)
        }
    }

    fun findRequestByTts(resourceId: String): String? {
        requestTtsIdMap.map {
            if (it.value == resourceId) {
                return it.key
            }
        }
        return null
    }

    fun findRequestByTemplate(templateId: String): String? {
        requestTemplateIdMap.map {
            if (it.value == templateId) {
                return it.key
            }
        }
        return null
    }
}