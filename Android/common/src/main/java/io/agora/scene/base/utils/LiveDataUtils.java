package io.agora.scene.base.utils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

/**
 * LiveData Util.
 */
public final class LiveDataUtils {

    private LiveDataUtils() {
    }

    /**
     * observer then remove.
     *
     * @param <T>      the type parameter
     * @param owner    the owner
     * @param liveData the live data
     * @param observer the observer
     */
    public static <T> void observerThenRemove(@NonNull LifecycleOwner owner,
                                              @NonNull LiveData<T> liveData,
                                              @NonNull Observer<? super T> observer) {
        liveData.observe(owner, new Observer<T>() {
            @Override
            public void onChanged(T t) {
                liveData.removeObserver(this);
                observer.onChanged(t);
            }
        });
    }
}
