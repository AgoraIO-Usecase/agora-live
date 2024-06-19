package io.agora.scene.ktv.live.listener

import io.agora.scene.ktv.ktvapi.MusicLoadStatus

enum class SongLoadFailReason(val value: Int) {
    MUSIC_DOWNLOAD_FAIL(0),
    CANCELED(1),
    UNKNOW(2),
}

interface SongLoadStateListener {
    /**
     * 音乐加载成功
     * @param songCode 歌曲编码，和loadMusic传入的songCode一致
     * @param lyricUrl 歌词地址
     */
    fun onMusicLoadSuccess(songCode: String,musicUri:String, lyricUrl: String)

    /**
     * 音乐加载失败
     * @param songCode 加载失败的歌曲编码
     * @param reason 歌曲加载失败的原因
     */
    fun onMusicLoadFail(songCode: String, reason: SongLoadFailReason)

    /**
     * 音乐加载进度
     * @param songCode 歌曲编码
     * @param percent 歌曲加载进度
     * @param status 歌曲加载的状态
     * @param msg 相关信息
     * @param lyricUrl 歌词地址
     */
    fun onMusicLoadProgress(songCode: String, percent: Int, status: MusicLoadStatus, lyricUrl: String?)
}