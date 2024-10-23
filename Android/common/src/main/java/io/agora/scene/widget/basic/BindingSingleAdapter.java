package io.agora.scene.widget.basic;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The type Binding single adapter.
 *
 * @param <Data>    the type parameter
 * @param <Binding> the type parameter
 */
public abstract class BindingSingleAdapter<Data, Binding extends ViewBinding> extends RecyclerView.Adapter<BindingViewHolder<Binding>> {
    protected final List<Data> mDataList = new ArrayList<>();

    /**
     * On create view holder binding view holder.
     *
     * @param parent   the parent
     * @param viewType the view type
     * @return the binding view holder
     */
    @NonNull
    @Override
    public BindingViewHolder<Binding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return createBindingViewHolder(getClass(), parent, 1);
    }

    /**
     * Insert first.
     *
     * @param item the item
     */
    public void insertFirst(Data item) {
        insert(0, item);
    }

    /**
     * Insert last.
     *
     * @param item the item
     */
    public void insertLast(Data item) {
        insert(getItemCount(), item);
    }

    /**
     * Insert.
     *
     * @param index the index
     * @param item  the item
     */
    public void insert(int index, Data item) {
        int itemCount = getItemCount();
        if (index < 0) {
            index = 0;
        }
        if (index > itemCount) {
            index = itemCount;
        }
        mDataList.add(index, item);
        notifyItemInserted(index);
    }

    /**
     * Replace.
     *
     * @param index the index
     * @param item  the item
     */
    public void replace(int index, Data item) {
        mDataList.set(index, item);
        notifyItemChanged(index);
    }

    /**
     * Reset all.
     *
     * @param list the list
     */
    public void resetAll(List<Data> list) {
        if (list == null) {
            list = new ArrayList<>();
        }
        mDataList.clear();
        mDataList.addAll(list);
        notifyDataSetChanged();
    }

    /**
     * Insert all.
     *
     * @param list the list
     */
    public void insertAll(List<Data> list) {
        if (list == null || list.size() <= 0) {
            return;
        }
        int itemCount = getItemCount();
        mDataList.addAll(list);
        notifyItemRangeInserted(itemCount, list.size());
    }

    /**
     * Insert all.
     *
     * @param list the list
     */
    public void insertAll(Data[] list) {
        if (list == null) {
            return;
        }
        int itemCount = getItemCount();
        Collections.addAll(mDataList, list);
        notifyItemRangeInserted(itemCount, list.length);
    }

    /**
     * Remove.
     *
     * @param index the index
     */
    public void remove(int index) {
        int itemCount = getItemCount();
        if (index < 0 || index > itemCount) {
            return;
        }
        mDataList.remove(index);
        notifyItemRemoved(index);
    }

    /**
     * Remove all.
     */
    public void removeAll() {
        int itemCount = getItemCount();
        if (itemCount <= 0) {
            return;
        }
        mDataList.clear();
        notifyItemRangeRemoved(0, itemCount);
    }

    /**
     * Gets item.
     *
     * @param index the index
     * @return the item
     */
    public @Nullable Data getItem(int index) {
        int itemCount = getItemCount();
        if (index < 0 || index >= itemCount) {
            return null;
        }
        return mDataList.get(index);
    }

    @Override
    public final int getItemCount() {
        return mDataList.size();
    }

    public final List<Data> getDataList() {
        return mDataList;
    }

    /**
     * Create binding view holder binding view holder.
     *
     * @param <Binding> the type parameter
     * @param aClass    the a class
     * @param parent    the parent
     * @param index     the index
     * @return the binding view holder
     */
    @NonNull
    public static <Binding extends ViewBinding> BindingViewHolder<Binding> createBindingViewHolder(Class<?> aClass, @NonNull ViewGroup parent, int index) {
        Type genericSuperclass = aClass.getGenericSuperclass();
        Type[] actualTypeArguments;
        if (!(genericSuperclass instanceof ParameterizedType)) {
            return createBindingViewHolder(aClass.getSuperclass(), parent, index);
        } else {
            actualTypeArguments = ((ParameterizedType) genericSuperclass).getActualTypeArguments();
            if (actualTypeArguments.length < (index + 1)) {
                return createBindingViewHolder(aClass.getSuperclass(), parent, index);
            }
        }

        Class<Binding> c = (Class<Binding>) actualTypeArguments[index];
        Binding binding = null;
        try {
            binding = (Binding) c.getDeclaredMethod("inflate", LayoutInflater.class).invoke(null, LayoutInflater.from(parent.getContext()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new BindingViewHolder<>(binding);
    }

}
