package io.agora.scene.show.widget.link

import android.view.View
import io.agora.scene.show.service.ShowMicSeatApply
import io.agora.scene.show.service.ShowUser

/**
 * On link dialog action listener
 *
 * @constructor Create empty On link dialog action listener
 */
interface OnLinkDialogActionListener {
    /**
     * On request message refreshing
     *
     * @param dialog
     */
    fun onRequestMessageRefreshing(dialog: LiveLinkDialog)

    /**
     * On accept mic seat apply chosen
     *
     * @param dialog
     * @param seatApply
     */
    fun onAcceptMicSeatApplyChosen(dialog: LiveLinkDialog, seatApply: ShowMicSeatApply, view: View)

    /**
     * On online audience refreshing
     *
     * @param dialog
     */
    fun onOnlineAudienceRefreshing(dialog: LiveLinkDialog)

    /**
     * On online audience invitation
     *
     * @param dialog
     * @param userItem
     */
    fun onOnlineAudienceInvitation(dialog: LiveLinkDialog, userItem: ShowUser, view: View)

    /**
     * On stop linking chosen
     *
     * @param dialog
     */
    fun onStopLinkingChosen(dialog: LiveLinkDialog, view: View)

    /**
     * On stop applying chosen
     *
     * @param dialog
     */
    fun onStopApplyingChosen(dialog: LiveLinkDialog, view: View)
}