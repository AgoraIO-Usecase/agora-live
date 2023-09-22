package io.agora.scene.base.bean;

/**
 * The type User.
 */
public class User {
    /**
     * The Head url.
     */
    public String headUrl;
    /**
     * The Mobile.
     */
    public String mobile;
    /**
     * The Name.
     */
    public String name;
    /**
     * The Sex.
     */
    public String sex;
    /**
     * The Status.
     */
    public int status;
    /**
     * The User no.
     */
    public String userNo;
    /**
     * The Token.
     */
    public String token;
    /**
     * The Id.
     */
    public Long id;

    /**
     * Get full head url string.
     *
     * @return the string
     */
    public String getFullHeadUrl() {
        if (headUrl.startsWith("http")) {
            return headUrl;
        }
        return "file:///android_asset/" + headUrl + ".png";
    }
}
