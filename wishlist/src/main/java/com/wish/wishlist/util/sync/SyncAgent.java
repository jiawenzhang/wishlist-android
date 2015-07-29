package com.wish.wishlist.util.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.util.ImageManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Created by jiawen on 15-07-11.
 */
public class SyncAgent {
    private static SyncAgent instance = null;
    private long m_items_to_upload;
    private static String TAG = "SyncAgent";

    public static SyncAgent getInstance() {
        if (instance == null) {
             instance = new SyncAgent();
        }
        return instance;
    }

    private SyncAgent() {}

    // call sync on app start up
    // how does parse trigger sync on the client? push notification?
    public void sync() {
        // sync from parse

        // get from parse the items with updated time > last synced time
        // save them in parseItemList
        final SharedPreferences sharedPref = WishlistApplication.getAppContext().getSharedPreferences(WishlistApplication.getAppContext().getString(R.string.app_name), Context.MODE_PRIVATE);
        final Date last_synced_time = new Date(sharedPref.getLong("last_synced_time", 0));
        //final long last_synced_time = sharedPref.getLong("last_synced_time", 0);
        Log.d(TAG, "last_synced_time " + last_synced_time.getTime());

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Item");
        query.whereGreaterThan("updatedAt", last_synced_time);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> itemList, com.parse.ParseException e) {
                if (e == null) {
                    Log.d(TAG, itemList.size() + " items updated since last synced time on Parse");
                    // add/update/remove local wish
                    HashSet<Long> parseItems = new HashSet<>();
                    for (ParseObject item : itemList) {
                        Log.d(TAG, item.getUpdatedAt().getTime() + " item " + item.getString(ItemDBManager.KEY_NAME) + " updateAt");
                        Log.d(TAG, last_synced_time.getTime() + " last sync time ");
                        WishItem localItem = WishItemManager.getInstance().getItemByObjectId(item.getObjectId());
                        if (localItem == null) {
                            // local item does not exist
                            localItem = fromParseObject(item, -1);
                            final ParseFile parseImage = item.getParseFile("image");
                            if (parseImage != null) {
                                saveParseImage(localItem, parseImage);
                            }

                            Log.d(TAG, "item " + localItem.getName() + " is new, save from Parse");
                            long item_id = localItem.saveToLocal();

                            // save the item tags
                            List<String> tags = item.getList(WishItem.PARSE_KEY_TAGS);
                            TagItemDBManager.instance().Update_item_tags(item_id, new ArrayList<>(tags));
                            parseItems.add(item_id);
                        } else {
                            if (localItem.getUpdatedTime() < item.getLong(ItemDBManager.KEY_UPDATED_TIME)) {
                                // local item exists, but parse item is newer
                                // need to handle delete
                                Log.d(TAG, "item " + localItem.getName() + " exists locally, but parse item is newer, overwrite local one");
                                localItem = fromParseObject(item, localItem.getId());
                                final ParseFile parseImage = item.getParseFile("image");
                                if (parseImage != null && !parseImage.getName().equals(localItem.getPicName())) {
                                    saveParseImage(localItem, parseImage);
                                }
                                localItem.saveToLocal();

                                // save the item tags
                                List<String> tags = item.getList(WishItem.PARSE_KEY_TAGS);
                                TagItemDBManager.instance().Update_item_tags(localItem.getId(), new ArrayList<>(tags));
                                parseItems.add(localItem.getId());
                            }
                        }
                    }

                    // sync to parse
                    // get from local the items with updated time > last synced time and push them to parse
                    ArrayList<WishItem> items = WishItemManager.getInstance().getItemsSinceLastSynced();
                    m_items_to_upload = items.size();
                    if (m_items_to_upload == 0) {
                        syncDone();
                        return;
                    }
                    for (WishItem item : items) {
                        if (parseItems.contains(item.getId())) {
                            // skip the items we just saved from parse
                            itemDone();
                            continue;
                        }
                        if (item.getObjectId().isEmpty()) { // parse does not have this item
                            Log.d(TAG, "item " + item.getName() + " does not exist on Parse, add to parse");
                            addToParse(item);
                        } else { // parse already has this item, update it
                            Log.d(TAG, "item " + item.getName() + " already exists on Parse, update parse");
                            updateParse(item);
                        }
                    }

                    // (parseItem could be marked as deleted, update local item to be deleted will just hide the item)
                } else {
                    Log.e(TAG, "Error: " + e.getMessage());
                }
            }
        });
    }

    private void saveParseImage(WishItem localItem, ParseFile parseImage)
    {
        try {
            // Fixme, need to delete the old images
            final byte[] imageBytes = parseImage.getData();
            ImageManager.saveByteToAlbum(imageBytes, parseImage.getName(), true);
            String imagePath = ImageManager.saveByteToAlbum(imageBytes, parseImage.getName(), false);
            localItem.setFullsizePicPath(imagePath);
        } catch (com.parse.ParseException e) {
            Log.e(TAG, e.toString());
        }
    }

    private void saveToParse(final ParseObject wishObject, final long item_id, final boolean saveImage, final boolean isNew)
    {
        // save the wish meta and image data to parse
        final ParseFile parseImage = wishObject.getParseFile(WishItem.PARSE_KEY_IMAGE);
        if (saveImage && parseImage != null) {
            parseImage.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        saveParseObject(wishObject, item_id, isNew);
                    } else {
                        Log.e(TAG, "save ParseFile failed " + e.toString());
                    }
                }
            });
        } else {
            saveParseObject(wishObject, item_id, isNew);
        }
    }

    private void saveParseObject(final ParseObject wishObject, final long item_id, final boolean isNew)
    {
        wishObject.saveInBackground(new SaveCallback() {
            @Override
            public void done(com.parse.ParseException e) {
                if (e == null) {
                    Log.d(TAG, "save wish success, object id: " + wishObject.getObjectId());
                    if (isNew) {
                        String object_id = wishObject.getObjectId();
                        WishItem item = WishItemManager.getInstance().getItemById(item_id);
                        item.setObjectId(object_id);
                        item.saveToLocal();
                    }
                } else {
                    Log.e(TAG, "save failed " + e.toString());
                }
                itemDone();
            }
        });
    }

    private void addToParse(WishItem item)
    {
        Log.d(TAG, "addToParse");

        final ParseObject wishObject = item.toParseObject();
        saveToParse(wishObject, item.getId(), true, true);
    }

    private void updateParse(final WishItem item)
    {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(ItemDBManager.DB_TABLE);

        // Retrieve the object by id
        query.getInBackground(item.getObjectId(), new GetCallback<ParseObject>() {
            public void done(ParseObject wishObject, com.parse.ParseException e) {
                if (e == null) {
                    String parseImageName = null;
                    ParseFile pf = wishObject.getParseFile(WishItem.PARSE_KEY_IMAGE);
                    boolean saveImage = false;
                    if (pf != null) {
                        parseImageName = pf.getName();
                    }
                    if (parseImageName == null) {
                        if (item.getPicName() != null) {
                            saveImage = true;
                        }
                    } else if (!parseImageName.equals(item.getPicName())) {
                        saveImage = true;
                    }
                    WishItem.toParseObject(item, wishObject);
                    saveToParse(wishObject, item.getId(), false, saveImage);
                } else {
                    Log.e(TAG, "update failed " + e.toString() + " object_id " + item.getObjectId());
                }
            }
        });
    }

    private void itemDone()
    {
        m_items_to_upload--;
        if (m_items_to_upload == 0) {
            syncDone();
        }
    }

    private void syncDone()
    {
        Log.d(TAG, "sync finished at " + System.currentTimeMillis());
        // all items are processed, sync is done
        // save current time as last synced time
        final SharedPreferences sharedPref = WishlistApplication.getAppContext().getSharedPreferences(WishlistApplication.getAppContext().getString(R.string.app_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong("last_synced_time", System.currentTimeMillis());
        editor.commit();
    }

    private WishItem fromParseObject(ParseObject item, long item_id)
    {
        WishItem wishItem = new WishItem(
                item_id,
                item.getObjectId(),
                item.getString(ItemDBManager.KEY_STORENAME),
                item.getString(ItemDBManager.KEY_NAME),
                item.getString(ItemDBManager.KEY_DESCRIPTION),
                item.getLong(ItemDBManager.KEY_UPDATED_TIME),
                null, // pic_str
                null, // _fullsizePhotoPath,
                item.getDouble(ItemDBManager.KEY_PRICE),
                item.getDouble(ItemDBManager.KEY_LATITUDE),
                item.getDouble(ItemDBManager.KEY_LONGITUDE),
                item.getString(ItemDBManager.KEY_ADDRESS),
                0, // priority
                item.getInt(ItemDBManager.KEY_COMPLETE),
                item.getString(ItemDBManager.KEY_LINK),
                item.getBoolean(ItemDBManager.KEY_DELETED));

        return wishItem;
    }
}


