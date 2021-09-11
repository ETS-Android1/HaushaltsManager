package com.example.lucas.haushaltsmanager.Database.Repositories.ChildExpenses.Exceptions;

import com.example.lucas.haushaltsmanager.Entities.Booking.Booking;
import com.example.lucas.haushaltsmanager.Entities.Booking.IBooking;

public class AddChildToChildException extends Exception {
    public AddChildToChildException(Booking child, IBooking parent) {
        super(
                String.format("It's not possible to addItem %s to %s, since %s is already a ChildExpense", child.getTitle(), parent.getTitle(), parent.getTitle())
        );
    }
}
