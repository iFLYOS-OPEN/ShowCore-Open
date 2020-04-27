package com.iflytek.cyber.iot.show.core.fragment

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.iflytek.cyber.iot.show.core.CoreApplication
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.api.IotApi
import com.iflytek.cyber.iot.show.core.model.Brand
import com.iflytek.cyber.iot.show.core.model.Message
import com.iflytek.cyber.iot.show.core.utils.clickWithTrigger
import com.iflytek.cyber.iot.show.core.utils.dp2Px
import com.iflytek.cyber.iot.show.core.widget.StyledAlertDialog
import com.makeramen.roundedimageview.RoundedImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BrandFragment : BaseFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var errorContainer: View
    private lateinit var loading: LottieAnimationView
    private var brandAdapter: BrandAdapter? = null

    private var needSync = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_brand, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recycler_view)
        errorContainer = view.findViewById(R.id.error_container)
        loading = view.findViewById(R.id.loading)

        view.findViewById<Button>(R.id.retry).setOnClickListener {
            showLoading()
            getIotList()
        }

        view.findViewById<View>(R.id.back).clickWithTrigger {
            if (needSync) {
                setFragmentResult(0, bundleOf(Pair("shouldSync", true)))
            }
            pop()
        }

        val layoutManager = GridLayoutManager(requireContext(), 4)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position == 0) {
                    4
                } else {
                    1
                }
            }
        }
        recyclerView.layoutManager = layoutManager

        setFragmentResult(0, bundleOf(Pair("shouldSync", false)))

        showLoading()
    }

    private fun showLoading() {
        recyclerView.isVisible = false
        errorContainer.isVisible = false
        loading.isVisible = true
        loading.playAnimation()
    }

    private fun hideLoading() {
        loading.isVisible = false
        loading.pauseAnimation()
    }

    private fun setupAdapter(items: ArrayList<Brand>) {
        if (brandAdapter == null) {
            brandAdapter = BrandAdapter(items) {
                if (it.authState == null) {
                    start(DeviceAuthFragment.newInstance(it.id))
                } else if (!it.authState.invalid) {
                    showUnBindDialog(it)
                }
            }
            recyclerView.adapter = brandAdapter
        } else {
            brandAdapter?.items = items
            brandAdapter?.notifyDataSetChanged()
        }
    }

    private fun showUnBindDialog(brand: Brand) {
        if (!isAdded || context == null) {
            return
        }

        StyledAlertDialog.Builder()
            .setTitle("是否解绑")
            .setPositiveButton("确认解绑", View.OnClickListener {
                unbindDevice(brand)
            })
            .setNegativeButton("取消", View.OnClickListener {
            })
            .show(childFragmentManager)
    }

    private fun unbindDevice(brand: Brand) {
        getIotApi()?.unbindDevice(brand.id)?.enqueue(object : Callback<Message> {
            override fun onResponse(call: Call<Message>, response: Response<Message>) {
                if (response.isSuccessful) {
                    needSync = true
                    if (isAdded && context != null) {
                        Toast.makeText(context, "解绑成功", Toast.LENGTH_SHORT).show()
                        getIotList()
                    }
                } else {
                    Toast.makeText(context, "解绑失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Message>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        view?.postDelayed(200) {
            getIotList()
        }
    }

    override fun onBackPressedSupport(): Boolean {
        if (needSync) {
            setFragmentResult(0, bundleOf(Pair("shouldSync", true)))
        }
        return super.onBackPressedSupport()
    }

    private fun getIotList() {
        getIotApi()?.getIots()?.enqueue(object : Callback<ArrayList<Brand>> {
            override fun onResponse(
                call: Call<ArrayList<Brand>>,
                response: Response<ArrayList<Brand>>
            ) {
                hideLoading()
                if (response.isSuccessful) {
                    errorContainer.isVisible = false
                    recyclerView.isVisible = true
                    val items = response.body()
                    if (items != null) {
                        setupAdapter(items)
                    }
                } else {
                    errorContainer.isVisible = true
                    recyclerView.isVisible = false
                }
            }

            override fun onFailure(call: Call<ArrayList<Brand>>, t: Throwable) {
                t.printStackTrace()
                hideLoading()
                errorContainer.isVisible = true
                recyclerView.isVisible = false
            }
        })
    }

    private inner class BrandAdapter(
        var items: ArrayList<Brand>,
        val onItemClickListener: (Brand) -> Unit
    ) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == 0) {
                val view = createHeaderView(parent.context)
                HeaderHolder(view)
            } else {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_device_brand, parent, false)
                BrandHolder(view)
            }
        }

        private fun createHeaderView(context: Context): TextView {
            val view = TextView(context)
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            view.layoutParams = layoutParams
            layoutParams.bottomMargin = 16.dp2Px()
            layoutParams.marginStart = 8.dp2Px()
            view.textSize = 24f
            view.text = "选择以下厂商进行绑定，以添加设备"
            view.setTextColor(Color.parseColor("#262626"))
            return view
        }

        override fun getItemCount(): Int {
            return items.size + 1
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == 0) {
                0
            } else {
                1
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is BrandHolder) {
                val item = items[position - 1]
                Glide.with(holder.itemView.context)
                    .load(item.iconUrl)
                    .into(holder.ivBrandIcon)
                holder.tvBrand.text = item.name
                if (item.authState != null) {
                    holder.tvAuthState.isVisible = true
                    if (!item.authState.invalid) {
                        holder.tvAuthState.text = "已绑定"
                    } else {
                        holder.tvAuthState.text = "已失效"
                    }
                } else {
                    holder.tvAuthState.isVisible = false
                }

                holder.clickable.setOnClickListener {
                    onItemClickListener.invoke(item)
                }
            }
        }

        inner class BrandHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivBrandIcon = itemView.findViewById<RoundedImageView>(R.id.iv_brand_icon)
            val tvBrand = itemView.findViewById<TextView>(R.id.tv_brand)
            val tvAuthState = itemView.findViewById<TextView>(R.id.tv_auth_state)
            val clickable = itemView.findViewById<View>(R.id.clickable)
        }

        inner class HeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }

    private fun getIotApi(): IotApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(IotApi::class.java)
        } else {
            null
        }
    }
}