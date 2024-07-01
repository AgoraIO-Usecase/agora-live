package io.agora.scene.show.widget.link

import android.view.View
import androidx.core.view.isVisible
import io.agora.scene.base.GlideApp
import io.agora.scene.base.manager.UserManager
import io.agora.scene.show.R
import io.agora.scene.show.databinding.ShowLiveLinkRequestMessageBinding
import io.agora.scene.show.service.ShowMicSeatApply
import io.agora.scene.widget.basic.BindingSingleAdapter
import io.agora.scene.widget.basic.BindingViewHolder
import io.agora.scene.widget.utils.CenterCropRoundCornerTransform

/**
 * Live link request view adapter
 *
 * @constructor Create empty Live link request view adapter
 */
class LiveLinkRequestViewAdapter: BindingSingleAdapter<ShowMicSeatApply, ShowLiveLinkRequestMessageBinding>() {
    /**
     * Is room owner
     */
    private var isRoomOwner : Boolean = true

    /**
     * On bind view holder
     *
     * @param holder
     * @param position
     */
    override fun onBindViewHolder(
        holder: BindingViewHolder<ShowLiveLinkRequestMessageBinding>,
        position: Int
    ) {
        val seatApply = getItem(position)!!
        val binding = holder.binding
        binding.titleItemUserStatus.text = seatApply.userName
        binding.coverUserIcon.visibility = View.VISIBLE
        GlideApp.with(binding.coverUserIcon).load(seatApply.getAvatarFullUrl())
            .fallback(R.mipmap.show_default_icon)
            .error(R.mipmap.show_default_icon)
            .transform(CenterCropRoundCornerTransform(10))
            .into(binding.coverUserIcon);
        if (isRoomOwner) {
            binding.userNum.isVisible = false
            binding.btnItemAgreeRequest.isEnabled = true
            binding.btnItemAgreeRequest.setText(R.string.show_agree_onseat)
            binding.btnItemAgreeRequest.setOnClickListener {
                onClickListener.onClick(it, seatApply, position)
            }
        } else {
            if (seatApply.userId == UserManager.getInstance().user.id.toString()) {
                binding.titleItemUserStatus.setTextColor(R.color.show_text)
            }
            binding.userNum.isVisible = true
            binding.userNum.text = (position+1).toString()
            binding.btnItemAgreeRequest.visibility = View.GONE
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
         * @param seatApply
         * @param position
         */
        fun onClick(view: View, seatApply: ShowMicSeatApply, position: Int)
    }

    /**
     * Set click listener
     *
     * @param listener
     */
    fun setClickListener(listener : OnClickListener) {
        onClickListener = listener
    }

    /**
     * Set is room owner
     *
     * @param value
     */
    fun setIsRoomOwner(value: Boolean) {
        isRoomOwner = value
    }
}