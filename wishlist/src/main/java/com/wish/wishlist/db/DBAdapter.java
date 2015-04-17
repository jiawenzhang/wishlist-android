package com.wish.wishlist.db;

import com.wish.wishlist.R;

import android.content.Context;
import android.util.Log;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/***
 * The DBAdapter class only gets called when the app first starts 
 * and its responsibility is to create/upgrade the tables. 
 * All other access to the data in the database 
 * is done through the individual "DBManager" class.
 */
public class DBAdapter {
	
	private static final boolean demo = false;
	private static DBAdapter instance = null;
	
	public static DBAdapter getInstance(Context contenxt) {
		if (instance == null) {
			instance = new DBAdapter(contenxt);
		}
		return instance;
	}
	//Database name
	public static final String DB_NAME = "WishList";

	//Database version
	public static final int DB_VERSION = 5;
	private static final String TAG="DBAdapter";

	public static final Patch[] PATCHES = new Patch[] {
		new Patch() {//db version 1 already done in onCreate
			public void apply(SQLiteDatabase db) {
				//db.execSQL("create table ...");
			}
			public void revert(SQLiteDatabase db) {
				//db.execSQL("drop table ...");
			}
		}
		, new Patch() {//db version 2
			public void apply(SQLiteDatabase db) {
				//delete sample items
				String sql = "DELETE FROM "
				+ ItemDBManager.DB_TABLE
				+ " WHERE "
				+ ItemDBManager.KEY_PHOTO_URL
				+ " LIKE '%sample'";
				
				db.execSQL(sql);

				//add user table
				db.execSQL(CREATE_TABLE_USER);
			}
		}

        , new Patch() {//db version 3
            public void apply(SQLiteDatabase db) {
                //add wish complete flag column in the Item table
                //representing if a wish is complete or not
                //set its default value to be 0
                String sql = "ALTER TABLE "
                        + ItemDBManager.DB_TABLE
                        + " ADD COLUMN complete INTEGER DEFAULT 0 NOT NULL";

                db.execSQL(sql);
            }
        }

        , new Patch() {//db version 4, 1.0.9 -> 1.0.10
            public void apply(SQLiteDatabase db) {
                //drop table ItemCategory and create table Tag
                String sql = "DROP TABLE IF EXISTS ItemCategory";
                db.execSQL(sql);
                db.execSQL(CREATE_TABLE_TAG);
                db.execSQL(CREATE_TABLE_TAGITEM);
            }
		}

        , new Patch() {//db version 5, 1.0.12 -> 1.0.13
            public void apply(SQLiteDatabase db) {
              //add wish link column in the Item table
             //representing the url of the wish
                String sql = "ALTER TABLE "
                        + ItemDBManager.DB_TABLE
                        + " ADD COLUMN link TEXT ";

                db.execSQL(sql);
            }
        }
	};
	
	//Query string to create table "Item"
	private static final String CREATE_TABLE_ITEM = "create table "
			+ ItemDBManager.DB_TABLE + " ("
			+ ItemDBManager.KEY_ID			+ " INTEGER PRIMARY KEY, "
			+ ItemDBManager.KEY_STORE_ID 	+ " INTEGER, "
			+ ItemDBManager.KEY_STORENAME	+ " TEXT, "
			+ ItemDBManager.KEY_NAME 		+ " TEXT, "
			+ ItemDBManager.KEY_DESCRIPTION + " TEXT, "
			+ ItemDBManager.KEY_DATE_TIME 	+ " TEXT, "
			+ ItemDBManager.KEY_PHOTO_URL 	+ " TEXT, "
			+ ItemDBManager.KEY_FULLSIZE_PHOTO_PATH 	+ " TEXT, "
			+ ItemDBManager.KEY_PRICE 		+ " REAL, "
			+ ItemDBManager.KEY_ADDRESS 	+ " TEXT, "
			+ ItemDBManager.KEY_PRIORITY 	+ " INTEGER, "
			+ ItemDBManager.KEY_COMPLETE 	+ " INTEGER, "
            + ItemDBManager.KEY_LINK 	    + " TEXT"
			+ ");";


	//Query string to create table "Tag"
	private static final String CREATE_TABLE_TAG = "create table "
            + TagDBManager.DB_TABLE + " ("
            + TagDBManager.KEY_ID  + " INTEGER PRIMARY KEY, "
            // We want to make sure no duplicate tag names can exist
            + TagDBManager.KEY_NAME + " TEXT UNIQUE"
			+ ");";

    //Query string to create table "TagItem"
    private static final String CREATE_TABLE_TAGITEM = "create table "
            + TagItemDBManager.DB_TABLE + " ("
            + TagItemDBManager.TAG_ID + " INTEGER, "
            + TagItemDBManager.ITEM_ID + " INTEGER, "
            + "FOREIGN KEY(" + TagItemDBManager.TAG_ID +")" + " REFERENCES Tag(_id), "
            + "FOREIGN KEY(" + TagItemDBManager.ITEM_ID +")" + " REFERENCES Item(_id), "
            + "PRIMARY KEY(" + TagItemDBManager.TAG_ID + ", " + TagItemDBManager.ITEM_ID + ")"
            + ");";

	//Query string to create table "store"
	private static final String CREATE_TABLE_STORE = "create table "
			+ StoreDBManager.DB_TABLE
			+ " (" 
			+ StoreDBManager.KEY_ID				+ " INTEGER PRIMARY KEY, "
			+ StoreDBManager.KEY_LOCATION_ID	+ " INTEGER, "
			+ StoreDBManager.KEY_NAME 			+ " TEXT"
			+ ");";
	
	//Query string to create table "location"
	private static final String CREATE_TABLE_LOCATION = "create table "
			+ LocationDBManager.DB_TABLE + " ("
			+ LocationDBManager.KEY_ID
			+ " integer primary key autoincrement, "
			+ LocationDBManager.KEY_LATITUDE
			+ " REAL,"
			+ LocationDBManager.KEY_LONGITUDE
			+ " REAL,"
			+ LocationDBManager.KEY_ADDSTR
			+ " TEXT,"
//			+ LocationDBManager.KEY_ADDLINE2
//			+ " TEXT,"
//			+ LocationDBManager.KEY_ADDLINE3
//			+ " TEXT,"
			+ LocationDBManager.KEY_STREET_NO
			+ " INTEGER," 
			+ LocationDBManager.KEY_STREET
			+ " TEXT," 
			+ LocationDBManager.KEY_CITY + " TEXT,"
			+ LocationDBManager.KEY_STATE + " TEXT,"
			+ LocationDBManager.KEY_COUNTRY + " TEXT,"
			+ LocationDBManager.KEY_POSTCODE + " TEXT" + ");";

	//Query string to create table "user"
	private static final String CREATE_TABLE_USER = "create table "
			+ UserDBManager.DB_TABLE + " ("
			+ UserDBManager.KEY_ID			+ " INTEGER PRIMARY KEY, "
			+ UserDBManager.USER_ID	+ " TEXT, "
			+ UserDBManager.USER_KEY	+ " TEXT, "
			+ UserDBManager.USER_DISPLAY_NAME	+ " TEXT, "
			+ UserDBManager.USER_EMAIL + " TEXT"
			+ ");";

	private static Context context;
	private DatabaseHelper DBHelper;
	private SQLiteDatabase db;

	/** * Constructor
	 * 
	 * @param contenxt
	 */
	private DBAdapter(Context contenxt) {
		context = contenxt;
	}
	
	public void createDB() {
		this.DBHelper = new DatabaseHelper(context);
		
		//according android sdk document, 
		//we must call open() getWritableDatabase() or getReadableDatabase() to actually create the tables;
		open();
		close();
	}

	//private static class DatabaseHelper extends SQLiteOpenHelper {
	// not sure why DatabaseHelper needs to be static
	private class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			//super(context, DB_NAME, null, DB_VERSION);
			super(context, DB_NAME, null, PATCHES.length); //
			Log.d(TAG, "PATCHES.length" + String.valueOf(PATCHES.length));
		}

		/***onCreate is called when the database is first created
		 * (non-Javadoc)
		 * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			// create table "item" and insert into the table
			db.execSQL(CREATE_TABLE_ITEM);
			if (demo) {
				ItemDBManager mItemDBManager = new ItemDBManager(context);
				mItemDBManager.open(db);
				String picUrl;
				picUrl = Integer.toHexString(R.drawable.new_ipad) + "sample";
				mItemDBManager.addItem(	1,
										"Apple Store",
										"ipad",
										"It is the new ipad with retina display",
										"2012-03-11 11:30:00",
										picUrl,
										" ",
										529f,
										"220 Yonge Street, Toronto, ON, M5B 2H1",
										0,
										0,
                                        "");
				
				picUrl = Integer.toHexString(R.drawable.cake) + "sample";
				mItemDBManager.addItem(	2,
										"dessert store",
										"chocolate cake", 
										"It looks delicisous", 
										"2012-03-17 18:22:35", 
										picUrl,
										" ",
										2.99f,
										"2243 Bloor ST W\nToronto, ON M6S 1N7\nCanada",
										3,
										0,
                                        "");
				
				picUrl = Integer.toHexString(R.drawable.tiffany) + "sample";
				mItemDBManager.addItem(	3,
										"tiffany",
										"tiffany necklace", 
										"beautiful", 
										"2012-06-03 03:40:50", 
										picUrl,
										" ",
										389f,
										"85 Bloor Street West, Toronto, Ontario\nM5S 1M1 Canada",
										2,
										0,
                                        "");
				
				picUrl = Integer.toHexString(R.drawable.d3) + "sample";
				mItemDBManager.addItem(	4,
										"Best buy",
										"diablo 3",
										"waiting for this game for years", 
										"2012-05-15 08:17:38", 
										picUrl,
										" ",
										59.0f,
										"65 Dundas Street West\nToronto, ON, M5G 2C3",
										1,
										0,
                                        "");
				
				picUrl = Integer.toHexString(R.drawable.mini_cooper) + "sample";
				mItemDBManager.addItem(	5,
										"BMW store",
										"mini cooper",
										"i like its color", 
										"2012-06-20 13:05:22", 
										picUrl,
										" ",
										20000.0f,
										"11 Sunlight Park Rd\nToronto, ON, M4M 1B5",
										1,
										0,
                                        "");
				
				picUrl = Integer.toHexString(R.drawable.sjobs_bio) + "sample";
				mItemDBManager.addItem(	6,
										"Indigo",
										"steve jobs biograhpy",
										"a must-read book", 
										"2012-06-22 19:08:20", 
										picUrl,
										" ",
										30.0f,
										"259 Richmond Street West Toronto ON M5V 3M6",
										1,
										0,
                                        "");

				mItemDBManager.close();
			}

			//create table "store"
			db.execSQL(CREATE_TABLE_STORE);
			if (demo) {
				StoreDBManager mStoreDBManager = new StoreDBManager(context);
				mStoreDBManager.open(db);
				mStoreDBManager.addStore("Apple Store", 		1);
				mStoreDBManager.addStore("dessert store",   		2);
				mStoreDBManager.addStore("tiffany",	3);
				mStoreDBManager.addStore("Best buy",		4);
				mStoreDBManager.addStore("BMW store",		5);
				mStoreDBManager.addStore("Indigo",		6);
				mStoreDBManager.close();
			}
			//create table "location"
			db.execSQL(CREATE_TABLE_LOCATION);
			if (demo) {
				LocationDBManager mLocationDBManager = new LocationDBManager(context);
				mLocationDBManager.open(db);
				mLocationDBManager.addLocation(43.653929,
												-79.3802132, 
											   "220 Yonge Street, Toronto, ON, M5B 2H1",
											   0, null, null, null, null, null);
				mLocationDBManager.addLocation(43.6509499,
												-79.477205, 
											   "2243 Bloor ST W\nToronto, ON M6S 1N7\nCanada",
											   0, null, null, null, null, null);
				mLocationDBManager.addLocation(43.6694098,
												-79.3904, 
											   "85 Bloor Street West, Toronto, Ontario\nM5S 1M1 Canadas",
											   0, null, null, null, null, null);
				mLocationDBManager.addLocation(43.6555876,
												-79.3835228, 
											   "65 Dundas Street West\nToronto, ON, M5G 2C3",
											   0, null, null, null, null, null);
				
				mLocationDBManager.addLocation(43.6561902,
												-79.3489359, 
											   "11 Sunlight Park Rd\nToronto, ON, M4M 1B5",
											   0, null, null, null, null, null);
				
				mLocationDBManager.addLocation(43.6489324,
												-79.3913844, 
											   "259 Richmond Street West Toronto ON M5V 3M6 ",
											   0, null, null, null, null, null);

				mLocationDBManager.close();
			}
			//create table "user", added on version 3
			db.execSQL(CREATE_TABLE_USER);

            //create table "Tag", added on version 4
            db.execSQL(CREATE_TABLE_TAG);

            //create table "TagItem" added on version 4
            db.execSQL(CREATE_TABLE_TAGITEM);
		}

		/***
		 * (non-Javadoc)
		 * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Adding any table mods to this guy here
			for (int i=oldVersion; i<newVersion; i++) {
				PATCHES[i].apply(db);
			}
		}

		//API LEVEL 11 starts to support onDowngrade
	//	@Override
	//	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	//		for (int i=oldVersion; i>newVersion; i++) {
	//			PATCHES[i-1].revert(db);
	//		}
	//	}
	}

	private static class Patch {
		public void apply(SQLiteDatabase db) {}
		public void revert(SQLiteDatabase db) {}
	}

	/**
	 * open the db
	 * 
	 * @return this
	 * @throws SQLException
	 *             return type: DBAdapter
	 */
	public DBAdapter open() throws SQLException {
		this.db = this.DBHelper.getWritableDatabase();
		return this;
	}

	/**
	 * close the db return type: void
	 */
	public void close() {
		this.DBHelper.close();
		this.db.close();
	}

}
