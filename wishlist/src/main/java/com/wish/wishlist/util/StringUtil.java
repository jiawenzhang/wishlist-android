package com.wish.wishlist.util;

import com.wish.wishlist.WishlistApplication;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.TimeZone;

/**
 * Created by jiawen on 2016-03-11.
 */
public final class StringUtil {
    private StringUtil() {}

    public static boolean sameArrays(ArrayList<String> arr1, ArrayList<String> arr2) {
        if (arr1 == null && arr2 == null) {
            return true;
        }

        if (arr1 == null || arr2 == null) {
            return false;
        }

        if (arr1.size() != arr2.size()) {
            return false;
        }
        HashSet<String> set1 = new HashSet<>(arr1);
        HashSet<String> set2 = new HashSet<>(arr2);
        return set1.equals(set2);
    }

    public static boolean compare(String str1, String str2) {
        return (str1 == null ? str2 == null : str1.equals(str2));
    }

    public static String UTCDate(Date date) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        return fmt.format(date);
    }

    private final static String NON_THIN = "[^iIl1\\.,']";

    private static int textWidth(String str) {
        return str.length() - str.replaceAll(NON_THIN, "").length() / 2;
    }

    public static String ellipsize(String text, int max) {
        if (textWidth(text) <= max)
            return text;

        // Start by chopping off at the word before max
        // This is an over-approximation due to thin-characters...
        int end = text.lastIndexOf(' ', max - 3);

        // Just one long word. Chop it off.
        if (end == -1)
            return text.substring(0, max-3) + "...";

        // Step forward as long as textWidth allows.
        int newEnd = end;
        do {
            end = newEnd;
            newEnd = text.indexOf(' ', end + 1);

            // No more spaces.
            if (newEnd == -1)
                newEnd = text.length();

        } while (textWidth(text.substring(0, newEnd) + "...") < max);

        return text.substring(0, end) + "...";
    }

    public static String randomHex(int size) {
        byte[] b = new byte[size];
        new Random(System.currentTimeMillis()).nextBytes(b);
        return bytesToHex(b);
    }

    final private static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] readByteFromAsset(String filename) throws IOException {
        InputStream in = WishlistApplication.getAppContext().getAssets().open(filename);
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len = 0;
        while ((len = in.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }

    public static String readFromAssets(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(WishlistApplication.getAppContext().getAssets().open(filename)));

        // do reading, usually loop until end of file reading
        StringBuilder sb = new StringBuilder();
        String mLine = reader.readLine();
        while (mLine != null) {
            sb.append(mLine); // process line
            mLine = reader.readLine();
        }
        reader.close();
        return sb.toString();
    }
}
