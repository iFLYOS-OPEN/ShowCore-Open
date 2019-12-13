package com.iflytek.cyber.evs.sdk.agent

import android.util.SparseArray
import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.model.Constant
import com.iflytek.cyber.evs.sdk.utils.AppUtil

/**
 * App操作模块。详细介绍见https://doc.iflyos.cn/device/evs/reference/app_action.html#app%E6%93%8D%E4%BD%9C
 */
abstract class AppAction {
    val version = "1.2"

    companion object {
        const val NAME_EXECUTE = "${Constant.NAMESPACE_APP_ACTION}.execute"
        const val NAME_CHECK = "${Constant.NAMESPACE_APP_ACTION}.check"
        const val NAME_CHECK_RESULT = "${Constant.NAMESPACE_APP_ACTION}.check_result"
        const val NAME_EXECUTE_SUCCEED = "${Constant.NAMESPACE_APP_ACTION}.execute_succeed"
        const val NAME_EXECUTE_FAILED = "${Constant.NAMESPACE_APP_ACTION}.execute_failed"

        const val KEY_TYPE = "type"
        const val KEY_ACTIONS = "actions"
        const val KEY_ACTION_ID = "action_id"
        const val KEY_RESULT = "result"
        const val KEY_EXECUTION_ID = "execution_id"
        const val KEY_CHECK_ID = "check_id"
        const val KEY_PACKAGE_NAME = "package_name"
        const val KEY_ACTION_NAME = "action_name"
        const val KEY_CLASS_NAME = "class_name"
        const val KEY_CATEGORY_NAME = "category_name"
        const val KEY_URI = "uri"
        const val KEY_FOREGROUND_APP = "foreground_app"
        const val KEY_ACTIVITY = "activity"
        const val KEY_CHINESE_NAME = "chinese_name"
        const val KEY_DATA = "data"
        const val KEY_VERSION = "version"
        const val KEY_START = "start"
        const val KEY_END = "end"
        const val KEY_EXTRAS = "extras"
        const val KEY_FAILURE_CODE = "failure_code"
        const val KEY_SUPPORTED_EXECUTE = "supported_execute"

        const val DATA_TYPE_ACTIVITY = "activity"
        const val DATA_TYPE_SERVICE = "service"
        const val DATA_TYPE_BROADCAST = "broadcast"
        const val DATA_TYPE_EXIT = "exit"

        const val FAILURE_LEVEL_ACTION_UNSUPPORTED = 3
        const val FAILURE_LEVEL_APP_NOT_FOUND = 2
        const val FAILURE_LEVEL_INTERNAL_ERROR = 1

        const val FAILURE_CODE_ACTION_UNSUPPORTED = "ACTION_UNSUPPORTED"
        const val FAILURE_CODE_APP_NOT_FOUND = "APP_NOT_FOUND"
        const val FAILURE_CODE_INTERNAL_ERROR = "INTERNAL_ERROR"
    }

    protected val codeMap = SparseArray<String>()

    init {
        codeMap.put(FAILURE_LEVEL_ACTION_UNSUPPORTED, FAILURE_CODE_ACTION_UNSUPPORTED)
        codeMap.put(FAILURE_LEVEL_APP_NOT_FOUND, FAILURE_CODE_APP_NOT_FOUND)
        codeMap.put(FAILURE_LEVEL_INTERNAL_ERROR, FAILURE_CODE_INTERNAL_ERROR)
    }

    abstract fun getSupportedExecute(): List<String>

    /**
     * 检测设备端对云端actions的支持情况。
     * @param payload 云端response的payload
     * @return 检测结果
     */
    abstract fun check(payload: JSONObject): JSONObject

    /**
     * 执行云端下发的actions。
     * @param payload 云端response的payload
     * @param result 本地执行结果
     * @return 是否执行成功
     */
    abstract fun execute(payload: JSONObject, result: JSONObject): Boolean

    /**
     * 获取前台app信息（包括包名、前台Activity类全名）。
     */
    abstract fun getForegroundApp(): AppUtil.AppInfo?
}