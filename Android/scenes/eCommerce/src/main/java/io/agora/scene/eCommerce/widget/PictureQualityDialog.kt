package io.agora.scene.eCommerce.widget

import android.content.Context
import android.util.Size
import android.view.LayoutInflater
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import io.agora.scene.eCommerce.R
import io.agora.scene.eCommerce.databinding.CommerceWidgetPictureQualityDialogBinding
import io.agora.scene.eCommerce.databinding.CommerceWidgetPictureQualityDialogItemBinding
import io.agora.scene.widget.basic.BindingSingleAdapter
import io.agora.scene.widget.basic.BindingViewHolder

/**
 * Picture quality dialog
 *
 * @constructor
 *
 * @param context
 */
class PictureQualityDialog(context: Context) : BottomDarkDialog(context) {

    companion object {
        /**
         * Quality Index 1080p
         */
        const val QUALITY_INDEX_1080P = 0

        /**
         * Quality Index 720p
         */
        const val QUALITY_INDEX_720P = 1

        /**
         * Quality Index 540p
         */
        const val QUALITY_INDEX_540P = 2

        /**
         * Quality Index 360p
         */
        const val QUALITY_INDEX_360P = 3

        /**
         * Quality Index 270p
         */
        const val QUALITY_INDEX_270P = 4

        /**
         * Quality Index 180p
         */
        const val QUALITY_INDEX_180P = 5

        @IntDef(
            QUALITY_INDEX_1080P,
            QUALITY_INDEX_720P,
            QUALITY_INDEX_540P,
            QUALITY_INDEX_360P,
            QUALITY_INDEX_270P,
            QUALITY_INDEX_180P
        )
        @Retention(AnnotationRetention.RUNTIME)
        public annotation class QualityIndex

        private val QualityItemList = arrayListOf(
            QualityItem(QUALITY_INDEX_1080P, R.string.commerce_picture_quality_1080p, Size(1080, 1920)),
            QualityItem(QUALITY_INDEX_720P, R.string.commerce_picture_quality_720p, Size(720, 1280)),
            QualityItem(QUALITY_INDEX_540P, R.string.commerce_picture_quality_540p, Size(540, 960)),
            QualityItem(QUALITY_INDEX_360P, R.string.commerce_picture_quality_360p, Size(360, 640)),
            QualityItem(QUALITY_INDEX_270P, R.string.commerce_picture_quality_270p, Size(270, 480)),
            QualityItem(QUALITY_INDEX_180P, R.string.commerce_picture_quality_180p, Size(180, 320)),
        )

        private var cacheSelectedIndex = QUALITY_INDEX_720P

        /**
         * Get cache quality resolution
         *
         */
        fun getCacheQualityResolution() = QualityItemList[cacheSelectedIndex].size
    }

    /**
     * Quality item
     *
     * @property qualityIndex
     * @property name
     * @property size
     * @constructor Create empty Quality item
     */
    private data class QualityItem(@QualityIndex val qualityIndex: Int, @StringRes val name: Int, val size: Size)


    /**
     * M binding
     */
    private val mBinding by lazy {
        CommerceWidgetPictureQualityDialogBinding.inflate(
            LayoutInflater.from(
                context
            )
        )
    }

    /**
     * Curr selected index
     */
    private var currSelectedIndex = cacheSelectedIndex

    /**
     * On quality select listener
     */
    private var onQualitySelectListener: ((PictureQualityDialog, Int, Size) -> Unit)? = null

    /**
     * M adapter
     */
    private val mAdapter by lazy {
        object : BindingSingleAdapter<QualityItem, CommerceWidgetPictureQualityDialogItemBinding>() {
            override fun onBindViewHolder(
                holder: BindingViewHolder<CommerceWidgetPictureQualityDialogItemBinding>,
                position: Int
            ) {
                val item = getItem(position) ?: return
                val selected = currSelectedIndex == position

                holder.binding.text.isActivated = selected
                holder.binding.text.text = context.getString(item.name)
                holder.binding.root.setOnClickListener {
                    updateSelectPosition(holder.adapterPosition)
                }
            }
        }
    }

    init {
        setBottomView(mBinding.root)
        mBinding.recycleView.adapter = mAdapter
        mAdapter.resetAll(QualityItemList)
    }

    /**
     * Set select quality
     *
     * @param width
     * @param height
     */
    fun setSelectQuality(width: Int, height: Int){
        val index = QualityItemList.indexOfFirst { it.size.width == width && it.size.height == height }
        updateSelectPosition(index)
    }

    /**
     * Set on quality select listener
     *
     * @param listener
     * @receiver
     */
    fun setOnQualitySelectListener(listener: ((PictureQualityDialog, Int, Size) -> Unit)) {
        onQualitySelectListener = listener
    }

    /**
     * Update select position
     *
     * @param selectPosition
     */
    private fun updateSelectPosition(selectPosition: Int) {
        val item = QualityItemList.getOrNull(selectPosition) ?: return
        if (currSelectedIndex == selectPosition) {
            return
        }
        val oIndex = currSelectedIndex
        currSelectedIndex = selectPosition

        mAdapter.notifyItemChanged(oIndex)
        mAdapter.notifyItemChanged(currSelectedIndex)

        onQualitySelectListener?.invoke(this, item.qualityIndex, Size(item.size.width, item.size.height))
    }

}