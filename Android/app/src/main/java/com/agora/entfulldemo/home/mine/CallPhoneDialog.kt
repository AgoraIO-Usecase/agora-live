package com.agora.entfulldemo.home.mine

import android.os.Bundle
import android.view.View
import com.agora.entfulldemo.databinding.AppDialogCallPhoneBinding
import io.agora.scene.base.component.BaseBottomSheetDialogFragment

/**
 * Call phone dialog
 *
 * @constructor Create empty Call phone dialog
 */
class CallPhoneDialog : BaseBottomSheetDialogFragment<AppDialogCallPhoneBinding>() {

    companion object {
        /**
         * Key Phone
         */
        const val KEY_PHONE = "phoneNum"
    }

    /**
     * Phone
     */
    private val phone by lazy {
        arguments?.getString(KEY_PHONE)
    }

    /**
     * On click call phone
     */
    var onClickCallPhone: (() -> Unit)? = null

    /**
     * On view created
     *
     * @param view
     * @param savedInstanceState
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.tvCallPhone.text = phone
        mBinding.tvCallPhone.setOnClickListener {
            onClickCallPhone?.invoke()
            dismiss()
        }
        mBinding.tvCancel.setOnClickListener {
            dismiss()
        }
    }
}
