package com.wish.wishlist.wish;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wish.wishlist.R;
import com.wish.wishlist.activity.ActivityBase;
import com.wish.wishlist.activity.MapActivity;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.util.DateTimeFormatter;
import com.wish.wishlist.social.ShareHelper;
import com.wish.wishlist.widgets.ClearableEditText;


public abstract class WishDetailActivity extends ActivityBase {
    public final static String ITEM = "Item";

    protected ImageView mPhotoView;
    protected ImageView mImgComplete;
    protected LinearLayout mCompleteInnerLayout;
    protected TextView mTextComplete;
    protected ClearableEditText mNameView;
    protected ClearableEditText mDescriptionView;
    private TextView mDateView;
    protected ClearableEditText mPriceView;
    protected ClearableEditText mStoreView;
    protected ClearableEditText mLocationView;
    protected TextView mLinkView;
    protected LinearLayout mLinkLayout;
    protected WishItem mItem;
    protected int mComplete = -1;

    protected abstract boolean myWish();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wishitem_detail);
        setupActionBar(R.id.item_detail_toolbar);

        mPhotoView = (ImageView) findViewById(R.id.imgPhotoDetail);
        mCompleteInnerLayout = (LinearLayout) findViewById(R.id.completeInnerLayout);
        mImgComplete = (ImageView) findViewById(R.id.completeImageView);
        mTextComplete = (TextView) findViewById(R.id.completeTextView);
        mNameView = (ClearableEditText) findViewById(R.id.itemNameDetail);
        mDescriptionView = (ClearableEditText) findViewById(R.id.itemDesription);
        mDateView = (TextView) findViewById(R.id.itemDateDetail);
        mPriceView = (ClearableEditText) findViewById(R.id.itemPrice);
        mStoreView = (ClearableEditText) findViewById(R.id.itemStore);
        mLocationView = (ClearableEditText) findViewById(R.id.itemLocation);
        mLinkView = (TextView) findViewById(R.id.itemLink);
        mLinkLayout = (LinearLayout) findViewById(R.id.linkLayout);

        Intent i = getIntent();
        mItem = i.getParcelableExtra(ITEM);
    }

    protected void showPhoto() {}

    protected void showItemInfo() {
        showPhoto();

        if (mItem.getComplete() == 1) {
            mCompleteInnerLayout.setVisibility(View.VISIBLE);
        } else {
            mCompleteInnerLayout.setVisibility(View.GONE);
        }

        String dateTimeStr = mItem.getUpdatedTimeStr();
        String dateTimeStrNew = DateTimeFormatter.getInstance().getDateTimeString(dateTimeStr);

        mNameView.setText(mItem.getName());
        if (mItem.getComplete() == 1) {
            final ImageView completeImage = (ImageView) findViewById(R.id.completeImageView);
            completeImage.setVisibility(View.VISIBLE);
        }

        mDateView.setText(dateTimeStrNew);

        // format the price
        String priceStr = mItem.getPriceAsString();
        if (priceStr != null) {
            mPriceView.setText(WishItem.priceStringWithCurrency(priceStr));
            mPriceView.setVisibility(View.VISIBLE);
        } else {
            mPriceView.setVisibility(View.GONE);
        }

        //used as a note
        String description = mItem.getDesc();
        if (!description.isEmpty()) {
            mDescriptionView.setText(description);
            mDescriptionView.setVisibility(View.VISIBLE);
        } else {
            mDescriptionView.setVisibility(View.GONE);
        }

        String storeName = mItem.getStoreName();
        if (!storeName.isEmpty()) {
            mStoreView.setText(storeName);
            mStoreView.setVisibility(View.VISIBLE);
        } else {
            mStoreView.setVisibility(View.GONE);
        }

        String address = mItem.getAddress();
        if (!address.equals("unknown") && !address.isEmpty()) {
            mLocationView.setText(address);
            mLocationView.setVisibility(View.VISIBLE);
        } else {
            mLocationView.setVisibility(View.GONE);
        }

        String link = mItem.getLink();
        if (link != null && !link.isEmpty()) {
            String url = mItem.getLink();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            String text = "<a href=\"" + url + "\">LINK</a>";
            mLinkView.setText(Html.fromHtml(text));
            mLinkView.setMovementMethod(LinkMovementMethod.getInstance());
            mLinkView.setVisibility(View.VISIBLE);
            mLinkLayout.setVisibility(View.VISIBLE);
        } else {
            mLinkView.setVisibility(View.GONE);
            mLinkLayout.setVisibility(View.GONE);
        }
    }

    protected void inflateMenu(int resId, Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(resId, menu);

        MenuItem item = menu.findItem(R.id.location);
        if (!mItem.hasGeoLocation()) {
            item.setVisible(false);
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
        long[] itemIds = new long[1];
        itemIds[0] = mItem.getId();
        ShareHelper share = new ShareHelper(this, itemIds);
        share.share();
    }

    protected void showOnMap() {
        if (mItem.getLatitude() == Double.MIN_VALUE && mItem.getLongitude() == Double.MIN_VALUE) {
            Toast toast = Toast.makeText(this, "location unknown", Toast.LENGTH_SHORT);
            toast.show();
        } else {
            Intent mapIntent = new Intent(this, MapActivity.class);
            mapIntent.putExtra(MapActivity.TYPE, MapActivity.MARK_ONE);
            mapIntent.putExtra(MapActivity.ITEM, mItem);
            mapIntent.putExtra(MapActivity.MY_WISH, myWish());
            startActivity(mapIntent);
        }
    }
}
