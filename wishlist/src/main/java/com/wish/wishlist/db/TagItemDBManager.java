package com.wish.wishlist.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;

/***
 * TagDBManager provides access to operations on data in ItemCategory table
 */
public class TagItemDBManager extends DBManager {
    public static final String TAG_ID = "tag_id";
	public static final String ITEM_ID = "item_id";
	public static final String DB_TABLE = "TagItem";

    private static TagItemDBManager _instance = null;

    public static TagItemDBManager instance() {
        if (_instance == null) {
            _instance = new TagItemDBManager();
        }
        return _instance;
    }

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 */
	private TagItemDBManager() {}

    private long Tag_item(String tagName, long itemId) {
        long tagId = TagDBManager.instance().createTag(tagName);
        return Tag_item(tagId, itemId);
    }

    private long Tag_item(long tagId, long itemId) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(TAG_ID, tagId);
        initialValues.put(ITEM_ID, itemId);
        long rowId = DBAdapter.getInstance().db().replace(DB_TABLE, null, initialValues);
        return rowId;
    }

    private void Untag_item(String tagName, long itemId) {
        long tagId = TagDBManager.instance().getIdByName(tagName);
        Untag_item(tagId, itemId);
    }

    private void Untag_item(long tagId, long itemId) {
        String where = TAG_ID + "=" + tagId + " AND " + ITEM_ID + "=" + itemId;
        DBAdapter.getInstance().db().delete(DB_TABLE, where, null);

        //Delete the tag in the tag table if no item is referencing it
        if (!tagExists(tagId)) {
            TagDBManager.instance().deleteTag(tagId);
        }
    }

    //This gets called when an item is deleted, we need to clean up both the TagItem table
    //and the Tag table
    public void Remove_tags_by_item(long itemId) {
        ArrayList<Long> tagIds = tagIds_by_item(itemId);

        //delete all the entries referencing this item in the TagItem table
        String where = ITEM_ID + "=" + itemId;
        DBAdapter.getInstance().db().delete(DB_TABLE, where, null);

        //Delete the tags in the tag table if no other items are referencing it
        for (long tagId : tagIds) {
            if (!tagExists(tagId)) {
                TagDBManager.instance().deleteTag(tagId);
            }
        }
    }

    Boolean tagExists(long tagId) {
        Cursor cursor = DBAdapter.getInstance().db().query(true, DB_TABLE, new String[]{TAG_ID}, TAG_ID + "=" + tagId, null, null, null, null, null);
        Boolean exists = cursor.getCount() >= 1;
        return exists;
    }

    public ArrayList<String> tags_of_item(long itemId) {
        Cursor cursor = DBAdapter.getInstance().db().query(true, DB_TABLE, new String[]{TAG_ID}, ITEM_ID + "=" + itemId, null, null, null, null, null);
        ArrayList<String> ids = new ArrayList<String>();
        while (cursor.moveToNext()) {
            ids.add(cursor.getString(cursor.getColumnIndexOrThrow(TAG_ID)));
        }
        if (ids.isEmpty()) {
            //We don't have any tags for this item, return an empty tag list
            return new ArrayList<String>();
        }
        ArrayList<String> tags = TagDBManager.instance().getTagsByIds(ids.toArray(new String[ids.size()]));
        return tags;
    }

    public ArrayList<Long> tagIds_by_item(long itemId) {
        Cursor cursor = DBAdapter.getInstance().db().query(true, DB_TABLE, new String[]{TAG_ID}, ITEM_ID + "=" + itemId, null, null, null, null, null);
        ArrayList<Long> tagIds = new ArrayList<Long>();
        while (cursor.moveToNext()) {
            long tagId = cursor.getLong(cursor.getColumnIndexOrThrow(TAG_ID));
            tagIds.add(new Long(tagId));
        }
        return tagIds;
    }

    public ArrayList<Long> ItemIds_by_tag(String tagName) {
        long tagId = TagDBManager.instance().getIdByName(tagName);
        Cursor cursor = DBAdapter.getInstance().db().query(true, DB_TABLE, new String[] { ITEM_ID }, TAG_ID + "=" + tagId, null, null, null, null, null);
        ArrayList<Long> ItemIds = new ArrayList<Long>();
        while (cursor.moveToNext()) {
            long item_id = cursor.getLong(cursor.getColumnIndexOrThrow(ITEM_ID));
            ItemIds.add(new Long(item_id));
        }
        return ItemIds;
    }

    //tag the item with the given tags
    public void Update_item_tags(long itemId, ArrayList<String> tags) {
        // app icon save in action bar clicked;
        ArrayList<String> oldTags = TagItemDBManager.instance().tags_of_item(itemId);

        //Remove the deleted tags
        for (String tag : oldTags) {
            if (!tags.contains(tag)) {
                TagItemDBManager.instance().Untag_item(tag, itemId);
            }
            else {
                //Remove the tags we already have so the following for loop will not tag them again
                tags.remove(tag);
            }
        }

        //Add the new tags
        for (String tag : tags) {
            TagItemDBManager.instance().Tag_item(tag, itemId);
        }
    }
}
