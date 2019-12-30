package com.iflytek.cyber.iot.show.core

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Message
import android.service.dreams.DreamService
import android.widget.ImageView
import android.widget.TextView
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.iot.show.core.api.WeatherApi
import com.iflytek.cyber.iot.show.core.model.Weather
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.SoftReference
import java.text.SimpleDateFormat
import java.util.*

class ShowCoreDream : DreamService(), AMapLocationListener {
    companion object {
        private const val CHANGE_WEATHER_TIME = 3L * 60 * 60 * 1000 // 检查天气预报时间间隔
    }

    private var tvTime: TextView? = null
    private var tvDateTime: TextView? = null
    private var tvWeather: TextView? = null
    private var ivWeather: ImageView? = null
    private var tvSlogan: TextView? = null

    private val timerHandler = TimerHandler(this)

    private var mLocationClient: AMapLocationClient? = null
    private val locationRunnable = Runnable {
        mLocationClient?.startLocation()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        setContentView(R.layout.layout_dream)

        tvTime = findViewById(R.id.time)
        tvDateTime = findViewById(R.id.date_time)
        tvWeather = findViewById(R.id.weather_text)
        ivWeather = findViewById(R.id.weather_icon)
        tvSlogan = findViewById(R.id.slogan)

        setDateAndTime()

        timerHandler.sendEmptyMessageDelayed(0, 1000)

        setupLocation()
    }

    private fun setupLocation() {
        val locationClient = AMapLocationClient(baseContext)
        val locationOption = AMapLocationClientOption()
        locationClient.setLocationListener(this)
        locationOption.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        locationOption.isOnceLocation = true
        locationClient.setLocationOption(locationOption)
        locationClient.startLocation()

        mLocationClient = locationClient
    }

    override fun onLocationChanged(location: AMapLocation?) {
        location?.let {
            val tvWeather = tvWeather ?: return
            if (it.errorCode == 0) {
                loadWeather(it)
            }
            tvWeather.postDelayed(locationRunnable, CHANGE_WEATHER_TIME)
        }
    }

    private fun loadWeather(location: AMapLocation) {
        AuthDelegate.getAuthResponseFromPref(baseContext)?.let {
            val currentLocation = (String.format(Locale.CHINESE, "%.2f", location.longitude)
                + "," + String.format(Locale.CHINESE, "%.2f", location.latitude))

            getWeatherApi()?.getWeather(currentLocation)?.enqueue(object : Callback<Weather> {
                override fun onFailure(call: Call<Weather>, t: Throwable) {
                    t.printStackTrace()
                    tvWeather?.text = null
                    ivWeather?.setImageDrawable(null)
                }

                @SuppressLint("SetTextI18n")
                override fun onResponse(call: Call<Weather>, response: Response<Weather>) {
                    if (response.isSuccessful) {
                        response.body()?.let { weather ->
                            tvWeather?.text = "${weather.temperature}℃ ${weather.description}"

                            ivWeather?.let { imageView ->
                                Glide.with(imageView)
                                    .asDrawable()
                                    .load(weather.icon)
                                    .transition(
                                        DrawableTransitionOptions.with(
                                            DrawableCrossFadeFactory.Builder()
                                                .setCrossFadeEnabled(true).build()
                                        )
                                    )
                                    .into(imageView)
                            }
                        }
                    } else {
                        tvWeather?.text = null
                        ivWeather?.setImageDrawable(null)
                    }
                }
            })
        }?:run {
            tvWeather?.text = null
            ivWeather?.setImageDrawable(null)
        }
    }

    private fun setDateAndTime() {
        val tvTime = tvTime ?: return
        val tvDate = tvDateTime ?: return

        val date = Date()

        val dateFormat = SimpleDateFormat("MM月dd日 EEEE", Locale.getDefault())
        tvDate.text = dateFormat.format(date)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        tvTime.text = timeFormat.format(date)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        tvTime = null
        tvSlogan = null
        tvWeather = null
        ivWeather = null
        tvDateTime = null

        timerHandler.removeMessages(0)
    }

    private fun getWeatherApi(): WeatherApi? {
        return CoreApplication.from(baseContext).createApi(WeatherApi::class.java)
    }

    private class TimerHandler internal constructor(
        service: ShowCoreDream,
        private val reference: SoftReference<ShowCoreDream> =
            SoftReference(service)
    ) : Handler() {

        override fun handleMessage(msg: Message) {
            val service = reference.get()
            if (service != null) {
                service.setDateAndTime()
                sendEmptyMessageDelayed(0, 1000)
            }
        }
    }
}