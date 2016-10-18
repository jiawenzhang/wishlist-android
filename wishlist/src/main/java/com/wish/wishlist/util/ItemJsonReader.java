package com.wish.wishlist.util;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.util.Patterns;

import com.wish.wishlist.activity.WebImage;
import com.wish.wishlist.model.Currency;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

/**
 * Created by jiawen on 2016-10-07.
 */

public final class ItemJsonReader {
    private static final String TAG = "ItemJsonReader";

    public static WebResult readObjString(String s) {
        WebResult result = new WebResult();

        JsonReader reader = new JsonReader(new StringReader(s));
        reader.setLenient(true);
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "url":
                        if (reader.peek() == JsonToken.NULL) {
                            reader.skipValue();
                        } else {
                            result.url = reader.nextString();
                        }
                        break;
                    case "title":
                        if (reader.peek() == JsonToken.NULL) {
                            reader.skipValue();
                        } else {
                            result.title = reader.nextString();
                        }
                        break;
                    case "description":
                        if (reader.peek() == JsonToken.NULL) {
                            reader.skipValue();
                        } else {
                            result.description = reader.nextString();
                        }
                        break;
                    case "price":
                        if (reader.peek() == JsonToken.NULL) {
                            reader.skipValue();
                        } else {
                            result.price = reader.nextString();
                        }
                        break;
                    case "price_number":
                        if (reader.peek() == JsonToken.NULL) {
                            reader.skipValue();
                        } else {
                            result.priceNumber = reader.nextDouble();
                        }
                        break;
                    case "currency":
                        if (reader.peek() == JsonToken.NULL) {
                            reader.skipValue();
                        } else {
                            result.currency = readCurrency(reader);
                        }
                        break;
                    case "og_image":
                        Analytics.send(Analytics.DEBUG, "GotImage", "OG");
                        reader.skipValue();
                        break;
                    case "twitter_image":
                        Analytics.send(Analytics.DEBUG, "GotImage", "Twitter");
                        reader.skipValue();
                        break;
                    case "image_urls":
                        if (reader.peek() == JsonToken.NULL) {
                            reader.skipValue();
                        } else {
                            result.imageUrls = readImageUrls(reader);
                        }
                        break;
                    case "images":
                        if (reader.peek() == JsonToken.NULL) {
                            reader.skipValue();
                        } else {
                            result.webImages = readImages(reader);
                        }
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
            }

            reader.endObject();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
        return result;
    }

    private static ArrayList<String> readImageUrls(JsonReader reader) throws IOException {
        ArrayList<String> imageUrls = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
            String url = reader.nextString();
            url = getValidImageUrl(url);
            if (url != null && !imageUrls.contains(url)) {
                imageUrls.add(url);
            }
        }
        reader.endArray();

        return imageUrls;
    }

    private static ArrayList<WebImage> readImages(JsonReader reader) throws IOException {
        ArrayList<WebImage> webImages = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
            WebImage webImage = readImage(reader);
            if (webImage != null) {
                webImages.add(webImage);
            }
        }
        reader.endArray();

        return webImages;
    }

    private static WebImage readImage(JsonReader reader) throws IOException {
        String url = null;
        int w = -1;
        int h = -1;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "url":
                    url = reader.nextString();
                    url = getValidImageUrl(url);
                    break;
                case "w":
                    w = reader.nextInt();
                    break;
                case "h":
                    h = reader.nextInt();
                    break;
                default:
                    reader.skipValue();
            }
        }
        reader.endObject();

        if (url != null) {
            return new WebImage(url, w, h, "", null);
        }
        return null;
    }

    private static Currency readCurrency(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.skipValue();
            return null;
        }

        Currency currency = new Currency();

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "code":
                    currency.code = reader.nextString();
                    break;
                case "symbol":
                    currency.symbol = reader.nextString();
                    break;
                default:
                    reader.skipValue();
            }
        }
        reader.endObject();
        return currency;
    }

    private static String getValidImageUrl(String src) {
        // mal-formatted Url, for example m.gamestop.com will give us img src like
        // "//www.gamestop.com/common/images/lbox/111111.jpg"

        // urbanoutffters.com's og:img is also mal-formatted
        // try to correct it

        if (src == null) {
            return null;
        }

        // trim the leading '/' if there is any
        src = src.replaceAll("^/+", "");
        if (!src.startsWith("http://") && !src.startsWith("https://")) {
            src = "http://" + src;
        }

        // validate the url
        // Fixme: Pattens.WEB_URL will tread $ in url as invalid
        // For example: http://i.ebayimg.com/00/s/NTAwWDUwMA==/z/sPUAAOSwNRdX4t3-/$_12.JPG
        // is treated as invalid url, but it is actually valid!
//        if (!Patterns.WEB_URL.matcher(src).matches()) {
//            Log.e(TAG, "Invalid url " + src);
//            return null;
//        }

        if (src.isEmpty()) {
            return null;
        }
        return src;
    }
}
