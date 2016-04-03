package com.wish.wishlist.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.image.ImageManager;
import com.wish.wishlist.image.PhotoFileCreater;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.util.StringUtil;
import com.wish.wishlist.wish.WebImgMeta;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by jiawen on 2016-04-03.
 */
public class DownloadTask {
    private static String TAG = "DownloadTask";
    private long mItemsToDownload;
    private HashMap<String, Target> mTargets = new HashMap<>();
    private Date mSyncedTime;

    /* listener */
    private DownloadTaskDoneListener mDownlaodTaskDoneListener;
    public interface DownloadTaskDoneListener {
        void downloadTaskDone(boolean success, Date syncedTime);
    }

    public void registerListener(DownloadTaskDoneListener l) {
        mDownlaodTaskDoneListener = l;
    }
    /* listener */


    public void run() {
        mItemsToDownload = 0;
        mTargets.clear();

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

                        final WishItem existingItem = WishItemManager.getInstance().getItemByObjectId(parseItem.getObjectId());
                        if (existingItem == null) {
                            if (parseItem.getBoolean(ItemDBManager.KEY_DELETED)) {
                                // item on parse is deleted, and we don't have the item locally either,
                                // so just ignore this item.
                                itemDownloadDone();
                                continue;
                            }
                            Log.d(TAG, "item " + parseItem.getString(ItemDBManager.KEY_NAME) + " does not exist locally, saving it");
                            saveNewFromParse(parseItem);
                        } else {
                            //Fixme: need a better solution here:
                            //we cannot rely on device local time as that's inaccurate and could be different from devices to devices
                            //if (existingItem.getUpdatedTime() >= parseItem.getLong(ItemDBManager.KEY_UPDATED_TIME)) {
                            //    itemDownloadDone();
                            //    continue;
                            //}
                            if (parseItem.getBoolean(ItemDBManager.KEY_DELETED)) {
                                Log.d(TAG, "item " + existingItem.getName() + " exists locally, deleting local one");

                                TagItemDBManager.instance().Remove_tags_by_item(existingItem.getId());
                                existingItem.removeImage();
                                ItemDBManager.deleteItem(existingItem.getId()); // remove from local db

                                itemDownloadDone();
                                continue;
                            }
                            Log.d(TAG, "item " + existingItem.getName() + " exists locally, overwrite local one");
                            updateFromParse(parseItem, existingItem);
                        }
                    }
                } else {
                    Log.e(TAG, "Error: " + e.getMessage());
                    downloadAllDone(false);
                }
            }
        });
    }

    private void saveNewFromParse(final ParseObject parseItem) {
        final String webImgMetaJSON = parseItem.getString(ItemDBManager.KEY_WEB_IMG_META_JSON);
        final ParseFile parseImage = parseItem.getParseFile(WishItem.PARSE_KEY_IMAGE);
        if (webImgMetaJSON == null && parseImage == null) {
            onPhotoDone(parseItem, null, null);
            return;
        }
        if (webImgMetaJSON != null) {
            WebImgMeta webImageMeta = WebImgMeta.fromJSON(webImgMetaJSON);
            if (webImageMeta != null) {
                downloadWebImage(webImageMeta.mUrl, parseItem, null);
            }
            return;
        }
        downloadParseImage(parseImage, parseItem, null);
    }

    private void updateFromParse(final ParseObject parseItem, WishItem existingItem) {
        Log.d(TAG, "updateFromParse: item " + existingItem.getName());
        String webImgMetaJSON = parseItem.getString(ItemDBManager.KEY_WEB_IMG_META_JSON);
        final ParseFile parseImage = parseItem.getParseFile(WishItem.PARSE_KEY_IMAGE);
        if (webImgMetaJSON == null && parseImage == null) {
            existingItem.removeImage();
            onPhotoDone(parseItem, existingItem, null);
            return;
        }
        if (webImgMetaJSON != null) {
            if (!webImgMetaJSON.equals(existingItem.getWebImgMetaJSON())) {
                // we have a new image, update it locally
                WebImgMeta webImgMeta = WebImgMeta.fromJSON(webImgMetaJSON);
                if (webImgMeta != null) {
                    downloadWebImage(webImgMeta.mUrl, parseItem, existingItem);
                } else {
                    Log.e(TAG, "webImgMeta null, parsing JSON error");
                    onPhotoDone(parseItem, existingItem, existingItem.getFullsizePicPath());
                }
            } else {
                onPhotoDone(parseItem, existingItem, existingItem.getFullsizePicPath());
            }
            return;
        }
        if (!SyncAgent.parseFileNameToLocal(parseImage.getName()).equals(existingItem.getPicName())) {
            downloadParseImage(parseImage, parseItem, existingItem);
        } else {
            onPhotoDone(parseItem, existingItem, existingItem.getFullsizePicPath());
        }
    }

    // onPhotoDone is called for every wish that is updated locally from parse
    // if downloading photo fails, we won't come here and the wish attributes from parse will not
    // be saved locally Fixme: how do we treat this wish to be not synced and sync it next time?
    private void onPhotoDone(final ParseObject parseItem, WishItem existingItem, final String newfullsizePicPath) {
        WishItem newItem;
        if (existingItem == null) {
            newItem = WishItem.fromParseObject(parseItem, -1);
        } else {
            newItem = WishItem.fromParseObject(parseItem, existingItem.getId());
        }
        Log.d(TAG, "onPhotoDone: item " + newItem.getName());

        newItem.setFullsizePicPath(newfullsizePicPath);

        // set syncedToServer to true so the uploadTask will not upload this wish
        newItem.setSyncedToServer(true);
        long item_id = newItem.saveToLocal();
        updateTags(parseItem, item_id);

        itemDownloadDone();
    }

    private void updateTags(ParseObject item, long item_id) {
        List<String> tags = item.getList(WishItem.PARSE_KEY_TAGS);
        TagItemDBManager.instance().Update_item_tags(item_id, new ArrayList<>(tags));
    }

    private void downloadWebImage(final String url, final ParseObject parseItem, final WishItem existingItem) {
        Log.d(TAG, "downloadWebImage " + url);
        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                if (bitmap != null) {
                    webImageLoaded(bitmap, parseItem, existingItem, url);
                } else {
                    //Fixme: on what circumstances will this be triggered? no network? url invalid?
                    //shall we call syncFailed here or simply skip the image and let the rest of the sync
                    //continue?
                    Log.e(TAG, "downloadWebImage->onBitmapLoaded null bitmap");
                    webImageLoaded(null, parseItem, existingItem, url);
                }
            }
            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                //Fixme: on what circumstances will this be triggered? no network? url invalid?
                //shall we call syncFailed here or simply skip the image and let the rest of the sync
                //continue?
                Log.e(TAG, "onBitmapFailed");
                webImageLoaded(null, parseItem, existingItem, url);
            }
        };

        mTargets.put(url, target);
        Picasso.with(WishlistApplication.getAppContext()).load(url).into(target);
    }

    private void webImageLoaded(final Bitmap bitmap, final ParseObject parseItem, WishItem existingItem, String url) {
        if (bitmap != null) {
            String fullsizePath = ImageManager.saveBitmapToFile(ImageManager.getScaleDownBitmap(bitmap, 1024));
            ImageManager.saveBitmapToThumb(bitmap, fullsizePath);
            if (existingItem != null) {
                existingItem.removeImage();
            }
            onPhotoDone(parseItem, existingItem, fullsizePath);
        } else {
            String fullsizePath = existingItem == null ? null : existingItem.getFullsizePicPath();
            onPhotoDone(parseItem, existingItem, fullsizePath);
        }

        mTargets.remove(url);
    }

    private void downloadParseImage(final ParseFile parseImage, final ParseObject parseItem, final WishItem existingItem) {
        Log.d(TAG, "downloadParseImage: item " + parseItem.getString(ItemDBManager.KEY_NAME));
        parseImage.getDataInBackground(new GetDataCallback() {
            @Override
            public void done(byte[] imageBytes, ParseException e) {
                if (e == null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    String fullsizePicPath = ImageManager.saveBitmapToFile(bitmap, SyncAgent.parseFileNameToLocal(parseImage.getName()), 100);
                    ImageManager.saveBitmapToFile(bitmap, PhotoFileCreater.getInstance().thumbFilePath(fullsizePicPath));
                    if (existingItem != null) {
                        existingItem.removeImage();
                    }
                    onPhotoDone(parseItem, existingItem, fullsizePicPath);
                } else {
                    Log.e(TAG, e.toString());
                    downloadAllDone(false);
                }
            }
        });
    }

    private void itemDownloadDone() {
        mItemsToDownload--;
        if (mItemsToDownload == 0) {
            downloadAllDone(true);
        }
    }

    private void downloadAllDone(boolean success) {
        // download from parse is finished, now upload to parse
        if (mDownlaodTaskDoneListener != null) {
            mDownlaodTaskDoneListener.downloadTaskDone(success, mSyncedTime);
        }
    }
}
