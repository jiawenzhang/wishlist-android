package com.wish.wishlist.sync;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by jiawen on 2016-04-03.
 */
public class DownloadImageTask {
    private static String TAG = "DownloadImageTask";
    private HashMap<Long, String> mItemUrl= new HashMap<>();
    private HashMap<Long, Target> mTargets = new HashMap<>();
    private boolean mImageChanged = false;

    class Result {
        // result code
        public int HEAD_HTTP_OK = 0;
        public int HEAD_HTTP_NOT_OK = 1;
        public int NETWORK_ERROR = 2;
        public int MALFORMEDURL = 3;

        Long itemId;
        public String url;
        public int code;
    }

    private class checkImageExists extends AsyncTask<Result, Void, Result> {
        @Override
        protected Result doInBackground(Result... params) {
            Result result = params[0];

            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = null;

            try {
                con = (HttpURLConnection) new URL(result.url).openConnection();
                con.setRequestMethod("HEAD");
                if (Build.VERSION.SDK_INT < 21) {
                    // Android's HttpURLConnection throws EOFException on HEAD requests for some urls
                    // failure url example: "http://img.canadacomputers.com/Products/300x300/008473/334.jpg"
                    // discussion: http://stackoverflow.com/questions/17638398/androids-httpurlconnection-throws-eofexception-on-head-requests
                    con.setRequestProperty("Accept-Encoding", "");
                }
                if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "file exists");
                    result.code = result.HEAD_HTTP_OK;
                } else {
                    // server responds with non http 200 code, it is likely the url has become invalid,
                    // like image has been removed or moved to a different url.
                    Log.e(TAG, "file may not exist, response code: " + con.getResponseCode() + " " + result.url);
                    result.code = result.HEAD_HTTP_NOT_OK;
                    Analytics.send(Analytics.DEBUG, "MayNoFile", con.getResponseMessage() + " " + result.url);
                }
            } catch (MalformedURLException e) {
                Log.e(TAG, "MalformedURLException " + e.toString());
                result.code = result.MALFORMEDURL;
                Analytics.send(Analytics.DEBUG, "CheckFileException", e.toString() + " " + result.url);
            } catch (Exception e) {
                // we may have a network error like timeout or java.net.UnknownHostException
                // need more experiments to test what other exception is possible
                Log.e(TAG, "check file exception " + e.toString() + " " + result.url);
                Analytics.send(Analytics.DEBUG, "CheckFileException", e.toString() + " " + result.url);
                result.code = result.NETWORK_ERROR;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
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
        void downloadImageTaskDone(boolean success, boolean imageChanged);
    }

    public void registerListener(DownloadImageTaskDoneListener l) {
        mDownlaodImageTaskDoneListener = l;
    }
    /* listener */


    public void run() {
        mTargets.clear();
        mItemUrl.clear();
        mImageChanged = false;

        ArrayList<Long> ids = ItemDBManager.getItemIdsToDownloadImg();
        for (Long item_id : ids) {
            WishItem item = WishItemManager.getInstance().getItemById(item_id);
            ImgMeta imgMeta = item.getImgMetaArray() == null ? null : item.getImgMetaArray().get(0);
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
        mTargets.remove(itemId);

        WishItem item = WishItemManager.getInstance().getItemById(itemId);
        if (item != null && bitmap != null) {
            mImageChanged = true;
            ImgMeta imgMeta = item.getImgMetaArray() == null ? null : item.getImgMetaArray().get(0);
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
                Analytics.send(Analytics.DEBUG, "ImgMetaNull", "JSON?");
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
            mDownlaodImageTaskDoneListener.downloadImageTaskDone(success, mImageChanged);
        }
    }

    private void checkImageResult(final Result result) {
        //download web image could fail for various reasons - the url becomes invalid, server cannot be
        //reached, host name cannot be resolved or there is no network etc. The major problem here is
        //the web image and the validity of its url is out of our own control, we will attempt to as many cases as possible here
        if (result.code == result.HEAD_HTTP_OK) {
            Target target = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    if (bitmap == null) {
                        //Fixme: on what circumstances will this be triggered? no network? url invalid?
                        Log.e(TAG, "downloadWebImage->onBitmapLoaded null bitmap");
                        Analytics.send(Analytics.DEBUG, "BitmapNull", result.url);
                    }
                    imageDownloaded(bitmap, result.itemId, result.url);
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {}

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                    Log.e(TAG, "onBitmapFailed");
                    Analytics.send(Analytics.DEBUG, "OnBitmapFailed", result.url);
                    imageDownloaded(null, result.itemId, result.url);
                }
            };

            mTargets.put(result.itemId, target);
            Picasso.with(WishlistApplication.getAppContext()).load(result.url).resize(ImageManager.IMG_WIDTH, 2048).centerInside().onlyScaleDown().into(target);
        } else if (result.code == result.HEAD_HTTP_NOT_OK) {
            // return non 200 when sending HEAD request to the url
            // server may not support HEAD request or the HEAD request is disabled, but GET may still work
            // For example: "http://ia.media-imdb.com/images/M/MV5BMTQ3MDQwMjQ5NV5BMl5BanBnXkFtZTgwMTY2MjAyOTE@._V1_UY1200_CR69,0,630,1200_AL_.jpg"
            // so let's still give it a try to download the file
            Target target = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    if (bitmap == null) {
                        //Fixme: on what circumstances will this be triggered? no network? url invalid?
                        Log.e(TAG, "downloadWebImage->onBitmapLoaded null bitmap");
                        Analytics.send(Analytics.DEBUG, "BitmapNull", result.url);
                    }
                    // Good! we can still download the file even HEAD request returns non 200
                    imageDownloaded(bitmap, result.itemId, result.url);
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {}

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                    Log.e(TAG, "onBitmapFailed");
                    Analytics.send(Analytics.DEBUG, "OnBitmapFailed", result.url);
                    // fail to download the image, it is likely the url has become invalid, because HEAD request returns non 200
                    // and download fails
                    invalidateImageUrl(result.itemId);
                    imageDownloaded(null, result.itemId, result.url);
                }
            };

            mTargets.put(result.itemId, target);
            Picasso.with(WishlistApplication.getAppContext()).load(result.url).resize(ImageManager.IMG_WIDTH, 2048).centerInside().onlyScaleDown().into(target);
        } else if (result.code == result.MALFORMEDURL) {
            invalidateImageUrl(result.itemId);
            itemImageDone(result.itemId);
        } else if (result.code == result.NETWORK_ERROR) {
            itemImageDone(result.itemId);
        }
    }

    private void invalidateImageUrl(long itemId) {
        // clear the wish's ImgMeta so we won't try to download from this url again
        WishItem item = WishItemManager.getInstance().getItemById(itemId);
        item.setImgMetaArray(null);
        item.setDownloadImg(false);
        item.saveToLocal();
    }

}
