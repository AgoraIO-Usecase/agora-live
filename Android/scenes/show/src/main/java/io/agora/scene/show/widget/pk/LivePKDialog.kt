package io.agora.scene.show.widget.pk

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.agora.scene.show.databinding.ShowLivePkDialogBinding
import io.agora.scene.show.service.ShowInteractionInfo

/**
 * Live p k dialog
 *
 * @constructor Create empty Live p k dialog
 */
class LivePKDialog : BottomSheetDialogFragment() {
    /**
     * M binding
     */
    private var mBinding : ShowLivePkDialogBinding? = null

    /**
     * Binding
     */
    private val binding get() = mBinding!!

    /**
     * Pk dialog listener
     */
    private lateinit var pkDialogListener : OnPKDialogActionListener

    /**
     * Pk fragment
     */
    private val pkFragment : LivePKRequestMessageFragment = LivePKRequestMessageFragment()

    /**
     * On create view
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mBinding = ShowLivePkDialogBinding.inflate(layoutInflater)
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

        binding.pager.getChildAt(0).overScrollMode = View.OVER_SCROLL_NEVER

        pkFragment.setListener(object : LivePKRequestMessageFragment.Listener {
            override fun onAcceptMicSeatItemChosen(roomItem: LiveRoomConfig, view: View) {
                pkDialogListener.onInviteButtonChosen(this@LivePKDialog, roomItem, view)
            }

            override fun onRequestRefreshing() {
                pkDialogListener.onRequestMessageRefreshing(this@LivePKDialog)
            }

            override fun onStopPKingChosen(view: View) {
                pkDialogListener.onStopPKingChosen(this@LivePKDialog, view)
            }
        })

        binding.pager.isSaveEnabled = false
        binding.pager.adapter =
            object : FragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle) {
                override fun getItemCount(): Int {
                    return 1
                }

                override fun createFragment(position: Int): Fragment {
                    return pkFragment
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
     * Set p k dialog action listener
     *
     * @param listener
     */
    fun setPKDialogActionListener(listener : OnPKDialogActionListener) {
        pkDialogListener = listener
    }

    /**
     * Set online broadcaster list
     *
     * @param interactionInfo
     * @param roomList
     * @param invitationList
     */
    fun setOnlineBroadcasterList(interactionInfo: ShowInteractionInfo?, roomList : List<LiveRoomConfig>) {
        pkFragment.setOnlineBroadcasterList(interactionInfo, roomList)
    }

    /**
     * Set p k invitation item status
     *
     * @param userName
     * @param status
     */
    fun setPKInvitationItemStatus(userName: String, status: Int?) {
        pkFragment.setPKInvitationItemStatus(userName, status)
    }
}