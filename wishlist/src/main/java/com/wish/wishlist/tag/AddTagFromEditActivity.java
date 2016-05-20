package com.wish.wishlist.tag;

/**
 * Created by jiawen on 2014-11-02.
 */

import android.content.Intent;
import java.util.ArrayList;
import java.util.Collections;

public class AddTagFromEditActivity extends AddTagActivity {
    public final static String TAGS = "tags";

    @Override
    protected void addTags() {
        ArrayList<String> tags = getIntent().getStringArrayListExtra(TAGS);
        if (tags != null) {
            for (String tag : tags) {
                completionView.addObject(tag);
                mCurrentTags.add(tag);
            }
        }
    }

    protected void onSave() {
        //Get the text after the last token in the view. This text has not been tokenized, but it should be regarded as a tag
        String lastTag = completionView.getText().toString().replaceFirst(PREFIX, "").replace(",", "").trim();
        if (!lastTag.isEmpty()) {
            mCurrentTags.add(lastTag);
        }
        ArrayList<String> tags = new ArrayList<String>();
        tags.addAll(mCurrentTags);
        Collections.sort(tags);

        //send the tags back to the EditItemInfo activity and close this activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra(TAGS, tags);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
