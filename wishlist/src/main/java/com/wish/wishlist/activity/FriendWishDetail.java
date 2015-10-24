package com.wish.wishlist.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.squareup.picasso.Picasso;

public class FriendWishDetail extends WishDetail {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
        mItem = i.getParcelableExtra(FriendsWish.ITEM);
        showItemInfo(mItem);

//        final View imageFrame = findViewById(R.id.imagePhotoDetailFrame);
//        imageFrame.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent i = new Intent(FriendWishDetail.this, FullscreenPhoto.class);
//                if (_fullsize_picture_str != null) {
//                    i.putExtra(EditItem.FULLSIZE_PHOTO_PATH, _fullsize_picture_str);
//                    startActivity(i);
//                }
//            }
//        });
    }

    @Override
    protected void showPhoto() {
        if (mItem.getPicURL() != null) {
            // we have the photo somewhere on the internet
            mPhotoView.setVisibility(View.VISIBLE);
            Picasso.with(this).load(mItem.getPicURL()).fit().centerCrop().into(mPhotoView);
        } else if (mItem.getPicParseURL() !=null ) {
            // we have the photo on Parse
            mPhotoView.setVisibility(View.VISIBLE);
            Picasso.with(this).load(mItem.getPicParseURL()).fit().centerCrop().into(mPhotoView);
        } else {
            mPhotoView.setVisibility(View.GONE);
        }
    }
}
