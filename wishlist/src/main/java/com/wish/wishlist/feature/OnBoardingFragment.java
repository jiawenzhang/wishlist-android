package com.wish.wishlist.feature;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.login.UserLoginActivity;
import com.wish.wishlist.util.Options;
import com.wish.wishlist.util.Util;
import com.wish.wishlist.wish.MyWishActivity;

public final class OnBoardingFragment extends Fragment {
    private static final String KEY_CONTENT = "OnBoardingFragment:Position";
    private int mPosition;
    private TextView mTextView1;
    private Button mButton;

    public static OnBoardingFragment newInstance(int position) {
        OnBoardingFragment fragment = new OnBoardingFragment();
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
        View v = inflater.inflate(R.layout.onboarding, container, false);
        mTextView1 = (TextView) v.findViewById(R.id.onBoardingText);
        mButton = (Button) v.findViewById(R.id.onBoardingButton);

        switch (mPosition) {
            case 0: {
                showFirstView();
                break;
            } case 1: {
                showShareView();
                break;
            } case 2: {
                if (Util.deviceAccountEnabled()) {
                    showSyncView();
                } else {
                    showCompleteView();
                }
                break;
            } case 3: {
                showCompleteView();
                break;
            }
        }
        return v;
    }

    private void showFirstView() {
        String text = "<br>THE<br>" +
                "<font color=#f2ca17><b>ONE PLACE</b><br></font>" +
                "FOR ALL<br>" +
                "YOUR<br>" +
                "WISHES";
        mTextView1.setText(Html.fromHtml(text));
        mButton.setVisibility(View.INVISIBLE);
    }

    private void showShareView() {
        String text = "<br>SHARE<br>" +
                "<font color=#f2ca17><b>WISH</b><br></font>" +
                "FROM<br>" +
                "OTHER APP<br>" +
                "OR WEB";
        mTextView1.setText(Html.fromHtml(text));
        mButton.setVisibility(View.INVISIBLE);
    }

    private void showSyncView() {
        String text = "<br><font color=#f2ca17><b>SYNC</b></font><br>" +
                "WISH<br>" +
                "ACROSS<br>" +
                "ALL<br>" +
                "DEVICES";
        mTextView1.setText(Html.fromHtml(text));
        mButton.setVisibility(View.INVISIBLE);
    }

    private void showCompleteView() {
        String text = "<br>MARK<br>" +
                "WISH<br>" +
                "<font color=#f2ca17><b>COMPLETE<br></b></font>" +
                "WHEN<br>" +
                "DONE";
        mTextView1.setText(Html.fromHtml(text));
        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // make sure on boarding is not shown next time user starts the app
                Options.ShowOnBoarding showOnBoarding = new Options.ShowOnBoarding(0);
                showOnBoarding.save();

                if (Util.deviceAccountEnabled()) {
                    Intent intent = new Intent(getActivity(), UserLoginActivity.class);
                    intent.putExtra(UserLoginActivity.FROM_SPLASH, true);
                    intent.putExtra(UserLoginActivity.ALLOW_SKIP, true);
                    startActivity(intent);
                } else {
                    startActivity(new Intent(getActivity(), MyWishActivity.class));
                }

                getActivity().finish();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CONTENT, mPosition);
    }
}