package com.example.lucas.haushaltsmanager;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.example.lucas.haushaltsmanager.Entities.Category;
import com.example.lucas.haushaltsmanager.Views.RoundedTextView;

import java.util.List;

public class CategoryAdapter extends BaseExpandableListAdapter {

    private List<Category> mCategoryData;
    private Context mContext;

    public CategoryAdapter(List<Category> categoryData, Context context) {

        mCategoryData = categoryData;
        mContext = context;
    }

    @Override
    public int getGroupCount() {
        return mCategoryData.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mCategoryData.get(groupPosition).getChildren().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mCategoryData.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mCategoryData.get(groupPosition).getChildren().get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    class ViewHolder {
        RoundedTextView roundedTextView;
        TextView txtCategoryName;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        Category groupCategory = (Category) getGroup(groupPosition);
        ViewHolder groupViewHolder;

        if (convertView == null) {

            groupViewHolder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.category_group_item, null);

            groupViewHolder.roundedTextView = (RoundedTextView) convertView.findViewById(R.id.category_itemn_rounded_text_view);
            groupViewHolder.txtCategoryName = (TextView) convertView.findViewById(R.id.category_item_name);

            convertView.setTag(groupViewHolder);
        } else {

            groupViewHolder = (ViewHolder) convertView.getTag();
        }

        String categoryName = groupCategory.getTitle();

        groupViewHolder.roundedTextView.setTextColor(Color.WHITE);// todo variabel machen
        groupViewHolder.roundedTextView.setCenterText(categoryName.substring(0, 1).toUpperCase());
        groupViewHolder.roundedTextView.setCircleColor(groupCategory.getColorString());
        groupViewHolder.roundedTextView.setCircleDiameter(33);
        groupViewHolder.txtCategoryName.setText(categoryName);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        Category childCategory = (Category) getChild(groupPosition, childPosition);
        ViewHolder childViewHolder;

        if (convertView == null) {

            childViewHolder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.category_child_item, null);

            childViewHolder.roundedTextView = (RoundedTextView) convertView.findViewById(R.id.category_itemn_rounded_text_view);
            childViewHolder.txtCategoryName = (TextView) convertView.findViewById(R.id.category_item_name);

            convertView.setTag(childViewHolder);
        } else {

            childViewHolder = (ViewHolder) convertView.getTag();
        }

        String categoryName = childCategory.getTitle();

        childViewHolder.roundedTextView.setTextColor(Color.WHITE);// todo variabel machen
        childViewHolder.roundedTextView.setCenterText(categoryName.substring(0, 1).toUpperCase());
        childViewHolder.roundedTextView.setCircleColor(childCategory.getColorString());
        childViewHolder.roundedTextView.setCircleDiameter(33);
        childViewHolder.txtCategoryName.setText(categoryName);

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}