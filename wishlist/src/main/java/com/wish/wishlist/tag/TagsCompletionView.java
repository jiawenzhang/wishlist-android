package com.wish.wishlist.tag;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tokenautocomplete.TokenCompleteTextView;
import com.wish.wishlist.R;

public class TagsCompletionView extends TokenCompleteTextView {

    public TagsCompletionView(Context context) {
        super(context);
    }

    public TagsCompletionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TagsCompletionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected View getViewForObject(Object object) {
        String tag = (String)object;

        LayoutInflater l = (LayoutInflater)getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        LinearLayout view = (LinearLayout)l.inflate(R.layout.tag_token, (ViewGroup) TagsCompletionView.this.getParent(), false);
        ((TextView)view.findViewById(R.id.name)).setText(tag);

        return view;
    }

    @Override
    protected Object defaultObject(String completionText) {
        return completionText;
    }
}
