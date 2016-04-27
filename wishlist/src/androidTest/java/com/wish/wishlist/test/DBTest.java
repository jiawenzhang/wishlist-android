/**
 * Created by jiawen on 2016-04-23.
 */

package com.wish.wishlist.test;

import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DBTest extends InstrumentationTestCase {

    private static final String TAG = "DBTest";
    static public int mWishCount = 20;

    @Test
    public void WishDBWriteAndRead() throws InterruptedException {
        // Generate mWishCount wishes and save them to db, read from db and check if they are the same
        for (int i = 0; i < mWishCount; i++) {
            WishItem item = Tester.generateWish();
            item.saveToLocal();

            WishItem savedItem = WishItemManager.getInstance().getItemById(item.getId());
            assertTrue(item.equals(savedItem));

            //WishImageDownloader mImageDownloader = new WishImageDownloader();
            //ArrayList<WishItem> items = new ArrayList<>();
            //items.add(item);
            //mImageDownloader.download(items);
        }
    }
}
