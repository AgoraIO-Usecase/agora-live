package io.agora.scene.show.widget.link

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.agora.scene.show.R
import io.agora.scene.show.databinding.ShowLiveLinkDialogBinding
import io.agora.scene.show.service.ShowInteractionInfo
import io.agora.scene.show.service.ShowMicSeatApply
import io.agora.scene.show.service.ShowUser

/**
 * Live link dialog
 *
 * @constructor Create empty Live link dialog
 */
class LiveLinkDialog : BottomSheetDialogFragment() {
    /**
     * M binding
     */
    private var mBinding : ShowLiveLinkDialogBinding? = null

    /**
     * Binding
     */
    private val binding get() = mBinding!!

    /**
     * Link dialog listener
     */
    private var linkDialogListener: OnLinkDialogActionListener? = null

    /**
     * Link fragment
     */
    private val linkFragment: LiveLinkRequestFragment = LiveLinkRequestFragment()

    /**
     * Online user fragment
     */
    private val onlineUserFragment: LiveLinkInvitationFragment = LiveLinkInvitationFragment()

    /**
     * Audience fragment
     */
    private val audienceFragment: LiveLinkAudienceFragment = LiveLinkAudienceFragment()

    /**
     * Is room owner
     */
    private var isRoomOwner: Boolean = true

    /**
     * Set is room owner
     *
     * @param value
     */
    fun setIsRoomOwner(value: Boolean) {
        isRoomOwner = value
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
        mBinding = ShowLiveLinkDialogBinding.inflate(LayoutInflater.from(context))
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

        WindowCompat.setDecorFitsSystemWindows(requireDialog().window!!, false)
        requireDialog().setOnShowListener {
            (view.parent as ViewGroup).setBackgroundColor(
                Color.TRANSPARENT
            )
        }
        ViewCompat.setOnApplyWindowInsetsListener(
            requireDialog().window!!.decorView
        ) { _: View?, insets: WindowInsetsCompat ->
            val inset =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.pager.setPadding(0, 0, 0, inset.bottom)
            WindowInsetsCompat.CONSUMED
        }

        binding.rBtnRequestMessage.isChecked = true
        binding.pager.getChildAt(0).overScrollMode = View.OVER_SCROLL_NEVER

        if (isRoomOwner) {
            linkFragment.setListener(object: LiveLinkRequestFragment.Listener {
                override fun onAcceptMicSeatItemChosen(view: View, seatApply: ShowMicSeatApply, position: Int) {
                    linkDialogListener?.onAcceptMicSeatApplyChosen(this@LiveLinkDialog, seatApply, view)
                }

                override fun onRequestRefreshing() {
                    linkDialogListener?.onRequestMessageRefreshing(this@LiveLinkDialog)
                }

                override fun onStopLinkingChosen(view: View) {
                    linkDialogListener?.onStopLinkingChosen(this@LiveLinkDialog, view)
                }
            })

            onlineUserFragment.setListener(object: LiveLinkInvitationFragment.Listener {
                override fun onInviteMicSeatItemChosen(view: View, userItem: ShowUser) {
                    linkDialogListener?.onOnlineAudienceInvitation(this@LiveLinkDialog, userItem, view)
                }

                override fun onRequestRefreshing() {
                    linkDialogListener?.onOnlineAudienceRefreshing(this@LiveLinkDialog)
                }

                override fun onStopLinkingChosen(view: View) {
                    linkDialogListener?.onStopLinkingChosen(this@LiveLinkDialog, view)
                }
            })

            val fragments = arrayOf<Fragment>(linkFragment, onlineUserFragment)
            binding.pager.isSaveEnabled = false
            binding.pager.adapter =
                object : FragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle) {
                    override fun getItemCount(): Int {
                        return fragments.size
                    }

                    override fun createFragment(position: Int): Fragment {
                        return fragments[position]
                    }
                }
            binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    if (position == 0) {
                        binding.rBtnRequestMessage.isChecked = true
                        binding.rBtnRequestMessage.setTypeface(null, Typeface.BOLD)
                        binding.rBtnOnlineUser.setTypeface(null, Typeface.NORMAL)
                    } else {
                        binding.rBtnOnlineUser.isChecked = true
                        binding.rBtnOnlineUser.setTypeface(null, Typeface.BOLD)
                        binding.rBtnRequestMessage.setTypeface(null, Typeface.NORMAL)
                    }
                }
            })
        } else {
            binding.radioGroup.visibility = View.INVISIBLE
            binding.rBtnRequestText.isVisible = true
            audienceFragment.setListener(object: LiveLinkAudienceFragment.Listener {
                override fun onRequestRefreshing() {
                    linkDialogListener?.onRequestMessageRefreshing(this@LiveLinkDialog)
                }

                override fun onStopLinkingChosen(view: View) {
                    linkDialogListener?.onStopLinkingChosen(this@LiveLinkDialog, view)
                }

                override fun onStopApplyingChosen(view: View) {
                    linkDialogListener?.onStopApplyingChosen(this@LiveLinkDialog, view)
                }
            })

            binding.pager.isSaveEnabled = false
            binding.pager.adapter =
                object : FragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle) {
                    override fun getItemCount(): Int {
                        return 1
                    }

                    override fun createFragment(position: Int): Fragment {
                        return audienceFragment
                    }
                }
        }
    }

    /**
     * On start
     *
     */
    override fun onStart() {
        super.onStart()
        binding.radioGroup.setOnCheckedChangeListener { _, i ->
            if (i === R.id.rBtnRequestMessage) {
                binding.pager.currentItem = 0
            } else if (i === R.id.rBtnOnlineUser) {
                binding.pager.currentItem = 1
            }
        }
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
     * Set link dialog action listener
     *
     * @param listener
     */
    fun setLinkDialogActionListener(listener : OnLinkDialogActionListener) {
        linkDialogListener = listener
    }

    /**
     * Set on seat status
     *
     * @param userName
     * @param status
     */
    fun setOnSeatStatus(userName: String, status: Int?) {
        if (isRoomOwner) {
            linkFragment.setOnSeatStatus(userName, status)
        } else {
            audienceFragment.setOnSeatStatus(userName, status)
        }
    }

    /**
     * Set seat apply list
     *
     * @param interactionInfo
     * @param list
     */
    fun setSeatApplyList(interactionInfo: ShowInteractionInfo?, list : List<ShowMicSeatApply>) {
        if (isRoomOwner) {
            linkFragment.setSeatApplyList(interactionInfo, list)
        } else {
            audienceFragment.setSeatApplyList(interactionInfo, list)
        }
    }

    /**
     * Set on apply success
     *
     */
    fun setOnApplySuccess() {
        audienceFragment.setOnApplySuccess()
    }


    /**
     * Set seat invitation list
     *
     * @param userList
     */
    fun setSeatInvitationList(userList : List<ShowUser>) {
        onlineUserFragment.setSeatInvitationList(userList)
    }

    /**
     * Set seat invitation item status
     *
     * @param user
     */
    fun setSeatInvitationItemStatus(user: ShowUser) {
        onlineUserFragment.setSeatInvitationItemStatus(user)
    }
}