package io.agora.scene.show.widget.link

import android.view.View
import io.agora.scene.base.GlideApp
import io.agora.scene.show.R
import io.agora.scene.show.databinding.ShowLiveLinkInvitationMessageBinding
import io.agora.scene.show.service.ShowInteractionStatus
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
            ShowInteractionStatus.linking -> {
                binding.btnItemInvite.isEnabled = false
                binding.btnItemInvite.setText(R.string.show_is_onseat)
                binding.btnItemInvite.setOnClickListener(null)
            }
            else -> {
                binding.btnItemInvite.isEnabled = true
                binding.btnItemInvite.setText(R.string.show_application)
                binding.btnItemInvite.setOnClickListener {
                    onClickListener.onClick(it, userItem, position)
                }
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
        fun onClick(view: View, userItem: ShowUser, position: Int)
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