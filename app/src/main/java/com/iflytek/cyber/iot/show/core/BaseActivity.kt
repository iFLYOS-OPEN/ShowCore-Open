@file:Suppress("LeakingThis")

package com.iflytek.cyber.iot.show.core

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import me.yokeyword.fragmentation.*
import me.yokeyword.fragmentation.anim.FragmentAnimator

abstract class BaseActivity : AppCompatActivity(), ISupportActivity {
    private val mDelegate: SupportActivityDelegate = SupportActivityDelegate(this)

    var isResume = false

    override fun getSupportDelegate(): SupportActivityDelegate {
        return mDelegate
    }

    /**
     * Perform some extra transactions.
     * 额外的事务：自定义Tag，添加SharedElement动画，操作非回退栈Fragment
     */
    override fun extraTransaction(): ExtraTransaction {
        return mDelegate.extraTransaction()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDelegate.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        isResume = true
    }

    override fun onPause() {
        super.onPause()

        isResume = false
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mDelegate.onPostCreate(savedInstanceState)
    }

    override fun onDestroy() {
        mDelegate.onDestroy()
        super.onDestroy()
    }

    /**
     * Note： return mDelegate.dispatchTouchEvent(ev) || super.dispatchTouchEvent(ev);
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return mDelegate.dispatchTouchEvent(ev) || super.dispatchTouchEvent(ev)
    }

    /**
     * 不建议复写该方法,请使用 [onBackPressedSupport] 代替
     */
    override fun onBackPressed() {
        mDelegate.onBackPressed()
    }

    /**
     * 该方法回调时机为,Activity回退栈内Fragment的数量 小于等于1 时,默认finish Activity
     * 请尽量复写该方法,避免复写onBackPress(),以保证SupportFragment内的onBackPressedSupport()回退事件正常执行
     */
    override fun onBackPressedSupport() {
        mDelegate.onBackPressedSupport()
    }

    /**
     * 获取设置的全局动画 copy
     *
     * @return FragmentAnimator
     */
    override fun getFragmentAnimator(): FragmentAnimator {
        return mDelegate.fragmentAnimator
    }

    /**
     * Set all fragments animation.
     * 设置Fragment内的全局动画
     */
    override fun setFragmentAnimator(fragmentAnimator: FragmentAnimator) {
        mDelegate.fragmentAnimator = fragmentAnimator
    }

    /**
     * Set all fragments animation.
     * 构建Fragment转场动画
     *
     *
     * 如果是在Activity内实现,则构建的是Activity内所有Fragment的转场动画,
     * 如果是在Fragment内实现,则构建的是该Fragment的转场动画,此时优先级 > Activity的onCreateFragmentAnimator()
     *
     * @return FragmentAnimator对象
     */
    override fun onCreateFragmentAnimator(): FragmentAnimator {
        return FragmentAnimator(
            R.anim.page_enter, R.anim.page_exit,
            R.anim.page_pop_enter, R.anim.page_pop_exit
        )
//        return mDelegate.onCreateFragmentAnimator()
    }

    override fun post(runnable: Runnable) {
        mDelegate.post(runnable)
    }

    /****************************************以下为可选方法(Optional methods)******************************************************/

    /**
     * 加载根 Fragment, 即 Activity 内的第一个 Fragment 或 Fragment 内的第一个子 Fragment
     *
     * @param containerId 容器 id
     * @param toFragment  目标 Fragment
     */
    fun loadRootFragment(containerId: Int, @NonNull toFragment: ISupportFragment) {
        mDelegate.loadRootFragment(containerId, toFragment)
    }

    fun loadRootFragment(
        containerId: Int,
        toFragment: ISupportFragment,
        addToBackStack: Boolean,
        allowAnimation: Boolean
    ) {
        mDelegate.loadRootFragment(containerId, toFragment, addToBackStack, allowAnimation)
    }

    /**
     * 加载多个同级根 Fragment,类似 WeChat, QQ主页的场景
     */
    fun loadMultipleRootFragment(
        containerId: Int,
        showPosition: Int,
        vararg toFragments: ISupportFragment
    ) {
        mDelegate.loadMultipleRootFragment(containerId, showPosition, *toFragments)
    }

    /**
     * show 一个 Fragment, hide 其他同栈所有 Fragment
     * 使用该方法时，要确保同级栈内无多余的 Fragment,(只有通过 loadMultipleRootFragment() 载入的 Fragment)
     *
     *
     * 建议使用更明确的[showHideFragment]
     *
     * @param showFragment 需要show的Fragment
     */
    fun showHideFragment(showFragment: ISupportFragment) {
        mDelegate.showHideFragment(showFragment)
    }

    /**
     * show一个Fragment,hide一个Fragment ; 主要用于类似微信主页那种 切换tab的情况
     */
    fun showHideFragment(showFragment: ISupportFragment, hideFragment: ISupportFragment) {
        mDelegate.showHideFragment(showFragment, hideFragment)
    }

    /**
     * It is recommended to use [SupportFragment.start].
     */
    fun start(toFragment: ISupportFragment) {
        mDelegate.start(toFragment)
    }

    /**
     * It is recommended to use [SupportFragment.start].
     *
     * @param launchMode Similar to Activity's LaunchMode.
     */
    fun start(toFragment: ISupportFragment, @ISupportFragment.LaunchMode launchMode: Int) {
        mDelegate.start(toFragment, launchMode)
    }

    /**
     * It is recommended to use [SupportFragment.startForResult].
     * Launch an fragment for which you would like a result when it poped.
     */
    fun startForResult(toFragment: ISupportFragment, requestCode: Int) {
        mDelegate.startForResult(toFragment, requestCode)
    }

    /**
     * It is recommended to use [SupportFragment.startWithPop].
     * Start the target Fragment and pop itself
     */
    fun startWithPop(toFragment: ISupportFragment) {
        mDelegate.startWithPop(toFragment)
    }

    /**
     * It is recommended to use [SupportFragment.startWithPopTo].
     *
     * @see .popTo
     * @see .start
     */
    fun startWithPopTo(
        toFragment: ISupportFragment,
        targetFragmentClass: Class<*>,
        includeTargetFragment: Boolean
    ) {
        mDelegate.startWithPopTo(toFragment, targetFragmentClass, includeTargetFragment)
    }

    /**
     * It is recommended to use [SupportFragment.replaceFragment].
     */
    fun replaceFragment(toFragment: ISupportFragment, addToBackStack: Boolean) {
        mDelegate.replaceFragment(toFragment, addToBackStack)
    }

    /**
     * Pop the fragment.
     */
    fun pop() {
        mDelegate.pop()
    }

    /**
     * Pop the last fragment transition from the manager's fragment
     * back stack.
     *
     *
     * 出栈到目标fragment
     *
     * @param targetFragmentClass   目标 fragment
     * @param includeTargetFragment 是否包含该 fragment
     */
    fun popTo(targetFragmentClass: Class<*>, includeTargetFragment: Boolean) {
        mDelegate.popTo(targetFragmentClass, includeTargetFragment)
    }

    /**
     * If you want to begin another FragmentTransaction immediately after popTo(), use this method.
     * 如果你想在出栈后, 立刻进行 FragmentTransaction 操作，请使用该方法
     */
    fun popTo(
        targetFragmentClass: Class<*>,
        includeTargetFragment: Boolean,
        afterPopTransactionRunnable: Runnable
    ) {
        mDelegate.popTo(targetFragmentClass, includeTargetFragment, afterPopTransactionRunnable)
    }

    fun popTo(
        targetFragmentClass: Class<*>,
        includeTargetFragment: Boolean,
        afterPopTransactionRunnable: Runnable,
        popAnim: Int
    ) {
        mDelegate.popTo(
            targetFragmentClass,
            includeTargetFragment,
            afterPopTransactionRunnable,
            popAnim
        )
    }

    /**
     * 当 Fragment 根布局 没有 设定 background 属性时,
     * Fragmentation 默认使用 Theme 的 android:windowBackground 作为Fragment的背景,
     * 可以通过该方法改变其内所有Fragment的默认背景。
     */
    fun setDefaultFragmentBackground(@DrawableRes backgroundRes: Int) {
        mDelegate.defaultFragmentBackground = backgroundRes
    }

    /**
     * 得到位于栈顶 Fragment
     */
    fun getTopFragment(): ISupportFragment? {
        return SupportHelper.getTopFragment(supportFragmentManager)
    }

    /**
     * 获取栈内的 Fragment 对象
     */
    fun <T : ISupportFragment> findFragment(fragmentClass: Class<T>): T? {
        return SupportHelper.findFragment(supportFragmentManager, fragmentClass)
    }
}