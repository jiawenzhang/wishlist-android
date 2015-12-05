package com.wish.wishlist.social;

import android.app.FragmentManager;
import android.app.Activity;
import android.content.Context;
import android.app.DialogFragment;

public class ShareHelper {
	Context _ctx;
	long[] mItemIds;

public ShareHelper(Context ctx, long[] itemIds) {
	_ctx = ctx;
	mItemIds = itemIds;
}

public void share() {
    DialogFragment newFragment = ShareAppDialogFragment.newInstance(mItemIds, _ctx);
    FragmentManager m = ((Activity) _ctx).getFragmentManager();
    newFragment.show(m, "dialog");
}
}

