package com.wish.wishlist.util;

/**
 * Created by jiawen on 15-10-05.
 */

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import com.parse.ParseObject;
import java.util.List;

public class WishAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected List<ParseObject> mWishList;
    private static final String TAG = "WishAdapter";

    public WishAdapter(List<ParseObject> wishList) {
        mWishList = wishList;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {}

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) { return null; }

    public void add(int position, ParseObject item) {
        mWishList.add(position, item);
        notifyItemInserted(position);
    }

    protected void remove(int position) {
        mWishList.remove(position);
        notifyItemRemoved(position);
    }

    public void removeAll() {
        int size = mWishList.size();
        mWishList.clear();
        notifyItemRangeRemoved(0, size);
    }

    // Return the size of your data set (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mWishList.size();
    }
}
