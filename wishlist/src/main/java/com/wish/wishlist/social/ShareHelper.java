package com.wish.wishlist.social;

import android.app.FragmentManager;
import android.app.Activity;
import android.content.Context;
import android.app.DialogFragment;

public class ShareHelper {
	Context _ctx;
	long _itemId;

public ShareHelper(Context ctx, long itemId) {
	_ctx = ctx;
	_itemId = itemId;
}

public void share() {
    DialogFragment newFragment = ShareAppDialogFragment.newInstance(_itemId, _ctx);
    FragmentManager m = ((Activity) _ctx).getFragmentManager();
    newFragment.show(m, "dialog");
}
}

