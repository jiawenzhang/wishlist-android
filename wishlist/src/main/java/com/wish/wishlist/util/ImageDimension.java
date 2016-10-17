package com.wish.wishlist.util;

/**
 * Created by jiawen on 2016-10-08.
 */

import android.os.Handler;
import android.util.Log;

import com.wish.wishlist.model.Dimension;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;

public final class ImageDimension {
    private static final String TAG = "ImageDimension";
    private static final ByteString JPEG_START_MARKER = ByteString.decodeHex("FF");
    private static final ByteString JPEG_BASELINE_MARKER = ByteString.decodeHex("C0");
    private static final ByteString JPEG_PROGRESSIVE_MARKER = ByteString.decodeHex("C2");

    private ImageDimension() {}

    public static Dimension extractImageDimension(String url, OkHttpClient client) {

        Request request;
        try {
            request = new Request.Builder()
                    .url(url)
                    .build();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.toString());
            return null;
        }

        OkHttpClient client_ = client;
        if (client_ == null) {
            client_ = new OkHttpClient();
        }

        Response response = null;
        try {
            response = client_.newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.e(TAG, "response not successful");
                response.close();
                return null;
            }

            switch (response.body().contentType().toString()) {
                case "image/jpeg":
                    Dimension d = decodeJpegDimension(response.body().byteStream());
                    response.close();
                    return d;
                default:
                    Log.e(TAG, "type not supported");
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } finally {
            if (response != null) response.close();
        }
        return null;

//        final Dimension d;
//        client_.newCall(request).enqueue(new Callback() {
//            @Override public void onFailure(Call call, IOException e) {
//                e.printStackTrace();
//            }
//
//            @Override public void onResponse(Call call, Response response) throws IOException {
//                switch (response.body().contentType().toString()) {
//                    case "image/jpeg":
//                        d = decodeJpegDimension(response.body().byteStream());
//                    default:
//                        Log.e(TAG, "type not supported");
//
//                        response.body().close();
//                }
//            }
//        });
    }

    private static Dimension decodeJpegDimension(InputStream in) throws IOException {
        // Jpeg stores in big endian
        BufferedSource jpegSource = Okio.buffer(Okio.source(in));
        Dimension dimension;

        while (true) {
            ByteString marker = jpegSource.readByteString(JPEG_START_MARKER.size());

            if (!marker.equals(JPEG_START_MARKER))
                continue;

            marker = jpegSource.readByteString(JPEG_START_MARKER.size());

            if (marker.equals(JPEG_BASELINE_MARKER) || marker.equals(JPEG_PROGRESSIVE_MARKER)) {
                jpegSource.skip(3);
                Short h = jpegSource.readShort();
                Short w = jpegSource.readShort();
                dimension = new Dimension(Integer.valueOf(w), Integer.valueOf(h));
                break;
            }
        }

        return dimension;
    }
}
