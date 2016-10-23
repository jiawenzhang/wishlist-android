package com.wish.wishlist.db;

import android.database.Cursor;
import android.database.SQLException;

/***
 * StoreDBManager provides access to opexarations on data in store table
 */
public class StoreDBManager extends DBManager {
    private static final String KEY_ID = "_id";
    private static final String KEY_NAME = "store_name";
    public static final String KEY_LOCATION_ID = "location_id";

    public static final String DB_TABLE = "store";
    private static final String TAG = "StoreDBManager";

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     */
    public StoreDBManager() {}

    public Cursor getStore(long _id) throws SQLException {

        Cursor mCursor = DBAdapter.getInstance().db().query(true, DB_TABLE, new String[]{KEY_ID, KEY_NAME, KEY_LOCATION_ID},
                        KEY_ID + "=" + _id, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }
}
