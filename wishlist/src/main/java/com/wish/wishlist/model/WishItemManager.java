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

    private WishItem itemFromCursor(ItemsCursor c) {
        long itemId = c.getLong(c.getColumnIndexOrThrow(ItemDBManager.KEY_ID));
        int access = c.getInt(c.getColumnIndexOrThrow(ItemDBManager.KEY_ACCESS));

        int latColIndex = c.getColumnIndexOrThrow(ItemDBManager.KEY_LATITUDE);
        Double latitude = c.isNull(latColIndex) ?  null : c.getDouble(latColIndex);

        int lngColIndex = c.getColumnIndexOrThrow(ItemDBManager.KEY_LONGITUDE);
        Double longitude = c.isNull(lngColIndex) ? null : c.getDouble(lngColIndex);

        String itemLocation = c.getString(c.getColumnIndexOrThrow(ItemDBManager.KEY_ADDRESS));
        String objectId = c.getString(c.getColumnIndexOrThrow(ItemDBManager.KEY_OBJECT_ID));
        String storeName = c.getString(c.getColumnIndexOrThrow(ItemDBManager.KEY_STORE_NAME));
        String picture_str = c.getString(c.getColumnIndexOrThrow(ItemDBManager.KEY_IMG_META_JSON));
        String fullsize_pic_path = c.getString(c.getColumnIndexOrThrow(ItemDBManager.KEY_FULLSIZE_PHOTO_PATH));
        String itemName = c.getString(c.getColumnIndexOrThrow(ItemDBManager.KEY_NAME));
        String itemDesc = c.getString(c.getColumnIndexOrThrow(ItemDBManager.KEY_DESCRIPTION));
        long updated_time = c.getLong(c.getColumnIndexOrThrow(ItemDBManager.KEY_UPDATED_TIME));

        int priceColIndex = c.getColumnIndexOrThrow(ItemDBManager.KEY_PRICE);
        Double itemPrice = c.isNull(priceColIndex) ? null : c.getDouble(priceColIndex);

        int itemPriority = c.getInt(c.getColumnIndexOrThrow(ItemDBManager.KEY_PRIORITY));
        int itemComplete = c.getInt(c.getColumnIndexOrThrow(ItemDBManager.KEY_COMPLETE));
        String itemLink = c.getString(c.getColumnIndexOrThrow(ItemDBManager.KEY_LINK));
        boolean deleted = c.getInt(c.getColumnIndexOrThrow(ItemDBManager.KEY_DELETED)) == 1;
        boolean synced_to_server = c.getInt(c.getColumnIndexOrThrow(ItemDBManager.KEY_SYNCED_TO_SERVER)) == 1;
        boolean download_img = c.getInt(c.getColumnIndexOrThrow(ItemDBManager.KEY_DOWNLOAD_IMG)) == 1;

        return new WishItem(
                itemId,
                objectId,
                access,
                storeName,
                itemName,
                itemDesc,
                updated_time,
                picture_str,
                fullsize_pic_path,
                itemPrice,
                latitude,
                longitude,
                itemLocation,
                itemPriority,
                itemComplete,
                itemLink,
                deleted,
                synced_to_server,
                download_img);
    }

    public void deleteItemById(long itemId) {
        WishItem item = getItemById(itemId);

        item.removeImage();
        TagItemDBManager.instance().Remove_tags_by_item(itemId);

        if (item.getObjectId() == null) {
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
