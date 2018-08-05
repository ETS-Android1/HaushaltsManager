package com.example.lucas.haushaltsmanager;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lucas.haushaltsmanager.Entities.ExpenseObject;

import java.util.ArrayList;
import java.util.List;

public class BookingAdapter extends ArrayAdapter<ExpenseObject> implements View.OnClickListener {

    private static class ViewHolder {
        TextView circleLetter;
        TextView txtTitle;
        TextView txtPrice;
        TextView txtCurrencySymbol;
        TextView txtPerson;
    }

    public BookingAdapter(List<ExpenseObject> data, Context context) {
        super(context, R.layout.booking_item, data);
    }

    @Override
    public void onClick(View v) {

        Toast.makeText(getContext(), "du hast geklickt", Toast.LENGTH_SHORT).show();
    }

    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        ExpenseObject expenseObject = getItem(position);
        ViewHolder viewHolder;

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.booking_item, parent, false);


            viewHolder.circleLetter = (TextView) convertView.findViewById(R.id.exp_listview_group_rounded_textview);
            viewHolder.txtTitle = (TextView) convertView.findViewById(R.id.exp_listview_group_title);
            viewHolder.txtPrice = (TextView) convertView.findViewById(R.id.exp_listview_group_price);
            viewHolder.txtCurrencySymbol = (TextView) convertView.findViewById(R.id.exp_listview_group_currency_symbol);
            viewHolder.txtPerson = (TextView) convertView.findViewById(R.id.exp_listview_group_person);

            convertView.setTag(viewHolder);
        } else {

            viewHolder = (ViewHolder) convertView.getTag();
        }

        String category = expenseObject.getCategory().getTitle();

        viewHolder.circleLetter.setText(category.substring(0, 1).toUpperCase());
        viewHolder.txtTitle.setText(expenseObject.getTitle());
        //TODO wenn es eine Multiuser funktionalität muss hier der benutzer eingetragen werden, der das Geld ausgegeben hat
        viewHolder.txtPerson.setText("");
        viewHolder.txtPrice.setText(String.format("%s", expenseObject.getUnsignedPrice()));
        viewHolder.txtCurrencySymbol.setText(expenseObject.getCurrency().getSymbol());

        return convertView;
    }
}
