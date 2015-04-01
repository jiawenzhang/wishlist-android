package com.wish.wishlist.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.*;
import android.widget.AbsListView;
import android.widget.AdapterView;
import com.etsy.android.grid.StaggeredGridView;
import com.wish.wishlist.R;
import com.wish.wishlist.WebImageAdapter;
import com.wish.wishlist.activity.EditItem;
import com.wish.wishlist.activity.WebImage;

import java.util.ArrayList;

public class StaggeredGridActivityFragment extends FragmentActivity {

    private static final String TAG = "StaggeredGridActivityFragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("Select an image");

        final FragmentManager fm = getSupportFragmentManager();

        // Create the list fragment and add it as our sole content.
        if (fm.findFragmentById(android.R.id.content) == null) {
            final StaggeredGridFragment fragment = new StaggeredGridFragment();
            Bundle args = new Bundle();
            args.putParcelableArrayList(EditItem.IMG_URLS, getIntent().getParcelableArrayListExtra(EditItem.IMG_URLS));
            fragment.setArguments(args);
            fm.beginTransaction().add(android.R.id.content, fragment).commit();
        }
    }

    private class StaggeredGridFragment extends Fragment implements
            AbsListView.OnScrollListener, AbsListView.OnItemClickListener {

        private StaggeredGridView mGridView;
        private WebImageAdapter mAdapter;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return inflater.inflate(R.layout.web_image_view, container, false);
        }

        @Override
        public void onActivityCreated(final Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            mGridView = (StaggeredGridView) getView().findViewById(R.id.staggered_web_image_view);

            if (savedInstanceState == null) {
                final LayoutInflater layoutInflater = getActivity().getLayoutInflater();
            }

            if (mAdapter == null) {
                ArrayList<WebImage> list = getArguments().getParcelableArrayList(EditItem.IMG_URLS);
                if (list == null) {
                    Log.d("AAA", "list null");
                } else {
                    for (WebImage img : list) {
                        Log.d("AAA", img.mUrl + " " + img.mId + " " + img.mWidth + " " + img.mHeight);
                    }
                }
                mAdapter = new WebImageAdapter(getActivity(), 0, list);
            }
            mGridView.setAdapter(mAdapter);
            mGridView.setOnScrollListener(this);
            mGridView.setOnItemClickListener(this);
        }

        @Override
        public void onScrollStateChanged(final AbsListView view, final int scrollState) {
            //Log.d(TAG, "onScrollStateChanged:" + scrollState);
        }

        @Override
        public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            //Toast.makeText(getActivity(), "Item Clicked: " + position, Toast.LENGTH_SHORT).show();
        }
    }
}
