package com.iflytek.cyber.iot.show.core.template

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation

class WeatherTemplateView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    companion object {
        private const val TYPE_NORMAL = 0
        private const val TYPE_FUTURE = 1
        private const val TYPE_NOWADAYS = 2
    }

    private val skillIconImage: ImageView
    private val backgroundImage: ImageView
    private val mainTitle: TextView
    private val subTitle: TextView

    // containers
    private val normalContainer: View
    private val nowadaysContainer: View
    private val normalHeader: View

    private val normalHeaderIcon: ImageView
    private val normalHeaderBody: TextView
    private val normalHeaderSubBody: TextView

    private val nowadaysIcon: ImageView
    private val nowadaysCondition: TextView
    private val nowadaysDescription: TextView

    private val weatherForecastRecyclerView: RecyclerView

    private val adapter = ListAdapter()

    private val innerOnClickBackListener = OnClickListener { v ->
        onClickBackListener?.onClick(v)
    }
    private var onClickBackListener: OnClickListener? = null

    private var currentPayload: String? = null
    private var currentWeather: WeatherPayload? = null

    private var forecastItemWidth = 0

    init {
        val childView = LayoutInflater.from(context).inflate(R.layout.layout_weather_template_normal, null)

        skillIconImage = childView.findViewById(R.id.skill_icon)
        backgroundImage = childView.findViewById(R.id.background_image)
        mainTitle = childView.findViewById(R.id.main_title)
        subTitle = childView.findViewById(R.id.sub_title)
        normalContainer = childView.findViewById(R.id.normal_container)
        normalHeader = childView.findViewById(R.id.normal_header)
        nowadaysContainer = childView.findViewById(R.id.nowadays_container)
        weatherForecastRecyclerView = childView.findViewById(R.id.forecast_recycler_view)
        normalHeaderIcon = childView.findViewById(R.id.normal_icon)
        normalHeaderBody = childView.findViewById(R.id.normal_body)
        normalHeaderSubBody = childView.findViewById(R.id.normal_sub_body)
        nowadaysCondition = childView.findViewById(R.id.nowadays_condition)
        nowadaysDescription = childView.findViewById(R.id.nowadays_description)
        nowadaysIcon = childView.findViewById(R.id.nowadays_icon)

        weatherForecastRecyclerView.adapter = adapter
        val layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        weatherForecastRecyclerView.layoutManager = layoutManager

        weatherForecastRecyclerView.post {
            forecastItemWidth = weatherForecastRecyclerView.width / 6
            adapter.notifyDataSetChanged()
        }

        childView.findViewById<View>(R.id.back)?.setOnClickListener(innerOnClickBackListener)

        addView(childView)
    }

    fun updatePayload(payload: String) {
        val gson = Gson()
        val weather = gson.fromJson(payload, WeatherPayload::class.java)
        val type = getWeatherType(weather)
        currentPayload?.let {
            val prevWeather = gson.fromJson(it, WeatherPayload::class.java)
            val prevType = getWeatherType(prevWeather)
            if (prevType != type) {
                setupView(type)
            }
        } ?: run {
            setupView(type)
        }

        if (!weather.currentWeather?.iconUrl.isNullOrEmpty()) {
            normalHeaderIcon.visibility = View.VISIBLE
            if (type == TYPE_NORMAL) {
                Glide.with(normalHeaderIcon)
                    .load(weather.currentWeather?.iconUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(normalHeaderIcon)
            } else {
                Glide.with(nowadaysIcon)
                    .load(weather.currentWeather?.iconUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(nowadaysIcon)
            }
        } else {
            normalHeaderIcon.visibility = View.GONE
        }

        val size = weather.weatherForecasts?.size ?: 0
        if (size in 1..6) {
            weatherForecastRecyclerView.layoutManager =
                GridLayoutManager(context, size)
        } else {
            weatherForecastRecyclerView.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        if (type == TYPE_NORMAL) {
            if (weather.currentWeather?.condition.isNullOrEmpty()) {
                if (weather.currentWeather?.description.isNullOrEmpty()) {
                    normalHeaderBody.text = weather.currentWeather?.highTemperature
                    normalHeaderSubBody.text = weather.currentWeather?.lowTemperature
                } else {
                    normalHeaderBody.text = weather.currentWeather?.description
                    normalHeaderSubBody.text = String.format("%s ~ %s",
                        weather.currentWeather?.lowTemperature,
                        weather.currentWeather?.highTemperature)
                }
            } else {
                normalHeaderBody.text = weather.currentWeather?.condition
                normalHeaderSubBody.text = String.format("%s ~ %s",
                    weather.currentWeather?.lowTemperature,
                    weather.currentWeather?.highTemperature)
            }
        } else if (type == TYPE_NOWADAYS) {
            nowadaysCondition.text = weather.currentWeather?.condition
            nowadaysDescription.text = weather.currentWeather?.description
        }

        mainTitle.text = weather.mainTitle
        weather.subTitle?.let { subTitle ->
            if (subTitle.isNotEmpty()) {
                this.subTitle.visibility = View.VISIBLE
                this.subTitle.text = subTitle
            } else {
                this.subTitle.visibility = View.GONE
            }
        } ?: run {
            this.subTitle.visibility = View.GONE
        }
        weather.skillIconUrl?.let { skillIconUrl ->
            if (skillIconUrl.isNotEmpty()) {
                skillIconImage.visibility = View.VISIBLE

                Glide.with(skillIconImage)
                    .load(skillIconUrl)
                    .apply(RequestOptions()
                        .transform(RoundedCornersTransformation(
                            resources.getDimensionPixelSize(R.dimen.dp_8), 0)))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(skillIconImage)
            } else {
                skillIconImage.visibility = View.GONE
            }
        } ?: run {
            skillIconImage.visibility = View.GONE
        }
        weather?.backButton?.let { backgroundImageUrl ->
            if (backgroundImageUrl.isNotEmpty()) {
                backgroundImage.visibility = View.VISIBLE

                Glide.with(backgroundImage)
                    .load(backgroundImageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(backgroundImage)
            } else {
                backgroundImage.visibility = View.GONE
            }
        } ?: run {
            backgroundImage.visibility = View.GONE
        }

        adapter.notifyDataSetChanged()

        this.currentPayload = payload
        this.currentWeather = weather
    }

    private fun setupView(type: Int) {
        when (type) {
            TYPE_FUTURE -> {
                normalContainer.visibility = View.VISIBLE
                normalHeader.visibility = View.GONE
                nowadaysContainer.visibility = View.GONE
            }
            TYPE_NORMAL -> {
                normalContainer.visibility = View.VISIBLE
                normalHeader.visibility = View.VISIBLE
                nowadaysContainer.visibility = View.GONE
            }
            TYPE_NOWADAYS -> {
                normalContainer.visibility = View.GONE
                nowadaysContainer.visibility = View.VISIBLE
            }
        }
    }

    fun setOnClickBackListener(onClickListener: OnClickListener?) {
        this.onClickBackListener = onClickListener
    }

    private fun getWeatherType(weather: WeatherPayload): Int {
        val isFutureWeatherType = weather.currentWeather == null ||
            (weather.currentWeather.highTemperature.isNullOrEmpty() &&
                weather.currentWeather.lowTemperature.isNullOrEmpty())
        return if (weather.weatherForecasts?.isNotEmpty() == true) {
            if (isFutureWeatherType) {
                TYPE_FUTURE
            } else {
                TYPE_NORMAL
            }
        } else {
            TYPE_NOWADAYS
        }
    }

    private inner class ListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val typeNormal = 0
        private val typeLarge = 1
        override fun getItemViewType(position: Int): Int {
            return currentWeather?.let { weather ->
                when (getWeatherType(weather)) {
                    TYPE_FUTURE ->
                        typeLarge
                    else ->
                        typeNormal
                }
            } ?: typeNormal
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == typeLarge) {
                ForecastViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_weather_forecast_large, parent, false))
            } else {
                ForecastViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_weather_forecast_normal, parent, false))
            }
        }

        override fun getItemCount(): Int {
            return currentWeather?.weatherForecasts?.size ?: 0
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ForecastViewHolder) {
                val layoutManager = weatherForecastRecyclerView.layoutManager
                val targetWidth =
                    if (layoutManager is GridLayoutManager && layoutManager.spanCount == itemCount)
                        ViewGroup.LayoutParams.MATCH_PARENT
                    else
                        forecastItemWidth
                if (targetWidth > 0 &&
                    holder.itemView.layoutParams.width != targetWidth) {
                    val layoutParams = holder.itemView.layoutParams
                    layoutParams.width = targetWidth
                    holder.itemView.layoutParams = layoutParams
                    Log.d("Weather", "Set to $targetWidth")
                }
                currentWeather?.weatherForecasts?.get(position)?.let { forecast ->
                    holder.tvWeekday?.text = forecast.weekday
                    if (!forecast.imageUrl.isNullOrEmpty()) {
                        holder.ivImage?.let { imageView ->
                            Glide.with(imageView)
                                .load(forecast.imageUrl)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(imageView)
                        }
                    }

                    if (holder.tvTemperature != null) {
                        holder.tvTemperature.text = String.format("%s ~ %s",
                            forecast.lowTemperature, forecast.highTemperature)
                    } else {
                        holder.tvHighTemperature?.text = forecast.highTemperature
                        holder.tvLowTemperature?.text = forecast.lowTemperature
                        holder.tvDate?.text = forecast.date
                    }
                }
            }
        }
    }

    class ForecastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView? = itemView.findViewById(R.id.forecast_image)
        val tvWeekday: TextView? = itemView.findViewById(R.id.forecast_weekday)

        // normal
        val tvTemperature: TextView? = itemView.findViewById(R.id.forecast_temperature)

        // large
        val tvHighTemperature: TextView? = itemView.findViewById(R.id.forecast_high_temperature)
        val tvLowTemperature: TextView? = itemView.findViewById(R.id.forecast_low_temperature)
        val tvDate: TextView? = itemView.findViewById(R.id.forecast_date)
    }

    data class WeatherPayload(
        @SerializedName(Constant.PAYLOAD_CURRENT_WEATHER) val currentWeather: CurrentWeather?,
        @SerializedName(Constant.PAYLOAD_WEATHER_FORECAST) val weatherForecasts: List<WeatherForecast>?,
        @SerializedName(Constant.PAYLOAD_MAIN_TITLE) val mainTitle: String?,
        @SerializedName(Constant.PAYLOAD_SUB_TITLE) val subTitle: String?,
        @SerializedName(Constant.PAYLOAD_SKILL_ICON_URL) val skillIconUrl: String?,
        @SerializedName(Constant.PAYLOAD_TEMPLATE_ID) val templateId: String,
        @SerializedName(Constant.PAYLOAD_TYPE) val type: String?,
        @SerializedName(Constant.PAYLOAD_BACK_BUTTON) val backButton: String?,
        @SerializedName(Constant.PAYLOAD_BACKGROUND_IMAGE_URL) val backgroundImageUrl: String?
    )

    data class CurrentWeather(
        @SerializedName(Constant.PAYLOAD_CONDITION) val condition: String?,
        @SerializedName(Constant.PAYLOAD_DESCRIPTION) val description: String?,
        @SerializedName(Constant.PAYLOAD_HIGH_TEMPERATURE) val highTemperature: String?,
        @SerializedName(Constant.PAYLOAD_LOW_TEMPERATURE) val lowTemperature: String?,
        @SerializedName(Constant.PAYLOAD_ICON_URL) val iconUrl: String?
    )

    data class WeatherForecast(
        @SerializedName(Constant.PAYLOAD_WEEKDAY) val weekday: String?,
        @SerializedName(Constant.PAYLOAD_DATE) val date: String?,
        @SerializedName(Constant.PAYLOAD_HIGH_TEMPERATURE) val highTemperature: String?,
        @SerializedName(Constant.PAYLOAD_LOW_TEMPERATURE) val lowTemperature: String?,
        @SerializedName(Constant.PAYLOAD_IMAGE_URL) val imageUrl: String?
    )

    class Builder(private val context: Context) {

        private var payload: String? = null
        private var onClickListener: OnClickListener? = null
        fun payload(payload: String): Builder {
            this.payload = payload
            return this
        }

        fun onClickBackListener(onClickListener: OnClickListener?): Builder {
            this.onClickListener = onClickListener
            return this
        }

        fun build(): WeatherTemplateView {
            val view = WeatherTemplateView(context)
            payload?.let { payload ->
                view.updatePayload(payload)
            }
            view.setOnClickBackListener(onClickListener)
            return view
        }
    }
}