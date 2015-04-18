package com.wish.wishlist.util.social;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.wish.wishlist.AnalyticsHelper;
import com.wish.wishlist.R;
import com.wish.wishlist.activity.FacebookPost;
import com.wish.wishlist.activity.WishItemPostToSNS;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;

import java.util.List;

/**
 * Created by jiawen on 15-03-15.
 */

public class ShareAppDialogFragment extends DialogFragment {
    static long _itemId;
    static Context _ctx;

    /**
     * Create a new instance of MyDialogFragment, providing "num"
     * as an argument.
     */
    static ShareAppDialogFragment newInstance(long itemId, Context context) {
        _itemId = itemId;
        _ctx = context;
        return new ShareAppDialogFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int style = DialogFragment.STYLE_NO_TITLE, theme;
        theme = android.R.style.Theme_Holo_Light_Dialog;
        setStyle(style, theme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Intent sendIntent = new Intent(android.content.Intent.ACTION_SEND);
        sendIntent.setType("*/*");
        List<ResolveInfo> _activities = getActivity().getPackageManager().queryIntentActivities(sendIntent, 0);

        // Move facebook to the top of the list
        for (ResolveInfo info : _activities) {
            if (info.activityInfo.packageName.contains("facebook")) {
                int i = _activities.indexOf(info);
                _activities.add(0, _activities.remove(i));
                break;
            }
        }

        View v = inflater.inflate(R.layout.share_app_grid, container, false);
        final GridView gridView = (GridView) v.findViewById(R.id.share_app_gridview);
        final ShareIntentListAdapter adapter = new ShareIntentListAdapter(getActivity(), R.layout.share_app, R.id.shareAppLabel, _activities.toArray());
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // do something here
                ResolveInfo info = (ResolveInfo) adapter.getItem(position);
                if (info.activityInfo.packageName.contains("facebook")) {
                    if (!isNetworkOnline()) {
                        Toast.makeText(_ctx, "Network not available", Toast.LENGTH_LONG).show();
                        return;
                    }
                    Intent facebookPostIntent = new Intent(_ctx, FacebookPost.class);
                    facebookPostIntent.putExtra("itemId", _itemId);
                    ((Activity) _ctx).startActivityForResult(facebookPostIntent, 1);

                    // Intent snsIntent = new Intent(_ctx, WishItemPostToSNS.class);
                    // snsIntent.putExtra("itemId", _itemId);
                    // ((Activity)_ctx).startActivityForResult(snsIntent, 1);
                    //new PostToFacebookDialog(_ctx, _message).show();
                } else {
                    WishItem item = WishItemManager.getInstance(_ctx).retrieveItembyId(_itemId);
                    String message = item.getShareMessage(false);
                    Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                    intent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
                    intent.setType("*/*");
                    //intent.putExtra(Intent.EXTRA_SUBJECT, _subject);
                    intent.putExtra(Intent.EXTRA_TEXT, message);
                    intent.putExtra(Intent.EXTRA_STREAM, item.getFullsizePicUri());
                    ((Activity) _ctx).startActivity(intent);
                }
                Tracker t = ((AnalyticsHelper) ((Activity) _ctx).getApplication()).getTracker(AnalyticsHelper.TrackerName.APP_TRACKER);
                t.send(new HitBuilders.EventBuilder()
                        .setCategory("Social")
                        .setAction("ShareWish")
                        .setLabel(info.activityInfo.packageName)
                        .build());

                dismiss();
            }
        });
        gridView.setAdapter(adapter);

        final Button button = (Button) v.findViewById(R.id.share_app_button);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gridView.setAdapter(adapter);
            }
        });

        return v;
    }

    public boolean isNetworkOnline() {
        boolean status = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) _ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getNetworkInfo(0);
            if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED) {
                status = true;
            } else {
                netInfo = cm.getNetworkInfo(1);
                if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED)
                    status = true;
            }
        } catch(Exception e){
            //e.printStackTrace();
            return false;
        }
        return status;
    }
}
