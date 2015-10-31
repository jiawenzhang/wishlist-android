package com.wish.wishlist.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.parse.ParseFile;
import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.util.UserAdapter;

import java.util.ArrayList;
import java.util.List;

public class FriendsBase extends ActivityBase {

    final static String TAG = "FriendsBase";

    protected RecyclerView mRecyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;
    protected ProgressDialog mProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        setupActionBar(R.id.find_friends_toolbar);

        mRecyclerView = (RecyclerView) findViewById(R.id.user_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        loadView();
    }

    protected void loadView() {}

    protected List<UserAdapter.UserMeta> getUserMetaList(final List<ParseUser> users) {
        ArrayList<UserAdapter.UserMeta> userMetaList = new ArrayList<>();
        for (final ParseUser user : users) {
            final ParseFile parseImage = user.getParseFile("profileImage");
            String imgUrl = null;
            if (parseImage != null) {
                imgUrl = parseImage.getUrl();
            }
            UserAdapter.UserMeta userMeta = new UserAdapter.UserMeta(
                    user.getObjectId(),
                    user.getString("name"),
                    user.getUsername(),
                    imgUrl);
            userMetaList.add(userMeta);
        }
        return userMetaList;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    protected void showProgressDialog(final String text) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(text);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                Log.d(TAG, text + " canceled");
                Toast.makeText(FriendsBase.this, "Check network", Toast.LENGTH_LONG).show();
            }
        });
        mProgressDialog.show();
    }

    protected void handleResult(final String friendId,
                                final boolean success,
                                final String successText,
                                UserAdapter adapter) {
        if (success) {
            adapter.remove(friendId);
            Toast.makeText(this, successText, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Check network", Toast.LENGTH_LONG).show();
        }
        mProgressDialog.dismiss();
    }
}
