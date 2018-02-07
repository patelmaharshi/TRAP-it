package com.vrtrappers.trapit;

import android.content.Context;
import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

class BookMarkListAdapter extends BaseAdapter {
    void clearAllData() {
        item_modelArrayList.clear(); //clear list
        notifyDataSetChanged(); //let your adapter know about the changes and reload view.
    }

    private Context context;
    private ArrayList<String[]> item_modelArrayList;
    private SparseBooleanArray mSelectedItemsIds;

    BookMarkListAdapter(Context context, ArrayList<String[]> item_modelArrayList) {
        this.context = context;
        this.item_modelArrayList = item_modelArrayList;
        mSelectedItemsIds = new SparseBooleanArray();
    }

    @Override
    public int getCount() {
        return item_modelArrayList.size();
    }

    @Override
    public String[] getItem(int position) {
        return item_modelArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.bookmark_list_row, parent, false);
            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.title_text);
            holder.sub_title = (TextView) convertView.findViewById(R.id.snippet_text);

            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        holder.title.setText(item_modelArrayList.get(position)[0]);
        holder.sub_title.setText(item_modelArrayList.get(position)[1]);


        /** Change background color of the selected items in list view  **/

        convertView
                .setBackgroundColor(mSelectedItemsIds.get(position) ? 0x9934B5E4
                        : Color.TRANSPARENT);
        return convertView;
    }

    private class ViewHolder {
        TextView title, sub_title;
    }


    /***
     * Methods required for do selections, remove selections, etc.
     */

    //Toggle selection methods
    public void toggleSelection(int position) {
        selectView(position, !mSelectedItemsIds.get(position));
    }


    //Remove selected selections
    public void removeSelection() {
        mSelectedItemsIds = new SparseBooleanArray();
        notifyDataSetChanged();
    }


    //Put or delete selected position into SparseBooleanArray
    public void selectView(int position, boolean value) {
        if (value)
            mSelectedItemsIds.put(position, value);
        else
            mSelectedItemsIds.delete(position);

        notifyDataSetChanged();
    }

    //Get total selected count
    int getSelectedCount() {
        return mSelectedItemsIds.size();
    }

    //Return all selected ids
    SparseBooleanArray getSelectedIds() {
        return mSelectedItemsIds;
    }
}
