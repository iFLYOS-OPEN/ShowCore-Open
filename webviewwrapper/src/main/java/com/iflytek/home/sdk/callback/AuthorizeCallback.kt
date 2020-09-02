package com.iflytek.home.sdk.callback

/**
 * 授权回调
 */
internal interface AuthorizeCallback {
    fun onAuthorizeUrlCall(tag: String, url: String)
}
