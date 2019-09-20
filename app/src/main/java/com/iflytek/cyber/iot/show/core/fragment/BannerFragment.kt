package com.iflytek.cyber.iot.show.core.fragment

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.gson.JsonParser
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.EngineService
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.MediaApi
import com.iflytek.cyber.iot.show.core.impl.prompt.PromptManager
import com.iflytek.cyber.iot.show.core.model.Banner
import com.iflytek.cyber.iot.show.core.model.MusicBody
import com.iflytek.cyber.iot.show.core.widget.StyledAlertDialog
import me.yokeyword.fragmentation.ISupportFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.Math.random
import java.net.UnknownHostException
import kotlin.math.roundToInt

class BannerFragment : Fragment() {
    private var banner: Banner? = null
    private var position = 0

    private var currentSummary = 0
    private var tvSummary: TextView? = null

    private val handler = Handler()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_banner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val banner: Banner? = arguments?.getParcelable("banner")
            ?: (savedInstanceState?.getParcelable("banner"))
        position = arguments?.getInt("position") ?: 0

        val background: ImageView = view.findViewById(R.id.background)
        val backgroundContainer: FrameLayout = view.findViewById(R.id.background_container)
        val tvTitle: TextView = view.findViewById(R.id.title)
        val tvSummary: TextView = view.findViewById(R.id.summary)
        val container = view.findViewById<View>(R.id.banner_container)
        container.tag = position

        // setup shadow
        val shadowColor = Color.parseColor("#19000000")
        val dy = resources.getDimensionPixelSize(R.dimen.dp_2).toFloat()
        val radius = resources.getDimensionPixelSize(R.dimen.dp_4).toFloat()
        tvTitle.setShadowLayer(radius, 0f, dy, shadowColor)
        tvSummary.setShadowLayer(radius, 0f, dy, shadowColor)

        tvTitle.text = banner?.title
        if (banner?.descriptions?.isNotEmpty() == true) {
            val index = (random() * (banner.descriptions.size - 1)).roundToInt()
            currentSummary = index
            tvSummary.text = banner.descriptions[index]
        } else {
            tvSummary.text = ""
        }
        this.tvSummary = tvSummary

        if (!banner?.cover.isNullOrEmpty()) {
            Glide.with(background)
                .load(banner?.cover)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(background)
            backgroundContainer.foreground = ColorDrawable(Color.parseColor("#33262626"))
        } else {
            val wallpaper = arrayOf(
                "file:///android_asset/wallpaper/wallpaper_a.jpg",
                "file:///android_asset/wallpaper/wallpaper_b.jpg",
                "file:///android_asset/wallpaper/wallpaper_c.jpg"
            )
            Glide.with(background)
                .load(wallpaper[(random() * (wallpaper.size - 1)).roundToInt()])
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(background)
            backgroundContainer.foreground = null
        }

        tvTitle.setOnClickListener {
            when (banner?.target) {
                "webview" -> {
                    val fragment = WebViewFragment()
                    val arguments = Bundle()
                    arguments.putString("url", banner.content)
                    fragment.arguments = arguments
                    (parentFragment as? BaseFragment)?.start(fragment)
                }
                "skill" -> {
                    val intent = Intent(context, EngineService::class.java)
                    intent.action = EngineService.ACTION_SEND_TEXT_IN
                    intent.putExtra(EngineService.EXTRA_QUERY, banner.content)
                    context?.startService(intent)
                }
                "video",
                "audio" -> {
                    val api = CoreApplication.from(it.context).createApi(MediaApi::class.java)
                    val mediaId = try {
                        Integer.parseInt(banner.content!!)
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        -1
                    }
                    if (mediaId != -1)
                        api?.playMusic(MusicBody(mediaId, null, null))?.enqueue(object :
                            Callback<String> {
                            override fun onFailure(call: Call<String>, t: Throwable) {
                                t.printStackTrace()
                                if (t is UnknownHostException) {
                                    // 网络不可用
                                    PromptManager.play(PromptManager.NETWORK_LOST)

                                    val intent = Intent(context, FloatingService::class.java)
                                    intent.action = FloatingService.ACTION_SHOW_NOTIFICATION
                                    intent.putExtra(FloatingService.EXTRA_MESSAGE, "网络连接异常，请重新设置")
                                    intent.putExtra(FloatingService.EXTRA_TAG, "network_error")
                                    intent.putExtra(
                                        FloatingService.EXTRA_POSITIVE_BUTTON_TEXT,
                                        "设置网络"
                                    )
                                    intent.putExtra(
                                        FloatingService.EXTRA_POSITIVE_BUTTON_ACTION,
                                        MainFragment2.ACTION_OPEN_WIFI
                                    )
                                    intent.putExtra(
                                        FloatingService.EXTRA_ICON_RES,
                                        R.drawable.ic_wifi_error_white_40dp
                                    )
                                    context?.startService(intent)
                                } else {
                                    val showNotification =
                                        Intent(context, FloatingService::class.java)
                                    showNotification.action =
                                        FloatingService.ACTION_SHOW_NOTIFICATION
                                    showNotification.putExtra(
                                        FloatingService.EXTRA_MESSAGE,
                                        "请求出现错误，请稍后再试"
                                    )
                                    showNotification.putExtra(
                                        FloatingService.EXTRA_ICON_RES,
                                        R.drawable.ic_default_error_white_40dp
                                    )
                                    showNotification.putExtra(
                                        FloatingService.EXTRA_POSITIVE_BUTTON_TEXT,
                                        getString(R.string.i_got_it)
                                    )
                                    context?.startService(showNotification)
                                }
                            }

                            override fun onResponse(
                                call: Call<String>,
                                response: Response<String>
                            ) {
                                if (response.isSuccessful) {
                                    (parentFragment as? BaseFragment)?.start(
                                        PlayerInfoFragment2(),
                                        ISupportFragment.SINGLETOP
                                    )
                                } else {
                                    if (response.code() == 401) {
                                        if (context != null &&
                                            AuthDelegate.getAuthResponseFromPref(context!!) == null
                                        ) {
                                            val disconnectNotification =
                                                Intent(context, FloatingService::class.java)
                                            disconnectNotification.action =
                                                FloatingService.ACTION_SHOW_NOTIFICATION
                                            disconnectNotification.putExtra(
                                                FloatingService.EXTRA_MESSAGE,
                                                getString(R.string.message_evs_auth_expired)
                                            )
                                            disconnectNotification.putExtra(
                                                FloatingService.EXTRA_TAG,
                                                "auth_error"
                                            )
                                            disconnectNotification.putExtra(
                                                FloatingService.EXTRA_ICON_RES,
                                                R.drawable.ic_default_error_white_40dp
                                            )
                                            disconnectNotification.putExtra(
                                                FloatingService.EXTRA_POSITIVE_BUTTON_TEXT,
                                                getString(R.string.re_auth)
                                            )
                                            disconnectNotification.putExtra(
                                                FloatingService.EXTRA_POSITIVE_BUTTON_ACTION,
                                                MainFragment2.ACTION_OPEN_AUTH
                                            )
                                            disconnectNotification.putExtra(
                                                FloatingService.EXTRA_KEEPING, true
                                            )
                                            context?.startService(disconnectNotification)
                                        }
                                    } else {
                                        try {
                                            val body = response.errorBody()?.string()
                                            val json = JsonParser().parse(body).asJsonObject
                                            val errorJson = json["error"].asJsonObject

                                            val showNotification =
                                                Intent(context, FloatingService::class.java)
                                            showNotification.action =
                                                FloatingService.ACTION_SHOW_NOTIFICATION
                                            showNotification.putExtra(
                                                FloatingService.EXTRA_MESSAGE,
                                                errorJson.get("message").asString
                                            )
                                            showNotification.putExtra(
                                                FloatingService.EXTRA_ICON_RES,
                                                R.drawable.ic_default_error_black_40dp
                                            )
                                            showNotification.putExtra(
                                                FloatingService.EXTRA_POSITIVE_BUTTON_TEXT,
                                                getString(R.string.i_got_it)
                                            )
                                            context?.startService(showNotification)
                                        } catch (t: Throwable) {
                                            t.printStackTrace()

                                            val showNotification =
                                                Intent(context, FloatingService::class.java)
                                            showNotification.action =
                                                FloatingService.ACTION_SHOW_NOTIFICATION
                                            showNotification.putExtra(
                                                FloatingService.EXTRA_MESSAGE,
                                                "请求出现错误，请稍后再试"
                                            )
                                            showNotification.putExtra(
                                                FloatingService.EXTRA_ICON_RES,
                                                R.drawable.ic_default_error_black_40dp
                                            )
                                            showNotification.putExtra(
                                                FloatingService.EXTRA_POSITIVE_BUTTON_TEXT,
                                                getString(R.string.i_got_it)
                                            )
                                            context?.startService(showNotification)
                                        }
                                    }
                                }
                            }
                        })
                }
            }
        }

        this.banner = banner
    }

    override fun onResume() {
        super.onResume()

        handler.postDelayed(updateSummaryRunnable(), 6 * 1000)
    }

    private fun updateSummaryRunnable(): Runnable = Runnable {
        banner?.let { banner ->
            if (isResumed) {
                if (banner.descriptions?.isNotEmpty() == true && banner.descriptions.size > 1) {
                    val next = (currentSummary + 1) % banner.descriptions.size
                    currentSummary = next
                    startUpdateSummaryAnimator(banner.descriptions[next])
                }
                handler.postDelayed(updateSummaryRunnable(), 6 * 1000)
            }
        }
    }

    private fun startUpdateSummaryAnimator(newText: String) {
        val animator = ValueAnimator.ofFloat(0f, 2f)
        animator.addUpdateListener {
            val value = it.animatedValue as Float
            if (value > 1) {
                if (tvSummary?.text.toString() != newText) {
                    tvSummary?.text = newText
                }
                tvSummary?.alpha = value - 1
            } else {
                tvSummary?.alpha = 1 - value
            }
        }
        animator.duration = 600
        animator.start()
    }

    override fun onPause() {
        super.onPause()

        handler.removeCallbacks(null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        banner?.let { banner ->
            outState.putParcelable("banner", banner)
        }
    }
}