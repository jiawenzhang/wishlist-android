package com.wish.wishlist.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.ParseInstallation;
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
    private Date mDownloadStamp;
    private boolean mWishMetaChanged = false;

    /* listener */
    private DownloadMetaTaskDoneListener mDownlaodMetaTaskDoneListener;
    public interface DownloadMetaTaskDoneListener {
        void downloadMetaTaskDone(boolean success, boolean wishMetaChanged);
    }

    public void registerListener(DownloadMetaTaskDoneListener l) {
        mDownlaodMetaTaskDoneListener = l;
    }
    /* listener */


    public void run() {
        mItemsToDownload = 0;
        mWishMetaChanged = false;

        // get from parse the items with updated time > last synced time
        final SharedPreferences sharedPref = WishlistApplication.getAppContext().getSharedPreferences(WishlistApplication.getAppContext().getString(R.string.app_name), Context.MODE_PRIVATE);
        final Date lastDownloadStamp = new Date(sharedPref.getLong(lastDownloadStampKey(), 0));
        Log.d(TAG, "lastDownloadStamp " + lastDownloadStamp.getTime() + " " + StringUtil.UTCDate(lastDownloadStamp));
        mDownloadStamp = lastDownloadStamp;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Item");
        query.whereGreaterThan("updatedAt", lastDownloadStamp);
        query.whereEqualTo(WishItem.PARSE_KEY_OWNER_ID, ParseUser.getCurrentUser().getObjectId());
        query.whereNotEqualTo(WishItem.PARSE_KEY_LAST_CHANGED_BY, ParseInstallation.getCurrentInstallation().getInstallationId());

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
                        if (parseItem.getUpdatedAt().getTime() > mDownloadStamp.getTime()) {
                            mDownloadStamp = parseItem.getUpdatedAt();
                        }

                        WishItem item = WishItemManager.getInstance().getItemByObjectId(parseItem.getObjectId());
                        if (item == null) {
                            if (parseItem.getBoolean(WishItem.PARSE_KEY_DELETED)) {
                                // item on parse is deleted, and we don't have the item locally either,
                                // so just ignore this item.
                                itemDownloadDone();
                                continue;
                            }
                            Log.d(TAG, "item " + parseItem.getString(WishItem.PARSE_KEY_NAME) + " does not exist locally, saving it");
                            item = WishItem.newFromParseObject(parseItem);

                            // it is a new item, we need to try to download it image.
                            item.setDownloadImg(true);
                        } else {
                            //Fixme: need a better solution here:
                            //we cannot rely on device local time as that's inaccurate and could be different from devices to devices
                            //if (existingItem.getUpdatedTime() >= parseItem.getLong(ItemDBManager.KEY_UPDATED_TIME)) {
                            //    itemDownloadDone();
                            //    continue;
                            //}
                            if (parseItem.getBoolean(WishItem.PARSE_KEY_DELETED)) {
                                Log.d(TAG, "item " + item.getName() + " exists locally, deleting local one");

                                TagItemDBManager.instance().Remove_tags_by_item(item.getId());
                                item.removeImage();
                                ItemDBManager.deleteItem(item.getId()); // remove from local db

                                mWishMetaChanged = true;
                                itemDownloadDone();
                                continue;
                            }

                            Log.d(TAG, "item " + item.getName() + " exists locally, overwrite local one");
                            String newImgMetaJSON = parseItem.getString(WishItem.PARSE_KEY_IMG_META_JSON);

                            boolean downloadImg = false;
                            if (!StringUtil.compare(newImgMetaJSON, item.getImgMetaJSON())) {
                                // image needs to be re-downloaded
                                downloadImg = true;
                            }

                            WishItem newItem = WishItem.newFromParseObject(parseItem);
                            newItem.setId(item.getId());
                            newItem.setFullsizePicPath(item.getFullsizePicPath());
                            newItem.setDownloadImg(downloadImg);
                            item = newItem;
                        }
                        // set syncedToServer to true so the uploadTask will not upload this wish
                        item.setSyncedToServer(true);
                        long item_id = item.saveToLocal();
                        updateTags(parseItem, item_id);
                        mWishMetaChanged = true;
                        itemDownloadDone();
                    }
                } else {
                    Log.e(TAG, "Error: " + e.getMessage());
                    downloadAllDone(false);
                }
            }
        });
    }

    private void updateTags(ParseObject parseItem, long item_id) {
        List<String> tags = parseItem.getList(WishItem.PARSE_KEY_TAGS);
        if (tags == null) {
            tags = new ArrayList<>();
        }
        TagItemDBManager.instance().Update_item_tags(item_id, new ArrayList<>(tags));
    }

    private void itemDownloadDone() {
        mItemsToDownload--;
        if (mItemsToDownload == 0) {
            downloadAllDone(true);
        }
    }

    private String lastDownloadStampKey() {
        final String LAST_DOWNLOAD_STAMP = "lastDownloadStamp";
        if (ParseUser.getCurrentUser() == null) {
            // this shall never happen
            Log.e(TAG, "user null");
            return LAST_DOWNLOAD_STAMP;
        }
        return ParseUser.getCurrentUser().getUsername() + "-" + LAST_DOWNLOAD_STAMP;
    }

    private void downloadAllDone(boolean success) {
        if (success) {
            // download meta is done, save the last download stamp
            Log.d(TAG, "sync download meta finished success: download stamp " + mDownloadStamp.getTime() + " " + StringUtil.UTCDate(mDownloadStamp));
            final SharedPreferences sharedPref = WishlistApplication.getAppContext().getSharedPreferences(WishlistApplication.getAppContext().getString(R.string.app_name), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putLong(lastDownloadStampKey(), mDownloadStamp.getTime());
            editor.commit();
        }

        if (mDownlaodMetaTaskDoneListener != null) {
            mDownlaodMetaTaskDoneListener.downloadMetaTaskDone(success, mWishMetaChanged);
        }
    }
}
