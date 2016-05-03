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
import com.wish.wishlist.util.Options;
import com.wish.wishlist.wish.MyWishActivity;

public final class OnBoardingFragment extends Fragment {
    private static final String KEY_CONTENT = "OnBoardingFragment:Position";
    private int mPosition;

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
        TextView textView1 = (TextView) v.findViewById(R.id.onBoardingText);
        Button button = (Button) v.findViewById(R.id.onBoardingButton);

        switch (mPosition) {
            case 0: {
                String text = "<br>THE<br>" +
                        "<font color=#f2ca17><b>ONE PLACE</b><br></font>" +
                        "FOR ALL<br>" +
                        "YOUR<br>" +
                        "WISHES";
                textView1.setText(Html.fromHtml(text));
                button.setVisibility(View.INVISIBLE);
                break;
            } case 1: {
                String text = "<br>SHARE<br>" +
                        "<font color=#f2ca17><b>WISH</b><br></font>" +
                        "FROM<br>" +
                        "OTHER APP<br>" +
                        "OR WEB";
                textView1.setText(Html.fromHtml(text));
                button.setVisibility(View.INVISIBLE);
                break;
            }
            case 2: {
                String text = "<br>MARK<br>" +
                        "WISH<br>" +
                        "<font color=#f2ca17><b>COMPLETE<br></b></font>" +
                        "WHEN<br>" +
                        "DONE";
                textView1.setText(Html.fromHtml(text));
                button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        // make sure on boarding is not shown next time user starts the app
                        Options.ShowOnBoarding showOnBoarding = new Options.ShowOnBoarding(0);
                        showOnBoarding.save();

                        startActivity(new Intent(getActivity(), MyWishActivity.class));
                        getActivity().finish();
                    }
                });
                break;
            }
        }
        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CONTENT, mPosition);
    }
}