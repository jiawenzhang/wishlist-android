package com.wish.wishlist.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Created by jiawen on 2016-04-25.
 */

@RunWith(Suite.class)
@Suite.SuiteClasses({
        DBTest.class,
        ParseServerTest.class
})
public class UnitTestSuite {
}
