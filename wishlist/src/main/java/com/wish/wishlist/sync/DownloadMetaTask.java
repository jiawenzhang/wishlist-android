package com.wish.wishlist.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.util.StringUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by jiawen on 2016-04-03.
 */
public class DownloadMetaTask {
    private static String TAG = "DownloadMetaTask";
    private long mItemsToDownload;
    private Date mSyncedTime;

    /* listener */
    private DownloadMetaTaskDoneListener mDownlaodMetaTaskDoneListener;
    public interface DownloadMetaTaskDoneListener {
        void downloadMetaTaskDone(boolean success, Date syncedTime);
    }

    public void registerListener(DownloadMetaTaskDoneListener l) {
        mDownlaodMetaTaskDoneListener = l;
    }
    /* listener */


    public void run() {
        mItemsToDownload = 0;

        // get from parse the items with updated time > last synced time
        final SharedPreferences sharedPref = WishlistApplication.getAppContext().getSharedPreferences(WishlistApplication.getAppContext().getString(R.string.app_name), Context.MODE_PRIVATE);
        final Date lastSyncedTime = new Date(sharedPref.getLong(SyncAgent.LAST_SYNCED_TIME, 0));
        Log.d(TAG, "lastSyncedTime " + lastSyncedTime.getTime() + " " + StringUtil.UTCDate(lastSyncedTime));
        mSyncedTime = lastSyncedTime;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Item");
        query.whereGreaterThan("updatedAt", lastSyncedTime);
        query.whereEqualTo(WishItem.PARSE_KEY_OWNDER_ID, ParseUser.getCurrentUser().getObjectId());

        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> itemList, com.parse.ParseException e) {
                if (e == null) {
                    mItemsToDownload = itemList.size();
                    Log.d(TAG, itemList.size() + " items to download");
                    if (mItemsToDownload == 0) {
                        downloadAllDone(true);
                        return;
                    }

                    // add/update/remove local wish
                    for (ParseObject parseItem : itemList) {
                        mSyncedTime = parseItem.getUpdatedAt();

                        WishItem item = WishItemManager.getInstance().getItemByObjectId(parseItem.getObjectId());
                        if (item == null) {
                            if (parseItem.getBoolean(ItemDBManager.KEY_DELETED)) {
                                // item on parse is deleted, and we don't have the item locally either,
                                // so just ignore this item.
                                itemDownloadDone();
                                continue;
                            }
                            Log.d(TAG, "item " + parseItem.getString(ItemDBManager.KEY_NAME) + " does not exist locally, saving it");
                            item = WishItem.fromParseObject(parseItem, -1);

                            // it is a new item, we need to try to download it image.
                            item.setDownloadImg(true);
                        } else {
                            //Fixme: need a better solution here:
                            //we cannot rely on device local time as that's inaccurate and could be different from devices to devices
                            //if (existingItem.getUpdatedTime() >= parseItem.getLong(ItemDBManager.KEY_UPDATED_TIME)) {
                            //    itemDownloadDone();
                            //    continue;
                            //}
                            if (parseItem.getBoolean(ItemDBManager.KEY_DELETED)) {
                                Log.d(TAG, "item " + item.getName() + " exists locally, deleting local one");

                                TagItemDBManager.instance().Remove_tags_by_item(item.getId());
                                item.removeImage();
                                ItemDBManager.deleteItem(item.getId()); // remove from local db

                                itemDownloadDone();
                                continue;
                            }

                            Log.d(TAG, "item " + item.getName() + " exists locally, overwrite local one");
                            String newImgMetaJSON = parseItem.getString(ItemDBManager.KEY_IMG_META_JSON);

                            boolean downloadImg = false;
                            if (!StringUtil.compare(newImgMetaJSON, item.getImgMetaJSON())) {
                                // image needs to be re-downloaded
                                downloadImg = true;
                            }
                            item = WishItem.fromParseObject(parseItem, item.getId());
                            item.setDownloadImg(downloadImg);
                        }
                        // set syncedToServer to true so the uploadTask will not upload this wish
                        item.setSyncedToServer(true);
                        long item_id = item.saveToLocal();
                        updateTags(parseItem, item_id);
                        itemDownloadDone();
                    }
                } else {
                    Log.e(TAG, "Error: " + e.getMessage());
                    downloadAllDone(false);
                }
            }
        });
    }

    private void updateTags(ParseObject item, long item_id) {
        List<String> tags = item.getList(WishItem.PARSE_KEY_TAGS);
        TagItemDBManager.instance().Update_item_tags(item_id, new ArrayList<>(tags));
    }

    private void itemDownloadDone() {
        mItemsToDownload--;
        if (mItemsToDownload == 0) {
            downloadAllDone(true);
        }
    }

    private void downloadAllDone(boolean success) {
        // download from parse is finished, now upload to parse
        if (mDownlaodMetaTaskDoneListener != null) {
            mDownlaodMetaTaskDoneListener.downloadMetaTaskDone(success, mSyncedTime);
        }
    }
}
