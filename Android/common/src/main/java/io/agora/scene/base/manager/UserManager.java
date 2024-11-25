package io.agora.scene.base.manager;

import java.util.Random;

import io.agora.scene.base.Constant;
import io.agora.scene.base.api.SSOUserInfo;
import io.agora.scene.base.bean.User;
import io.agora.scene.base.utils.SPUtil;

/**
 * The type User manager.
 */
public final class UserManager {
    /**
     * The constant instance.
     */
    private volatile static UserManager instance;
    /**
     * The M user.
     */
    private User mUser;

    /**
     * Instantiates a new User manager.
     */
    private UserManager() {
    }

    /**
     * Gets user.
     *
     * @return the user
     */
    public User getUser() {
        if (mUser != null) {
            return mUser;
        }
        SSOUserInfo ssoUserInfo = SSOUserManager.getUser();

        User user = new User();
        user.headUrl = getOrRandomAvatar();
        String displayName = ssoUserInfo.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = getOrRandomNickname();
        }
        user.name = displayName;
        user.id = (long) ssoUserInfo.getAccountUid().hashCode();
        user.userNo = user.id + "";

        mUser = user;
        return mUser;
    }

    /**
     * Update user name string.
     *
     * @param name the name
     * @return the string
     */
    public String updateUserName(String name) {
        if (mUser != null) {
            mUser.name = name;
            SPUtil.putString(Constant.CURRENT_NICKNAME, name);
            return mUser.name;
        }
        return "";

    }

    /**
     * Get user avatar full url string.
     *
     * @param headUrl the head url
     * @return the string
     */
    public String getUserAvatarFullUrl(String headUrl) {
        if (headUrl.startsWith("http")) {
            return headUrl;
        }
        return "file:///android_asset/" + headUrl + ".png";
    }

    private String getOrRandomAvatar() {
        String avatar = SPUtil.getString(Constant.CURRENT_AVATAR, "");
        if (avatar.isEmpty()) {
            int index = new Random().nextInt(100) % 4 + 1;
            avatar = "avatar_" + index;
            SPUtil.putString(Constant.CURRENT_AVATAR, avatar);
        }
        return avatar;
    }

    private String randomName() {
        String[] names = new String[]{"Ezra", "Pledge", "Bonnie", "Seeds", "Shannon", "Red-Haired", "Montague", "Primavera", "Lucille", "Tess"};
        int index = new Random().nextInt(100) % names.length;
        return names[index];
    }

    private String getOrRandomNickname() {
        String nickname = SPUtil.getString(Constant.CURRENT_NICKNAME, "");
        if (nickname.isEmpty()) {
            nickname = randomName();
            SPUtil.putString(Constant.CURRENT_NICKNAME, nickname);
        }
        return nickname;
    }


    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static UserManager getInstance() {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) {
                    instance = new UserManager();
                }
            }
        }
        return instance;
    }

    public void clear() {
        mUser = null;
    }
}