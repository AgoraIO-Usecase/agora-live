package io.agora.scene.show.widget.pk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.agora.scene.base.component.BaseFragment
import io.agora.scene.show.R
import io.agora.scene.show.databinding.ShowLivePkRequestMessageListBinding
import io.agora.scene.show.service.ShowInteractionInfo
import io.agora.scene.show.service.ShowInteractionStatus

/**
 * Live p k request message fragment
 *
 * @constructor Create empty Live p k request message fragment
 */
class LivePKRequestMessageFragment : BaseFragment() {
    /**
     * M binding
     */
    private var mBinding: ShowLivePkRequestMessageListBinding? = null

    /**
     * Binding
     */
    private val binding get() = mBinding!!

    /**
     * M listener
     */
    private lateinit var mListener: Listener

    /**
     * Link p k view adapter
     */
    private val linkPKViewAdapter: LivePKViewAdapter = LivePKViewAdapter()

    /**
     * On create
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        linkPKViewAdapter.setClickListener(object : LivePKViewAdapter.OnClickListener {
            override fun onClick(view: View, roomItem: LiveRoomConfig, position: Int) {
                mListener.onAcceptMicSeatItemChosen(roomItem, view)
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
        mBinding = ShowLivePkRequestMessageListBinding.inflate(layoutInflater)
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
        binding.onlineBoardcasterList.adapter = linkPKViewAdapter
        binding.smartRefreshLayout.setOnRefreshListener {
            mListener.onRequestRefreshing()
        }
        binding.iBtnStopPK.setOnClickListener {
            mListener.onStopPKingChosen(it)
        }
        binding.smartRefreshLayout.autoRefresh()
    }

    /**
     * Set online broadcaster list
     *
     * @param interactionInfo
     * @param roomList
     */
    fun setOnlineBroadcasterList(interactionInfo: ShowInteractionInfo?, roomList: List<LiveRoomConfig>) {
        if (mBinding == null) return
        if (roomList.isEmpty()) {
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
        linkPKViewAdapter.resetAll(roomList)
        binding.smartRefreshLayout.finishRefresh()
    }

    /**
     * Set p k invitation item status
     *
     * @param userName
     * @param status
     */
    fun setPKInvitationItemStatus(userName: String, status: Int?) {
        if (mBinding == null) return
        updateUI(userName, status)
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
         * @param roomItem
         */
        fun onAcceptMicSeatItemChosen(roomItem: LiveRoomConfig, view: View)

        /**
         * On request refreshing
         *
         */
        fun onRequestRefreshing()

        /**
         * On stop p king chosen
         *
         */
        fun onStopPKingChosen(view: View)
    }

    /**
     * Update u i
     *
     * @param userName
     * @param status
     */
    private fun updateUI(userName: String, status: Int?) {
        if (status == ShowInteractionStatus.pking) {
            binding.textPking.isVisible = true
            binding.iBtnStopPK.isVisible = true
            if (isAdded) {
                binding.textPking.text = getString(R.string.show_pk_to, userName)
            }
        } else if (status == null) {
            binding.iBtnStopPK.isVisible = false
            binding.textPking.isVisible = false
        }
    }
}