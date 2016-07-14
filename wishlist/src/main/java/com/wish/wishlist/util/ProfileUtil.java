package com.wish.wishlist.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.ProfileChangeEvent;
import com.wish.wishlist.image.ImageManager;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Created by jiawen on 2016-01-16.
 */
public class ProfileUtil {
    static final String TAG = "ProfileUtil";

    public static void downloadProfileImage() {
        // save profile image
        final ParseFile parseImage = ParseUser.getCurrentUser().getParseFile("profileImage");
        String fileName = profileImageName();
        if (parseImage != null && fileName != null) {
            try {
                final byte[] imageBytes = parseImage.getData();
                final File profileImageFile = new File(WishlistApplication.getAppContext().getFilesDir(), fileName);
                ImageManager.saveByteToPath(imageBytes, profileImageFile.getAbsolutePath());
                EventBus.getInstance().post(new ProfileChangeEvent(ProfileChangeEvent.ProfileChangeType.image));
                Log.d(TAG, "profile image downloaded and saved");
            } catch (com.parse.ParseException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    public static void downloadProfileImageIfNotExists() {
        if (!profileImageExists()) {
            downloadProfileImage();
        }
    }

    public static File profileImageFile() {
        String fileName = profileImageName();
        if (fileName == null) {
            return null;
        }
        return new File(WishlistApplication.getAppContext().getFilesDir(), fileName);
    }

    public static boolean profileImageExists() {
        String fileName = profileImageName();
        if (fileName == null) {
            return false;
        }
        final File profileImageFile = new File(WishlistApplication.getAppContext().getFilesDir(), fileName);
        return profileImageFile.exists();
    }

    public static String profileImageName() {
        ParseUser user = ParseUser.getCurrentUser();
        if (user == null) {
            return null;
        }
        return user.getObjectId() + "-profile-image.jpg";
    }

    public static boolean saveProfileImageToFile(Bitmap bitmap) {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bs);
        return saveProfileImageToFile(bs.toByteArray());
    }

    public static boolean saveProfileImageToFile(byte[] data) {
        File profileImageFile = profileImageFile();
        if (profileImageFile == null) {
            return false;
        }
        if (ImageManager.saveByteToPath(data, profileImageFile.getAbsolutePath())) {
            // Wishlist activity listens to this and update the profile info in navigation view
            EventBus.getInstance().post(new ProfileChangeEvent(ProfileChangeEvent.ProfileChangeType.image));
            return true;
        }
        return false;
    }

    public static Bitmap profileImageBitmap() {
        File profileImageFile = ProfileUtil.profileImageFile();
        if (profileImageFile == null || !profileImageFile.exists()) {
            return null;
        }
        return BitmapFactory.decodeFile(profileImageFile.getAbsolutePath());
    }

    public static Drawable generateProfileImage() {
        ParseUser user = ParseUser.getCurrentUser();
        if (user == null) {
            return null;
        }

        String name = user.getString("name");
        if (name == null || name.isEmpty()) {
            return null;
        }

        String username = user.getUsername();
        if (username == null) {
            username = "magic@username.com";
        }

        // generate color based on a key (same key returns the same color), useful for list/grid views
        ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT
        int color = generator.getColor(username);

        return TextDrawable.builder()
                .buildRound(name.substring(0,1).toUpperCase(), color);

    }

    private static Bitmap drawableToBitmap(Drawable drawable, int width, int height) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Bitmap generateProfileImage(String name, String username, int size) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        if (username == null) {
            username = "magic@username.com";
        }

        // generate color based on a key (same key returns the same color), useful for list/grid views
        ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT
        int color = generator.getColor(username);

        Drawable d = TextDrawable.builder()
                .buildRound(name.substring(0,1).toUpperCase(), color);

        // we need to convert the TextDrawable to Bitmap here as the TextDrawable has intrinsicWidth = -1, this will cause problems
        // when loading the drawable to a CircleImageView in the UserAdapter
        return drawableToBitmap(d, size, size);
    }
}
