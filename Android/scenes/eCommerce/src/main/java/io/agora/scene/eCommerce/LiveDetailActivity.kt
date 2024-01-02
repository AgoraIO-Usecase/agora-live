package io.agora.scene.eCommerce

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import io.agora.scene.base.TokenGenerator
import io.agora.scene.base.component.BaseViewBindingActivity
import io.agora.scene.eCommerce.databinding.CommerceLiveDetailActivityBinding
import io.agora.scene.eCommerce.service.ROOM_AVAILABLE_DURATION
import io.agora.scene.eCommerce.service.ShowRoomDetailModel
import io.agora.scene.eCommerce.utils.RunnableWithDenied
import io.agora.scene.widget.dialog.PermissionLeakDialog
import io.agora.scene.widget.utils.StatusBarUtil


/**
 * Live detail activity
 *
 * @constructor Create empty Live detail activity
 */
class LiveDetailActivity : BaseViewBindingActivity<CommerceLiveDetailActivityBinding>(),
    LiveDetailFragment.OnMeLinkingListener {
    /**
     * Tag
     */
    private val TAG = "LiveDetailActivity"

    companion object {
        private const val EXTRA_ROOM_DETAIL_INFO_LIST = "roomDetailInfoList"
        private const val EXTRA_ROOM_DETAIL_INFO_LIST_SELECTED_INDEX =
            "roomDetailInfoListSelectedIndex"
        private const val EXTRA_ROOM_DETAIL_INFO_LIST_SCROLLABLE = "roomDetailInfoListScrollable"


        /**
         * Launch
         *
         * @param context
         * @param roomDetail
         */
        fun launch(context: Context, roomDetail: ShowRoomDetailModel) {
            launch(
                context,
                arrayListOf(roomDetail),
                0,
                false
            )
        }

        /**
         * Launch
         *
         * @param context
         * @param roomDetail
         * @param selectedIndex
         * @param scrollable
         */
        fun launch(
            context: Context,
            roomDetail: ArrayList<ShowRoomDetailModel>,
            selectedIndex: Int,
            scrollable: Boolean
        ) {
            context.startActivity(Intent(context, LiveDetailActivity::class.java).apply {
                putExtra(EXTRA_ROOM_DETAIL_INFO_LIST, roomDetail)
                putExtra(EXTRA_ROOM_DETAIL_INFO_LIST_SELECTED_INDEX, selectedIndex)
                putExtra(EXTRA_ROOM_DETAIL_INFO_LIST_SCROLLABLE, scrollable)
            })
        }
    }

    /**
     * M room info list
     */
    private val mRoomInfoList by lazy {
        intent.getParcelableArrayListExtra<ShowRoomDetailModel>(
            EXTRA_ROOM_DETAIL_INFO_LIST
        )!!
    }

    /**
     * M scrollable
     */
    private val mScrollable by lazy {
        intent.getBooleanExtra(
            EXTRA_ROOM_DETAIL_INFO_LIST_SCROLLABLE,
            true
        )
    }

    /**
     * Position None
     */
    private val POSITION_NONE = -1

    /**
     * Vp fragments
     */
    private val vpFragments = SparseArray<LiveDetailFragment>()

    /**
     * Curr load position
     */
    private var currLoadPosition = POSITION_NONE

    /**
     * Toggle video run
     */
    private var toggleVideoRun: RunnableWithDenied? = null

    /**
     * Toggle audio run
     */
    private var toggleAudioRun: Runnable? = null

    /**
     * Get permissions
     *
     */
    override fun getPermissions() {
        if (toggleVideoRun != null) {
            toggleVideoRun?.run()
            toggleVideoRun = null
        }
        if (toggleAudioRun != null) {
            toggleAudioRun?.run()
            toggleAudioRun = null
        }
    }


    /**
     * Toggle self video
     *
     * @param isOpen
     * @param callback
     * @receiver
     */
    fun toggleSelfVideo(isOpen: Boolean, callback : (result:Boolean) -> Unit) {
        if (isOpen) {
            toggleVideoRun = object :RunnableWithDenied(){
                override fun onDenied() {
                    callback.invoke(false)
                }

                override fun run() {
                    callback.invoke(true)
                }
            }
            requestCameraPermission(true)
        } else {
            callback.invoke(true)
        }
    }

    /**
     * Toggle self audio
     *
     * @param isOpen
     * @param callback
     * @receiver
     */
    fun toggleSelfAudio(isOpen: Boolean, callback : () -> Unit) {
        if (isOpen) {
            toggleAudioRun = Runnable {
                callback.invoke()
            }
            requestRecordPermission(true)
        } else {
            callback.invoke()
        }
    }

    /**
     * On permission dined
     *
     * @param permission
     */
    override fun onPermissionDined(permission: String?) {
        if (toggleVideoRun != null && permission == Manifest.permission.CAMERA) {
            toggleVideoRun?.onDenied()
        }
        PermissionLeakDialog(this).show(permission, { getPermissions() }
        ) { launchAppSetting(permission) }
    }

    /**
     * On me linking
     *
     * @param isLinking
     */
    override fun onMeLinking(isLinking: Boolean) {
        binding.viewPager2.isUserInputEnabled = !isLinking
    }

    /**
     * Get view binding
     *
     * @param inflater
     * @return
     */
    override fun getViewBinding(inflater: LayoutInflater): CommerceLiveDetailActivityBinding {
        return  CommerceLiveDetailActivityBinding.inflate(inflater)
    }

    /**
     * Init view
     *
     * @param savedInstanceState
     */
    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        StatusBarUtil.hideStatusBar(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.viewPager2) { v: View?, insets: WindowInsetsCompat ->
            val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.viewPager2.setPaddingRelative(inset.left, 0, inset.right, inset.bottom)
            WindowInsetsCompat.CONSUMED
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val selectedRoomIndex = intent.getIntExtra(EXTRA_ROOM_DETAIL_INFO_LIST_SELECTED_INDEX, 0)

        TokenGenerator.expireSecond =
            ROOM_AVAILABLE_DURATION / 1000 + 10 // 20min + 10s，加10s防止临界条件下报token无效

        val preloadCount = 3
        binding.viewPager2.offscreenPageLimit = preloadCount - 2
        val fragmentAdapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = if (mScrollable) Int.MAX_VALUE else 1

            override fun createFragment(position: Int): Fragment {
                val roomInfo = if (mScrollable) {
                    mRoomInfoList[position % mRoomInfoList.size]
                } else {
                    mRoomInfoList[selectedRoomIndex]
                }
                return LiveDetailFragment.Companion.newInstance(
                    roomInfo
                ).apply {
                    vpFragments.put(position, this)
                    if (position == binding.viewPager2.currentItem) {
                        startLoadPageSafely()
                    }
                }
            }
        }
        binding.viewPager2.adapter = fragmentAdapter
        binding.viewPager2.isUserInputEnabled = mScrollable
        if (mScrollable) {
            binding.viewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {

                private val PRE_LOAD_OFFSET = 0.01f

                private var preLoadPosition = POSITION_NONE
                private var lastOffset = 0f
                private var scrollStatus: Int = ViewPager2.SCROLL_STATE_IDLE

                //private var hasPageSelected = false

                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    Log.d(TAG, "PageChange onPageScrollStateChanged state=$state")
                    when(state){
                        ViewPager2.SCROLL_STATE_SETTLING -> binding.viewPager2.isUserInputEnabled = false
                        ViewPager2.SCROLL_STATE_IDLE -> {
                            binding.viewPager2.isUserInputEnabled = true
                            //if(!hasPageSelected){
                                if(preLoadPosition != POSITION_NONE){
                                    vpFragments[preLoadPosition]?.stopLoadPage(true)
                                }
                                vpFragments[currLoadPosition]?.reLoadPage()
                                preLoadPosition = POSITION_NONE
                                lastOffset = 0f
                            //}
                            //hasPageSelected = false
                        }
                    }
                    scrollStatus = state
                }

                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                    Log.d(TAG, "PageChange onPageScrolled positionOffset=$positionOffset")
                    if (scrollStatus == ViewPager2.SCROLL_STATE_DRAGGING) {
                        if (lastOffset > 0f) {
                            val isMoveUp = (positionOffset - lastOffset) > 0
                            if (isMoveUp && positionOffset >= PRE_LOAD_OFFSET && preLoadPosition == POSITION_NONE) {
                                preLoadPosition = currLoadPosition + 1
                                vpFragments[preLoadPosition]?.startLoadPageSafely()
                            } else if (!isMoveUp && positionOffset <= (1 - PRE_LOAD_OFFSET) && preLoadPosition == POSITION_NONE) {
                                preLoadPosition = currLoadPosition - 1
                                vpFragments[preLoadPosition]?.startLoadPageSafely()
                            }
                        }
                        lastOffset = positionOffset
                    }
                }

                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    Log.d(
                        TAG,
                        "PageChange onPageSelected position=$position, currLoadPosition=$currLoadPosition, preLoadPosition=$preLoadPosition"
                    )
                    if (currLoadPosition != POSITION_NONE) {
                        if (preLoadPosition != POSITION_NONE) {
                            if (position == preLoadPosition) {
                                vpFragments[currLoadPosition]?.stopLoadPage(true)
                            } else {
                                vpFragments[preLoadPosition]?.stopLoadPage(true)
                                vpFragments[currLoadPosition]?.reLoadPage()
                            }
                        }
                        if (currLoadPosition != position) {
                            vpFragments[currLoadPosition]?.stopLoadPage(true)
                            vpFragments[position]?.startLoadPageSafely()
                        }
                    }
                    currLoadPosition = position
                    preLoadPosition = POSITION_NONE
                    lastOffset = 0f
                    //hasPageSelected = true
                }

            })
            binding.viewPager2.setCurrentItem(
                Int.MAX_VALUE / 2 - Int.MAX_VALUE / 2 % mRoomInfoList.size + selectedRoomIndex,
                false
            )
        } else {
            currLoadPosition = 0
        }
    }

    /**
     * Finish
     *
     */
    override fun finish() {
        vpFragments[currLoadPosition]?.stopLoadPage(false)
        VideoSetting.resetBroadcastSetting()
        VideoSetting.resetAudienceSetting()
        TokenGenerator.expireSecond = -1
        RtcEngineInstance.cleanCache()
        super.finish()
    }

}