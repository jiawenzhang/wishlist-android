package com.wish.wishlist.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.wish.wishlist.wish.ImgMeta;
import com.wish.wishlist.wish.ImgMetaArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Created by jiawen on 2016-04-13.
 */
class DBMigrationTo_7 {
    private final static String TAG = "DBMigrationTo_7";

    // ImgMeta struct in db version 6
    private static class ImgMeta_6 {
        private final static String TAG = "ImgMeta_6";

        private final static String URL = "url";
        private final static String W = "w";
        private final static String H = "h";

        public String mUrl;
        public int mWidth;
        public int mHeight;

        ImgMeta_6(String url, int w, int h) {
            mUrl = url;
            mWidth = w;
            mHeight = h;
        }

        public String toJSON() {
            JSONArray imageArray = new JSONArray();
            JSONObject imgJson = new JSONObject();
            try {
                imgJson.put(URL, mUrl);
                imgJson.put(W, mWidth);
                imgJson.put(H, mHeight);
                imageArray.put(imgJson);
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                return null;
            }
            Log.d(TAG, imageArray.toString());
            return imageArray.toString();
        }

        static ImgMeta_6 fromJSON(final String JSON) {
            try {
                JSONArray jsonArray = new JSONArray(JSON);
                if (jsonArray.length() == 0) {
                    return null;
                }
                JSONObject jsonObj = jsonArray.getJSONObject(0);
                return new ImgMeta_6(jsonObj.getString(URL), jsonObj.getInt(W), jsonObj.getInt(H));
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                return null;
            }
        }
    }

    public static void run(SQLiteDatabase db) {
        //add wish download_img column in the Item table
        //if true, the wish needs to download image from server
        String sql1 = "ALTER TABLE "
                + ItemDBManager.DB_TABLE
                + " ADD COLUMN download_img INTEGER NOT NULL DEFAULT(0)";

        db.execSQL(sql1);


        // change empty string to null
        String sql2 = "UPDATE "
                + ItemDBManager.DB_TABLE
                + " SET " + ItemDBManager.KEY_OBJECT_ID
                + " = NULL WHERE " + ItemDBManager.KEY_OBJECT_ID + "=''";

        String sql3 = "UPDATE "
                + ItemDBManager.DB_TABLE
                + " SET " + ItemDBManager.KEY_STORE_NAME
                + " = NULL WHERE " + ItemDBManager.KEY_STORE_NAME + "=''";

        String sql4 = "UPDATE "
                + ItemDBManager.DB_TABLE
                + " SET " + ItemDBManager.KEY_DESCRIPTION
                + " = NULL WHERE " + ItemDBManager.KEY_DESCRIPTION + "=''";

        String sql5 = "UPDATE "
                + ItemDBManager.DB_TABLE
                + " SET " + ItemDBManager.KEY_IMG_META_JSON
                + " = NULL WHERE " + ItemDBManager.KEY_IMG_META_JSON + "=''";

        String sql6 = "UPDATE "
                + ItemDBManager.DB_TABLE
                + " SET " + ItemDBManager.KEY_FULLSIZE_PHOTO_PATH
                + " = NULL WHERE " + ItemDBManager.KEY_FULLSIZE_PHOTO_PATH + "=''";

        String sql7 = "UPDATE "
                + ItemDBManager.DB_TABLE
                + " SET " + ItemDBManager.KEY_ADDRESS
                + " = NULL WHERE " + ItemDBManager.KEY_ADDRESS + "='' OR "
                + ItemDBManager.KEY_ADDRESS + "='unknown'";

        String sql8 = "UPDATE "
                + ItemDBManager.DB_TABLE
                + " SET " + ItemDBManager.KEY_LINK
                + " = NULL WHERE " + ItemDBManager.KEY_LINK + "=''";

        // change Double.MIN_VALUE to null
        Double smallNumber = 0.000001;
        String sql9 = String.format(Locale.US, "UPDATE Item SET price=NULL WHERE price>0 AND price<'%f' ", smallNumber);
        String sql10 = String.format(Locale.US, "UPDATE Item SET latitude=NULL WHERE latitude>0 AND latitude<'%f' ", smallNumber);
        String sql11 = String.format(Locale.US, "UPDATE Item SET longitude=NULL WHERE longitude>0 AND longitude<'%f' ", smallNumber);

        db.execSQL(sql2);
        db.execSQL(sql3);
        db.execSQL(sql4);
        db.execSQL(sql5);
        db.execSQL(sql6);
        db.execSQL(sql7);
        db.execSQL(sql8);
        db.execSQL(sql9);
        db.execSQL(sql10);
        db.execSQL(sql11);

        // add "loc: web" to all img_meta_json
        String sql = "SELECT " + ItemDBManager.KEY_ID + ", " + ItemDBManager.KEY_IMG_META_JSON + " FROM Item";
        Cursor c = db.rawQuery(sql, null);
        if (c != null) {
            c.moveToFirst();
            while (!c.isAfterLast()) {
                long id = c.getLong(c.getColumnIndexOrThrow(ItemDBManager.KEY_ID));
                String json_6 = c.getString(c.getColumnIndexOrThrow(ItemDBManager.KEY_IMG_META_JSON));
                ContentValues cv = new ContentValues();
                if (json_6 != null) {
                    ImgMeta_6 imgMeta6 = ImgMeta_6.fromJSON(json_6);
                    String json_7 = null;
                    if (imgMeta6 != null) {
                        ImgMeta imgMeta7 = new ImgMeta(ImgMeta.WEB, imgMeta6.mUrl, imgMeta6.mWidth, imgMeta6.mHeight);
                        json_7 = new ImgMetaArray(imgMeta7).toJSON();
                    }
                    cv.put(ItemDBManager.KEY_IMG_META_JSON, json_7);
                    String where = String.format(Locale.US, "_id = '%d'", id);
                    db.update(ItemDBManager.DB_TABLE, cv, where, null);
                }
                c.moveToNext();
            }
            c.close();
        }
    }
}
