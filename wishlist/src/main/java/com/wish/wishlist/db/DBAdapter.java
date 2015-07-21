package com.wish.wishlist.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/***
 * The DBAdapter class only gets called when the app first starts 
 * and its responsibility is to create/upgrade the tables. 
 * All other access to the data in the database 
 * is done through the individual "DBManager" class.
 */
public class DBAdapter {

    private static final boolean demo = false;
    private static DBAdapter instance = null;

    public static DBAdapter getInstance(Context context) {
        if (instance == null) {
            instance = new DBAdapter(context);
        }
        return instance;
    }
    //Database name
    public static final String DB_NAME = "WishList";

    //Database version
    public static final int DB_VERSION = 6;
    private static final String TAG = "DBAdapter";

    public static final Patch[] PATCHES = new Patch[] {
            new Patch() {//db version 1 already done in onCreate
                public void apply(SQLiteDatabase db) {
                    //mDb.execSQL("create table ...");
                }
                public void revert(SQLiteDatabase db) {
                    //mDb.execSQL("drop table ...");
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

            , new Patch() {//db version 6, 23->24
        public void apply(SQLiteDatabase db) {
            //add wish latitude and longitude column in the Item table
            String sql1 = "ALTER TABLE "
                    + ItemDBManager.DB_TABLE
                    + " ADD COLUMN latitude REAL ";

            String sql2 = "ALTER TABLE "
                    + ItemDBManager.DB_TABLE
                    + " ADD COLUMN longitude REAL ";

            db.execSQL(sql1);
            db.execSQL(sql2);

            String sql3 = "ALTER TABLE "
                    + ItemDBManager.DB_TABLE
                    + " ADD COLUMN object_id TEXT "; // parse object id

            //number of milliseconds since the standard base time known as "the epoch", namely January 1, 1970, 00:00:00 GMT.
            String sql4 = "ALTER TABLE "
                    + ItemDBManager.DB_TABLE
                    + " ADD COLUMN updated_time INTEGER ";

            db.execSQL(sql3);
            db.execSQL(sql4);

            //convert date_time in String to updated_time in long(ms)
            String sql = String.format("SELECT _id, date_time FROM Item");
            Cursor c = db.rawQuery(sql, null);
            if (c != null) {
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                c.moveToFirst();
                while (!c.isAfterLast()){
                    long id = c.getLong(c.getColumnIndexOrThrow("_id"));
                    String updated_time_str = c.getString(c.getColumnIndexOrThrow("date_time"));
                    long updated_time = 0;
                    try {
                        Date date = f.parse(updated_time_str);
                        updated_time = date.getTime();
                        Log.d(TAG, "updated_time " + updated_time);
                    } catch (ParseException e1) {
                        Log.e(TAG, e1.toString());
                    }

                    ContentValues cv = new ContentValues();
                    cv.put(ItemDBManager.KEY_UPDATED_TIME, updated_time);
                    String where = String.format("_id = '%d'", id);
                    db.update(ItemDBManager.DB_TABLE, cv, where, null);

                    c.moveToNext();
                }
            }
        }
    }
    };

    //Query string to create table "Item"
    private static final String CREATE_TABLE_ITEM = "create table "
            + ItemDBManager.DB_TABLE + " ("
            + ItemDBManager.KEY_ID			+ " INTEGER PRIMARY KEY, "
            + ItemDBManager.KEY_OBJECT_ID	+ " TEXT, "
            + ItemDBManager.KEY_STORE_ID 	+ " INTEGER, "
            + ItemDBManager.KEY_STORENAME	+ " TEXT, "
            + ItemDBManager.KEY_NAME 		+ " TEXT, "
            + ItemDBManager.KEY_DESCRIPTION + " TEXT, "
            + ItemDBManager.KEY_UPDATED_TIME+ " INTEGER, "
            + ItemDBManager.KEY_PHOTO_URL 	+ " TEXT, "
            + ItemDBManager.KEY_FULLSIZE_PHOTO_PATH 	+ " TEXT, "
            + ItemDBManager.KEY_PRICE 		+ " REAL, "
            + ItemDBManager.KEY_ADDRESS 	+ " TEXT, "
            + ItemDBManager.KEY_LATITUDE 	+ " REAL, "
            + ItemDBManager.KEY_LONGITUDE 	+ " REAL, "
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
    private DatabaseHelper mDBHelper;
    private SQLiteDatabase mDb = null;

    /** * Constructor
     *
     * @param context
     */
    private DBAdapter(Context context) {
        DBAdapter.context = context;
    }

    public void createDB() {
        this.mDBHelper = new DatabaseHelper(context);

        //according to android sdk document,
        //we must call open() getWritableDatabase() or getReadableDatabase() to actually create the tables;
        open();
    }

    //private static class DatabaseHelper extends SQLiteOpenHelper {
    // not sure why DatabaseHelper needs to be static
    private class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
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
                mItemDBManager.addItem(
                        "",
                        "Apple Store",
                        "iPad mini",
                        "It is the new iPad with retina display",
                        1340211922000L,
                        //"2012-03-11 11:30:00",
                        "",
                        "/storage/emulated/legacy/Pictures/.WishListPhoto/ipad_mini.jpg",
                        529f,
                        "220 Yonge Street, Toronto, ON, M5B 2H1",
                        43.653929,
                        -79.3802132,
                        0,
                        0,
                        "");

                mItemDBManager.addItem(
                        "",
                        "Coach store",
                        "Leather bag",
                        "What a beautiful bag! Cannot help noticing it.",
                        1340406500000L,
                        //"2012-03-17 18:22:35",
                        "",
                        "/storage/emulated/legacy/Pictures/.WishListPhoto/bag.jpg",
                        299.00f,
                        "2243 Bloor ST W\nToronto, ON M6S 1N7\nCanada",
                        43.6509499,
                        -79.477205,
                        3,
                        1,
                        "");

                mItemDBManager.addItem(
                        "",
                        "Tiffany",
                        "Starfish necklace",
                        "Gorgeous",
                        1331479800000L,
                        //"2012-06-03 03:40:50",
                        "",
                        "/storage/emulated/legacy/Pictures/.WishListPhoto/tiffany.jpg",
                        389f,
                        "85 Bloor Street West, Toronto, Ontario\nM5S 1M1 Canada",
                        43.6694098,
                        -79.3904,
                        2,
                        0,
                        "");

                mItemDBManager.addItem(
                        "",
                        "Bay Company",
                        "High hel",
                        "lala",
                        1332022955000L,
                        //"2012-05-15 08:17:38",
                        "",
                        "/storage/emulated/legacy/Pictures/.WishListPhoto/shoe.jpg",
                        289.0f,
                        "65 Dundas Street West\nToronto, ON, M5G 2C3",
                        43.6555876,
                        -79.3835228,
                        1,
                        0,
                        "");

                mItemDBManager.addItem(
                        "",
                        "Bay Inc.",
                        "Earring",
                        "I like its color",
                        1338709250000L,
                        //"2012-06-20 13:05:22",
                        "",
                        "/storage/emulated/legacy/Pictures/.WishListPhoto/ear_ring.jpg",
                        99.0f,
                        "11 Sunlight Park Rd\nToronto, ON, M4M 1B5",
                        43.6561902,
                        -79.3489359,
                        1,
                        1,
                        "");

                mItemDBManager.addItem(
                        "",
                        "Indigo",
                        "Wooden lantern",
                        "nice",
                        1337084258000L,
                        //"2012-06-22 19:08:20",
                        "",
                        "/storage/emulated/legacy/Pictures/.WishListPhoto/wooden_lanton.jpg",
                        59.0f,
                        "259 Richmond Street West Toronto ON M5V 3M6",
                        43.6489324,
                        -79.3913844,
                        1,
                        0,
                        "");
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
            for (int i = oldVersion; i < newVersion; i++) {
                PATCHES[i].apply(db);
            }
            Log.d(TAG, "version " + newVersion);

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
        if (mDb == null || !mDb.isOpen()) {
            mDb = mDBHelper.getWritableDatabase();
        }
        return this;
    }

    /**
     * close the db return type: void
     */
    public void close() {
        mDBHelper.close();
        mDb.close();
    }

    public SQLiteDatabase db()
    {
        return mDb;
    }

}