package com.aripio.wishlist;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class WishItemProvider extends ContentProvider{

	public static final Uri CONTENT_URI = 
        Uri.parse("content://com.aripio.wishlist.provider");
	
	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		return 0;
    }

    @Override
    public String getType(Uri uri) {
    	return null;
    }

	@Override
	public Uri insert(Uri uri, ContentValues values) {
	   return null;
	}

	@Override
	public boolean onCreate() {
	   return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
	      String[] selectionArgs, String sortOrder) {
	   return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
	      String[] selectionArgs) {
	   return 0;
	}
}
