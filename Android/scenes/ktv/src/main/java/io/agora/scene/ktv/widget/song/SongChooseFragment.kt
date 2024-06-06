package io.agora.scene.ktv.widget.song

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.scwang.smart.refresh.layout.api.RefreshLayout
import io.agora.scene.base.component.BaseViewBindingFragment
import io.agora.scene.base.component.OnItemClickListener
import io.agora.scene.ktv.databinding.KtvFragmentSongListBinding

/**
 * 歌单列表
 */
class SongChooseFragment : BaseViewBindingFragment<KtvFragmentSongListBinding?>(), OnItemClickListener<SongItem?> {

    var listener: Listener? = null

    private val mChooseAdapter: SongChooseViewAdapter = object : SongChooseViewAdapter() {
        public override fun onSongChosen(song: SongItem, position: Int) {
           listener?.onClickSongItem(song)
        }
    }

    override fun getViewBinding(layoutInflater: LayoutInflater, viewGroup: ViewGroup?): KtvFragmentSongListBinding {
        return KtvFragmentSongListBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding?.apply {
            rvRankList.adapter = mChooseAdapter
            smartRefreshLayout.setOnRefreshListener { refreshLayout: RefreshLayout? ->
                listener?.onRefresh(smartRefreshLayout)
            }
            smartRefreshLayout.autoRefresh()
            // 歌单一次性加载，没有更多数据
            smartRefreshLayout.setEnableLoadMore(false)
        }
    }

    fun setRefreshingResult(list: List<SongItem>) {
        binding?.llEmpty?.isVisible = list.isEmpty()
        mChooseAdapter.resetAll(list)
        binding?.apply {
            smartRefreshLayout.finishRefresh()
        }
    }

    fun setSongItemStatus(songItem: SongItem, isChosen: Boolean) {
        val itemCount: Int = mChooseAdapter.itemCount
        for (i in 0 until itemCount) {
            val item: SongItem = mChooseAdapter.getItem(i)?:continue
            if (item.songNo == songItem.songNo) {
                item.isChosen = isChosen
                item.loading = songItem.loading
                mChooseAdapter.notifyItemChanged(i)
                break
            }
        }
    }

    /**
     * On fragment call back
     *
     * @constructor Create empty On fragment call back
     */
    interface Listener {
        /**
         * On refresh.
         *
         * @param refreshLayout the refresh layout
         */
        fun onRefresh(refreshLayout: RefreshLayout){}

        /**
         * On load more.
         *
         * @param refreshLayout the refresh layout
         */
        fun onLoadMore(refreshLayout: RefreshLayout){}

        /**
         * On click song item.
         *
         * @param songItem the song item
         */
        fun onClickSongItem(songItem: SongItem){}
    }

}
