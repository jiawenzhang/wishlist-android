package com.wish.wishlist.event;

/**
 * Created by jiawen on 15-10-04.
 */

// a singleton instance for getting the bus.
public final class EventBus {
    private static final MainThreadBus BUS = new MainThreadBus();

    public static MainThreadBus getInstance() {
        return BUS;
    }

    private EventBus() {}
}
