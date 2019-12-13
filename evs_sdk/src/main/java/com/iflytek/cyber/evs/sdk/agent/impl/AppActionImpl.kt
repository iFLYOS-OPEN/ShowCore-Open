package com.iflytek.cyber.evs.sdk.agent.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.agent.AppAction
import com.iflytek.cyber.evs.sdk.utils.AppUtil

class AppActionImpl(private val context: Context) : AppAction() {

    override fun getSupportedExecute(): List<String> {
        return listOf(DATA_TYPE_ACTIVITY, DATA_TYPE_BROADCAST, DATA_TYPE_SERVICE)
    }

    override fun check(payload: JSONObject): JSONObject {
        val checkId = payload.getString(KEY_CHECK_ID)
        val actionArray: JSONArray = payload.getJSONArray(KEY_ACTIONS) as JSONArray

        val resultPayload = JSONObject()
        resultPayload[KEY_CHECK_ID] = checkId

        val resultArray = JSONArray()

        for (i in 0 until actionArray.size) {
            val action = actionArray[i] as JSONObject
            val actionId = action.getString(KEY_ACTION_ID)
            val data = action.getJSONObject(KEY_DATA)
            val pkgName = data.getString(KEY_PACKAGE_NAME)
            val uri = data.getString(KEY_URI)
            val supported = AppUtil.isPackageExist(context, pkgName, uri)

            val checkResult = JSONObject()
            checkResult[KEY_ACTION_ID] = actionId
            checkResult[KEY_RESULT] = supported

            resultArray.add(checkResult)
        }

        resultPayload[KEY_ACTIONS] = resultArray

        return resultPayload
    }

    override fun execute(payload: JSONObject, result: JSONObject): Boolean {
        val executeId = payload.getString(KEY_EXECUTION_ID)
        val actionArray = payload.getJSONArray(KEY_ACTIONS)

        var isSuccess = false
        var errorLevel = 0

        for (i in 0 until actionArray.size) {
            try {
                val action = actionArray[i] as JSONObject
                val actionId = action.getString(KEY_ACTION_ID)
                val data = action.getJSONObject(KEY_DATA)
                val type = data.getString(KEY_TYPE)
                val pkgName = data.getString(KEY_PACKAGE_NAME)
                val actionName = data.getString(KEY_ACTION_NAME)
                val className = data.getString(KEY_CLASS_NAME)
                val uri = data.getString(KEY_URI)
                val categoryName = data.getString(KEY_CATEGORY_NAME)
                val extras = data.getJSONObject(KEY_EXTRAS)
                val version = data.getJSONObject(KEY_VERSION)
                var ver_start = 0
                var ver_end = Int.MAX_VALUE

                if (version != null) {
                    ver_start = version.getIntValue(KEY_START)
                    ver_end = version.getIntValue(KEY_END)
                }

                if (!pkgName.isNullOrEmpty()) {
                    val appInfo = AppUtil.getAppInfo(context, pkgName)
                    if (appInfo == null) {
                        // 不存在对应的app
                        if (errorLevel < FAILURE_LEVEL_APP_NOT_FOUND) {
                            errorLevel = FAILURE_LEVEL_APP_NOT_FOUND
                        }
                        continue
                    } else {
                        if (appInfo.version in ver_start..ver_end) {

                        } else {
                            // 版本不符合，暂时当不存在app处理
                            if (errorLevel < FAILURE_LEVEL_APP_NOT_FOUND) {
                                errorLevel = FAILURE_LEVEL_APP_NOT_FOUND
                            }
                            continue
                        }
                    }
                }

                val intent: Intent? = if (actionName.isNullOrEmpty()) {
                    if (uri.isNullOrEmpty()) {
                        if (className.isNullOrEmpty()) {
                            context.packageManager.getLaunchIntentForPackage(pkgName)
                        } else {
                            Intent()
                        }
                    } else {
                        Intent.parseUri(uri, 0)
                    }
                } else {
                    Intent(actionName)
                }

                if (!pkgName.isNullOrEmpty()) {
                    intent?.setPackage(pkgName)
                }

                if (!className.isNullOrEmpty()) {
                    intent?.setClassName(pkgName, className)
                }

                if (!categoryName.isNullOrEmpty()) {
                    intent?.addCategory(categoryName)
                }

                if (!uri.isNullOrEmpty()) {
                    intent?.run {
                        if (this.data == null) {
                            this.data = Uri.parse(uri)
                        }
                    }
                }

                if (!extras.isNullOrEmpty()) {
                    for (key: String in extras.keys) {
                        when (val value = extras[key]) {
                            is Int -> {
                                intent?.putExtra(key, value)
                            }
                            is String -> {
                                intent?.putExtra(key, value)
                            }
                            is Double -> {
                                intent?.putExtra(key, value)
                            }
                            is Boolean -> {
                                intent?.putExtra(key, value)
                            }
                            is Long -> {
                                // may not happen
                                intent?.putExtra(key, value)
                            }
                            is Float -> {
                                // may not happen
                                intent?.putExtra(key, value)
                            }
                            is Short -> {
                                // may not happen
                                intent?.putExtra(key, value)
                            }
                            is List<*> -> {
                                if (value.isNotEmpty()) {
                                    @Suppress("UNCHECKED_CAST")
                                    when (value[0]) {
                                        is Int -> {
                                            (value as? List<Int>)?.toTypedArray()?.let {
                                                val array = IntArray(it.size) { index ->
                                                    it[index]
                                                }
                                                intent?.putExtra(key, array)
                                            }
                                        }
                                        is Byte -> {
                                            // may not happen
                                            (value as? List<Byte>)?.toTypedArray()?.let {
                                                val array = ByteArray(it.size) { index ->
                                                    it[index]
                                                }
                                                intent?.putExtra(key, array)
                                            }
                                        }
                                        is Float -> {
                                            // may not happen
                                            (value as? List<Float>)?.toTypedArray()?.let {
                                                val array = FloatArray(it.size) { index ->
                                                    it[index]
                                                }
                                                intent?.putExtra(key, array)
                                            }
                                        }
                                        is Double -> {
                                            (value as? List<Double>)?.toTypedArray()?.let {
                                                val array = DoubleArray(it.size) { index ->
                                                    it[index]
                                                }
                                                intent?.putExtra(key, array)
                                            }
                                        }
                                        is String -> {
                                            (value as? List<String>)?.toTypedArray()?.let {
                                                intent?.putExtra(key, it)
                                            }
                                        }
                                        is Short -> {
                                            // may not happen
                                            (value as? List<Short>)?.toTypedArray()?.let {
                                                val array = ShortArray(it.size) { index ->
                                                    it[index]
                                                }
                                                intent?.putExtra(key, array)
                                            }
                                        }
                                        is Boolean -> {
                                            // may not happen
                                            (value as? List<Boolean>)?.toTypedArray()?.let {
                                                val array = BooleanArray(it.size) { index ->
                                                    it[index]
                                                }
                                                intent?.putExtra(key, array)
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                intent?.putExtra(key, extras.getString(key))
                            }
                        }

                    }
                }

                if (!type.isNullOrEmpty() && intent != null) {
                    if (!AppUtil.isActionSupported(context, intent, type)) {
                        if (type != DATA_TYPE_BROADCAST) {
                            // 不支持action
                            if (errorLevel < FAILURE_LEVEL_ACTION_UNSUPPORTED) {
                                errorLevel = FAILURE_LEVEL_ACTION_UNSUPPORTED
                            }
                            continue
                        }
                    }
                }

                when (type) {
                    DATA_TYPE_SERVICE -> {
                        context.startService(intent)
                        isSuccess = true
                    }
                    DATA_TYPE_BROADCAST -> {
                        context.sendBroadcast(intent)
                        isSuccess = true
                    }
                    else -> {
                        // 打开app
                        if (intent == null) {
                            if (errorLevel < FAILURE_LEVEL_INTERNAL_ERROR) {
                                errorLevel = FAILURE_LEVEL_INTERNAL_ERROR
                            }
                        } else {
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                            isSuccess = true
                        }
                    }
                }

                if (isSuccess) {
                    result[KEY_ACTION_ID] = actionId
                    return true
                }
            } catch (t: Throwable) {
                t.printStackTrace()

                errorLevel = FAILURE_LEVEL_ACTION_UNSUPPORTED
            }
        }

        // 执行失败
        result[KEY_EXECUTION_ID] = executeId
        result[KEY_FAILURE_CODE] = codeMap[errorLevel]

        return false
    }

    override fun getForegroundApp(): AppUtil.AppInfo? {
        return AppUtil.getForegroundApp(context)
    }

}