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
            return v;
        } else {
            View v = inflater.inflate(R.layout.new_feature, container, false);
            View tv = v.findViewById(R.id.newFeatureText);
            View imageView = v.findViewById(R.id.newFeatureImage);

            switch (mPosition) {
                case 1:
                    ((ImageView) imageView).setImageResource(R.drawable.tap_share);
                    ((TextView) tv).setText("Tap the share button");
                    break;
                case 2:
                    ((ImageView) imageView).setImageResource(R.drawable.share_dialog_tapped);
                    ((TextView) tv).setText("Choose share to Facebook");
                    break;
                case 3:
                    ImageView image_view = (ImageView) (imageView);
                    image_view.setImageResource(R.drawable.facebook_post);
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) image_view.getLayoutParams();
                    params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, 60);
                    image_view.setLayoutParams(params);

                    ((TextView) tv).setText("Your friends can like, comment or share your wish");
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