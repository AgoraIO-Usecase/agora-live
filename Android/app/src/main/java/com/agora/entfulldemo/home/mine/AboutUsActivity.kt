package com.agora.entfulldemo.home.mine

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.agora.entfulldemo.BuildConfig
import com.agora.entfulldemo.R
import com.agora.entfulldemo.databinding.AppAboutInfoItemBinding
import com.agora.entfulldemo.databinding.AppAboutSceneItemBinding
import com.agora.entfulldemo.databinding.AppActivityAboutUsBinding
import com.agora.entfulldemo.webview.WebViewActivity
import io.agora.rtc2.RtcEngine
import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.base.component.BaseViewBindingActivity
import io.agora.scene.base.component.OnButtonClickListener
import io.agora.scene.base.utils.ToastUtils
import io.agora.scene.widget.dialog.CommonDialog

/**
 * About us activity
 *
 * @constructor Create empty About us activity
 */
class AboutUsActivity : BaseViewBindingActivity<AppActivityAboutUsBinding>() {

    /**
     * Service phone
     */
    private val servicePhone = "408.879.5885"

    /**
     * Web site
     */
    private val webSite = "https://www.agora.io/en/"


    /**
     * Counts
     */
    private var counts = 0

    /**
     * Debug mode open time
     */
    private val debugModeOpenTime: Long = 2000

    /**
     * Begin time
     */
    private var beginTime: Long = 0

    /**
     * Adapter
     */
    private val adapter = AboutUsAdapter()

    private val debugClickCount = 5

    /**
     * Get view binding
     *
     * @param inflater
     * @return
     */
    override fun getViewBinding(inflater: LayoutInflater): AppActivityAboutUsBinding {
        return AppActivityAboutUsBinding.inflate(inflater)
    }

    /**
     * On create
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.rvAboutUs.adapter = adapter
        setupAppInfo()
        setupDebugMode()
        setupClickWebAction()
        setupClickPhoneAction()
    }

    /**
     * Setup app info
     *
     */// 设置语聊App的信息
    private fun setupAppInfo() {
        adapter.scenes = mutableListOf<SceneInfo>()
        if (BuildConfig.VERSION_NAME.isNotEmpty()) {
            adapter.appInfo = AppInfo(
                this.getString(R.string.app_about_name),
                "Version: 20240614-" + BuildConfig.VERSION_NAME + "-" + RtcEngine.getSdkVersion(),
                servicePhone,
                webSite
            )
        }
    }

    /**
     * Setup click web action
     *
     */
    private fun setupClickWebAction() {
        adapter.onClickWebSiteListener = {
            WebViewActivity.launch(this, webSite)
        }
    }

    /**
     * Setup click phone action
     *
     */
    private fun setupClickPhoneAction() {
        adapter.onClickPhoneListener = {
            val dialog = CallPhoneDialog().apply {
                arguments = Bundle().apply {
                    putString(CallPhoneDialog.KEY_PHONE, servicePhone)
                }
            }
            dialog.onClickCallPhone = {
                val intent = Intent(Intent.ACTION_DIAL)
                val uri = Uri.parse("tel:$servicePhone")
                intent.setData(uri)
                startActivity(intent)
            }
            dialog.show(supportFragmentManager, "CallPhoneDialog")
        }
    }

    /**
     * Setup debug mode
     *
     */
    private fun setupDebugMode() {
        binding.tvDebugMode.visibility = View.INVISIBLE
        adapter.onClickVersionListener = {
            if (counts == 0 || System.currentTimeMillis() - beginTime > debugModeOpenTime) {
                beginTime = System.currentTimeMillis()
                counts = 0
            }
            counts++
            if (counts > debugClickCount) {
                counts = 0
                binding.tvDebugMode.visibility = View.VISIBLE
                AgoraApplication.the().enableDebugMode(true)
                ToastUtils.showToast(R.string.app_debug_open)
            }
        }
        binding.tvDebugMode.setOnClickListener {
            showDebugModeCloseDialog()
        }
        if (AgoraApplication.the().isDebugModeOpen) {
            binding.tvDebugMode.visibility = View.VISIBLE
        }
    }

    /**
     * Show debug mode close dialog
     *
     */
    private fun showDebugModeCloseDialog() {
        val dialog = CommonDialog(this)
        dialog.setDialogTitle(getString(R.string.app_exit_debug))
        dialog.setDescText(getString(R.string.app_exit_debug_tip))
        dialog.setDialogBtnText(
            getString(R.string.cancel),
            getString(R.string.app_exit)
        )
        dialog.onButtonClickListener = object : OnButtonClickListener {
            override fun onLeftButtonClick() {
                // do nothing
            }
            override fun onRightButtonClick() {
                counts = 0
                binding.tvDebugMode.visibility = View.GONE
                AgoraApplication.the().enableDebugMode(false)
                ToastUtils.showToast(R.string.app_debug_off)
            }
        }
        dialog.show()
    }
}

/**
 * App info
 *
 * @property name
 * @property version
 * @property servicePhone
 * @property webSite
 * @constructor Create empty App info
 */
private data class AppInfo(
    val name: String,
    val version: String,
    val servicePhone: String,
    val webSite: String
)

/**
 * Scene info
 *
 * @property name
 * @property version
 * @constructor Create empty Scene info
 */
private data class SceneInfo(
    val name: String,
    val version: String
)

/**
 * About us adapter
 *
 * @constructor Create empty About us adapter
 */
private class AboutUsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * View Type App Info
     */
    private val viewTypeAppInfo = 0

    /**
     * View Type Scene Info
     */
    private val viewTypeSceneInfo = 1

    /**
     * App info
     */
    var appInfo: AppInfo? = null

    /**
     * Scenes
     */
    var scenes = mutableListOf<SceneInfo>()

    /**
     * On click phone listener
     */
    var onClickPhoneListener: (() -> Unit)? = null

    /**
     * On click web site listener
     */
    var onClickWebSiteListener: (() -> Unit)? = null

    /**
     * On click version listener
     */
    var onClickVersionListener: (() -> Unit)? = null

    /**
     * On create view holder
     *
     * @param parent
     * @param viewType
     * @return
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == viewTypeAppInfo) {
            val binding = AppAboutInfoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            AppInfoViewHolder(binding, binding.root)
        } else {
            val binding = AppAboutSceneItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SceneInfoViewHolder(binding, binding.root)
        }
    }

    /**
     * On bind view holder
     *
     * @param holder
     * @param position
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == viewTypeAppInfo) {
            val current = holder as AppInfoViewHolder
            appInfo?.let {
                current.binding.tvAppName.text = it.name
                current.binding.tvVersion.text = it.version
                current.binding.tvServiceNumber.text = it.servicePhone
                current.binding.tvHomeWebSite.text = it.webSite
            }
            current.binding.tvSceneSubTitle.visibility = if (scenes.size > 1) View.VISIBLE else View.INVISIBLE
            current.binding.tvVersion.setOnClickListener {
                onClickVersionListener?.invoke()
            }
            current.binding.vServicePhone.setOnClickListener {
                onClickPhoneListener?.invoke()
            }
            current.binding.vHomeWebPage.setOnClickListener {
                onClickWebSiteListener?.invoke()
            }
        } else if (holder.itemViewType == viewTypeSceneInfo) {
            val current = holder as SceneInfoViewHolder
            val index = position - 1
            val model = scenes[index]
            current.binding.tvTitle.text = model.name
            current.binding.tvVersion.text = model.version
        }
    }

    /**
     * Get item view type
     *
     * @param position
     * @return
     */
    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            viewTypeAppInfo
        } else {
            viewTypeSceneInfo
        }
    }

    /**
     * Get item count
     *
     * @return
     */
    override fun getItemCount(): Int {
        return scenes.size + 1
    }

    /**
     * App info view holder
     *
     * @property binding
     * @constructor
     *
     * @param itemView
     */
    inner class AppInfoViewHolder(
        val binding: AppAboutInfoItemBinding,
        itemView: View
    ) : RecyclerView.ViewHolder(itemView)

    /**
     * Scene info view holder
     *
     * @property binding
     * @constructor
     *
     * @param itemView
     */
    inner class SceneInfoViewHolder(
        val binding: AppAboutSceneItemBinding,
        itemView: View
    ) : RecyclerView.ViewHolder(itemView)
}