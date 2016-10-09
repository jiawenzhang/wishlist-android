package com.wish.wishlist.test;

import com.wish.wishlist.util.DoubleUtil;
import com.wish.wishlist.util.StringUtil;

import org.junit.Test;
import java.util.regex.Pattern;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UtilTest {

    @Test
    public void DoubleUtil() {
        //assertEquals("Conversion from celsius to fahrenheit failed", 1, 1, 0.001);
        assertTrue(DoubleUtil.compare(0.1, 0.1));
        assertTrue(DoubleUtil.compare(null, null));
        assertTrue(!DoubleUtil.compare(0.2, null));
        assertTrue(!DoubleUtil.compare(null, 0.2));
        assertTrue(!DoubleUtil.compare(0.1, 0.2));
    }

    @Test
    public void StringUtil() {
        assertTrue(StringUtil.compare("test", "test"));
        assertTrue(StringUtil.compare(null, null));
        assertTrue(StringUtil.compare("", ""));
        assertTrue(!StringUtil.compare(null, "test"));
        assertTrue(!StringUtil.compare("test", null));
        assertTrue(!StringUtil.compare("test1", "test2"));
    }
}
