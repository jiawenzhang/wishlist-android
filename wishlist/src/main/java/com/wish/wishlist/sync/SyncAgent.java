package com.wish.wishlist.sync;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.GetCallback;
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
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.image.ImageManager;
import com.wish.wishlist.util.ProfileUtil;

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
    private Date m_synced_time;
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
        if (ParseUser.getCurrentUser() == null) {
            Log.d(TAG, "user not login, sync is disabled ");
            return;
        }
        // sync from parse

        // get from parse the items with updated time > last synced time
        // save them in parseItemList
        final SharedPreferences sharedPref = WishlistApplication.getAppContext().getSharedPreferences(WishlistApplication.getAppContext().getString(R.string.app_name), Context.MODE_PRIVATE);
        final Date last_synced_time = new Date(sharedPref.getLong("last_synced_time", 0));
        Log.d(TAG, "last_synced_time " + last_synced_time.getTime());
        m_synced_time = last_synced_time;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Item");
        query.whereGreaterThan("updatedAt", last_synced_time);
        query.whereEqualTo(WishItem.PARSE_KEY_OWNDER_ID, ParseUser.getCurrentUser().getObjectId());
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> itemList, com.parse.ParseException e) {
                if (e == null) {
                    m_downloaded_items.clear();
                    m_items_to_download = itemList.size();
                    if (m_items_to_download == 0) {
                        SyncAgent.this.mDownloadWishDoneListener.onDownloadWishDone();
                        uploadToParse();
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
                            if (existingItem.getUpdatedTime() >= parseItem.getLong(ItemDBManager.KEY_UPDATED_TIME)) {
                                itemDownloadDone();
                                continue;
                            }
                            Log.d(TAG, "item " + existingItem.getName() + " exists locally, but parse item is newer, overwrite local one");
                            // parseItem could be marked as deleted, update local item to be deleted will just hide the item
                            updateFromParse(parseItem, existingItem);
                        }
                    }
                } else {
                    SyncAgent.this.mDownloadWishDoneListener.onDownloadWishDone();
                    Log.e(TAG, "Error: " + e.getMessage());
                }
            }
        });
    }

    private void uploadToParse() {
        // sync to parse
        // get from local the items with synced_to_parse == false and push them to parse
        //Log.d(TAG, "uploadToParse");
        ArrayList<WishItem> items = WishItemManager.getInstance().getItemsNotSyncedToServer();
        m_items_to_upload = items.size();
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
        String photoURL = parseItem.getString(ItemDBManager.KEY_PHOTO_URL);
        final ParseFile parseImage = parseItem.getParseFile(WishItem.PARSE_KEY_IMAGE);
        if (photoURL == null && parseImage == null) {
            onPhotoDone(parseItem, null, null);
            return;
        }
        if (photoURL != null) {
            saveWebImage(photoURL, parseItem, null);
            return;
        }
        saveParseImage(parseImage, parseItem, null);
    }

    private void updateFromParse(final ParseObject parseItem, WishItem existingItem)
    {
        Log.d(TAG, "updateFromParse: item " + existingItem.getName());
        String photoURL = parseItem.getString(ItemDBManager.KEY_PHOTO_URL);
        final ParseFile parseImage = parseItem.getParseFile("image");
        if (photoURL == null && parseImage == null) {
            existingItem.removeImage();
            onPhotoDone(parseItem, existingItem, null);
            return;
        }
        if (photoURL != null) {
            if (!photoURL.equals(existingItem.getPicURL())) {
                // we have a new image, update it locally
                saveWebImage(photoURL, parseItem, existingItem);
            } else {
                onPhotoDone(parseItem, existingItem, existingItem.getFullsizePicPath());
            }
            return;
        }
        if (!parseImage.getName().equals(existingItem.getPicName())) {
            saveParseImage(parseImage, parseItem, existingItem);
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

        itemDownloadDone();
        if (m_items_to_download == 0) {
            // notify list/grid view to refresh
            SyncAgent.this.mSyncWishChangedListener.onSyncWishChanged();
        }
        m_synced_time = parseItem.getUpdatedAt();
    }

    private void updateTags(ParseObject item, long item_id) {
        List<String> tags = item.getList(WishItem.PARSE_KEY_TAGS);
        TagItemDBManager.instance().Update_item_tags(item_id, new ArrayList<>(tags));
    }

    private void saveWebImage(final String url, final ParseObject parseItem, final WishItem existingItem)
    {
        Log.d(TAG, "saveWebImage " + url);
        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                if (bitmap != null) {
                    bitmapLoaded(bitmap, parseItem, existingItem, url);
                } else {
                    Log.e(TAG, "saveWebImage->onBitmapLoaded null bitmap");
                }
            }
            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {}
        };
        m_targets.put(url, target);

        Picasso.with(WishlistApplication.getAppContext()).load(url).into(target);
    }

    private void bitmapLoaded(final Bitmap bitmap, final ParseObject parseItem, WishItem existingItem, String url)
    {
        String fullsizePath = ImageManager.saveBitmapToAlbum(bitmap);
        ImageManager.saveBitmapToThumb(bitmap, fullsizePath);
        if (existingItem != null) {
            existingItem.removeImage();
        }
        onPhotoDone(parseItem, existingItem, fullsizePath);
        m_targets.remove(url);

        SyncAgent.this.mSyncWishChangedListener.onSyncWishChanged();
    }

    private void saveParseImage(ParseFile parseImage, final ParseObject parseItem, WishItem existingItem)
    {
        Log.d(TAG, "saveParseImage: item " + parseItem.getString(ItemDBManager.KEY_NAME));
        try {
            // Fixme: getData is gets data from network syncrhonousely, blocks the UI
            final byte[] imageBytes = parseImage.getData();
            ImageManager.saveByteToAlbum(imageBytes, parseImage.getName(), /*thumb*/true);
            String fullsizePicPath = ImageManager.saveByteToAlbum(imageBytes, parseImage.getName(), /*thumb*/false);
            if (existingItem != null) {
                existingItem.removeImage();
            }
            onPhotoDone(parseItem, existingItem, fullsizePicPath);
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
                    m_synced_time = wishObject.getUpdatedAt();
                    WishItem item = WishItemManager.getInstance().getItemById(item_id);
                    Log.d(TAG, "set item " + item.getName() + " synced to true");
                    item.setSyncedToServer(true);
                    if (isNew) {
                        String object_id = wishObject.getObjectId();
                        item.setObjectId(object_id);
                    }
                    item.saveToLocal();
                } else {
                    Log.e(TAG, "save failed " + e.toString());
                }
                itemUploadDone();
            }
        });
    }

    private void addToParse(WishItem item)
    {
        Log.d(TAG, "Adding item " + item.getName() + " to Parse");

        final ParseObject wishObject = item.toParseObject();
        saveToParse(wishObject, item.getId(), true, true);
    }

    private void updateParse(final WishItem item)
    {
        Log.d(TAG, "Updating item " + item.getName() + " to Parse");
        final ParseQuery<ParseObject> query = ParseQuery.getQuery(ItemDBManager.DB_TABLE);

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
            // download from parse is finished, now upload to parse
            SyncAgent.this.mDownloadWishDoneListener.onDownloadWishDone();
            uploadToParse();
        }
    }

    private void syncDone()
    {
        Log.d(TAG, "sync finished at " + m_synced_time.getTime());
        // all items are processed, sync is done
        // save current time as last synced time
        final SharedPreferences sharedPref = WishlistApplication.getAppContext().getSharedPreferences(WishlistApplication.getAppContext().getString(R.string.app_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong("last_synced_time", m_synced_time.getTime());
        editor.commit();
    }

    public void register(Activity activity) {
        try {
            this.mSyncWishChangedListener = (OnSyncWishChangedListener) activity;
            this.mDownloadWishDoneListener = (OnDownloadWishDoneListener) activity;
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
                    ParseUser currentUser = (ParseUser) object;
                    final ParseFile parseImage = currentUser.getParseFile("profileImage");
                    try {
                        ProfileUtil.saveProfileImageToFile(parseImage.getData());
                    } catch (com.parse.ParseException e2) {
                        Log.e(TAG, e2.toString());
                    }
                } else {
                    Log.e(TAG, "fail to fetch Parse user");
                }
            }
        });
    }

    public interface OnSyncWishChangedListener {
        void onSyncWishChanged();
    }

    public interface OnDownloadWishDoneListener {
        void onDownloadWishDone();
    }
}

