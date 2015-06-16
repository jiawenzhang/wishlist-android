package com.wish.wishlist.db;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

/***
 * LocationDBManager provides access to operations on data in location table
 */
public class LocationDBManager extends DBManager {

    public static final String KEY_ID = "_id";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE= "longitude";

    public static final String DB_TABLE = "location";
    private static final String TAG="LocationDBManager";

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     *
     * @param ctx
     *            the Context within which to work
     */
    public LocationDBManager(Context ctx) {
        super(ctx);
    }

    /**
     * Return a Cursor positioned at the location that matches the given rowId
     *
     * @param rowId
     * @return Cursor positioned to matching location, if found
     * @throws SQLException
     *             if location could not be found/retrieved
     */
    public Cursor getLocation(long rowId) throws SQLException {
        Cursor mCursor = DBAdapter.getInstance(mCtx).db().query(true, DB_TABLE, null, KEY_ID + "=" + rowId, null, null,
                null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public double getLatitude(long rowId) throws SQLException {
        Cursor mCursor = DBAdapter.getInstance(mCtx).db().query(true, DB_TABLE, null, KEY_ID + "=" + rowId, null, null,
                null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        double lat = 3;
        lat = mCursor.getDouble(mCursor.getColumnIndexOrThrow(LocationDBManager.KEY_LATITUDE));
        return lat;
    }

    public double getLongitude(long rowId) throws SQLException {
        Cursor mCursor = DBAdapter.getInstance(mCtx).db().query(true, DB_TABLE, null, KEY_ID + "=" + rowId, null, null,
                        null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        double lng = 2;
        lng = mCursor.getDouble(mCursor.getColumnIndexOrThrow(LocationDBManager.KEY_LONGITUDE));
        return lng;
    }
}
