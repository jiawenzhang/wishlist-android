package com.wish.wishlist.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.db.ItemDBManager;

/**
 * Created by jiawen on 15-08-24.
 */
public class Options {
    public String _key;
    public int _val;

    Options(String key, int val) {
        _key = key;
        _val = val;
    }

    static SharedPreferences pref() {
        Context ctx = WishlistApplication.getAppContext();
        return ctx.getSharedPreferences(ctx.getString(R.string.app_name), Context.MODE_PRIVATE);
    }

    public int val() {
        return _val;
    }

    public void setVal(int val) {
        _val = val;
    }

    public void read() {
        _val = pref().getInt(_key, 0);
    }

    public void save() {
        SharedPreferences.Editor editor = pref().edit();
        editor.putInt(_key, _val);
        editor.commit();
    }

    /**
     * Created by jiawen on 15-08-24.
     */
    public static class View extends Options {
        public static final String KEY = "viewOption";
        public static final int LIST = 0;
        public static final int GRID = 1;

        public View(int val) {
            super(KEY, val);
        }
    }

    /**
     * Created by jiawen on 15-08-24.
     */

    public static class Status extends Options {
        public static final String KEY = "statusOption";
        public static final int ALL = 0;
        public static final int COMPLETED = 1;
        public static final int IN_PROGRESS = 2;

        public Status(int val) {
            super(KEY, val);
        }
    }

    /**
     * Created by jiawen on 15-08-24.
     */

    public static class Tag {
        public static final String KEY = "tagOption";
        private String _key;
        private String _val;

        public Tag(String val) {
            _key = KEY;
            _val = val;
        }

        public String val() {
            return _val;
        }

        public void setVal(String val) {
            _val = val;
        }

        public void read() {
            _val = pref().getString(_key, null);
        }

        public void save() {
            SharedPreferences.Editor editor = pref().edit();
            editor.putString(_key, _val);
            editor.commit();
        }
    }

    /**
     * Created by jiawen on 15-08-24.
     */

    public static class Sort extends Options {
        public static final String KEY = "sortOption";
        public static final int ID = 0;
        public static final int NAME = 1;
        public static final int UPDATED_TIME = 2;
        public static final int PRICE = 3;
        public static final int PRIORITY = 4;

        public Sort(int val) {
            super(KEY, val);
        }

        public String toString() {
            switch (_val) {
                case ID:
                    return ItemDBManager.KEY_ID;
                case NAME:
                    return ItemDBManager.KEY_NAME;
                case UPDATED_TIME:
                    return ItemDBManager.KEY_UPDATED_TIME;
                case PRICE:
                    return ItemDBManager.KEY_PRICE;
                case PRIORITY:
                    return ItemDBManager.KEY_PRIORITY;
                default:
                    return null;
            }
        }
    }
}