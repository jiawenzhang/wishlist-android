package com.wish.wishlist.social;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.app.DialogFragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Toast;

import com.wish.wishlist.R;
//import com.wish.wishlist.activity.FacebookPostActivity;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.util.Analytics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiawen on 15-03-15.
 */

public class ShareAppDialogFragment extends DialogFragment {
    static final String TAG = "ShareAppDialogFragment";
    static long[] mItemIds;
    static Context mCtx;
    static final Boolean ENABLE_SHARE_TO_FB = false;

    /**
     * Create a new instance of MyDialogFragment, providing "num"
     * as an argument.
     */
    static ShareAppDialogFragment newInstance(long[] itemIds, Context context) {
        mItemIds = itemIds;
        mCtx = context;
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

        if (ENABLE_SHARE_TO_FB) {
            // Move facebook to the top of the list
            for (ResolveInfo info : _activities) {
                if (info.activityInfo.packageName.contains("facebook")) {
                    int i = _activities.indexOf(info);
                    _activities.add(0, _activities.remove(i));
                    break;
                }
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
                if (info.activityInfo.packageName.contains("facebook") && ENABLE_SHARE_TO_FB) {
                    if (!isNetworkOnline()) {
                        Toast.makeText(mCtx, "Network not available", Toast.LENGTH_LONG).show();
                        return;
                    }
                    // Fixme: need to fix for facebook sdk 4.6
                    //Intent facebookPostIntent = new Intent(mCtx, FacebookPostActivity.class);
                    //facebookPostIntent.putExtra("itemId", _itemId);
                    //((Activity) mCtx).startActivityForResult(facebookPostIntent, 1);

                    // Intent snsIntent = new Intent(mCtx, PostToSNSActivity.class);
                    // snsIntent.putExtra("itemId", _itemId);
                    // ((Activity)mCtx).startActivityForResult(snsIntent, 1);
                    //new PostToFacebookDialog(mCtx, _message).show();
                } else {
                    // Fixme: share to text message does not work

                    String text = "a wish";
                    if (mItemIds.length > 1) {
                        text = mItemIds.length + " wishes";
                    }

                    String message = "Shared " + text + " from Beans Wishlist\n\n";
                    ArrayList<Uri> imageUris = new ArrayList<>();
                    for (long item_id : mItemIds) {
                        WishItem item = WishItemManager.getInstance().getItemById(item_id);
                        message += (item.getName() + "\n\n");

                        String path = item.getFullsizePicPath();
                        if (path != null && !path.isEmpty()) {
                            try {
                                Uri uri = FileProvider.getUriForFile(getActivity(), "com.wish.wishlist.fileprovider", new File(item.getFullsizePicPath()));
                                Log.d(TAG, uri.toString());
                                imageUris.add(uri);
                            } catch (IllegalArgumentException e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                    }

                    Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                    intent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
                    intent.setType("*/*");
                    intent.setAction(Intent.ACTION_SEND_MULTIPLE);

                    //intent.putExtra(Intent.EXTRA_SUBJECT, _subject);
                    intent.putExtra(Intent.EXTRA_TEXT, message);
                    intent.putExtra(Intent.EXTRA_STREAM, imageUris);
                    mCtx.startActivity(intent);
                }

                Analytics.send(Analytics.SOCIAL, "ShareWish", info.activityInfo.packageName);

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
            ConnectivityManager cm = (ConnectivityManager) mCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
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
