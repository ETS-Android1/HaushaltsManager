package com.example.lucas.haushaltsmanager.ExpenseImporter.SavingService;

import com.example.lucas.haushaltsmanager.Entities.Account;
import com.example.lucas.haushaltsmanager.Entities.Booking.Booking;

public interface ISaver {
    void revert();

    void finish();

    void persist(Booking booking, Account account);
}
