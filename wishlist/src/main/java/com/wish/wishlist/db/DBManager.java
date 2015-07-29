package com.wish.wishlist.db;

/***
 * DBManager is the base class of various subclasses to access data in db table
 */
public class DBManager {
    private static final String TAG = "DBManager";

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     */
    public DBManager() {
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
