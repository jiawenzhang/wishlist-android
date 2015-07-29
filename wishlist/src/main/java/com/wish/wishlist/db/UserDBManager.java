package com.wish.wishlist.db;

/***
 * StoreDBManager provides access to opexarations on data in store table
 */
public class UserDBManager extends DBManager {
    public static final String KEY_ID = "_id";
    public static final String USER_ID = "user_id";
    public static final String USER_KEY = "user_key";
    public static final String USER_DISPLAY_NAME = "user_display_name";
    public static final String USER_EMAIL = "user_email";

    public static final String DB_TABLE = "user";
    private static final String TAG="UserDBManager";

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     */
    public UserDBManager() {}
}
