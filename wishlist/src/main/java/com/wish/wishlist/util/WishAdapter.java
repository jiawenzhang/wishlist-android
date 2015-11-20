package com.wish.wishlist.util;

/**
 * Created by jiawen on 15-10-05.
 */

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.SwappingHolder;
import com.wish.wishlist.model.WishItem;

import java.util.List;

public class WishAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /****************** WishTapListener ************************/
    onWishTapListener mWishTapListener;
    public interface onWishTapListener {
        void onWishTapped(WishItem item);
    }

    protected void onWishTapped(WishItem item) {
        if (mWishTapListener != null) {
            mWishTapListener.onWishTapped(item);
        }
    }

    public void setWishTapListener(Activity a) {
        mWishTapListener = (onWishTapListener) a;
    }
    /***********************************************************/

    /****************** WishLongTapListener ************************/
    onWishLongTapListener mWishLongTapListener;
    public interface onWishLongTapListener {
        void onWishLongTapped();
    }

    protected void onWishLongTapped() {
        if (mWishLongTapListener != null) {
            mWishLongTapListener.onWishLongTapped();
        }
    }

    public void setWishLongTapListener(Activity a) {
        if (onWishLongTapListener.class.isInstance(a)) {
            mWishLongTapListener = (onWishLongTapListener) a;
        }
    }
    /***********************************************************/

    /****************** WishSelectedListener ************************/
    onWishSelectedListener mWishSelectedListener;
    public interface onWishSelectedListener {
        void onWishSelected(long itemId);
    }

    protected void onWishSelected(long itemId) {
        if (mWishSelectedListener != null) {
            mWishSelectedListener.onWishSelected(itemId);
        }
    }

    public void setWishSelectedListener(Activity a) {
        mWishSelectedListener = (onWishSelectedListener) a;
    }
    /***********************************************************/

    public class ItemSwappingHolder extends SwappingHolder implements View.OnClickListener, View.OnLongClickListener {
        public ItemSwappingHolder(View itemView, MultiSelector multiSelector) {
            super(itemView, multiSelector);

            itemView.setOnClickListener(this);
            itemView.setLongClickable(true);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            // remember which item is selected
            final WishItem item = mWishList.get(getAdapterPosition());
            final long itemId =  item.getId();
            if (mMultiSelector.tapSelection(this)) {
                Log.d(TAG, "selection mode wish clicked");
                onWishSelected(itemId);
                // Selection mode is on, so tapSelection() toggled item selection.
            } else {
                // Selection mode is off; handle normal item click here.
                Log.d(TAG, "normal, wish clicked");
                onWishTapped(item);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            Log.d(TAG, "onLongClick");
            final WishItem selectedItem = mWishList.get(getAdapterPosition());
            onWishLongTapped();
            onWishSelected(selectedItem.getId());
            mMultiSelector.setSelected(this, true);
            return true;
        }
    }

    protected MultiSelector mMultiSelector;
    protected List<WishItem> mWishList;
    private static final String TAG = "WishAdapter";

    public WishAdapter(List<WishItem> wishList, Activity fromActivity, MultiSelector ms) {
        mWishList = wishList;
        mMultiSelector = ms;
        setWishTapListener(fromActivity);
        setWishLongTapListener(fromActivity);
        setWishSelectedListener(fromActivity);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {}

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) { return null; }

    // Return the size of your data set (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mWishList.size();
    }

    public void setWishList(final List<WishItem> wishList) {
        mWishList = wishList;
        notifyDataSetChanged();
    }

    public void add(int position, final WishItem item) {
        mWishList.add(position, item);
        notifyItemInserted(position);
    }

    protected void remove(int position) {
        mWishList.remove(position);
        notifyItemRemoved(position);
    }

    public void removeByItemIds(List<Long> item_ids) {
        final int size = mWishList.size();
        for(int i = size - 1; i >= 0; i--) {
            final long item_id = mWishList.get(i).getId();
            if (item_ids.contains(item_id)) {
                mWishList.remove(i);
                notifyItemRemoved(i);
                item_ids.remove(item_id);
            }
        }
    }

    public void removeAll() {
        int size = mWishList.size();
        mWishList.clear();
        notifyItemRangeRemoved(0, size);
    }
}
