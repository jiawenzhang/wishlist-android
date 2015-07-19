package com.wish.wishlist.util.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.wish.wishlist.R;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Created by jiawen on 15-07-11.
 */
public class SyncAgent {
    private Context m_context;
    private static SyncAgent instance = null;
    private static String TAG = "SyncAgent";

    public static SyncAgent getInstance(Context context) {
        if (instance == null) {
             instance = new SyncAgent(context);
        }
        return instance;
    }

    private SyncAgent(Context context)
    {
        m_context = context;
    }

    // call sync on app start up
    // how does parse trigger sync on the client? push notification?
    public void sync() {
        // sync from parse

        // get from parse the items with updated time > last synced time
        // save them in parseItemList
        final SharedPreferences sharedPref = m_context.getSharedPreferences(m_context.getString(R.string.app_name), Context.MODE_PRIVATE);
        final Date last_synced_time = new Date(sharedPref.getLong("last_synced_time", 0));
        //final long last_synced_time = sharedPref.getLong("last_synced_time", 0);
        Log.d(TAG, "last_synced_time " + last_synced_time);

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Item");
        //query.whereGreaterThan(ItemDBManager.KEY_DATE_TIME, last_synced_time);
        query.whereGreaterThan("updatedAt", last_synced_time);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> itemList, com.parse.ParseException e) {
                if (e == null) {
                    Log.d(TAG, itemList.size() + " items updated since last synced time on Parse");
                    // add/update/remove local wish
                    HashSet<Long> parseItems = new HashSet<>();
                    for (ParseObject item : itemList) {
                        Log.d(TAG, "item updateAt  " + item.getUpdatedAt().getTime());
                        Log.d(TAG, "last sync time " + last_synced_time.getTime());
                        WishItem localItem = WishItemManager.getInstance(m_context).getItemByObjectId(item.getObjectId());
                        if (localItem == null) {
                            // local item does not exist
                            localItem = fromParseObject(item);
                            long item_id = localItem.saveToLocal();
                            parseItems.add(item_id);
                            Log.d(TAG, localItem.getName() + " item is new, save from Parse");
                        } else {
                            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            long local_time = 0;
                            try {
                                Date d = f.parse(localItem.getDate());
                                local_time = d.getTime();
                            } catch (ParseException e1) {
                                Log.e(TAG, e1.toString());
                            }

                            if (local_time < item.getUpdatedAt().getTime()) {
                                // local item exists, but parse item is newer
                                // need to handle delete
                                Log.d(TAG, localItem.getName() + " item exists locally, but parse item is newer, overwrite local one");
                                localItem = fromParseObject(item);
                                localItem.saveToLocal();
                                parseItems.add(localItem.getId());
                            }
                        }
                    }

                    // sync to parse
                    // get from local the items with updated time > last synced time and push them to parse
                    ArrayList<WishItem> items = WishItemManager.getInstance(m_context).getItemsSinceLastSynced();
                    for (WishItem item : items) {
                        if (parseItems.contains(item.getId())) {
                            // skip the items we just saved from parse
                            continue;
                        }
                        if (item.getObjectId().isEmpty()) { // parse does not have this item
                            Log.d(TAG, item.getName() + " does not exist on Parse, add to parse");
                            item.addToParse();
                        } else { // parse already has this item, update it
                            Log.d(TAG, item.getName() + " already exists on Parse, update parse");
                            item.updateParse();
                        }
                    }

                    // for each item in parseItemList
                    // if (local does not have item), add item
                    // if (local has item)
                    //    if (local updated time > parse updated time), keep local
                    //    else update local
                    // (parseItem could be marked as deleted, update local item to be deleted will just hide the item)
                } else {
                    Log.e(TAG, "Error: " + e.getMessage());
                }
            }
        });

    }

    private WishItem fromParseObject(ParseObject item)
    {
        Long time_ms = item.getLong(ItemDBManager.KEY_DATE_TIME);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date_str = sdf.format(new Date(time_ms));
        WishItem wishItem = new WishItem(
                m_context,
                -1,
                item.getObjectId(),
                item.getString(ItemDBManager.KEY_STORENAME),
                item.getString(ItemDBManager.KEY_NAME),
                item.getString(ItemDBManager.KEY_DESCRIPTION),
                date_str,
                null, // pic_str
                null, // _fullsizePhotoPath,
                item.getDouble(ItemDBManager.KEY_PRICE),
                item.getDouble(ItemDBManager.KEY_LATITUDE),
                item.getDouble(ItemDBManager.KEY_LONGITUDE),
                item.getString(ItemDBManager.KEY_ADDRESS),
                0, // priority
                item.getInt(ItemDBManager.KEY_COMPLETE),
                item.getString(ItemDBManager.KEY_LINK));

        return wishItem;
    }
}


