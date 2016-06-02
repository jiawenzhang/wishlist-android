package com.wish.wishlist.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/* A custom EditText that allows multiple lines while overwriting the behavior of "Enter" button to go to next action instead
  of having a line break

  When used, set the following in xml
  android:imeOptions="actionNext"
  android:inputType="textMultiLine"

  this is based on the following post
  http://stackoverflow.com/questions/5014219/multiline-edittext-with-done-softinput-action-label-on-2-3
*/
public class ActionNextClearableEditText extends ClearableEditText {
    public ActionNextClearableEditText(Context context) {
        super(context);
    }

    public ActionNextClearableEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ActionNextClearableEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection connection = super.onCreateInputConnection(outAttrs);
        int imeActions = outAttrs.imeOptions&EditorInfo.IME_MASK_ACTION;
        if ((imeActions&EditorInfo.IME_ACTION_NEXT) != 0) {
            // clear the existing action
            outAttrs.imeOptions ^= imeActions;
            // set the DONE action
            outAttrs.imeOptions |= EditorInfo.IME_ACTION_NEXT;
        }
        if ((outAttrs.imeOptions&EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        }
        return connection;
    }
}

