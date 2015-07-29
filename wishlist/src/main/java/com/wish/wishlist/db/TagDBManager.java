package com.wish.wishlist.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

import java.util.ArrayList;

/***
 * TagDBManager provides access to operations on data in ItemCategory table
 */
public class TagDBManager extends DBManager {
    public static final String KEY_ID = "_id";
    public static final String KEY_NAME = "name";

    public static final String DB_TABLE = "Tag";
    private static final String TAG="TagDBManager";

    private static TagDBManager _instance = null;

    public static TagDBManager instance() {
        if (_instance == null) {
            _instance = new TagDBManager();
        }
        return _instance;
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     */
    private TagDBManager() {}

    /**
     * Create a new tag. If the tag exists, replace it. If successfully created return the new rowId
     * for that tag, otherwise return a -1 to indicate failure.
     *
     * @param name
     * @return rowId or -1 if failed
     */
    public long createTag(String name) {
        String where = KEY_NAME + " = ?";
        Cursor cursor = DBAdapter.getInstance().db().query(DB_TABLE, new String[]{KEY_ID}, where, new String[]{name}, null, null, null);
        while (cursor.moveToNext()) {
            long tagId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID));
            return tagId;
        }
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NAME, name);
        long tagId = DBAdapter.getInstance().db().insert(DB_TABLE, null, initialValues);
        return tagId;
    }

    public void deleteTag(String name) {
        String where = KEY_NAME + "=" + name;
        DBAdapter.getInstance().db().delete(DB_TABLE, where, null);
    }

    /**
     * Delete the tag with the given rowId
     *
     * @param rowId
     * @return true if deleted, false otherwise
     */
    public boolean deleteTag(long tagId) {
        boolean success = DBAdapter.getInstance().db().delete(DB_TABLE, KEY_ID + "=" + tagId, null) > 0; //$NON-NLS-1$
        return success;
    }

    /**
     * Return a Cursor over the list of all tags in the database
     *
     * @return Cursor over all cars
     */
    public ArrayList<String> getAllTags() {
        ArrayList<String> tagList = new ArrayList<String>();
        Cursor cursor = DBAdapter.getInstance().db().query(DB_TABLE, new String[]{KEY_ID, KEY_NAME}, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                String tagName = cursor.getString(cursor.getColumnIndexOrThrow(TagDBManager.KEY_NAME));
                tagList.add(tagName);
            } while (cursor.moveToNext());
        }
        return tagList;
    }

    public ArrayList<String> getTagsByIds(String[] ids) {
        ArrayList<String> tags = new ArrayList<String>();
        String query = "SELECT * FROM Tag"
                + " WHERE rowId IN (" + makePlaceholders(ids.length) + ")";
        Cursor cursor = DBAdapter.getInstance().db().rawQuery(query, ids);
        while (cursor.moveToNext()) {
            tags.add(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)));
        }
        return tags;
    }

    public long getIdByName(String name) {
        long tagId = -1;
        String where = KEY_NAME + " = ?";
        Cursor cursor = DBAdapter.getInstance().db().query(DB_TABLE, new String[]{KEY_ID}, where, new String[]{name}, null, null, null);
        while (cursor.moveToNext()) {
            tagId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID));
        }
        return tagId;
    }

    /**
     * Return a Cursor positioned at the Tag that matches the given rowId
     *
     * @param rowId
     * @return Cursor positioned to matching tags, if found
     * @throws SQLException
     *             if car could not be found/retrieved
     */
    public Cursor getTag(long rowId) throws SQLException {

        Cursor mCursor = DBAdapter.getInstance().db().query(true, DB_TABLE, new String[]{KEY_ID, KEY_NAME},
                        KEY_ID + "=" + rowId, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Update the Tag.
     *
     * @param rowId
     * @param name
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateTag(long rowId, String name) {
        ContentValues args = new ContentValues();
        args.put(KEY_NAME, name);
        return DBAdapter.getInstance().db().update(DB_TABLE, args, KEY_ID + "=" + rowId, null) > 0;
    }
}
