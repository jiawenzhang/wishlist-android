package com.wish.wishlist.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.wish.wishlist.R;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.util.DateTimeFormatter;
import com.wish.wishlist.util.social.ShareHelper;

public class WishDetail extends ActivityBase {

    public final static String ITEM = "Item";

    protected ImageView mPhotoView;
    private TextView mNameView;
    private TextView mDescrpView;
    private TextView mDateView;
    private TextView mPriceView;
    private TextView mStoreView;
    private TextView mLocationView;
    private TextView mLinkView;
    protected TagsCompletionView mTagsView;
    protected WishItem mItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wishitem_detail);
        setupActionBar(R.id.item_detail_toolbar);

        mPhotoView = (ImageView) findViewById(R.id.imgPhotoDetail);
        mNameView = (TextView) findViewById(R.id.itemNameDetail);
        mDescrpView = (TextView) findViewById(R.id.itemDesriptDetail);
        mDateView = (TextView) findViewById(R.id.itemDateDetail);
        mPriceView = (TextView) findViewById(R.id.itemPriceDetail);
        mStoreView = (TextView) findViewById(R.id.itemStoreDetail);
        mLocationView = (TextView) findViewById(R.id.itemLocationDetail);
        mLinkView = (TextView) findViewById(R.id.itemLink);

        // tagsView will gain focus automatically when the activity starts, and it will trigger the keyboard to
        // show up if we don't have the following line.
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        mTagsView = (TagsCompletionView) findViewById(R.id.ItemTagsView);
        View.OnTouchListener otl = new View.OnTouchListener() {
            public boolean onTouch (View v, MotionEvent event) {
                return true;
                // the listener has consumed the event
                // this is to prevent touch event on the tagsView such as
                // keyboard pops up, text select, copy/paste etc.
            }
        };
        mTagsView.setOnTouchListener(otl);
        mTagsView.setCursorVisible(false);

        Intent i = getIntent();
        mItem = i.getParcelableExtra(ITEM);
    }

    protected void showPhoto() {}

    protected void showItemInfo() {
        showPhoto();

        String dateTimeStr = mItem.getUpdatedTimeStr();
        String dateTimeStrNew = DateTimeFormatter.getInstance().getDateTimeString(dateTimeStr);

        mNameView.setText(mItem.getName());
        if (mItem.getComplete() == 1) {
            final ImageView completeImage = (ImageView) findViewById(R.id.item_checkmark_complete);
            completeImage.setVisibility(View.VISIBLE);
        }
        mDateView.setText(dateTimeStrNew);

        // format the price
        String priceStr = mItem.getPriceAsString();
        if (priceStr != null) {
            mPriceView.setText(WishItem.priceStringWithCurrency(priceStr));
            mPriceView.setVisibility(View.VISIBLE);
        }
        else {
            mPriceView.setVisibility(View.GONE);
        }

        //used as a note
        String descrptStr = mItem.getDesc();
        if (!descrptStr.equals("")) {
            mDescrpView.setText(descrptStr);
            mDescrpView.setVisibility(View.VISIBLE);
        }
        else {
            mDescrpView.setVisibility(View.GONE);
        }

        String storeName = mItem.getStoreName();
        if (!storeName.equals("")) {
            mStoreView.setText(storeName);
            mStoreView.setVisibility(View.VISIBLE);
        }
        else {
            mStoreView.setVisibility(View.GONE);
        }

        String address = mItem.getAddress();
        if (!address.equals("unknown") && !address.equals("")) {
            mLocationView.setText(address);
            mLocationView.setVisibility(View.VISIBLE);
        }
        else {
            mLocationView.setVisibility(View.GONE);
        }

        String link = mItem.getLink();
        if (link != null && !link.isEmpty()) {
            String url = mItem.getLink();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            String text = "<a href=\"" + url + "\">Link</a>";
            mLinkView.setText(Html.fromHtml(text));
            mLinkView.setMovementMethod(LinkMovementMethod.getInstance());
            mLinkView.setVisibility(View.VISIBLE);
        } else {
            mLinkView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        long itemId = menuItem.getItemId();
        if (itemId == android.R.id.home) {
            Intent resultIntent = new Intent();
            setResult(RESULT_OK, resultIntent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    protected void shareItem() {
        ShareHelper share = new ShareHelper(this, mItem.getId());
        share.share();
    }

    protected void showOnMap() {
        if (mItem.getLatitude() == Double.MIN_VALUE && mItem.getLongitude() == Double.MIN_VALUE) {
            Toast toast = Toast.makeText(this, "location unknown", Toast.LENGTH_SHORT);
            toast.show();
        } else {
            Intent mapIntent = new Intent(this, Map.class);
            mapIntent.putExtra(Map.TYPE, Map.MARK_ONE);
            mapIntent.putExtra(Map.ITEM, mItem);
            startActivity(mapIntent);
        }
    }
}
