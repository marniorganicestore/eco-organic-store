package com.ecoorganicstore.common.web;

public final class PageWindow {
    public static final int MAX_SIZE = 48;
    public static final int MAX_PAGE = 200;
    public static final int SHOP_SIZE = 12;
    public static final int REVIEW_SIZE = 8;
    public static final int ORDER_SIZE = 10;
    public static final int ADMIN_SIZE = 20;

    private PageWindow() {}

    public static int page(int requested) {
        if (requested < 0) return 0;
        return Math.min(requested, MAX_PAGE);
    }

    public static int size(int requested, int fallback) {
        int chosen = requested < 1 ? fallback : requested;
        return Math.min(chosen, MAX_SIZE);
    }
}
