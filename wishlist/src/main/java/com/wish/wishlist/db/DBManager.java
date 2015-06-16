package com.wish.wishlist.db;

import android.content.Context;

/***
 * DBManager is the base class of various subclasses to access data in db table
 */
public class DBManager {
    protected final Context mCtx;
    private static final String TAG = "DBManager";

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     *
     * @param ctx
     *            the Context within which to work
     */
    public DBManager(Context ctx) {
        this.mCtx = ctx;
    }

    protected String makePlaceholders(int len) {
        if (len < 1) {
            // It will lead to an invalid query anyway ..
            throw new RuntimeException("No placeholders");
        } else {
            StringBuilder sb = new StringBuilder(len * 2 - 1);
            sb.append("?");
            for (int i = 1; i < len; i++) {
                sb.append(",?");
            }
            return sb.toString();
        }
    }
}
