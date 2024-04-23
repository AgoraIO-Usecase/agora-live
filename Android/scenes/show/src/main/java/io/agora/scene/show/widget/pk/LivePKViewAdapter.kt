package io.agora.scene.show.widget.pk

import android.view.View
import androidx.core.view.isVisible
import io.agora.scene.base.GlideApp
import io.agora.scene.show.R
import io.agora.scene.show.databinding.ShowLivePkRequestMessageBinding
import io.agora.scene.show.service.ShowInteractionStatus
import io.agora.scene.widget.basic.BindingSingleAdapter
import io.agora.scene.widget.basic.BindingViewHolder
import io.agora.scene.widget.utils.CenterCropRoundCornerTransform

/**
 * Live p k view adapter
 *
 * @constructor Create empty Live p k view adapter
 */
class LivePKViewAdapter: BindingSingleAdapter<LiveRoomConfig, ShowLivePkRequestMessageBinding>() {
    /**
     * On bind view holder
     *
     * @param holder
     * @param position
     */
    override fun onBindViewHolder(
        holder: BindingViewHolder<ShowLivePkRequestMessageBinding>,
        position: Int
    ) {
        val roomItem = getItem(position)!!
        val binding = holder.binding
        binding.titleItemBoardcasterStatus.text = roomItem.getOwnerName() + " (ID:${roomItem.getRoomId()})"
        binding.coverBoardcasterIcon.visibility = View.VISIBLE
        GlideApp.with(binding.coverBoardcasterIcon).load(getFullHeadUrl(roomItem.getOwnerAvatar()))
            .fallback(R.mipmap.show_default_icon)
            .error(R.mipmap.show_default_icon)
            .transform(CenterCropRoundCornerTransform(999))
            .into(binding.coverBoardcasterIcon)
        when (roomItem.getInteractStatus()) {
            ShowInteractionStatus.idle.value -> {
                if (roomItem.isWaitingForPK()) {
                    binding.btnItemRequest.isEnabled = false
                    binding.btnItemRequest.setText(R.string.show_application_waitting)
                    binding.btnItemRequest.setOnClickListener(null)
                } else {
                    binding.btnItemRequest.isEnabled = true
                    binding.btnItemRequest.setText(R.string.show_application)
                    binding.btnItemRequest.setOnClickListener {
                        onClickListener.onClick(roomItem, position)
                    }
                }
            }
            ShowInteractionStatus.pking.value -> {
                binding.btnItemRequest.isEnabled = false
                binding.btnItemRequest.setText(R.string.show_interacting)
                binding.btnItemRequest.setOnClickListener(null)
            }
            ShowInteractionStatus.onSeat.value -> {
                binding.btnItemRequest.isEnabled = false
                binding.btnItemRequest.setText(R.string.show_interacting)
                binding.btnItemRequest.setOnClickListener(null)
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
         * @param roomItem
         * @param position
         */
        fun onClick(roomItem: LiveRoomConfig, position: Int)
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
     * Get full head url string.
     *
     * @return the string
     */
    private fun getFullHeadUrl(headUrl: String): String {
        return if (headUrl.startsWith("http")) {
            headUrl
        } else "file:///android_asset/$headUrl.png"
    }
}