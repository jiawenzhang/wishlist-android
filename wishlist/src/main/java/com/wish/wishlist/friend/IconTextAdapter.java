package com.wish.wishlist.friend;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wish.wishlist.R;

import java.util.List;

/**
 * Created by jiawen on 2016-01-11.
 */

public class IconTextAdapter extends ArrayAdapter<IconTextAdapter.Entry> {

    public static class Entry {
        public Entry(String text, int res) {
            this.text = text;
            this.imgRes = res;
        }
        String text;
        int imgRes;
    }

    private final List<Entry> list;
    private final Activity context;

    static class ViewHolder {
        TextView txtView;
        ImageView imgView;
    }

    public IconTextAdapter(Activity context, List<Entry> list) {
        super(context, R.layout.icon_text, list);
        this.context = context;
        this.list = list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            LayoutInflater inflator = context.getLayoutInflater();
            view = inflator.inflate(R.layout.icon_text, null);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.txtView = (TextView) view.findViewById(R.id.text);
            viewHolder.imgView = (ImageView) view.findViewById(R.id.image);
            view.setTag(viewHolder);
        } else {
            view = convertView;
        }

        ViewHolder holder = (ViewHolder) view.getTag();
        holder.txtView.setText(list.get(position).text);
        holder.imgView.setImageResource(list.get(position).imgRes);
        return view;
    }
}
