package io.agora.scene.show.widget.link

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.agora.scene.base.component.BaseFragment
import io.agora.scene.show.databinding.ShowLiveLinkInvitationMessageListBinding
import io.agora.scene.show.service.ShowUser

/**
 * Live link invitation fragment
 *
 * @constructor Create empty Live link invitation fragment
 */
class LiveLinkInvitationFragment : BaseFragment() {
    /**
     * M binding
     */
    private var mBinding : ShowLiveLinkInvitationMessageListBinding? = null

    /**
     * Binding
     */
    private val binding get() = mBinding!!

    /**
     * Link invitation view adapter
     */
    private val linkInvitationViewAdapter : LiveLinkInvitationViewAdapter = LiveLinkInvitationViewAdapter()

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
        linkInvitationViewAdapter.setClickListener(object : LiveLinkInvitationViewAdapter.OnClickListener {
            override fun onClick(userItem: ShowUser, position: Int) {
                // 主播发起邀请
                mListener?.onInviteMicSeatItemChosen(userItem)
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
        mBinding = ShowLiveLinkInvitationMessageListBinding.inflate(LayoutInflater.from(context))
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
        binding.linkInvitationList.adapter = linkInvitationViewAdapter
        binding.smartRefreshLayout.setOnRefreshListener {
            mListener?.onRequestRefreshing()
        }
        binding.smartRefreshLayout.autoRefresh()
    }

    /**
     * Set seat invitation list
     *
     * @param list
     */
    fun setSeatInvitationList(list : List<ShowUser>) {
        if (mBinding == null) return
        if (list.isEmpty()) {
            binding.linkRequestListEmptyImg.visibility = View.VISIBLE
            binding.linkRequestListEmpty.visibility = View.VISIBLE
        } else {
            binding.linkRequestListEmptyImg.visibility = View.GONE
            binding.linkRequestListEmpty.visibility = View.GONE
        }
        linkInvitationViewAdapter.resetAll(list)
        binding.smartRefreshLayout.finishRefresh()
    }

    /**
     * Set seat invitation item status
     *
     * @param user
     */
    fun setSeatInvitationItemStatus(user: ShowUser) {
        val itemCount: Int = linkInvitationViewAdapter.itemCount
        for (i in 0 until itemCount) {
            linkInvitationViewAdapter.getItem(i)?.let {
                if (it.userId == user.userId) {
                    linkInvitationViewAdapter.replace(i, ShowUser(
                        it.userId,
                        it.avatar,
                        it.userName,
                        user.status
                    ))
                    linkInvitationViewAdapter.notifyItemChanged(i)
                    return
                }
            }
        }
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
         * On invite mic seat item chosen
         *
         * @param userItem
         */
        fun onInviteMicSeatItemChosen(userItem: ShowUser)

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
    }
}