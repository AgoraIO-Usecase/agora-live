package io.agora.scene.widget.basic;

import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import java.lang.reflect.ParameterizedType;

/**
 * The type Binding view holder.
 *
 * @param <T> the type parameter
 */
public class BindingViewHolder<T extends ViewBinding> extends RecyclerView.ViewHolder {
    /**
     * The Binding.
     */
    public final T binding;

    /**
     * Instantiates a new Binding view holder.
     *
     * @param binding the binding
     */
    public BindingViewHolder(@NonNull T binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    /**
     * Inflate t.
     *
     * @param inflater the inflater
     * @return the t
     */
    public T inflate(@NonNull LayoutInflater inflater) {
        try {
            Class<T> c = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            if (c != null) {
                return (T) c.getDeclaredMethod("inflate", LayoutInflater.class).invoke(null, inflater);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
