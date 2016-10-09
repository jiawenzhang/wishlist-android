package com.wish.wishlist.util;

import com.wish.wishlist.activity.WebImage;
import com.wish.wishlist.model.Currency;

import java.util.ArrayList;

/**
 * Created by jiawen on 15-05-14.
 */
public class WebResult {
    public String url;
    public ArrayList<String> imageUrls =  new ArrayList<>();
    public ArrayList<WebImage> webImages = new ArrayList<>();
    public String title;
    public String description;
    public String price;
    public Double priceNumber;
    public Currency currency;
    public Boolean attemptedAllFromJsoup = false; // we have tried all results from Jsoup
    public Boolean attemptedDynamicHtml = false; // we have tried results from webview
}

