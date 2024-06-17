package io.agora.scene.base.component;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import io.agora.scene.base.utils.UiUtil;

/**
 * The type Base recycler view adapter.
 *
 * @param <B> the type parameter
 * @param <T> the type parameter
 * @param <H> the type parameter
 */
public class BaseRecyclerViewAdapter<B extends ViewBinding, T, H extends BaseRecyclerViewAdapter.BaseViewHolder<B, T>> extends RecyclerView.Adapter<H> {

    /**
     * The Data list.
     */
    public List<T> dataList;
    /**
     * The M on item click listener.
     */
    private final OnItemClickListener<T> mOnItemClickListener;
    /**
     * The Selected index.
     */
    private int selectedIndex = -1;

    /**
     * The Binding class.
     */
    private Class<B> bindingClass;
    /**
     * The View holder class.
     */
    private final Class<H> viewHolderClass;


    /**
     * Instantiates a new Base recycler view adapter.
     *
     * @param dataList        the data list
     * @param viewHolderClass the view holder class
     */
    public BaseRecyclerViewAdapter(@Nullable List<T> dataList, Class<H> viewHolderClass) {
        this(dataList, null, viewHolderClass);
    }


    /**
     * Instantiates a new Base recycler view adapter.
     *
     * @param dataList        the data list
     * @param listener        the listener
     * @param viewHolderClass the view holder class
     */
    public BaseRecyclerViewAdapter(@Nullable List<T> dataList, @Nullable OnItemClickListener<T> listener, Class<H> viewHolderClass) {
        this.viewHolderClass = viewHolderClass;
        if (dataList == null) {
            this.dataList = new ArrayList<>();
        } else {
            this.dataList = new ArrayList<>(dataList);
        }

        this.mOnItemClickListener = listener;
    }

    /**
     * Create holder h.
     *
     * @param mBinding the m binding
     * @return the h
     */
    @Nullable
    private H createHolder(B mBinding) {
        ensureBindingClass();
        try {
            Constructor<H> constructor = viewHolderClass.getConstructor(bindingClass);
            return constructor.newInstance(mBinding);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * On create view holder h.
     *
     * @param parent   the parent
     * @param viewType the view type
     * @return the h
     */
    @NonNull
    @Override
    public H onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        B mBinding = getViewBindingByReflect(LayoutInflater.from(parent.getContext()), parent);
        H holder = createHolder(mBinding);

        assert holder != null;

        if (mOnItemClickListener != null) {
            holder.mListener = (view, position, itemViewType) -> {
                T itemData = getItemData(position);
                if (itemData == null) {
                    mOnItemClickListener.onItemClick(view, position, viewType);
                } else {
                    mOnItemClickListener.onItemClick(itemData, view, position, viewType);
                }
            };
        }
        return holder;
    }

    /**
     * On bind view holder.
     *
     * @param holder   the holder
     * @param position the position
     */
    @Override
    public void onBindViewHolder(@NonNull H holder, int position) {
        T data = dataList.get(position);
        holder.binding(data, selectedIndex);
    }

    /**
     * Gets item count.
     *
     * @return the item count
     */
    @Override
    public int getItemCount() {
        return dataList == null ? 0 : dataList.size();
    }

    /**
     * Gets item data.
     *
     * @param position the position
     * @return the item data
     */
    @Nullable
    public T getItemData(int position) {
        if (dataList == null) {
            return null;
        }

        if (position < 0 || dataList.size() <= position) {
            return null;
        }

        return dataList.get(position);
    }

    /**
     * Gets view binding by reflect.
     *
     * @param inflater  the inflater
     * @param container the container
     * @return the view binding by reflect
     */
    public B getViewBindingByReflect(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        ensureBindingClass();
        try {
            return UiUtil.getViewBinding(bindingClass, inflater, container);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Contains boolean.
     *
     * @param data the data
     * @return the boolean
     */
//<editor-fold desc="CURD">
    public boolean contains(@NonNull T data) {
        if (dataList == null) {
            return false;
        }
        return dataList.contains(data);
    }

    /**
     * Index of int.
     *
     * @param data the data
     * @return the int
     */
    public int indexOf(@NonNull T data) {
        if (dataList == null) {
            return -1;
        }
        return dataList.indexOf(data);
    }

    /**
     * Sets data list.
     *
     * @param dataList the data list
     */
    public void setDataList(@NonNull List<T> dataList) {
        this.dataList = dataList;
        notifyDataSetChanged();
    }

    /**
     * Add item.
     *
     * @param data the data
     */
    public void addItem(@NonNull T data) {
        if (dataList == null) {
            dataList = new ArrayList<>();
        }

        int index = dataList.indexOf(data);
        if (index < 0) {
            dataList.add(data);
            notifyItemInserted(dataList.size() - 1);
        } else {
            dataList.set(index, data);
            notifyItemChanged(index);
        }
    }

    /**
     * Add item.
     *
     * @param data  the data
     * @param index the index
     */
    public void addItem(@NonNull T data, int index) {
        if (dataList == null) {
            dataList = new ArrayList<>();
        }

        int indexTemp = dataList.indexOf(data);
        if (indexTemp < 0) {
            dataList.add(index, data);
            notifyItemRangeChanged(index, dataList.size() - index);
        } else {
            dataList.set(index, data);
            notifyItemChanged(index);
        }
    }

    /**
     * Update.
     *
     * @param index the index
     * @param data  the data
     */
    public void update(int index, @NonNull T data) {
        if (dataList == null) {
            dataList = new ArrayList<>();
        }

        dataList.set(index, data);
        notifyItemChanged(index);
    }

    /**
     * Clear.
     */
    public void clear() {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        dataList.clear();
        notifyDataSetChanged();
    }

    /**
     * Delete item.
     *
     * @param posion the posion
     */
    public void deleteItem(@Size(min = 0) int posion) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        if (0 <= posion && posion < dataList.size()) {
            dataList.remove(posion);
            notifyItemRemoved(posion);
        }
    }

    /**
     * Delete item.
     *
     * @param data the data
     */
    public void deleteItem(@NonNull T data) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        int index = dataList.indexOf(data);
        if (0 <= index && index < dataList.size()) {
            dataList.remove(data);
            notifyItemRemoved(index);
        }
    }

    /**
     * Ensure binding class.
     */
//</editor-fold>
    public void ensureBindingClass() {
        if (bindingClass == null) {
            bindingClass = UiUtil.getGenericClass(viewHolderClass, 0);
        }
    }

    /**
     * The type Base view holder.
     *
     * @param <B> the type parameter
     * @param <T> the type parameter
     */
    public static abstract class BaseViewHolder<B extends ViewBinding, T> extends RecyclerView.ViewHolder {
        /**
         * The M listener.
         */
        OnHolderItemClickListener mListener;
        /**
         * The M binding.
         */
        protected final B mBinding;

        /**
         * Instantiates a new Base view holder.
         *
         * @param mBinding the m binding
         */
        public BaseViewHolder(@NonNull B mBinding) {
            super(mBinding.getRoot());
            this.mBinding = mBinding;
            mBinding.getRoot().setOnClickListener(this::onItemClick);
        }

        /**
         * On item click.
         *
         * @param view the view
         */
        public void onItemClick(View view) {
            if (mListener != null) {
                mListener.onItemClick(view, getAdapterPosition(), getItemViewType());
            }
        }

        /**
         * The interface On holder item click listener.
         */
        interface OnHolderItemClickListener {
            /**
             * On item click.
             *
             * @param view         the view
             * @param position     the position
             * @param itemViewType the item view type
             */
            void onItemClick(View view, int position, int itemViewType);
        }

        /**
         * Binding.
         *
         * @param data          the data
         * @param selectedIndex the selected index
         */
        public abstract void binding(@Nullable T data, int selectedIndex);
    }
}
