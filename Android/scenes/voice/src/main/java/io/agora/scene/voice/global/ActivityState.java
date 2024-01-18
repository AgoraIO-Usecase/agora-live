package io.agora.scene.voice.global;

import android.app.Activity;

import java.util.List;

/**
 * Created by shuwei on 2017/12/18.
 */

public interface ActivityState {
    /**
     * get current Activity
     */
    Activity current();

    /**
     * Gets activity list.
     *
     * @return the activity list
     */
    List<Activity> getActivityList();

    /**
     * The total number of activities in the task stack.
     */
    int count();
    /**
     * Check whether the application is in the foreground, i.e. visible.
     */
    boolean isFront();
}
