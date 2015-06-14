package com.wish.wishlist.db;

import android.content.ContentValues;
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
    public static final String KEY_ADDSTR = "addStr";
    public static final String KEY_STREET_NO = "street_no";
    public static final String KEY_STREET = "street";
    public static final String KEY_CITY = "city";
    public static final String KEY_STATE = "state";
    public static final String KEY_COUNTRY = "country";
    public static final String KEY_POSTCODE = "postcode";

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
     * Add a new location. If the location is successfully created return the new rowId
     * for that location, otherwise return a -1 to indicate failure.
     *
     * @param name
     * @return rowId or -1 if failed
     */
    public long addLocation(double lat, double lng, String addStr, int streetNO, String street, String city,
                            String state, String country, String postcode) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_LATITUDE, lat);
        initialValues.put(KEY_LONGITUDE, lng);
        initialValues.put(KEY_ADDSTR, addStr);
        initialValues.put(KEY_STREET_NO, streetNO);
        initialValues.put(KEY_STREET, street);
        initialValues.put(KEY_CITY, city);
        initialValues.put(KEY_STATE, state);
        initialValues.put(KEY_COUNTRY, country);
        initialValues.put(KEY_POSTCODE, postcode);

        return this.mDb.insert(DB_TABLE, null, initialValues);
    }

    public long updateLocation(long id, double lat, double lng, String addStr, int streetNO, String street, String city,
                               String state, String country, String postcode) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_LATITUDE, lat);
        initialValues.put(KEY_LONGITUDE, lng);
        initialValues.put(KEY_ADDSTR, addStr);
        initialValues.put(KEY_STREET_NO, streetNO);
        initialValues.put(KEY_STREET, street);
        initialValues.put(KEY_CITY, city);
        initialValues.put(KEY_STATE, state);
        initialValues.put(KEY_COUNTRY, country);
        initialValues.put(KEY_POSTCODE, postcode);

        String where = String.format("_id = '%d'", id);
        return this.mDb.update(DB_TABLE, initialValues, where, null);
    }
    /**
     * Delete the location with the given rowId
     *
     * @param rowId
     * @return true if deleted, false otherwise
     */
    public boolean deleteLocation(long rowId) {

        return this.mDb.delete(DB_TABLE, KEY_ID + "=" + rowId, null) > 0; //$NON-NLS-1$
    }

    /**
     * Return a Cursor over the list of all location in the database
     *
     * @return Cursor over all location
     */
    public Cursor getAllLocation() {

        return this.mDb.query(DB_TABLE, null, null, null, null, null, null);
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

        Cursor mCursor =

                this.mDb.query(true, DB_TABLE, null, KEY_ID + "=" + rowId, null, null,
                        null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public double getLatitude(long rowId) throws SQLException {

        Cursor mCursor =

                this.mDb.query(true, DB_TABLE, null, KEY_ID + "=" + rowId, null, null,
                        null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        double lat = 3;
        lat = mCursor.getDouble(mCursor.
                getColumnIndexOrThrow(LocationDBManager.KEY_LATITUDE));
        return lat;
    }

    public double getLongitude(long rowId) throws SQLException {

        Cursor mCursor =

                this.mDb.query(true, DB_TABLE, null, KEY_ID + "=" + rowId, null, null,
                        null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        double lng = 2;
        lng = mCursor.getDouble(mCursor.
                getColumnIndexOrThrow(LocationDBManager.KEY_LONGITUDE));
        return lng;
    }


    /**
     * Return the address positioned at the location that matches the given rowId
     *
     * @param rowId
     * @return String of address matching location id, if found; otherwise, return null
     */
    public String getAddress(long _id){

        String addressStr = null;
        Cursor mCursor =

                this.mDb.query(true, DB_TABLE, new String[] { KEY_ADDSTR }, KEY_ID + "=" + _id, null, null,
                        null, null, null);
        if (mCursor != null) {

            mCursor.moveToFirst();
            addressStr =  mCursor.getString(mCursor.
                    getColumnIndexOrThrow(LocationDBManager.KEY_ADDSTR));

        }
        return addressStr;
    }
}
