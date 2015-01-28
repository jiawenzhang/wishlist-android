package com.wish.wishlist.util.social;

import java.util.List; 

import android.util.Log; 
import android.app.Activity;
import android.app.AlertDialog; 
import android.content.Context; 
import android.content.DialogInterface; 
import android.content.Intent; 
import android.content.pm.ResolveInfo; 

import com.facebook.android.Facebook;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.wish.wishlist.R;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;  
import com.wish.wishlist.activity.WishItemPostToSNS;
import com.wish.wishlist.AnalyticsHelper;
import com.wish.wishlist.util.DialogOnShowListener;

public class ShareHelper {
	Context _ctx;
	long _itemId;
//	String _subject;
//	String _message;
	Facebook _facebook;
//	Bitmap _bitmap;

//public ShareHelper(Context ctx, String subject, String message, Bitmap bitmap) {
public ShareHelper(Context ctx, long itemId) {
	_ctx = ctx; 
	_itemId = itemId;
	//_subject = subject;
	//_message = message;
	//_bitmap = bitmap;
	_facebook = null;
}

public Facebook share() {
	Intent sendIntent = new Intent(android.content.Intent.ACTION_SEND);
	sendIntent.setType("*/*");
	List activities = _ctx.getPackageManager().queryIntentActivities(sendIntent, 0);
	AlertDialog.Builder builder = new AlertDialog.Builder(_ctx); 
	builder.setIcon(0); //no icon in the title
	builder.setTitle("Share wish via"); 
	final ShareIntentListAdapter adapter = new ShareIntentListAdapter((Activity)_ctx, R.layout.share_app_list, R.id.shareAppLabel, activities.toArray()); 
	builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			ResolveInfo info = (ResolveInfo) adapter.getItem(which);
			if (info.activityInfo.packageName.contains("facebook")) { 
				//Intent facebookPostIntent = new Intent(_ctx, FacebookPost.class);
				//facebookPostIntent.putExtra("itemId", _itemId);
				//((Activity)_ctx).startActivityForResult(facebookPostIntent, 1);
				Intent snsIntent = new Intent(_ctx, WishItemPostToSNS.class);
				snsIntent.putExtra("itemId", _itemId);
				((Activity)_ctx).startActivityForResult(snsIntent, 1);
				//new PostToFacebookDialog(_ctx, _message).show();
				Log.d("share", "facebook");
			}
			else {
				WishItem item = WishItemManager.getInstance(_ctx).retrieveItembyId(_itemId);
				String message = item.getShareMessage(false);
				Log.d("share", "others");
				Intent intent = new Intent(android.content.Intent.ACTION_SEND);
				intent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
				intent.setType("*/*");
				//intent.putExtra(Intent.EXTRA_SUBJECT, _subject);
				intent.putExtra(Intent.EXTRA_TEXT, message);
				intent.putExtra(Intent.EXTRA_STREAM, item.getFullsizePicUri());
				((Activity)_ctx).startActivity(intent);

			}
            Tracker t = ((AnalyticsHelper) ((Activity)_ctx).getApplication()).getTracker(AnalyticsHelper.TrackerName.APP_TRACKER);
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("Social")
                    .setAction("ShareWish")
                    .setLabel(info.activityInfo.packageName)
                    .build());
		}
	});

    AlertDialog dialog = builder.create();
    dialog.setOnShowListener(new DialogOnShowListener((Activity)_ctx));
    dialog.show();
	return _facebook;
}
}

