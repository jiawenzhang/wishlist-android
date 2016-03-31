package com.wish.wishlist.sync;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.ProfileChangeEvent;
import com.wish.wishlist.image.PhotoFileCreater;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.image.ImageManager;
import com.wish.wishlist.util.NetworkHelper;
import com.wish.wishlist.util.ProfileUtil;
import com.wish.wishlist.util.StringUtil;
import com.wish.wishlist.wish.WebImgMeta;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by jiawen on 15-07-11.
 */
public class SyncAgent {
    private static SyncAgent instance = null;
    private long m_items_to_upload;
    private long m_items_to_download;
    private HashSet<Long> m_downloaded_items = new HashSet<>();
    private HashMap<String, Target> m_targets = new HashMap<>();
    private OnSyncWishChangedListener mSyncWishChangedListener;
    private OnDownloadWishDoneListener mDownloadWishDoneListener;
    private Date mSyncedTime;

    private boolean mDownloading = false;
    private boolean mSyncing = false;
    private boolean mScheduleToSync = false;
    private static String TAG = "SyncAgent";
    private static String LAST_SYNCED_TIME = "lastSyncedTime";

    public static SyncAgent getInstance() {
        if (instance == null) {
             instance = new SyncAgent();
        }
        return instance;
    }

    private SyncAgent() {}

    public boolean downloading() {
        return mDownloading;
    }

    // call sync on app start up
    // how does parse trigger sync on the client? push notification?
    public void sync() {
        Log.d(TAG, "sync");
        if (!WishlistApplication.getAppContext().getResources().getBoolean(R.bool.enable_account)) {
            return;
        }

        if (ParseUser.getCurrentUser() == null) {
            Log.d(TAG, "user not login, sync is disabled ");
            return;
        }
        // sync from parse
        if (mSyncing) {
            // if we are in the process of syncing, run sync again after the current sync is finished
            Log.d(TAG, "mSync true, schedule sync");
            mScheduleToSync = true;
            return;
        }
        if (!NetworkHelper.getInstance().isNetworkAvailable()) {
            Log.d(TAG, "no network, sync is not started");
            // Fixme: shall we attempt sync later?
            return;
        }

        mSyncing = true;

        // get from parse the items with updated time > last synced time
        // save them in parseItemList
        final SharedPreferences sharedPref = WishlistApplication.getAppContext().getSharedPreferences(WishlistApplication.getAppContext().getString(R.string.app_name), Context.MODE_PRIVATE);
        final Date lastSyncedTime = new Date(sharedPref.getLong(LAST_SYNCED_TIME, 0));
        Log.d(TAG, "lastSyncedTime " + lastSyncedTime.getTime() + " " + StringUtil.UTCDate(lastSyncedTime));
        mSyncedTime = lastSyncedTime;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Item");
        query.whereGreaterThan("updatedAt", lastSyncedTime);
        query.whereEqualTo(WishItem.PARSE_KEY_OWNDER_ID, ParseUser.getCurrentUser().getObjectId());

        mDownloading = true;
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> itemList, com.parse.ParseException e) {
                if (e == null) {
                    m_downloaded_items.clear();
                    m_items_to_download = itemList.size();
                    Log.d(TAG, itemList.size() + " items to download");
                    if (m_items_to_download == 0) {
                        downloadAllDone();
                        return;
                    }

                    // add/update/remove local wish
                    for (ParseObject parseItem : itemList) {
                        final WishItem existingItem = WishItemManager.getInstance().getItemByObjectId(parseItem.getObjectId());
                        if (existingItem == null) {
                            // item does not exist locally
                            if (parseItem.getBoolean(ItemDBManager.KEY_DELETED)) {
                                // item on parse is deleted, and we don't have the item locally either,
                                // so just ignore this item.
                                itemDownloadDone();
                                continue;
                            }
                            saveFromParse(parseItem);
                        } else {
                            //Fixme: need a better solution here:
                            //we cannot rely on device local time as that's inaccurate and could be different from devices to devices
                            //if (existingItem.getUpdatedTime() >= parseItem.getLong(ItemDBManager.KEY_UPDATED_TIME)) {
                            //    itemDownloadDone();
                            //    continue;
                            //}

                            if (parseItem.getBoolean(ItemDBManager.KEY_DELETED)) {
                                Log.d(TAG, "item " + existingItem.getName() + " exists locally, deleting local one");
                                existingItem.removeImage();
                                TagItemDBManager.instance().Remove_tags_by_item(existingItem.getId());
                                ItemDBManager.deleteItem(existingItem.getId()); // remove from local db

                                mSyncedTime = parseItem.getUpdatedAt();
                                itemDownloadDone();
                                if (m_items_to_download == 0) {
                                    // we have finished downloading items, notify list/grid view to refresh
                                    if (mSyncWishChangedListener != null) {
                                        mSyncWishChangedListener.onSyncWishChanged();
                                    }
                                }
                                continue;
                            }
                            Log.d(TAG, "item " + existingItem.getName() + " exists locally, overwrite local one");
                            updateFromParse(parseItem, existingItem);
                        }
                    }
                } else {
                    Log.e(TAG, "Error: " + e.getMessage());
                    syncFailed();
                }
            }
        });
    }

    private void uploadToParse() {
        // get from local the items with synced_to_parse == false and push them to parse
        Log.d(TAG, "uploadToParse");
        ArrayList<WishItem> items = WishItemManager.getInstance().getItemsNotSyncedToServer();
        m_items_to_upload = items.size();
        Log.d(TAG, "uploading " + m_items_to_upload + " items");
        if (m_items_to_upload == 0) {
            syncDone();
            return;
        }
        for (WishItem item : items) {
            if (m_downloaded_items.contains(item.getId())) {
                // skip the items we just saved from parse
                itemUploadDone();
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
    }

    private void saveFromParse(final ParseObject parseItem)
    {
        String webImgMetaJSON = parseItem.getString(ItemDBManager.KEY_WEB_IMG_META_JSON);
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

    private void updateFromParse(final ParseObject parseItem, WishItem existingItem)
    {
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
        if (!parseFileNameToLocal(parseImage.getName()).equals(existingItem.getPicName())) {
            downloadParseImage(parseImage, parseItem, existingItem);
        } else {
            onPhotoDone(parseItem, existingItem, existingItem.getFullsizePicPath());
        }
    }

    // onPhotoDone is called for every wish that is updated locally from parse
    // if downloading photo fails, we won't come here and the wish attributes from parse will not
    // be saved locally Fixme: how do we treat this wish to be not synced and sync it next time?
    private void onPhotoDone(final ParseObject parseItem, WishItem existingItem, final String newfullsizePicPath)
    {
        WishItem newItem;
        if (existingItem == null) {
            newItem = WishItem.fromParseObject(parseItem, -1);
        } else {
            newItem = WishItem.fromParseObject(parseItem, existingItem.getId());
        }
        Log.d(TAG, "onPhotoDone: item " + newItem.getName());

        newItem.setFullsizePicPath(newfullsizePicPath);
        long item_id = newItem.saveToLocal();
        updateTags(parseItem, item_id);

        m_downloaded_items.add(item_id);

        mSyncedTime = parseItem.getUpdatedAt();
        itemDownloadDone();
        if (m_items_to_download == 0) {
            // we have finished downloading items, notify list/grid view to refresh
            if (mSyncWishChangedListener != null) {
                mSyncWishChangedListener.onSyncWishChanged();
            }
        }
    }

    private void updateTags(ParseObject item, long item_id) {
        List<String> tags = item.getList(WishItem.PARSE_KEY_TAGS);
        TagItemDBManager.instance().Update_item_tags(item_id, new ArrayList<>(tags));
    }

    private void downloadWebImage(final String url, final ParseObject parseItem, final WishItem existingItem)
    {
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

        m_targets.put(url, target);
        Picasso.with(WishlistApplication.getAppContext()).load(url).into(target);
    }

    private void webImageLoaded(final Bitmap bitmap, final ParseObject parseItem, WishItem existingItem, String url)
    {
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

        m_targets.remove(url);

        if (mSyncWishChangedListener != null) {
            mSyncWishChangedListener.onSyncWishChanged();
        }
    }

    private void downloadParseImage(final ParseFile parseImage, final ParseObject parseItem, final WishItem existingItem)
    {
        Log.d(TAG, "downloadParseImage: item " + parseItem.getString(ItemDBManager.KEY_NAME));
        parseImage.getDataInBackground(new GetDataCallback() {
            @Override
            public void done(byte[] imageBytes, ParseException e) {
                if (e == null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    String fullsizePicPath = ImageManager.saveBitmapToFile(bitmap, parseFileNameToLocal(parseImage.getName()), 100);
                    ImageManager.saveBitmapToFile(bitmap, PhotoFileCreater.getInstance().thumbFilePath(fullsizePicPath));
                    if (existingItem != null) {
                        existingItem.removeImage();
                    }
                    onPhotoDone(parseItem, existingItem, fullsizePicPath);
                } else {
                    Log.e(TAG, e.toString());
                    syncFailed();
                }
            }
        });
    }

    private void saveToParseWithImage(final ParseObject wishObject, final WishItem item, final boolean isNew) {
        // we save a scale-down thumbnail image to Parse to save space
        Log.d(TAG, "thumbPicPath " + item.getThumbPicPath());

        // decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            BitmapFactory.decodeFile(item.getThumbPicPath(), options);
        } catch (Exception e) {
            Log.e(TAG, "fail to decode thumb file, skipping uploading image to parse");
            uploadParseObject(wishObject, item.getId(), isNew);
        }

        final int w = options.outWidth;
        final int h = options.outHeight;

        final byte[] data = ImageManager.readFile(item.getThumbPicPath());
        final ParseFile parseImage = new ParseFile(item.getPicName(), data);
        parseImage.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.e(TAG, "parse img url " + parseImage.getUrl());
                    wishObject.put(WishItem.PARSE_KEY_IMAGE, parseImage);
                    wishObject.put(WishItem.PARSE_KEY_IMG_META_JSON, new WebImgMeta(parseImage.getUrl(), w, h).toJSON());
                    uploadParseObject(wishObject, item.getId(), isNew);
                } else {
                    Log.e(TAG, "save ParseFile failed " + e.toString());
                    syncFailed();
                }
            }
        });
    }

    private void uploadParseObject(final ParseObject wishObject, final long item_id, final boolean isNew)
    {
        wishObject.saveInBackground(new SaveCallback() {
            @Override
            public void done(com.parse.ParseException e) {
                if (e == null) {
                    Log.d(TAG, "save wish success, object id: " + wishObject.getObjectId());
                    mSyncedTime = wishObject.getUpdatedAt();

                    WishItem item = WishItemManager.getInstance().getItemById(item_id);
                    if (item.getDeleted()) {
                        // we have updated the wish on parse server as deleted, we can now safely remove the item in local db
                        ItemDBManager.deleteItem(item.getId());
                        itemUploadDone();
                        return;
                    }

                    Log.d(TAG, "set item " + item.getName() + " synced to true");
                    item.setSyncedToServer(true);
                    if (isNew) {
                        String object_id = wishObject.getObjectId();
                        item.setObjectId(object_id);
                    }
                    item.saveToLocal();
                    itemUploadDone();
                } else {
                    Log.e(TAG, "save failed " + e.toString());
                    syncFailed();
                }
            }
        });
    }

    private void addToParse(final WishItem item)
    {
        Log.d(TAG, "Adding item " + item.getName() + " to Parse");
        final ParseObject wishObject = item.toParseObject();

        if (item.getWebImgMetaJSON() != null || item.getThumbPicPath() == null) {
            // if we have an web url for the photo, we don't upload the photo to Parse so that we can save server space
            // when the other device sync down the wish, it will download the photo from the web url
            uploadParseObject(wishObject, item.getId(), true);
            return;
        }
        saveToParseWithImage(wishObject, item, true);
    }

    private void updateParse(final WishItem item)
    {
        Log.d(TAG, "Updating item " + item.getName() + " to Parse");
        final ParseQuery<ParseObject> query = ParseQuery.getQuery(ItemDBManager.DB_TABLE);

        // Retrieve the object by id
        query.getInBackground(item.getObjectId(), new GetCallback<ParseObject>() {
            public void done(final ParseObject wishObject, com.parse.ParseException e) {
                if (e == null) {
                    boolean saveImage = false;
                    if (!item.getDeleted()) {
                        // if we are deleting the wish, we don't need to save the image
                        String parseImageName = null;
                        ParseFile pf = wishObject.getParseFile(WishItem.PARSE_KEY_IMAGE);
                        if (pf != null) {
                            parseImageName = pf.getName();
                        }
                        if (parseImageName == null) {
                            if (item.getPicName() != null) {
                                saveImage = true;
                            }
                        } else if (!parseFileNameToLocal(parseImageName).equals(item.getPicName())) {
                            saveImage = true;
                        }
                    }
                    WishItem.toParseObject(item, wishObject);
                    if (saveImage) {
                        saveToParseWithImage(wishObject, item, false);
                    } else {
                        uploadParseObject(wishObject, item.getId(), false);
                    }
                } else {
                    Log.e(TAG, "update failed " + e.toString() + " object_id " + item.getObjectId());
                    syncFailed();
                }
            }
        });
    }

    private void itemUploadDone()
    {
        m_items_to_upload--;
        if (m_items_to_upload == 0) {
            syncDone();
        }
    }

    private void itemDownloadDone()
    {
        m_items_to_download--;
        if (m_items_to_download == 0) {
            downloadAllDone();
        }
    }

    private void downloadAllDone()
    {
        mDownloading = false;

        if (mDownloadWishDoneListener != null) {
            mDownloadWishDoneListener.onDownloadWishDone(true);
        }

        // download from parse is finished, now upload to parse
        uploadToParse();
    }

    private void syncDone()
    {
        // both download and upload is done, sync is finished
        Log.d(TAG, "sync finished at " + mSyncedTime.getTime() + " " + StringUtil.UTCDate(mSyncedTime));
        // all items are downloaded, save the last synced down time
        final SharedPreferences sharedPref = WishlistApplication.getAppContext().getSharedPreferences(WishlistApplication.getAppContext().getString(R.string.app_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(LAST_SYNCED_TIME, mSyncedTime.getTime());
        editor.commit();

        mSyncing = false;
        if (mScheduleToSync) {
            mScheduleToSync = false;
            sync();
        }
    }

    private void syncFailed() {
        Log.e(TAG, "syncFailed");

        // when sync fails (most likely network unavailable), let's just do nothing
        // util the next sync is triggered
        mSyncing = false;
        mDownloading = false;
        mScheduleToSync = false;

        // notify listener to stop spinning and show an error
        if (mDownloadWishDoneListener != null) {
            mDownloadWishDoneListener.onDownloadWishDone(false);
        }
    }

    public void register(Activity activity) {
        try {
            mSyncWishChangedListener = (OnSyncWishChangedListener) activity;
            mDownloadWishDoneListener = (OnDownloadWishDoneListener) activity;
        }
        catch (final ClassCastException e) {
            Log.e(TAG, "fail to register");
            throw new ClassCastException(activity.toString() + " must implement OnSyncWishChanged/OnDownloadWishDone");
        }
    }

    public void updateProfileFromParse() {
        ParseUser.getCurrentUser().fetchInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException e) {
                if (e == null) {
                    Log.d(TAG, "success to fetch Parse user");
                    EventBus.getInstance().post(new ProfileChangeEvent(ProfileChangeEvent.ProfileChangeType.name));
                    EventBus.getInstance().post(new ProfileChangeEvent(ProfileChangeEvent.ProfileChangeType.email));

                    ParseUser currentUser = (ParseUser) object;
                    final ParseFile parseImage = currentUser.getParseFile("profileImage");
                    if (parseImage != null) {
                        parseImage.getDataInBackground(new GetDataCallback() {
                            @Override
                            public void done(byte[] data, ParseException e) {
                                if (e == null) {
                                    ProfileUtil.saveProfileImageToFile(data);
                                } else {
                                    Log.e(TAG, "fail to get profile image data " + e.toString());
                                }
                            }
                        });
                    }
                } else {
                    Log.e(TAG, "fail to fetch Parse user");
                }
            }
        });
    }

    private String parseFileNameToLocal(String parseFileName) {
        // when we save file to parse (aws s3), its file name will be prefixed by a server generated random string.
        // for example:
        // file name we uploaded                    IMG173391503.jpg
        // file name actually saved on server       4ab50fb811da73520a71bf6a6e7c8844_IMG173391503.jpg
        return parseFileName.substring(parseFileName.indexOf('_') + 1);
    }

    public interface OnSyncWishChangedListener {
        void onSyncWishChanged();
    }

    public interface OnDownloadWishDoneListener {
        void onDownloadWishDone(boolean success);
    }
}

