package com.example.lucas.haushaltsmanager.RecyclerView.ViewHolder;

import android.view.View;
import android.widget.TextView;

import com.example.lucas.haushaltsmanager.App.app;
import com.example.lucas.haushaltsmanager.Entities.Category;
import com.example.lucas.haushaltsmanager.Entities.Currency;
import com.example.lucas.haushaltsmanager.Entities.Expense.ExpenseObject;
import com.example.lucas.haushaltsmanager.R;
import com.example.lucas.haushaltsmanager.RecyclerView.RecyclerViewItems.ChildItem;
import com.example.lucas.haushaltsmanager.RecyclerView.RecyclerViewItems.IRecyclerItem;
import com.example.lucas.haushaltsmanager.Utils.MoneyUtils;
import com.example.lucas.haushaltsmanager.Utils.ViewUtils;
import com.example.lucas.haushaltsmanager.Views.RoundedTextView;

import androidx.annotation.ColorRes;

public class ChildViewHolder extends AbstractViewHolder {
    private static final String TAG = ChildViewHolder.class.getSimpleName();

    private RoundedTextView mRoundedTextView;
    private TextView mTitle;
    private TextView mPrice;
    private TextView mCurrency;
    private TextView mPerson;

    public ChildViewHolder(View itemView) {
        super(itemView);

        mRoundedTextView = itemView.findViewById(R.id.recycler_view_child_rounded_textview);
        mTitle = itemView.findViewById(R.id.recycler_view_child_title);
        mPrice = itemView.findViewById(R.id.recycler_view_child_price);
        mCurrency = itemView.findViewById(R.id.recycler_view_child_currency);
        mPerson = itemView.findViewById(R.id.recycler_view_child_person);
    }

    @Override
    public void bind(IRecyclerItem item) {
        if (!(item instanceof ChildItem)) {
            throw new IllegalArgumentException(String.format("Wrong type given in %s", TAG));
        }

        ExpenseObject expense = (ExpenseObject) item.getContent();

        setRoundedTextViewText(expense.getCategory());
        setTitle(expense.getTitle());
        setPrice(expense.getUnsignedPrice(), expense.isExpenditure());
        setCurrency(expense.getCurrency(), expense.isExpenditure());
        setPerson("");

        setBackgroundColor();
    }

    private void setBackgroundColor() {
        if (itemView.isSelected())
            itemView.setBackgroundColor(getColor(R.color.list_item_highlighted));
        else
            itemView.setBackgroundColor(getColor(R.color.list_item_background));
    }

    private void setRoundedTextViewText(Category category) {
        if (ViewUtils.getColorBrightness(category.getColorString()) > 0.5) {
            mRoundedTextView.setTextColor(getColor(R.color.primary_text_color_dark));
        } else {
            mRoundedTextView.setTextColor(getColor(R.color.primary_text_color_bright));
        }
        mRoundedTextView.setCenterText(category.getTitle().charAt(0) + "");
        mRoundedTextView.setCircleColor(category.getColorInt());
    }

    private void setTitle(String title) {
        mTitle.setText(title);
    }

    private void setPrice(double price, boolean isExpenditure) {
        mPrice.setText(MoneyUtils.toHumanReadablePrice(price));

        if (isExpenditure)
            mPrice.setTextColor(getColor(R.color.booking_expense));
        else
            mPrice.setTextColor(getColor(R.color.booking_income));
    }

    private void setCurrency(Currency currency, boolean isExpenditure) {
        mCurrency.setText(currency.getSymbol());

        if (isExpenditure)
            mCurrency.setTextColor(getColor(R.color.booking_expense));
        else
            mCurrency.setTextColor(getColor(R.color.booking_income));
    }

    private void setPerson(String person) {
        mPerson.setText(person);
    }

    private int getColor(@ColorRes int colorRes) {
        return app.getContext().getResources().getColor(colorRes);
    }
}
