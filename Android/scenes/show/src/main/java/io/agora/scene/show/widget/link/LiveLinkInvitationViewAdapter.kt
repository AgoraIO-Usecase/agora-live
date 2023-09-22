package io.agora.scene.show.widget.link

import android.view.View
import io.agora.scene.base.GlideApp
import io.agora.scene.show.R
import io.agora.scene.show.databinding.ShowLiveLinkInvitationMessageBinding
import io.agora.scene.show.service.ShowRoomRequestStatus
import io.agora.scene.show.service.ShowUser
import io.agora.scene.widget.basic.BindingSingleAdapter
import io.agora.scene.widget.basic.BindingViewHolder
import io.agora.scene.widget.utils.CenterCropRoundCornerTransform

/**
 * Live link invitation view adapter
 *
 * @constructor Create empty Live link invitation view adapter
 */
class LiveLinkInvitationViewAdapter: BindingSingleAdapter<ShowUser, ShowLiveLinkInvitationMessageBinding>() {
    /**
     * On bind view holder
     *
     * @param holder
     * @param position
     */
    override fun onBindViewHolder(
        holder: BindingViewHolder<ShowLiveLinkInvitationMessageBinding>,
        position: Int
    ) {
        val userItem = getItem(position)!!
        val binding = holder.binding
        binding.titleItemUserStatus.text = userItem.userName
        binding.coverUserIcon.visibility = View.VISIBLE
        GlideApp.with(binding.coverUserIcon).load(userItem.getAvatarFullUrl())
            .fallback(R.mipmap.show_default_icon)
            .error(R.mipmap.show_default_icon)
            .transform(CenterCropRoundCornerTransform(10))
            .into(binding.coverUserIcon)
        when (userItem.status) {
            ShowRoomRequestStatus.accepted.value -> {
                binding.btnItemInvite.isEnabled = false
                binding.btnItemInvite.setText(R.string.show_is_onseat)
                binding.btnItemInvite.setOnClickListener(null)
            }
            ShowRoomRequestStatus.idle.value -> {
                binding.btnItemInvite.isEnabled = true
                binding.btnItemInvite.setText(R.string.show_application)
                binding.btnItemInvite.setOnClickListener {
                    onClickListener.onClick(userItem, position)
                }
            }
            ShowRoomRequestStatus.waitting.value -> {
                binding.btnItemInvite.isEnabled = false
                binding.btnItemInvite.setText(R.string.show_application_waitting)
                binding.btnItemInvite.setOnClickListener(null)
            }
            ShowRoomRequestStatus.rejected.value -> {
                binding.btnItemInvite.isEnabled = false
                binding.btnItemInvite.setText(R.string.show_reject_onseat)
                binding.btnItemInvite.setOnClickListener(null)
            }
        }
    }

    /**
     * On click listener
     */
    private lateinit var onClickListener : OnClickListener

    /**
     * On click listener
     *
     * @constructor Create empty On click listener
     */
    interface OnClickListener {
        /**
         * On click
         *
         * @param userItem
         * @param position
         */
        fun onClick(userItem: ShowUser, position: Int)
    }

    /**
     * Set click listener
     *
     * @param listener
     */
    fun setClickListener(listener : OnClickListener) {
        onClickListener = listener
    }
}