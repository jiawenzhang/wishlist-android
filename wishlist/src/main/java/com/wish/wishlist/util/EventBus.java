package com.wish.wishlist.util;

import com.squareup.otto.Bus;

/**
 * Created by jiawen on 15-10-04.
 */

// a singleton instance for getting the bus.
public final class EventBus {
    private static final Bus BUS = new Bus();

    public static Bus getInstance() {
        return BUS;
    }

    private EventBus() {}
}
