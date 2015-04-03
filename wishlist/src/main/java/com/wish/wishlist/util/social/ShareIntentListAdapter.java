package com.wish.wishlist.util.social;

import android.app.Activity;
import android.content.pm.ResolveInfo; 
import android.view.LayoutInflater; 
import android.view.View; 
import android.view.ViewGroup; 
import android.widget.ArrayAdapter; 
import android.widget.ImageView; 
import android.widget.TextView;  
import com.wish.wishlist.R;

public class ShareIntentListAdapter extends ArrayAdapter {
	Activity _ctx;
	Object[] _items;
	int _layoutId; 

public ShareIntentListAdapter(Activity context, int layoutId, int textViewResourceId, Object[] items) {
	super(context, layoutId, textViewResourceId, items); 
	_ctx = context;
	_items = items; 
	_layoutId = layoutId;
}

public View getView(int pos, View convertView, ViewGroup parent) { 
	LayoutInflater inflater=_ctx.getLayoutInflater();
	View view = inflater.inflate(_layoutId, null);
	TextView label = (TextView) view.findViewById(R.id.shareAppLabel);
    label.setText(((ResolveInfo)_items[pos]).activityInfo.loadLabel(_ctx.getPackageManager()).toString());
	ImageView image = (ImageView) view.findViewById(R.id.shareAppIcon);
    image.setImageDrawable(((ResolveInfo)_items[pos]).activityInfo.loadIcon(_ctx.getPackageManager()));
    return(view);
}
}
