package com.example.lucas.haushaltsmanager.Database.Repositories.ChildExpenses;

import android.database.Cursor;

import com.example.lucas.haushaltsmanager.Database.TransformerInterface;
import com.example.lucas.haushaltsmanager.entities.Category;
import com.example.lucas.haushaltsmanager.entities.Booking.Booking;
import com.example.lucas.haushaltsmanager.entities.Price;

import java.util.Calendar;
import java.util.UUID;

public class ChildExpenseTransformer implements TransformerInterface<Booking> {
    private final TransformerInterface<Category> categoryTransformer;

    public ChildExpenseTransformer(
            TransformerInterface<Category> categoryTransformer
    ) {
        this.categoryTransformer = categoryTransformer;
    }

    @Override
    public Booking transform(Cursor c) {
        UUID id = getId(c);
        String title = c.getString(c.getColumnIndex("title"));
        String notice = c.getString(c.getColumnIndex("notice"));
        UUID accountId = getAccountId(c);
        Category expenseCategory = categoryTransformer.transform(c);
        Price price = extractPrice(c);
        Calendar date = extractDate(c);

        if (c.isLast()) {
            c.close();
        }

        return new Booking(
                id,
                title,
                price,
                date,
                expenseCategory,
                notice,
                accountId,
                Booking.EXPENSE_TYPES.CHILD_EXPENSE
        );
    }

    private UUID getId(Cursor c) {
        String rawId = c.getString(c.getColumnIndex("id"));

        return UUID.fromString(rawId);
    }

    private UUID getAccountId(Cursor c) {
        String rawAccountId = c.getString(c.getColumnIndex("account_id"));

        return UUID.fromString(rawAccountId);
    }

    private Calendar extractDate(Cursor c) {
        Calendar date = Calendar.getInstance();
        String dateString = c.getString(c.getColumnIndex("date"));
        date.setTimeInMillis(Long.parseLong(dateString));

        return date;
    }

    private Price extractPrice(Cursor c) {
        double price = c.getDouble(c.getColumnIndex("price"));
        boolean expenditure = c.getInt(c.getColumnIndex("expenditure")) == 1;

        return new Price(price, expenditure);
    }
}
