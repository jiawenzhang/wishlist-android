package com.wish.wishlist.sync;

import android.graphics.BitmapFactory;
import android.util.Log;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.image.ImageManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.util.StringUtil;
import com.wish.wishlist.wish.ImgMeta;
import com.wish.wishlist.wish.ImgMetaArray;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by jiawen on 2016-04-03.
 */
public class UploadTask {
    private static String TAG = "UploadTask";
    private long mItemsToUpload;
    private Date mSyncStamp;

    /* listener */
    private UploadTaskDoneListener mUploadTaskDoneListener;
    public interface UploadTaskDoneListener {
        void uploadTaskDone(boolean success, Date syncStamp);
    }

    public void registerListener(UploadTaskDoneListener l) {
        mUploadTaskDoneListener = l;
    }
    /* listener */


    public void run(Date syncStamp) {
        mSyncStamp = syncStamp;
        mItemsToUpload = 0;
        uploadToParse();
    }

    private void uploadToParse() {
        // get from local the items with synced_to_parse == false and upload them to parse
        ArrayList<WishItem> items = WishItemManager.getInstance().getItemsNotSyncedToServer();
        Log.d(TAG, "uploading " + items.size() + " items");
        mItemsToUpload = items.size();
        if (items.size() == 0) {
            uploadAllDone(true);
            return;
        }

        for (WishItem item : items) {
            if (item.getObjectId() == null) { // parse does not have this item
                Log.d(TAG, "item " + item.getName() + " does not exist on Parse, add to parse");
                addToParse(item);
            } else { // parse already has this item, update it
                Log.d(TAG, "item " + item.getName() + " already exists on Parse, update parse");
                updateParse(item);
            }
        }
    }

    private void uploadToParseWithImage(final ParseObject wishObject, final WishItem item, final boolean isNew) {
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
                    Log.d(TAG, "parse img url " + parseImage.getUrl());
                    List<ParseFile>  parseImageList = new ArrayList<>();
                    parseImageList.add(parseImage);
                    wishObject.put(WishItem.PARSE_KEY_IMAGES, parseImageList);
                    ImgMeta meta = new ImgMeta(ImgMeta.PARSE, parseImage.getUrl(), w, h);
                    String imgMetaJSON = new ImgMetaArray(meta).toJSON();
                    wishObject.put(WishItem.PARSE_KEY_IMG_META_JSON, imgMetaJSON);
                    item.setImgMetaJSON(imgMetaJSON);
                    item.saveToLocal();
                    uploadParseObject(wishObject, item.getId(), isNew);
                } else {
                    Log.e(TAG, "upload ParseFile failed " + e.toString());
                    itemUploadDone();
                }
            }
        });
    }

    private void uploadParseObject(final ParseObject wishObject, final long item_id, final boolean isNew) {
        wishObject.put(WishItem.PARSE_KEY_LAST_CHANGED_BY, ParseInstallation.getCurrentInstallation().getInstallationId());
        wishObject.saveInBackground(new SaveCallback() {
            @Override
            public void done(com.parse.ParseException e) {
                if (e == null) {
                    Log.d(TAG, "save wish success, object id: " + wishObject.getObjectId());
                    if (wishObject.getUpdatedAt().getTime() > mSyncStamp.getTime()) {
                        mSyncStamp = wishObject.getUpdatedAt();
                    }

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
                    Log.e(TAG, "upload wish meta failed " + e.toString());
                    // we simply skip uploading the item in this sync and attempt to upload it again on next sync
                    itemUploadDone();
                }
            }
        });
    }

    private void addToParse(final WishItem item) {
        Log.d(TAG, "Adding new item " + item.getName() + " to Parse");
        final ParseObject wishObject = item.toParseObject();
        if (item.getImgMetaJSON() != null || item.getThumbPicPath() == null) {
            // if we have an web url for the photo, we don't upload the photo to Parse so that we can save server space
            // when the other device sync down the wish, it will download the photo from the web url
            uploadParseObject(wishObject, item.getId(), true);
            return;
        }
        uploadToParseWithImage(wishObject, item, true);
    }

    private void updateParse(final WishItem item) {
        Log.d(TAG, "Updating item " + item.getName() + " to Parse");
        final ParseQuery<ParseObject> query = ParseQuery.getQuery(ItemDBManager.DB_TABLE);

        // Retrieve the object by id
        query.getInBackground(item.getObjectId(), new GetCallback<ParseObject>() {
            public void done(final ParseObject wishObject, com.parse.ParseException e) {
                if (e == null) {
                    boolean uploadImage = false;
                    if (!item.getDeleted()) {
                        // if we are deleting the wish, we don't need to upload the image
                        Object list = wishObject.get(WishItem.PARSE_KEY_IMAGES);
                        if (list != JSONObject.NULL) {
                            // there is an image on parse
                            @SuppressWarnings("unchecked")
                            List<ParseFile> pFileList = (List<ParseFile>) list;
                            ParseFile pf = pFileList.get(0);
                            String parseImageName = pf.getName();
                            // upload if local has an image and its name is different from parse image name
                            if (item.getPicName() != null &&
                                !StringUtil.compare(SyncAgent.parseFileNameToLocal(parseImageName), item.getPicName())) {
                                uploadImage = true;
                            }
                        } else {
                            // there is no image on parse
                            // upload if local has an image and it is not from web
                            if (item.getPicName() != null) {
                                if (item.getImgMetaArray() == null) {
                                    uploadImage = true;
                                } else if (!item.getImgMetaArray().get(0).mLocation.equals(ImgMeta.WEB)) {
                                    uploadImage = true;
                                }
                            }
                        }
                    }

                    WishItem.toParseObject(item, wishObject);
                    if (uploadImage) {
                        uploadToParseWithImage(wishObject, item, false);
                    } else {
                        uploadParseObject(wishObject, item.getId(), false);
                    }
                } else {
                    Log.e(TAG, "update wish meta failed " + e.toString() + " object_id " + item.getObjectId());
                    itemUploadDone();
                }
            }
        });
    }

    private void itemUploadDone() {
        mItemsToUpload--;
        if (mItemsToUpload == 0) {
            uploadAllDone(true);
        }
    }

    private void uploadAllDone(boolean success) {
        if (mUploadTaskDoneListener != null) {
            mUploadTaskDoneListener.uploadTaskDone(success, mSyncStamp);
        }
    }
}
