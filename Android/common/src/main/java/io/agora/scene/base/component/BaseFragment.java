package io.agora.scene.base.component;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * The type Base fragment.
 */
public abstract class BaseFragment extends Fragment {
    /**
     * The Is init.
     */
    private boolean isInit = false;

    /**
     * On view created.
     *
     * @param view               the view
     * @param savedInstanceState the saved instance state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!isInit) {
            initView();
            initListener();
            requestData();
            isInit = true;
        }
    }

    /**
     * On destroy view.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isInit = false;
    }

    /**
     * Init view.
     */
    public void initView() {
    }

    /**
     * Init listener.
     */
    public void initListener() {
    }

    /**
     * Request data.
     */
    public void requestData() {
    }

}
