/**
 * Created by jiawen on 2016-04-23.
 */

package com.wish.wishlist.test;

import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.wish.wishlist.model.Dimension;
import com.wish.wishlist.util.ImageDimension;

import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class ImageDimensionTest extends InstrumentationTestCase {

    @Test
    public void WishDBWriteAndRead() throws InterruptedException {
        Dimension d;
        d = ImageDimension.extractImageDimension("http://i5.walmartimages.ca/images/Large/473/4_1/954734_1.jpg", null);
        assertEquals(d.getWidth(), 460);
        assertEquals(d.getHeight(), 460);

        d = ImageDimension.extractImageDimension("http://i5.walmartimages.ca/images/Thumbnails/248/289/248289.jpg", null);
        assertEquals(d.getWidth(), 210);
        assertEquals(d.getHeight(), 210);

        d = ImageDimension.extractImageDimension("http://i.ebayimg.com/images/m/m9m8c6HzBqMRgtonxhG840w/s-l300.jpg", null);
        assertEquals(d.getWidth(), 300);
        assertEquals(d.getHeight(), 300);
    }
}
