package io.agora.scene.base.component;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import io.agora.scene.base.utils.UiUtil;

/**
 * BaseBottomSheetDialogFragment.
 * @param <B>
 */
public class BaseBottomSheetDialogFragment<B extends ViewBinding> extends BottomSheetDialogFragment {
    protected B mBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = getViewBindingByReflect(inflater, container);
        return mBinding != null ? mBinding.getRoot() : null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(requireDialog().getWindow(), false);
        requireDialog().setOnShowListener(dialog -> ((ViewGroup) view.getParent()).setBackgroundColor(Color.TRANSPARENT));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    private B getViewBindingByReflect(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        try {
            Class<B> c = UiUtil.getGenericClass(getClass(), 0);
            return UiUtil.getViewBinding(c, inflater, container);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Set bundle args.
     * @param bundleArgs
     * @return BaseBottomSheetDialogFragment
     */
    public BaseBottomSheetDialogFragment<B> setBundleArgs(Bundle bundleArgs) {
        setArguments(bundleArgs);
        return this;
    }

    protected void hideKeyboard(EditText editText) {
        editText.clearFocus();
        Activity context = getActivity();
        if (context == null) {
            return;
        }
        // hide the soft keyboard
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    protected void showKeyboard(EditText editText) {
        Activity context = getActivity();
        if (context == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, 0);
    }
}