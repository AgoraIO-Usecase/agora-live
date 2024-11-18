package io.agora.scene.base.manager;

import android.text.TextUtils;

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
        user.name = ssoUserInfo.getDisplayName();
        user.id = (long) ssoUserInfo.getProfileId();
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
//        mUser.name = name;
//        saveUserInfo(mUser);
        return mUser.name;
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
}