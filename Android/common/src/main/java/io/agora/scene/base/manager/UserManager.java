package io.agora.scene.base.manager;

import android.text.TextUtils;

import java.util.Random;

import io.agora.scene.base.Constant;
import io.agora.scene.base.bean.User;
import io.agora.scene.base.utils.GsonUtils;
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
        readingUserInfoFromPrefs();
        return mUser;
    }

    /**
     * Login with random user info.
     */
    public void loginWithRandomUserInfo() {
        saveUserInfo(randomUserInfo());
    }

    /**
     * Update random avatar string.
     *
     * @return the string
     */
    public String updateRandomAvatar() {
        mUser.headUrl = randomAvatar();
        saveUserInfo(mUser);
        return mUser.getFullHeadUrl();
    }

    /**
     * Update user name string.
     *
     * @param name the name
     * @return the string
     */
    public String updateUserName(String name) {
        mUser.name = name;
        saveUserInfo(mUser);
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

    /**
     * Logout.
     */
    public void logout() {
        writeUserInfoToPrefs(true);
    }


    /**
     * Save user info.
     *
     * @param user the user
     */
    private void saveUserInfo(User user) {
        mUser = user;
        writeUserInfoToPrefs(false);
    }

    /**
     * Random user info user.
     *
     * @return the user
     */
    private User randomUserInfo() {
        User user = new User();
        user.headUrl = randomAvatar();
        user.name = randomName();
        user.id = (long) randomId();
        user.userNo = user.id + "";
        return user;
    }

    /**
     * Random avatar string.
     *
     * @return the string
     */
    private static String randomAvatar() {
        int index = new Random().nextInt(100) % 4 + 1;
        return "avatar_" + index;
    }

    /**
     * Random name string.
     *
     * @return the string
     */
    private static String randomName() {
        String[] names = new String[]{"Ezra", "Pledge", "Bonnie", "Seeds", "Shannon", "Red-Haired", "Montague", "Primavera", "Lucille", "Tess"};
        int index = new Random().nextInt(100) % names.length;
        return names[index];
    }

    /**
     * Random id int.
     *
     * @return the int
     */
    private static int randomId() {
        return new Random().nextInt(1000) + 10000;
    }

    /**
     * Write user info to prefs.
     *
     * @param isLogOut the is log out
     */
    private void writeUserInfoToPrefs(boolean isLogOut) {
        if (isLogOut) {
            mUser = null;
            SPUtil.putString(Constant.CURRENT_USER, "");
        } else {
            SPUtil.putString(Constant.CURRENT_USER, getUserInfoJson());
        }
    }

    /**
     * Reading user info from prefs.
     */
    private void readingUserInfoFromPrefs() {
        String userInfo = SPUtil.getString(Constant.CURRENT_USER, "");
        if (!TextUtils.isEmpty(userInfo)) {
            mUser = GsonUtils.getGson().fromJson(userInfo, User.class);
        }
    }

    /**
     * Gets user info json.
     *
     * @return the user info json
     */
    private String getUserInfoJson() {
        return GsonUtils.getGson().toJson(mUser);
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

    /**
     * Is login boolean.
     *
     * @return the boolean
     */
    public boolean isLogin() {
        if (mUser == null) {
            readingUserInfoFromPrefs();
            return mUser != null && !TextUtils.isEmpty(mUser.userNo);
        }
        return true;
    }

}
