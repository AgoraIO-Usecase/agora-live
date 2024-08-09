package io.agora.scene.voice.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.*
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import io.agora.scene.base.component.BaseBottomSheetDialogFragment
import io.agora.scene.base.utils.ToastUtils
import io.agora.scene.voice.R
import io.agora.scene.voice.databinding.VoiceDialogCreateRoomBinding
import io.agora.scene.voice.ui.activity.VoiceRoomSoundSelectionActivity
import io.agora.scene.voice.viewmodel.VoiceCreateViewModel
import io.agora.voice.common.ui.IParserSource
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class CreateRoomDialog constructor(
    private val context: Context,
): BaseBottomSheetDialogFragment<VoiceDialogCreateRoomBinding>(), IParserSource {

    private var currentPosition = 0

    private lateinit var roomCreateViewModel: VoiceCreateViewModel

    private var window: Window? = null
    private var loadingView: View? = null

    private var oldInput = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        window = dialog.window
        return dialog
    }

    private fun setTips(tips: String) {
        mBinding.apply {
            // 创建一个 Drawable 对象并设置它的大小
            val icon = ContextCompat.getDrawable(root.context, R.mipmap.ic_tip_error)
            icon?.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
            // 创建一个 SpannableString 并将图标设置在字符串的开始位置
            val spannableString = SpannableString("  $tips")
            val imageSpan = ImageSpan(icon!!, ImageSpan.ALIGN_BASELINE)
            spannableString.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            // 设置 SpannableString 到 TextView
            tvTips.text = spannableString
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it.decorView) { v: View?, insets: WindowInsetsCompat ->
                val systemInset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(0, 0, 0, systemInset.bottom)
                WindowInsetsCompat.CONSUMED
            }
        }

        roomCreateViewModel = ViewModelProvider(this)[VoiceCreateViewModel::class.java]
        setTips(getString(R.string.voice_create_room_tips))
        randomName()
        mBinding.btnRandom.setOnClickListener {
            randomName()
        }
        mBinding.cbPassword.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mBinding.layoutPassword.visibility = View.VISIBLE
                mBinding.tvPWDTips.visibility = View.VISIBLE
                mBinding.etCode.requestFocus()
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(mBinding.etCode, InputMethodManager.SHOW_IMPLICIT)
            } else {
                hideInput()
                mBinding.layoutPassword.visibility = View.GONE
                mBinding.tvPWDTips.visibility = View.GONE
            }
        }
        mBinding.btnCreateRoom.setOnClickListener {
            createRoom()
        }
        mBinding.etCode.setOnTextChangeListener {  }

        mBinding.layoutRoomName.isEndIconVisible = mBinding.etRoomName.isFocused
        mBinding.etRoomName.setOnFocusChangeListener { v, hasFocus ->
            mBinding.layoutRoomName.isEndIconVisible = hasFocus
        }

        mBinding.layoutRoomName.setEndIconOnClickListener {
            mBinding.etRoomName.setText("")
        }

        activity?.window?.let { window ->
            val initialWindowHeight = Rect().apply { window.decorView.getWindowVisibleDisplayFrame(this) }.height()
            mBinding.root.viewTreeObserver.addOnGlobalLayoutListener {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (mBinding == null) { return@postDelayed }
                    val currentWindowHeight = Rect().apply { window.decorView.getWindowVisibleDisplayFrame(this) }.height()
                    if (currentWindowHeight < initialWindowHeight) {
                    } else {
                        mBinding.etCode.clearFocus()
                        mBinding.etRoomName.clearFocus()
                    }
                }, 300)
            }
        }
    }

    private fun randomName() {
        val date = Date()
        val month = SimpleDateFormat("MM").format(date)
        val day = SimpleDateFormat("dd").format(date)
        val roomName =  getString(R.string.voice_room_create_chat_room) + "-" + month + day + "-" + (Math.random() * 999 + 1).roundToInt()
        mBinding.etRoomName.setText(roomName)
    }

    private fun createRoom() {
        val roomName = mBinding.etRoomName.text.toString()
        if (TextUtils.isEmpty(roomName)) {
            ToastUtils.showToast(R.string.voice_please_input_room_name)
            return
        }
        val isPrivate = mBinding.cbPassword.isChecked
        val password = mBinding.etCode.text.toString()
        if (isPrivate && password.length < 4) {
            ToastUtils.showToast(getString(R.string.voice_please_input_4_pwd))
            return
        }
        dismiss()
        activity?.let {
            VoiceRoomSoundSelectionActivity.startActivity(it, roomName, !isPrivate, password, 0)
        }
    }

    private fun showLoadingView() {
        window?.apply {
            decorView.post { addLoadingView() }
            decorView.postDelayed({ hideLoadingView() }, 5000)
        }
    }

    private fun addLoadingView() {
        if (this.loadingView == null) {
            val rootView = window?.decorView?.findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0) as ViewGroup
            this.loadingView = LayoutInflater.from(context).inflate(R.layout.view_base_loading, rootView, false)
            rootView.addView(
                this.loadingView,
                ViewGroup.LayoutParams(-1, -1)
            )
        }
        this.loadingView?.visibility = View.VISIBLE
    }

    private fun hideLoadingView() {
        if (loadingView == null) {
            return
        }
        window?.apply {
            decorView.post {
                if (loadingView != null) {
                    loadingView?.visibility = View.GONE
                }
            }
        }
    }
    private fun hideInput() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val v = window?.peekDecorView()
        if (v != null) {
            imm.hideSoftInputFromWindow(v.windowToken, 0)
            mBinding.etCode.clearFocus()
        }
    }
}