package io.agora.scene.show.widget.link

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.agora.scene.base.component.BaseFragment
import io.agora.scene.base.manager.UserManager
import io.agora.scene.show.R
import io.agora.scene.show.databinding.ShowLiveLinkAudienceBinding
import io.agora.scene.show.service.ShowInteractionInfo
import io.agora.scene.show.service.ShowInteractionStatus
import io.agora.scene.show.service.ShowMicSeatApply

/**
 * Live link audience fragment
 *
 * @constructor Create empty Live link audience fragment
 */
class LiveLinkAudienceFragment : BaseFragment() {
    /**
     * M binding
     */
    private var mBinding : ShowLiveLinkAudienceBinding? = null

    /**
     * Binding
     */
    private val binding get() = mBinding!!

    /**
     * Link request view adapter
     */
    private val linkRequestViewAdapter : LiveLinkRequestViewAdapter = LiveLinkRequestViewAdapter()

    /**
     * M listener
     */
    private var mListener : Listener? = null

    /**
     * On create
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        linkRequestViewAdapter.setIsRoomOwner(false)
    }

    /**
     * On create view
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mBinding = ShowLiveLinkAudienceBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    /**
     * On view created
     *
     * @param view
     * @param savedInstanceState
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textLinking.setText(R.string.show_can_apply)
        binding.linkRequestList.adapter = linkRequestViewAdapter
        binding.iBtnStopLink.setOnClickListener {
            // 观众停止连麦
            mListener?.onStopLinkingChosen()
        }
        binding.iBtnCancelApply.setOnClickListener {
            // 观众撤回申请
            mListener?.onStopApplyingChosen()
            binding.iBtnStopLink.isVisible = false
            binding.iBtnCancelApply.isVisible = false
        }
        binding.smartRefreshLayout.setOnRefreshListener {
            mListener?.onRequestRefreshing()
        }
        binding.smartRefreshLayout.autoRefresh()
    }

    /**
     * Set on seat status
     *
     * @param userName
     * @param status
     */
    fun setOnSeatStatus(userName: String, status: Int?) {
        if (mBinding == null) return
        if (status == ShowInteractionStatus.linking) {
            if (userName == UserManager.getInstance().user.name) {
                binding.iBtnCancelApply.isVisible = false
                binding.iBtnStopLink.isVisible = true
                binding.textLinking.setText(R.string.show_linking)
            }
        } else if (status == null) {
            binding.iBtnStopLink.isVisible = false
            binding.textLinking.setText(R.string.show_can_apply)
        }
    }

    /**
     * Set on apply success
     *
     */
    fun setOnApplySuccess() {
        if (mBinding == null) return
        binding.iBtnCancelApply.isVisible = true
    }

    /**
     * Set seat apply list
     *
     * @param interactionInfo
     * @param list
     */
    fun setSeatApplyList(interactionInfo: ShowInteractionInfo?, list: List<ShowMicSeatApply>) {
        if (mBinding == null) return
        if (list.isEmpty()) {
            binding.linkRequestListEmptyImg.visibility = View.VISIBLE
            binding.linkRequestListEmpty.visibility = View.VISIBLE
        } else {
            binding.linkRequestListEmptyImg.visibility = View.GONE
            binding.linkRequestListEmpty.visibility = View.GONE

            val apply = list.filter { it.userId == UserManager.getInstance().user.id.toString() }.getOrNull(0)
            if (apply != null) {
                setOnApplySuccess()
            }
        }

        if (interactionInfo != null && interactionInfo.interactStatus == ShowInteractionStatus.linking &&
            interactionInfo.userId == UserManager.getInstance().user.id.toString()
        ) {
            binding.iBtnCancelApply.isVisible = false
            binding.iBtnStopLink.isVisible = true
            binding.textLinking.setText(R.string.show_linking)
        }
        linkRequestViewAdapter.resetAll(list)
        binding.smartRefreshLayout.finishRefresh()
    }

    /**
     * Set listener
     *
     * @param listener
     */
    fun setListener(listener : Listener) {
        mListener = listener
    }

    /**
     * Listener
     *
     * @constructor Create empty Listener
     */
    interface Listener {
        /**
         * On request refreshing
         *
         */
        fun onRequestRefreshing()

        /**
         * On stop linking chosen
         *
         */
        fun onStopLinkingChosen()

        /**
         * On stop applying chosen
         *
         */
        fun onStopApplyingChosen()
    }
}