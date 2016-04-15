package com.wish.wishlist.sync;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.image.ImageManager;
import com.wish.wishlist.image.PhotoFileCreater;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.util.Analytics;
import com.wish.wishlist.wish.ImgMeta;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by jiawen on 2016-04-03.
 */
public class DownloadImageTask {
    private static String TAG = "DownloadImageTask";
    private HashMap<Long, String> mItemUrl= new HashMap<>();
    private HashMap<String, Target> mTargets = new HashMap<>();

    class Result {
        // result code
        public int EXISTS = 0;
        public int NO_FILE = 1;
        public int NETWORK_ERROR = 2;

        Long itemId;
        public String url;
        public int code;
    }

    private class checkImageExists extends AsyncTask<Result, Void, Result> {
        @Override
        protected Result doInBackground(Result... params) {
            Result result = params[0];

            try {
                HttpURLConnection.setFollowRedirects(false);
                HttpURLConnection con =  (HttpURLConnection) new URL(result.url).openConnection();
                con.setRequestMethod("HEAD");
                if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "file exists");
                    result.code = result.EXISTS;
                } else {
                    // server responds with non http 200 code, it is likely the url has become invalid,
                    // like image has been removed or moved to a different url.
                    Log.e(TAG, "file does not exist, response code: " + con.getResponseCode());
                    result.code = result.NO_FILE;
                    Analytics.send(Analytics.DEBUG, "NoFile", con.getResponseMessage() + " " + result.url);
                }
            } catch (Exception e) {
                // we may have a network error like timeout or java.net.UnknownHostException
                // need more experiments to test what other exception is possible
                Log.e(TAG, "check file error " + e.toString());
                result.code = result.NETWORK_ERROR;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Result result) {
            checkImageResult(result);
        }
    }

    /* listener */
    private DownloadImageTaskDoneListener mDownlaodImageTaskDoneListener;
    public interface DownloadImageTaskDoneListener {
        void downloadImageTaskDone(boolean success);
    }

    public void registerListener(DownloadImageTaskDoneListener l) {
        mDownlaodImageTaskDoneListener = l;
    }
    /* listener */


    public void run() {
        mTargets.clear();
        mItemUrl.clear();

        ArrayList<Long> ids = ItemDBManager.getItemIdsToDownloadImg();
        for (Long item_id : ids) {
            WishItem item = WishItemManager.getInstance().getItemById(item_id);
            ImgMeta imgMeta = item.getImgMeta();
            if (imgMeta != null) {
                // this wish's image needs to be downloaded
                Log.d(TAG, "item \"" + item.getName() + "\" need to download image");
                mItemUrl.put(item_id, imgMeta.mUrl);
            }
        }

        if (mItemUrl.isEmpty()) {
            Log.d(TAG, "0 item need to download image");
            imageAllDone(true);
            return;
        }

        Log.d(TAG, mItemUrl.size() + " items downloading images ...");
        for (HashMap.Entry<Long, String> entry : mItemUrl.entrySet()) {
            Result result = new Result();
            result.itemId = entry.getKey();
            result.url = entry.getValue();
            new checkImageExists().execute(result);
        }
    }

    private void imageDownloaded(final Bitmap bitmap, Long itemId, String url) {
        mTargets.remove(url);

        WishItem item = WishItemManager.getInstance().getItemById(itemId);
        if (item != null && bitmap != null) {
            ImgMeta imgMeta = item.getImgMeta();
            if (imgMeta != null) {
                String fullsizePicPath = null;
                if (imgMeta.mLocation.equals(ImgMeta.PARSE) ) {
                    //Fixme: unsafe to get file name this way if it is an arbitrary url, but since the url is from AWS S3, we know this works.
                    Log.d(TAG, "image downloaded from parse " + url);
                    String fileName = url.substring(url.lastIndexOf('/') + 1);
                    fullsizePicPath = ImageManager.saveBitmapToFile(bitmap, SyncAgent.parseFileNameToLocal(fileName), 100);
                    ImageManager.saveBitmapToFile(bitmap, PhotoFileCreater.getInstance().thumbFilePath(fullsizePicPath));
                } else if (imgMeta.mLocation.equals(ImgMeta.WEB)) {
                    Log.d(TAG, "image downloaded from web " + url);
                    fullsizePicPath = ImageManager.saveBitmapToFile(bitmap);
                    ImageManager.saveBitmapToThumb(bitmap, fullsizePicPath);
                } else {
                    Log.e(TAG, "imgMeta location unknown");
                }
                item.removeImage();
                item.setFullsizePicPath(fullsizePicPath);
                item.setDownloadImg(false);
                item.saveToLocal();
            } else {
                Log.e(TAG, "imgMeta null, JSON parse error?");
            }
        }
        itemImageDone(itemId);
    }

    private void itemImageDone(Long itemId) {
        mItemUrl.remove(itemId);
        if (mItemUrl.size() == 0) {
            imageAllDone(true);
        }
    }

    private void imageAllDone(boolean success) {
        // download from parse is finished, now upload to parse
        if (mDownlaodImageTaskDoneListener != null) {
            mDownlaodImageTaskDoneListener.downloadImageTaskDone(success);
        }
    }

    private void checkImageResult(final Result result) {
        //download web image could fail for various reasons - the url becomes invalid, server cannot be
        //reached, host name cannot be resolved or there is no network etc. The major problem here is
        //the web image and the validity of its url is out of our own control, we will attempt to handle them here
        if (result.code == result.EXISTS) {
            Target target = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    if (bitmap == null) {
                        //Fixme: on what circumstances will this be triggered? no network? url invalid?
                        Log.e(TAG, "downloadWebImage->onBitmapLoaded null bitmap");
                    }
                    imageDownloaded(bitmap, result.itemId, result.url);
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {}

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                    Log.e(TAG, "onBitmapFailed");
                    imageDownloaded(null, result.itemId, result.url);
                }
            };

            mTargets.put(result.url, target);
            Picasso.with(WishlistApplication.getAppContext()).load(result.url).resize(ImageManager.IMG_WIDTH, 0).into(target);
        } else if (result.code == result.NO_FILE) {
            // invalid url, clear the wish's ImgMeta so we won't try to download from this url again
            WishItem item = WishItemManager.getInstance().getItemById(result.itemId);
            item.setImgMeta(null, null, 0, 0);
            item.setDownloadImg(false);
            item.saveToLocal();

            itemImageDone(result.itemId);
        } else if (result.code == result.NETWORK_ERROR) {
            itemImageDone(result.itemId);
        }
    }
}
