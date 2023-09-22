package io.agora.scene.show.widget

import android.content.Context
import android.view.LayoutInflater
import io.agora.scene.show.databinding.ShowWidgetBottomLightListDialogBinding
import io.agora.scene.show.databinding.ShowWidgetBottomLightListItemBinding
import io.agora.scene.widget.basic.BindingSingleAdapter
import io.agora.scene.widget.basic.BindingViewHolder


/**
 * Bottom light list dialog
 *
 * @constructor
 *
 * @param context
 */
open class BottomLightListDialog(context: Context) : BottomLightDialog(context) {

    /**
     * M binding
     */
    private val mBinding by lazy {
        ShowWidgetBottomLightListDialogBinding.inflate(
            LayoutInflater.from(
                context
            )
        )
    }

    /**
     * On item selected listener
     */
    private var onItemSelectedListener: ((BottomLightListDialog, Int)->Unit)? = null

    /**
     * Selected item
     */
    private var selectedItem = -1

    /**
     * M adapter
     */
    private val mAdapter by lazy {
        object : BindingSingleAdapter<String, ShowWidgetBottomLightListItemBinding>(){
            override fun onBindViewHolder(
                holder: BindingViewHolder<ShowWidgetBottomLightListItemBinding>,
                position: Int
            ) {
                val item = getItem(position) ?: return
                holder.binding.text.text = item
                holder.binding.text.isActivated = selectedItem == position
                holder.binding.text.setOnClickListener {
                    innerSetSelected(holder.adapterPosition)
                }
            }
        }
    }

    init {
        setBottomView(mBinding.root)
        mBinding.recyclerView.adapter = mAdapter
    }

    /**
     * Set title
     *
     * @param title
     */
    override fun setTitle(title: CharSequence?) {
        super.setTitle(title)
        mBinding.tvTitle.text = title
        mBinding.tvCancel.setOnClickListener {
            dismiss()
        }
    }

    /**
     * Set on selected changed listener
     *
     * @param listener
     * @receiver
     */
    fun setOnSelectedChangedListener(listener: (BottomLightListDialog, Int)->Unit){
        onItemSelectedListener = listener
    }

    /**
     * Set list data
     *
     * @param data
     */
    fun setListData(data: List<String>){
        mAdapter.resetAll(data)
    }

    /**
     * Set selected position
     *
     * @param position
     */
    fun setSelectedPosition(position: Int){
        innerSetSelected(position)
    }

    /**
     * Get selected position
     *
     */
    fun getSelectedPosition() = selectedItem


    /**
     * Inner set selected
     *
     * @param position
     */
    private fun innerSetSelected(position: Int){
        if(position == selectedItem){
            return
        }
        val oPosition = selectedItem
        selectedItem = position
        if(oPosition >= 0){
            mAdapter.notifyItemChanged(oPosition)
        }
        mAdapter.notifyItemChanged(selectedItem)
        onItemSelectedListener?.invoke(this, selectedItem)
    }





}