package com.agora.entfulldemo.home;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.entfulldemo.R;
import com.agora.entfulldemo.databinding.AppFragmentHomeIndexBinding;
import com.agora.entfulldemo.databinding.AppItemHomeIndexBinding;
import com.agora.entfulldemo.home.constructor.ScenesConstructor;
import com.agora.entfulldemo.home.constructor.ScenesModel;
import com.agora.entfulldemo.home.holder.HomeIndexHolder;

import java.util.List;

import io.agora.scene.base.ReportApi;
import io.agora.scene.base.ServerConfig;
import io.agora.scene.base.component.BaseRecyclerViewAdapter;
import io.agora.scene.base.component.BaseViewBindingFragment;
import io.agora.scene.base.component.OnItemClickListener;
import io.agora.scene.base.utils.ToastUtils;
import io.agora.scene.widget.utils.UiUtils;

/**
 * The type Home index fragment.
 */
public class HomeIndexFragment extends BaseViewBindingFragment<AppFragmentHomeIndexBinding> {

    /**
     * Gets view binding.
     *
     * @param inflater  the inflater
     * @param container the container
     * @return the view binding
     */
    @NonNull
    @Override
    protected AppFragmentHomeIndexBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return AppFragmentHomeIndexBinding.inflate(inflater);
    }

    /**
     * Init view.
     */
    @Override
    public void initView() {

        Context context = getContext();
        if (context != null) {
            List<ScenesModel> scenesModels = ScenesConstructor.buildData(context);
            BaseRecyclerViewAdapter<AppItemHomeIndexBinding, ScenesModel, HomeIndexHolder> homeIndexAdapter = new BaseRecyclerViewAdapter<>(scenesModels, new OnItemClickListener<ScenesModel>() {
                @Override
                public void onItemClick(@NonNull ScenesModel scenesModel, View view, int position, long viewType) {
                    if (UiUtils.isFastClick(1000)) {
                        return;
                    }
                    if (scenesModel.getActive()) {
                        reportEnter(scenesModel);
                        goScene(scenesModel);
                    }
                }
            }, HomeIndexHolder.class);
            getBinding().rvScenes.setAdapter(homeIndexAdapter);

            if (ServerConfig.getEnvRelease()){
                getBinding().tvDevEnv.setVisibility(View.GONE);
            }else {
                getBinding().tvDevEnv.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Report enter.
     *
     * @param scenesModel the scenes model
     */
    private void reportEnter(@NonNull ScenesModel scenesModel) {
        ReportApi.reportEnter(scenesModel.getScene().name(), aBoolean -> null, null);
    }

    /**
     * Go scene.
     *
     * @param scenesModel the scenes model
     */
    private void goScene(@NonNull ScenesModel scenesModel) {
        Intent intent = new Intent();
        intent.setClassName(requireContext(), scenesModel.getClazzName());
        try {
            startActivity(intent);
        } catch (Exception e) {
            ToastUtils.showToast(R.string.app_coming_soon);
        }
    }

    /**
     * Init listener.
     */
    @Override
    public void initListener() {
    }
}
