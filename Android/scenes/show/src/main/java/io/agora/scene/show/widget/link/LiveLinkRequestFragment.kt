package io.agora.scene.show.widget.link

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.agora.scene.base.component.BaseFragment
import io.agora.scene.show.R
import io.agora.scene.show.databinding.ShowLiveLinkRequestMessageListBinding
import io.agora.scene.show.service.ShowInteractionInfo
import io.agora.scene.show.service.ShowInteractionStatus
import io.agora.scene.show.service.ShowMicSeatApply

/**
 * Live link request fragment
 *
 * @constructor Create empty Live link request fragment
 */
class LiveLinkRequestFragment : BaseFragment() {
    /**
     * M binding
     */
    private var mBinding: ShowLiveLinkRequestMessageListBinding? = null

    /**
     * Binding
     */
    private val binding get() = mBinding!!

    /**
     * Link request view adapter
     */
    private val linkRequestViewAdapter: LiveLinkRequestViewAdapter = LiveLinkRequestViewAdapter()

    /**
     * M listener
     */
    private var mListener: Listener? = null

    /**
     * On create
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        linkRequestViewAdapter.setClickListener(object : LiveLinkRequestViewAdapter.OnClickListener {
            override fun onClick(view: View, seatApply: ShowMicSeatApply, position: Int) {
                mListener?.onAcceptMicSeatItemChosen(view, seatApply, position)
            }
        })
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
        mBinding = ShowLiveLinkRequestMessageListBinding.inflate(LayoutInflater.from(context))
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
        binding.linkRequestList.adapter = linkRequestViewAdapter
        binding.iBtnStopLink.setOnClickListener {
            mListener?.onStopLinkingChosen(it)
        }
        binding.smartRefreshLayout.setOnRefreshListener {
            mListener?.onRequestRefreshing()
        }
        binding.smartRefreshLayout.autoRefresh()
    }

    /**
     * On destroy view
     *
     */
    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }

    /**
     * Set on seat status
     *
     * @param userName
     * @param status
     */
    fun setOnSeatStatus(userName: String, status: Int?) {
        if (mBinding == null) return
        if (status == null) {
            binding.iBtnStopLink.isVisible = false
            binding.textLinking.isVisible = false
        } else if (status == ShowInteractionStatus.linking) {
            binding.textLinking.isVisible = true
            binding.iBtnStopLink.isVisible = true
            binding.textLinking.text = getString(R.string.show_link_to, userName)
        }
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
        }
        if (interactionInfo == null) {
            updateUI("", null)
        } else {
            updateUI(interactionInfo.userName, interactionInfo.interactStatus)
        }
        linkRequestViewAdapter.resetAll(list)
        binding.smartRefreshLayout.finishRefresh()
    }

    /**
     * Set listener
     *
     * @param listener
     */
    fun setListener(listener: Listener) {
        mListener = listener
    }

    /**
     * Listener
     *
     * @constructor Create empty Listener
     */
    interface Listener {
        /**
         * On accept mic seat item chosen
         *
         * @param seatApply
         * @param position
         */
        fun onAcceptMicSeatItemChosen(view: View, seatApply: ShowMicSeatApply, position: Int)

        /**
         * On request refreshing
         *
         */
        fun onRequestRefreshing()

        /**
         * On stop linking chosen
         *
         */
        fun onStopLinkingChosen(view: View)
    }

    /**
     * Update u i
     *
     * @param userName
     * @param status
     */
    private fun updateUI(userName: String, status: Int?) {
        if (status == ShowInteractionStatus.linking) {
            binding.textLinking.isVisible = true
            binding.iBtnStopLink.isVisible = true
            binding.textLinking.text = getString(R.string.show_link_to, userName)
        } else if (status == null) {
            binding.iBtnStopLink.isVisible = false
            binding.textLinking.isVisible = false
        }
    }
}