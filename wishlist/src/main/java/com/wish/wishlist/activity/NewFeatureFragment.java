package com.wish.wishlist.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wish.wishlist.R;

public final class NewFeatureFragment extends Fragment {
    private static final String KEY_CONTENT = "NewFeatureFragment:Position";
    private int mPosition;

    public static NewFeatureFragment newInstance(int position) {
        NewFeatureFragment fragment = new NewFeatureFragment();
        fragment.mPosition = position;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ((savedInstanceState != null) && savedInstanceState.containsKey(KEY_CONTENT)) {
            mPosition = savedInstanceState.getInt(KEY_CONTENT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mPosition == 0) {
            View v = inflater.inflate(R.layout.new_feature_start, container, false);

            final Button button = (Button) v.findViewById(R.id.newFeatureStartButton);
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    startActivity(new Intent(getActivity(), WishList.class));
                    getActivity().finish();
                }
            });

            return v;
        } else {
            View v = inflater.inflate(R.layout.new_feature, container, false);
            View tv = v.findViewById(R.id.newFeatureText);
            View tv_bottom = v.findViewById(R.id.newFeatureTextBottom);
            View imageView = v.findViewById(R.id.newFeatureImage);

            switch (mPosition) {
                case 1:
                    ((ImageView) imageView).setImageResource(R.drawable.etsy_bag);
                    ((TextView) tv).setText("Browse items in various apps and tap the share button");
                    break;
                case 2:
                    ((ImageView) imageView).setImageResource(R.drawable.etsy_share);
                    ((TextView) tv).setText("Choose Wishlist, and the items are saved directly to your wishlist");
                    ((TextView) tv_bottom).setText("Supported apps include amazon, eBay, Etsy, Fancy, Pinterest, Wish, Wanelo and much more");
                    break;
                case 3:
                    ImageView image_view = (ImageView) (imageView);
                    image_view.setImageResource(R.drawable.chrome_share);
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) image_view.getLayoutParams();
                    //params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, /*bottomMargin*/ 30);
                    image_view.setLayoutParams(params);

                    tv_bottom.setVisibility(View.GONE);

                    ((TextView) tv).setText("You can also save items to Wishlist directly from web browser");
                    final Button button = (Button) v.findViewById(R.id.newFeatureButton);
                    button.setVisibility(View.VISIBLE);
                    button.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            startActivity(new Intent(getActivity(), WishList.class));
                            getActivity().finish();
                        }
                    });
                    break;
            }
            return v;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CONTENT, mPosition);
    }
}