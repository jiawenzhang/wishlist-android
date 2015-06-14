package com.wish.wishlist.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

/***
 * StoreDBManager provides access to opexarations on data in store table
 */
public class StoreDBManager extends DBManager {
    public static final String KEY_ID = "_id";
    public static final String KEY_NAME = "store_name";
    public static final String KEY_LOCATION_ID = "location_id";

    public static final String DB_TABLE = "store";
    private static final String TAG="StoreDBManager";

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     *
     * @param ctx
     *            the Context within which to work
     */
    public StoreDBManager(Context ctx) {
        super(ctx);
    }

    /**
     * Add a new store. If the store is successfully created return the new rowId
     * for that store, otherwise return a -1 to indicate failure.
     *
     * @param name
     * @return rowId or -1 if failed
     */
    public long addStore(String name, long locationID) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_LOCATION_ID, locationID);
        initialValues.put(KEY_NAME, name);
        return this.mDb.insert(DB_TABLE, null, initialValues);
    }

    /**
     * Delete the store with the given rowId
     *
     * @param rowId
     * @return true if deleted, false otherwise
     */
    public boolean deleteStore(long rowId) {

        return this.mDb.delete(DB_TABLE, KEY_ID + "=" + rowId, null) > 0; //$NON-NLS-1$
    }

    /**
     * Return a Cursor over the list of all stores in the database
     *
     * @return Cursor over all stores
     */
    public Cursor getAllStores() {

        return this.mDb.query(DB_TABLE, new String[] { KEY_ID, KEY_NAME, KEY_LOCATION_ID},
                null, null, null, null, null);
    }

    /**
     * Return a Cursor positioned at the store that matches the given rowId
     *
     * @param rowId
     * @return Cursor positioned to matching store, if found
     * @throws SQLException
     *             if store could not be found/retrieved
     */
    public Cursor getStore(long _id) throws SQLException {

        Cursor mCursor =

                this.mDb.query(true, DB_TABLE, new String[] { KEY_ID, KEY_NAME, KEY_LOCATION_ID},
                        KEY_ID + "=" + _id, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Return a store name that matches the given Id
     *
     * @param rowId
     * @return a String of store name positioned to matching store id, if found; otherwise, return null
     */
    public String getStoreName(long _id){
        String storeName = null;
        Cursor mCursor =

                this.mDb.query(true, DB_TABLE, new String[] { KEY_NAME },
                        KEY_ID + "=" + _id, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();

            storeName = mCursor.getString(mCursor.
                    getColumnIndexOrThrow(StoreDBManager.KEY_NAME));

        }
        return storeName;
    }

    /**
     * Update the store.
     *
     * @param rowId
     * @param name
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateStore(long rowId, String name, long locationID) {
        ContentValues args = new ContentValues();
        args.put(KEY_NAME, name);
        args.put(KEY_LOCATION_ID, locationID);

        return this.mDb.update(DB_TABLE, args, KEY_ID + "=" + rowId, null) > 0;
    }
}
