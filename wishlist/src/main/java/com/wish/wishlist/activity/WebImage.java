package com.wish.wishlist.activity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by jiawen on 15-03-30.
 */
public class WebImage implements Parcelable {
    public String mUrl;
    public int mWidth;
    public int mHeight;
    public String mId;

    public WebImage(Parcel in) {
        mUrl = in.readString();
        mWidth = in.readInt();
        mHeight = in.readInt();
        mId = in.readString();
    }
    public WebImage(String url, int width, int height, String id) {
        mUrl = url;
        mWidth = width;
        mHeight = height;
        mId = id;
    }
    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mUrl);
        dest.writeInt(mWidth);
        dest.writeInt(mHeight);
        dest.writeString(mId);
    }
    public static final Parcelable.Creator CREATOR =
            new Parcelable.Creator() {
                public WebImage createFromParcel(Parcel in) {
                    return new WebImage(in);
                }
                public WebImage[] newArray(int size) {
                    return new WebImage[size];
                }
            };
}
