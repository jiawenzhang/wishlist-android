package com.wish.wishlist.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.db.ItemDBManager.ItemsCursor;
import com.wish.wishlist.db.TagItemDBManager;

public class WishItemManager {
    private static WishItemManager instance = null;

    public static WishItemManager getInstance() {
        if (instance == null) {
            instance = new WishItemManager();
        }
        return instance;
    }

    private WishItemManager() {}

    public ArrayList<WishItem> getItemsSinceLastSynced() {
        ItemDBManager itemDBManager = new ItemDBManager();

        ArrayList<Long> ids = itemDBManager.getItemsSinceLastSynced();
        ArrayList<WishItem> items = new ArrayList<>();
        // Fixme: optimize this by using one SQL to get all items into on cursor
        for (Long id : ids) {
            ItemsCursor wishItemCursor = itemDBManager.getItem(id);
            if (wishItemCursor.getCount() == 0) {
                continue;
            }
            items.add(itemFromCursor(wishItemCursor));
        }
        return items;
    }

    public ArrayList<WishItem> getAllItems() {
        ItemDBManager itemDBManager = new ItemDBManager();

        ArrayList<Long> ids = itemDBManager.getAllItemIds();
        ArrayList<WishItem> items = new ArrayList<>();
        // Fixme: optimize this by using one SQL to get all items into on cursor
        for (Long id : ids) {
            ItemsCursor wishItemCursor = itemDBManager.getItem(id);
            if (wishItemCursor.getCount() == 0) {
                continue;
            }
            items.add(itemFromCursor(wishItemCursor));
        }
        return items;
    }

    public List<WishItem> searchItems(final String query, final String sortOption)
    {
        final ItemDBManager itemDBManager = new ItemDBManager();
        final ItemsCursor c = itemDBManager.searchItems(query, sortOption);
        List<WishItem> itemList = new ArrayList<>();
        if (c != null) {
            c.moveToFirst();
            while (!c.isAfterLast()){
                itemList.add(itemFromCursor(c));
                c.moveToNext();
            }
        }
        return itemList;
    }

    public List<WishItem> getItems(final String nameQuery, String sortOption, Map<String,String> where, ArrayList<Long> itemIds) {
        final ItemDBManager itemDBManager = new ItemDBManager();
        final ItemsCursor c = itemDBManager.getItems(nameQuery, sortOption, where, itemIds);
        List<WishItem> itemList = new ArrayList<>();
        if (c != null) {
            c.moveToFirst();
            while (!c.isAfterLast()){
                itemList.add(itemFromCursor(c));
                c.moveToNext();
            }
        }
        return itemList;
    }

    public ArrayList<WishItem> getItemsNotSyncedToServer()
    {
        ItemDBManager itemDBManager = new ItemDBManager();

        ArrayList<Long> ids = itemDBManager.getItemsNotSyncedToServer();
        ArrayList<WishItem> items = new ArrayList<>();
        // Fixme: optimize this by using one SQL to get all items into on cursor
        for (Long id : ids) {
            ItemsCursor wishItemCursor = itemDBManager.getItem(id);
            if (wishItemCursor.getCount() == 0) {
                continue;
            }
            items.add(itemFromCursor(wishItemCursor));
        }
        return items;
    }

    public WishItem getItemByObjectId(String object_id)
    {
        ItemDBManager itemDBManager = new ItemDBManager();

        ItemsCursor wishItemCursor = itemDBManager.getItemByObjectId(object_id);
        if (wishItemCursor.getCount() == 0) {
            return null;
        }
        return itemFromCursor(wishItemCursor);
    }

    public WishItem getItemById(long itemId)
    {
        ItemDBManager itemDBManager = new ItemDBManager();

        ItemsCursor wishItemCursor = itemDBManager.getItem(itemId);
        if (wishItemCursor.getCount() == 0) {
            return null;
        }

        return itemFromCursor(wishItemCursor);
    }

    private WishItem itemFromCursor(ItemsCursor wishItemCursor)
    {
        long itemId = wishItemCursor.getLong(wishItemCursor.getColumnIndexOrThrow(ItemDBManager.KEY_ID));

        int access = wishItemCursor.getInt(wishItemCursor.getColumnIndexOrThrow(ItemDBManager.KEY_ACCESS));

        double latitude = wishItemCursor.getDouble(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_LATITUDE));

        double longitude = wishItemCursor.getDouble(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_LONGITUDE));

        String itemLocation = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_ADDRESS));

        String objectId = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_OBJECT_ID));

        String storeName = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_STORENAME));

        String picture_str = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_IMG_META_JSON));

        String fullsize_pic_path = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_FULLSIZE_PHOTO_PATH));

        String itemName = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_NAME));

        String itemDesc = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_DESCRIPTION));

        long updated_time = wishItemCursor.getLong(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_UPDATED_TIME));

        double itemPrice = wishItemCursor.getDouble(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_PRICE));

        int itemPriority = wishItemCursor.getInt(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_PRIORITY));

        int itemComplete = wishItemCursor.getInt(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_COMPLETE));

        String itemLink = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_LINK));

        boolean deleted = wishItemCursor.getInt(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_DELETED)) == 1;

        boolean synced_to_server = wishItemCursor.getInt(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_SYNCED_TO_SERVER)) == 1;

        boolean download_img = wishItemCursor.getInt(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_DOWNLOAD_IMG)) == 1;

        WishItem item = new WishItem(itemId, objectId, access, storeName, itemName, itemDesc,
                updated_time, picture_str, fullsize_pic_path, itemPrice, latitude, longitude,
                itemLocation, itemPriority, itemComplete, itemLink, deleted, synced_to_server, download_img);

        return item;
    }

    public void deleteItemById(long itemId) {
        WishItem item = getItemById(itemId);

        item.removeImage();
        TagItemDBManager.instance().Remove_tags_by_item(itemId);

        // check null is needed because during db migration where object_id column is added, migrated wish's
        // object_id is all null!
        if (item.getObjectId() == null || item.getObjectId().isEmpty()) {
            // this wish has never been uploaded to parse, so it is safe to just delete it from db
            ItemDBManager.deleteItem(itemId);
            return;
        }

        // the wish exists on parse server, mark it as deleted so other device knows to delete it on sync
        // item.clear();
        item.setDeleted(true);
        item.setUpdatedTime(System.currentTimeMillis());
        item.setSyncedToServer(false);
        item.saveToLocal();
    }
}
