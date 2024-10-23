package io.agora.scene.ktv.widget.song;

/**
 * The type Song item.
 */
public class SongItem {
    /**
     * The Song no.
     */
    public String songNo;// 歌曲的唯一标识
    /**
     * The Song name.
     */
    public String songName;
    /**
     * The Image url.
     */
    public String imageUrl; // 歌曲封面
    /**
     * The Singer.
     */
    public String singer; // 歌手名

    /**
     * The Chooser.
     */
// 已点歌曲信息
    public String chooser; // 点歌人
    /**
     * The Chooser id.
     */
    public String chooserId;
    /**
     * The Is chosen.
     */
    public boolean isChosen; // 是否已被点

    public Boolean loading = false; //

    /**
     * @param songNo
     * @param songName
     * @param imageUrl
     * @param singer
     * @param chooser
     * @param isChosen
     * @param chooserId
     */
    public SongItem(String songNo, String songName,
                    String imageUrl, String singer,
                    String chooser, boolean isChosen,
                    String chooserId) {
        this.songNo = songNo;
        this.songName = songName;
        this.imageUrl = imageUrl;
        this.singer = singer;
        this.chooser = chooser;
        this.isChosen = isChosen;
        this.chooserId = chooserId;
    }

    // 用于存放原始数据
    private Object tag;

    /**
     * Sets tag.
     *
     * @param <T> the type parameter
     * @param tag the tag
     */
    public <T> void setTag(T tag) {
        this.tag = tag;
    }

    /**
     * Get tag t.
     *
     * @param <T>   the type parameter
     * @param clazz the clazz
     * @return the t
     */
    public <T> T getTag(Class<T> clazz) {
        if (!clazz.isInstance(tag)) {
            return null;
        }
        return (T) tag;
    }
}
