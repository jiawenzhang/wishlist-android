package com.wish.wishlist.tag;

/**
 * Created by jiawen on 2014-11-02.
 */

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.TextView;

import com.wish.wishlist.R;
import com.wish.wishlist.activity.ActivityBase;
import com.wish.wishlist.db.TagDBManager;

public class FindTagActivity extends ActivityBase {
    TagListAdapter tagsAdapter = null;
    protected final static String ITEM_ID = "item_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.find_tag);
        setupActionBar(R.id.find_tag_toolbar);

        showTags();
    }

    private void showTags() {
        ArrayList<String> tagList;
        tagList = TagDBManager.instance().getAllTags();

        tagsAdapter = new TagListAdapter(this, R.layout.tag_list, tagList);
        ListView listView = (ListView) findViewById(R.id.taglist);
        // Assign adapter to ListView
        listView.setAdapter(tagsAdapter);

        //enables filtering for the contents of the given ListView
        listView.setTextFilterEnabled(true);
        setTagClick(listView);

        EditText tagFilter = (EditText) findViewById(R.id.tagFilter);
        tagFilter.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tagsAdapter.getFilter().filter(s.toString());
            }
        });
    }

    protected void setTagClick(ListView listView) {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String tag = (String) parent.getItemAtPosition(position);

                //close this activity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("tag", tag);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    private class TagListAdapter extends ArrayAdapter<String> {
        private ArrayList<String> originalList;
        private ArrayList<String> tagList;
        private TagFilter filter;

        public TagListAdapter(Context context, int textViewResourceId, ArrayList<String> tagList) {
            super(context, textViewResourceId, tagList);
            this.tagList = new ArrayList<>();
            this.tagList.addAll(tagList);
            this.originalList = new ArrayList<>();
            this.originalList.addAll(tagList);
        }

        @Override
        public Filter getFilter() {
            if (filter == null) {
                filter = new TagFilter();
            }
            return filter;
        }

        private class ViewHolder {
            TextView tag;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = vi.inflate(R.layout.tag_list, null);
                holder = new ViewHolder();
                holder.tag = (TextView) convertView.findViewById(R.id.tagName);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            String tag = tagList.get(position);
            holder.tag.setText(tag);
            return convertView;
        }

        private class TagFilter extends Filter
        {
            @Override
            protected FilterResults performFiltering (CharSequence constraint){

                constraint = constraint.toString().toLowerCase();
                FilterResults result = new FilterResults();
                if (constraint != null && constraint.toString().length() > 0) {
                    ArrayList<String> filteredItems = new ArrayList<String>();

                    for (String tag : originalList) {
                        if (tag.toLowerCase().contains(constraint)) {
                            filteredItems.add(tag);
                        }
                    }
                    result.count = filteredItems.size();
                    result.values = filteredItems;
                }
                else {
                    synchronized (this)
                    {
                        result.values = originalList;
                        result.count = originalList.size();
                    }
                }
                return result;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults (CharSequence constraint, FilterResults results){
                tagList = (ArrayList<String>) results.values;
                notifyDataSetChanged();
                clear();
                for (String tag : tagList) {
                    add(tag);
                }
                notifyDataSetInvalidated();
            }
        }
    }
}
