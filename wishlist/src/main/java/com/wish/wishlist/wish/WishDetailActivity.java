package com.wish.wishlist.wish;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.wish.wishlist.R;
import com.wish.wishlist.activity.ActivityBase;
import com.wish.wishlist.activity.MapActivity;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.util.DateTimeFormatter;
import com.wish.wishlist.social.ShareHelper;

public abstract class WishDetailActivity extends ActivityBase {

    public final static String ITEM = "Item";

    protected ImageView mPhotoView;
    private TextView mNameView;
    private TextView mDescrpView;
    private TextView mDateView;
    private TextView mPriceView;
    private TextView mStoreView;
    private TextView mLocationView;
    private TextView mLinkView;
    protected WishItem mItem;

    protected abstract boolean myWish();

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
        } else {
            mPriceView.setVisibility(View.GONE);
        }

        //used as a note
        String descrptStr = mItem.getDesc();
        if (!descrptStr.equals("")) {
            mDescrpView.setText(descrptStr);
            mDescrpView.setVisibility(View.VISIBLE);
        } else {
            mDescrpView.setVisibility(View.GONE);
        }

        String storeName = mItem.getStoreName();
        if (!storeName.equals("")) {
            mStoreView.setText(storeName);
            mStoreView.setVisibility(View.VISIBLE);
        } else {
            mStoreView.setVisibility(View.GONE);
        }

        String address = mItem.getAddress();
        if (!address.equals("unknown") && !address.equals("")) {
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
