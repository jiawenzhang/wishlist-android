package com.wish.wishlist.util;

import com.wish.wishlist.activity.WebImage;
import java.util.ArrayList;

/**
 * Created by jiawen on 15-05-14.
 */
public class WebResult {
    public ArrayList<WebImage> _webImages = new ArrayList<>();
    public String _title;
    public String _description;
    public Boolean _attemptedAllFromJsoup = false; // we have tried all results from Jsoup
    public Boolean _attemptedDynamicHtml = false; // we have tried results from webview
}
