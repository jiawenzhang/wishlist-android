package com.wish.wishlist.db;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;
import android.util.Log;

import com.wish.wishlist.R;

public class ItemDBManager extends DBManager {
	public static final String KEY_ID = "_id";
	public static final String KEY_OBJECT_ID = "object_id";
	public static final String KEY_STORE_ID = "store_id";
	public static final String KEY_STORENAME = "store_name";
	public static final String KEY_NAME = "item_name";
	public static final String KEY_DESCRIPTION = "description";
	public static final String KEY_DATE_TIME = "date_time";
	public static final String KEY_PHOTO_URL = "picture";
	public static final String KEY_FULLSIZE_PHOTO_PATH = "fullsize_picture";
	public static final String KEY_PRICE = "price";
	public static final String KEY_ADDRESS = "location";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_PRIORITY = "priority";
	public static final String KEY_COMPLETE = "complete";
    public static final String KEY_LINK = "link";

	public static final String DB_TABLE = "Item";
	private static final String TAG = "ItemDBManager";

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public ItemDBManager(Context ctx) {
        super(ctx);
	}

	/**
	 * Add a new item to the database. The item will have a status of open.
	 * 
	 * @param item_id
	 *            unique id of the item
	 * @param name
	 *            The item name
	 * @param description
	 *            The name description
	 */
	public long addItem(String object_id, String store_name, String name, String description, String date_time,
			String picture_uri, String fullsize_picture_path, double price, String address, double latitude, double longitude,
			int priority, int complete, String link) {
		// String sql = String.format(
		// "INSERT INTO ITEM (_id, name, description, create_date, store_id,  picture, price, location, priority) "
		// +
		// "VALUES ( NULL, '%s', '%s', '%s', '%d', '%s', '%f', '%s', '%d')",
		// name, description, date, store_id, picture_uri, price, location,
		// priority);
		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_OBJECT_ID, object_id);
		initialValues.put(KEY_STORENAME, store_name);
		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_DESCRIPTION, description);
		initialValues.put(KEY_DATE_TIME, date_time);
		initialValues.put(KEY_PHOTO_URL, picture_uri);
		initialValues.put(KEY_FULLSIZE_PHOTO_PATH, fullsize_picture_path);
		initialValues.put(KEY_PRICE, price);
		initialValues.put(KEY_ADDRESS, address);
		initialValues.put(KEY_LATITUDE, latitude);
		initialValues.put(KEY_LONGITUDE, longitude);
		initialValues.put(KEY_PRIORITY, priority);
		initialValues.put(KEY_COMPLETE, complete);
        initialValues.put(KEY_LINK, link);

		long id = DBAdapter.getInstance(mCtx).db().insert(DB_TABLE, null, initialValues);
		return id;
	}

	/**
	 * Update a item in the database.
	 * 
	 * @param _id
	 *            The id of the existing item
	 * @param name
	 *            The item name
	 * @param description
	 *            The item description
	 */
	public void updateItem(long _id, String object_id, String store_name, String name, String description, String date_time,
			String picture_uri, String fullsize_picture_path, double price, String address, double latitude, double longitude,
			int priority, int complete, String link) {

//		String sql = String.format("UPDATE Item " + "SET item_name = '%s',  "
//				+ " description = '%s', " + " date_time = '%s', "
//				+ " store_id = '%d' " + "WHERE _id = '%d' ", name, description,
//				date, store_id, _id);
//		try {
//			writableDB().execSQL(sql);
//		} catch (SQLException e) {
//			Log.e("Error writing an exsiting item", e.toString());
//		}
		
		ContentValues initialValues = new ContentValues();

        initialValues.put(KEY_OBJECT_ID, object_id);
		initialValues.put(KEY_STORENAME, store_name);
		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_DESCRIPTION, description);
		initialValues.put(KEY_DATE_TIME, date_time);
		initialValues.put(KEY_PHOTO_URL, picture_uri);
		initialValues.put(KEY_FULLSIZE_PHOTO_PATH, fullsize_picture_path);
		initialValues.put(KEY_PRICE, price);
		initialValues.put(KEY_ADDRESS, address);
		initialValues.put(KEY_LATITUDE, latitude);
		initialValues.put(KEY_LONGITUDE, longitude);
		initialValues.put(KEY_PRIORITY, priority);
		initialValues.put(KEY_COMPLETE, complete);
        initialValues.put(KEY_LINK, link);

		String where = String.format("_id = '%d'", _id);
		DBAdapter.getInstance(mCtx).db().update(DB_TABLE, initialValues, where, null);
	}

	/**
	 * Delete a item from the database.
	 * 
	 * @param _id
	 *            The id of the item to delete
	 */
	public void deleteItem(long _id) {
		//delete from item table
		String sql = String.format("DELETE FROM Item " + "WHERE _id = '%d' ", _id);
		try {
			DBAdapter.getInstance(mCtx).db().execSQL(sql);
		} catch (SQLException e) {
			Log.e("Error deleting item", e.toString());
		}
        TagItemDBManager.instance(mCtx).Remove_tags_by_item(_id);

        //delete tags associated with this item
		
		//delete from location table

		//delete from store table
	}

	/** Returns the number of Items */
	public int getItemsCount() {

		Cursor c = null;
		try {
			c = DBAdapter.getInstance(mCtx).db().rawQuery(
					"SELECT count(*) FROM Item", null);
			if (0 >= c.getCount()) {
				return 0;
			}
			c.moveToFirst();
			return c.getInt(0);
		} finally {
			if (null != c) {
				try {
					c.close();
				} catch (SQLException e) {
				}
			}
		}
	}

	public static class ItemsCursor extends SQLiteCursor {
		public static enum SortBy {
			item_name, date_time, price, priority, _id,
		}

		private static final String QUERY = "SELECT _id, item_name, store_name, description, date_time, store_id, picture, fullsize_picture, price, location, priority "
				+ "FROM Item " + "ORDER BY ";
		
//		private static final String QUERY_NAME = "SELECT _id, item_name, description, date_time, store_id, picture, price, location, priority "
//			+ "FROM Item " + "WHERE item_name LIKE Book " + "ORDER BY ";

		private ItemsCursor(SQLiteDatabase db, SQLiteCursorDriver driver,
				String editTable, SQLiteQuery query) {
			super(db, driver, editTable, query);
		}

		private static class Factory implements SQLiteDatabase.CursorFactory {
			@Override
			public Cursor newCursor(SQLiteDatabase db,
					SQLiteCursorDriver driver, String editTable,
					SQLiteQuery query) {
				return new ItemsCursor(db, driver, editTable, query);
			}
		}
	}

	/**
	 * Return a sorted ItemsCursor
	 * 
	 * @param sortBy
	 *            the sort criteria
	 */

	public ItemsCursor getItems(String sortOption, Map<String,String> where, ArrayList<Long> itemIds) {
		String sql;
        String WHERE = "";
		if (where == null || where.isEmpty()) {
		}
		else {
			//right now, we assume there is only one entry in where
			String field = "";
			String value = "";
			for (String key : where.keySet()) {
				field = key;
				value = where.get(key);
			}
            WHERE = "WHERE " + field + "=" + value;
		}
        if (!itemIds.isEmpty()) {
            if (WHERE.isEmpty()) {
                WHERE = "WHERE _id IN (";
            }
            else {
                WHERE += " AND _id IN (";
            }
            for (Long id : itemIds) {
                WHERE += id + ", ";
            }
            //remote the last ', '
            WHERE = WHERE.substring(0, WHERE.length()-2);
            WHERE += ")";
        }
		if (sortOption.equals(ItemsCursor.SortBy.date_time.toString())) {
			// sort by date is most recent at the top
			sortOption = sortOption + " DESC";
		}
        sql = "SELECT * FROM Item " + WHERE + " ORDER BY " + sortOption;

		SQLiteDatabase d = DBAdapter.getInstance(mCtx).db();
		ItemsCursor c = (ItemsCursor) d.rawQueryWithFactory(
				new ItemsCursor.Factory(), sql, null, null);
		c.moveToFirst();
		return c;
	}
	
//	/**
//	 * Return a sorted ItemsCursor matching the search quest by name
//	 * 
//	 * @param sortBy
//	 *            the sort criteria
//	 */
//	public ItemsCursor getItemsByName(String name, ItemsCursor.SortBy sortBy) {
//		String sql = ItemsCursor.QUERY_NAME + sortBy.toString();
//		SQLiteDatabase d = readableDB();
//		ItemsCursor c = (ItemsCursor) d.rawQueryWithFactory(
//				new ItemsCursor.Factory(), sql, null, null);
//		c.moveToFirst();
//		return c;
//	}

	/**
	 * Return the cursor of item with id equal to _id
	 * 
	 * @param _id
	 * @return
	 */
	public ItemsCursor getItem(long _id) {
		String sql = String.format("SELECT * FROM Item " + "WHERE _id = '%d' ", _id);
		ItemsCursor c = (ItemsCursor) DBAdapter.getInstance(mCtx).db().rawQueryWithFactory(
				new ItemsCursor.Factory(), sql, null, null);

		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

    public ItemsCursor getItemByObjectId(String object_id) {
        String sql = String.format("SELECT * FROM Item " + "WHERE object_id = '%s' ", object_id);
        ItemsCursor c = (ItemsCursor) DBAdapter.getInstance(mCtx).db().rawQueryWithFactory(
                new ItemsCursor.Factory(), sql, null, null);

        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }

	/**
	 * Return a sorted ItemsCursor matching the search quest by name
	 * 
	 */
	public ItemsCursor searchItems(String query) {
		String sql = String.format("SELECT * FROM Item "
				+ "WHERE item_name LIKE '%%%s%%' ", query);

		SQLiteDatabase d = DBAdapter.getInstance(mCtx).db();
		ItemsCursor c = (ItemsCursor) d.rawQueryWithFactory(
				new ItemsCursor.Factory(), sql, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}
	
	/**
	 * Return a sorted ItemsCursor matching the search quest by name
	 * ordered by sortBy
	 * 
	 */
	public ItemsCursor searchItems(String query, String sortOption) {
		String sql = String.format("SELECT * FROM Item "
				+ "WHERE item_name LIKE '%%%s%%' " + "ORDER BY " + sortOption, query);
		
		SQLiteDatabase d = DBAdapter.getInstance(mCtx).db();
		ItemsCursor c = (ItemsCursor) d.rawQueryWithFactory(
				new ItemsCursor.Factory(), sql, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}
	
	/**
	 * get the latitude and longitude according to item id
	 * @param _id the _id of the item in table Item
	 * @return double[2] with [0] the latitude, [1] the longitude
	 */

	public long getlocationIdbyItemId(long _itemId){
		Cursor locationC = getItemLocationCursor(_itemId);
		if(locationC != null){
			return locationC.getLong(locationC.
					getColumnIndexOrThrow(LocationDBManager.KEY_ID));
		}
		else return -1;
	}

	public ArrayList<Long> getAllItemIds(){
		String sql = String.format("SELECT _id FROM Item");
		SQLiteDatabase d = DBAdapter.getInstance(mCtx).db();
		ItemsCursor c = (ItemsCursor) d.rawQueryWithFactory(
				new ItemsCursor.Factory(), sql, null, null);

		ArrayList<Long> ids = new ArrayList<>();
		if (c != null) {
			c.moveToFirst();
			while (!c.isAfterLast()){
				long id = c.getLong(c.getColumnIndexOrThrow(KEY_ID));
				ids.add(id);
				c.moveToNext();
			}
		}
		return ids;
	}

	public ArrayList<Long> getItemsWithLocation(){
		String sql = String.format("SELECT _id, latitude, longitude FROM Item");
		SQLiteDatabase d = DBAdapter.getInstance(mCtx).db();
		ItemsCursor c = (ItemsCursor) d.rawQueryWithFactory(
				new ItemsCursor.Factory(), sql, null, null);

		long id;
		ArrayList<Long> ids = new ArrayList<>();
		if (c != null) {
			c.moveToFirst();
			while (!c.isAfterLast()){
				id = c.getLong(c.getColumnIndexOrThrow(KEY_ID));
				//skip the items having unknown locations
				double latitude = c.getDouble(c.getColumnIndexOrThrow(KEY_LATITUDE));
				double longitude = c.getDouble(c.getColumnIndexOrThrow(KEY_LONGITUDE));
				if (latitude != Double.MIN_VALUE && longitude != Double.MAX_VALUE) {
					ids.add(id);
				}
				c.moveToNext();
			}
		}
		return ids;
	}

    public ArrayList<Long> getItemsSinceLastSynced()
    {
        String sql = String.format("SELECT _id, date_time FROM Item");
        SQLiteDatabase d = DBAdapter.getInstance(mCtx).db();
        ItemsCursor c = (ItemsCursor) d.rawQueryWithFactory(
                new ItemsCursor.Factory(), sql, null, null);

        long id;
        long last_synced_time = mCtx.getSharedPreferences(mCtx.getString(R.string.app_name), Context.MODE_PRIVATE).getLong("last_synced_time", 0);
        ArrayList<Long> ids = new ArrayList<>();
        if (c != null) {
            c.moveToFirst();
            while (!c.isAfterLast()){
                String dateStr = c.getString(c.getColumnIndexOrThrow(KEY_DATE_TIME));
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                long local_time = 0;
                try {
                    Date date = f.parse(dateStr);
                    local_time = date.getTime();
                } catch (ParseException e1) {
                    Log.e(TAG, e1.toString());
                }

                if (local_time > last_synced_time) {
                    id = c.getLong(c.getColumnIndexOrThrow(KEY_ID));
                    ids.add(id);
                }
                c.moveToNext();
            }
        }
        return ids;
    }

	/**
	 * get the Cursor of table store according to item id
	 * @param long _id: the _id of the item in table Item
	 * @return the Cursor of store where the Item belongs to
	 */
	//
	public Cursor getItemStoreCursor(long _id){
		String sql = String.format("SELECT store_id FROM Item " + "WHERE _id = '%d' ",
				_id);
		SQLiteDatabase d = DBAdapter.getInstance(mCtx).db();
		ItemsCursor itemC = (ItemsCursor) d.rawQueryWithFactory(
				new ItemsCursor.Factory(), sql, null, null);

		Cursor storeC = null;
		if (itemC != null) {
			//get the store id
			itemC.moveToFirst();
			long storeID = itemC.getLong(itemC
					.getColumnIndexOrThrow(ItemDBManager.KEY_STORE_ID));

			StoreDBManager storeDBA;
			storeDBA = new StoreDBManager(mCtx);

			// get store cursor
			storeC = storeDBA.getStore(storeID);
		}

		return storeC;
	}
	
	/**
	 * get the Cursor of table location according to item id
	 * @param long _id: the _id of the item in table Item
	 * @return the Cursor of location where the Item is located
	 */
	public Cursor getItemLocationCursor(long _id){
		Cursor storeC = getItemStoreCursor(_id);
		Cursor locationC = null;
		if (storeC != null) {
			LocationDBManager locationDBA;
			locationDBA = new LocationDBManager(mCtx);

			//get the location id
			long locationID = storeC.getLong(storeC
					.getColumnIndexOrThrow(StoreDBManager.KEY_LOCATION_ID));

			//get the latitude and longitude from table location
			locationC = locationDBA.getLocation(locationID);
		}

		return locationC;
	}

}
