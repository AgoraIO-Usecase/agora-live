package io.agora.scene.widget.dialog;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;

import io.agora.scene.base.component.BaseDialog;
import io.agora.scene.base.databinding.DialogEditNameBinding;
import io.agora.scene.base.manager.UserManager;

/**
 * The type Edit name dialog.
 */
public class EditNameDialog extends BaseDialog<DialogEditNameBinding> {
    /**
     * The On define click listener.
     */
    private OnDefineClickListener onDefineClickListener;

    /**
     * Instantiates a new Edit name dialog.
     *
     * @param context the context
     */
    public EditNameDialog(@NonNull Context context) {
        super(context);
    }

    /**
     * Gets view binding.
     *
     * @param inflater the inflater
     * @return the view binding
     */
    @NonNull
    @Override
    protected DialogEditNameBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogEditNameBinding.inflate(inflater);
    }

    /**
     * Init view.
     */
    @Override
    protected void initView() {
        getBinding().btnCancel.setOnClickListener(view -> {
            dismiss();
        });
        getBinding().btnDefine.setOnClickListener(view -> {
            if (onDefineClickListener != null) {
                onDefineClickListener.onDefineClicked(getBinding().etDeviceName.getText().toString());
            }
            dismiss();
        });
        getBinding().etDeviceName.setText(UserManager.getInstance().getUser().name);
        getBinding().etDeviceName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable != null && editable.length() > 0) {
                    getBinding().iBtnClear.setVisibility(View.VISIBLE);
                } else {
                    getBinding().iBtnClear.setVisibility(View.GONE);
                }
            }
        });
        getBinding().iBtnClear.setOnClickListener(view -> {
            getBinding().etDeviceName.setText("");
        });
    }

    /**
     * Sets on define click listener.
     *
     * @param onDefineClickListener the on define click listener
     */
    public void setOnDefineClickListener(OnDefineClickListener onDefineClickListener) {
        this.onDefineClickListener = onDefineClickListener;
    }

    /**
     * Sets dialog title.
     *
     * @param title the title
     */
    public void setDialogTitle(String title) {
        getBinding().tvTitle.setText(title);
    }

    /**
     * Sets dialog input hint.
     *
     * @param title the title
     */
    public void setDialogInputHint(String title) {
        getBinding().tvTitle.setText(title);
        getBinding().etDeviceName.setHint(title);
    }

    /**
     * Sets gravity.
     */
    @Override
    protected void setGravity() {
//        getWindow().setLayout(
//                UiUtil.dp2px(300),
//                UiUtil.dp2px(230)
//        );
        getWindow().getAttributes().gravity = Gravity.CENTER;
    }

    /**
     * The interface On define click listener.
     */
    public interface OnDefineClickListener {
        /**
         * On define clicked.
         *
         * @param name the name
         */
        void onDefineClicked(String name);
    }
}
