package com.wish.wishlist.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import com.squareup.picasso.Picasso;
import com.wish.wishlist.activity.WebImage;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

/**
 * Created by jiawen on 15-05-14.
 */


public class GetWebItemTask extends AsyncTask<WebRequest, Integer, WebResult> {
    final static String TAG = "GetWebItemTask";
    Context mContext;
    private OnWebResult mListener;

    public interface OnWebResult {
        void onWebResult(WebResult result);
    }

    public GetWebItemTask(Context context, OnWebResult listener) {
        super();
        this.mContext = context;
        this.mListener = listener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    WebResult getImages(WebRequest request) {
        WebResult result = new WebResult();
        try {
            //Connection.Response response = Jsoup.connect(urls[0]).followRedirects(true).execute();
            Document doc;
            if (request.html != null && !request.html.isEmpty()) {
                result._attemptedDynamicHtml = true;
                doc = Jsoup.parse(request.html);
            } else {
                doc = Jsoup.connect(request.url)
                        .header("Accept-Encoding", "gzip, deflate")
                        .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/23.0")
                        .maxBodySize(0)
                        .timeout(600000)
                        .get();
            }

            // Prefer og:title if the site has it
            Elements og_title_element = doc.head().select("meta[property=og:title]");
            if (!og_title_element.isEmpty()) {
                String og_title = og_title_element.first().attr("content");
                Log.d(TAG, "og:title : " + og_title);
                result._title = og_title;
            }
            if (result._title == null || result._title.trim().isEmpty()) {
                Elements meta_title_element = doc.head().select("meta[name=title]");
                if (!meta_title_element.isEmpty()) {
                    String meta_title = meta_title_element.first().attr("content");
                    Log.d(TAG, "meta:title : " + meta_title);
                    result._title = meta_title;
                }
            }
            if (result._title == null || result._title.trim().isEmpty()) {
                result._title = doc.title();
            }

            Elements og_description_element = doc.head().select("meta[property=og:description]");
            if (!og_description_element.isEmpty()) {
                String og_description = og_description_element.first().attr("content");
                Log.d(TAG, "og:description : " + og_description);
                result._description = og_description;
            }
            if (result._description == null || result._description.trim().isEmpty()) {
                Elements meta_description_element = doc.head().select("meta[name=description]");
                if (!meta_description_element.isEmpty()) {
                    String meta_description = meta_description_element.first().attr("content");
                    Log.d(TAG, "meta:description : " + meta_description);
                    result._description = meta_description;
                }
            }

            // Prefer twitter image over facebook image, because facebook og:image requires dimension of
            // 1200 x 630 or 600 x 315, which will crop a tall image in an ugly way

            // Some websites use twitter:image, some use twitter:image:src, make sure we cover both.
            Elements twitter_image_element = doc.head().select("meta[property=twitter:image]");
            if (twitter_image_element.isEmpty()) {
                twitter_image_element = doc.head().select("meta[property=twitter:image:src]");
            }
            if (!twitter_image_element.isEmpty()) {
                String twitter_image_src = twitter_image_element.first().attr("content");
                Bitmap image = null;
                try {
                    image = Picasso.with(mContext).load(twitter_image_src).get();
                } catch (IOException e) {}

                if (image != null) {
                    result._webImages.add(new WebImage(twitter_image_src, image.getWidth(), image.getHeight(), "", image));
                    if (image.getWidth() >= 100 && image.getHeight() >= 100) {
                        Log.d(TAG, "twitter:image:src " + twitter_image_src + " " + image.getWidth() + "X" + image.getHeight());
                        if (!request.getAllImages) {
                            return result;
                        }
                    }
                }
            }

            Elements og_image = doc.head().select("meta[property=og:image]");
            if (!og_image.isEmpty()) {
                String og_image_src = og_image.first().attr("content");
                Bitmap image = null;
                try {
                    image = Picasso.with(mContext).load(og_image_src).get();
                } catch (IOException e) {}

                if (image != null) {
                    result._webImages.add(new WebImage(og_image_src, image.getWidth(), image.getHeight(), "", image));
                    // some websites like kijiji will return us a tiny og:image
                    // let's try to get more images if this happens.
                    if (image.getWidth() >= 100 && image.getHeight() >= 100) {
                        Log.d(TAG, "og:image src: " + og_image_src + " " + image.getWidth() + "X" + image.getHeight());
                        if (!request.getAllImages) {
                            return result;
                        }
                    }
                }
            }

            // Didn't find og:image tag, so retrieve all the images in the website, filter them by type and size, and
            // let user choose one.
            Elements img_elements = doc.getElementsByTag("img");
            Log.d(TAG, "Found " + img_elements.size() + " img elements");
            for (Element el : img_elements) {
                //Log.d(TAG, "el tostring " + el.toString());

                String src = el.absUrl("src");
                // width and height can be in the format of 100px,
                // remove the non digit part of the string.
                //String width = el.attr("width").replaceAll("[^0-9]", "");
                //String height = el.attr("height").replaceAll("[^0-9]", "");
                String style = el.attr("style");

                // filter out hidden images, gif and png images. (gif and png are usually used for icons etc.)
                if (src.isEmpty() || style.contains("display:none") || src.endsWith(".gif") || src.endsWith(".png")) {
                    continue;
                }

                try {
                    final Bitmap image = Picasso.with(mContext).load(src).get();
                    // filter out small images
                    if (image == null || image.getWidth() <= 100 || image.getHeight() <= 100) {
                        continue;
                    }
                    Log.d(TAG, "adding " + src);
                    result._webImages.add(new WebImage(src, image.getWidth(), image.getHeight(), el.id(), image));
                } catch (IOException e) {
                    Log.d(TAG, "IOException " + e.toString());
                }
            }

            // when there are more than one images, we use picasso to load image from url, so need to pass the bitmap
            if (result._webImages.size() > 1) {
                for (WebImage webImage : result._webImages) {
                    webImage.mBitmap = null;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        return result;
    }

    protected WebResult doInBackground(WebRequest... requests) {
        WebRequest request = requests[0];
        return getImages(request);
    }

    protected void onProgressUpdate(Integer... progress) {
    }

    protected void onPostExecute(WebResult result) {
        mListener.onWebResult(result);
    }
}

