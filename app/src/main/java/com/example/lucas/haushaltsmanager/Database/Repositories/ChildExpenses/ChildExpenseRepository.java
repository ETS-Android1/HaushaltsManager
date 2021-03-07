package com.example.lucas.haushaltsmanager.Database.Repositories.ChildExpenses;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.lucas.haushaltsmanager.Database.DatabaseManager;
import com.example.lucas.haushaltsmanager.Database.ExpensesDbHelper;
import com.example.lucas.haushaltsmanager.Database.QueryInterface;
import com.example.lucas.haushaltsmanager.Database.Repositories.Accounts.AccountRepository;
import com.example.lucas.haushaltsmanager.Database.Repositories.Accounts.Exceptions.AccountNotFoundException;
import com.example.lucas.haushaltsmanager.Database.Repositories.Bookings.BookingTransformer;
import com.example.lucas.haushaltsmanager.Database.Repositories.Bookings.Exceptions.CannotDeleteExpenseException;
import com.example.lucas.haushaltsmanager.Database.Repositories.Bookings.Exceptions.ExpenseNotFoundException;
import com.example.lucas.haushaltsmanager.Database.Repositories.Bookings.ExpenseRepository;
import com.example.lucas.haushaltsmanager.Database.Repositories.ChildCategories.ChildCategoryTransformer;
import com.example.lucas.haushaltsmanager.Database.Repositories.ChildExpenses.Exceptions.AddChildToChildException;
import com.example.lucas.haushaltsmanager.Database.Repositories.ChildExpenses.Exceptions.CannotDeleteChildExpenseException;
import com.example.lucas.haushaltsmanager.Database.Repositories.ChildExpenses.Exceptions.ChildExpenseNotFoundException;
import com.example.lucas.haushaltsmanager.Database.Repositories.Currencies.CurrencyTransformer;
import com.example.lucas.haushaltsmanager.Database.TransformerInterface;
import com.example.lucas.haushaltsmanager.Entities.Account;
import com.example.lucas.haushaltsmanager.Entities.Expense.ExpenseObject;
import com.example.lucas.haushaltsmanager.Entities.Price;

import java.util.ArrayList;
import java.util.List;

public class ChildExpenseRepository implements ChildExpenseRepositoryInterface {
    private SQLiteDatabase mDatabase;
    private ExpenseRepository mBookingRepo;
    private AccountRepository mAccountRepo;
    private final ChildExpenseTransformer transformer;
    private final TransformerInterface<ExpenseObject> bookingTransformer;

    public ChildExpenseRepository(Context context) {
        DatabaseManager.initializeInstance(new ExpensesDbHelper(context));

        mDatabase = DatabaseManager.getInstance().openDatabase();
        mBookingRepo = new ExpenseRepository(context);
        mAccountRepo = new AccountRepository(context);
        transformer = new ChildExpenseTransformer(
                new CurrencyTransformer(),
                new ChildCategoryTransformer()
        );
        bookingTransformer = new BookingTransformer(
                new CurrencyTransformer(),
                new ChildCategoryTransformer()
        );
    }

    public boolean exists(ExpenseObject expense) {
        Cursor c = executeRaw(new ChildBookingExistsQuery(expense));

        if (c.moveToFirst()) {

            c.close();
            return true;
        }

        c.close();
        return false;
    }

    /**
     * Diese Funktion stellt sicher dass keine Kind zu einer ChildExpense hinzugefügt werden kann.
     * Sie überprüft ebenfalls ob die Parentbuchung bereits ChildExpenses hat oder nicht.
     * Hat die Parentbuchung keine Kinder wird eine Dummy Ausgabe erstellt, zu der die Kinder hinzugefügt werden.
     *
     * @param childExpense  Buchung welche dem Parent als Kind hinzugefügt werden soll
     * @param parentBooking Buchung der ein neues Kind hinzugefügt werden soll
     * @return ChildExpense, mit dem korrekten Index
     */
    public ExpenseObject addChildToBooking(ExpenseObject childExpense, ExpenseObject parentBooking) throws AddChildToChildException {
        if (exists(parentBooking))
            throw new AddChildToChildException(childExpense, parentBooking);

        if (parentBooking.isParent()) {

            return insert(parentBooking, childExpense);
        } else {

            try {
                mBookingRepo.delete(parentBooking);

                ExpenseObject dummyParentExpense = ExpenseObject.createDummyExpense();
                dummyParentExpense.setCategory(parentBooking.getCategory());
                dummyParentExpense.setCurrency(parentBooking.getCurrency());

                dummyParentExpense.addChild(parentBooking);

                dummyParentExpense = mBookingRepo.insert(dummyParentExpense);

                return insert(dummyParentExpense, childExpense);
            } catch (CannotDeleteExpenseException e) {
                //Kann nicht passieren, da nur Buchung mit Kindern nicht gelöscht werden können und ich hier vorher übeprüft habe ob die Buchung Kinder hat oder nicht
                // TODO: Die isChild funktionalität so implementieren, dass nich NULL zurückgegeben werden muss.
                return null;
            }
        }
    }

    /**
     * Methode um mehrere Buchungen zusammenzufügen
     *
     * @param expenses Liste der Buchungen die zusammengefügt werden sollen
     * @return Parent der zusammengefügten Buchungen, mit den hinzugefügten KinmDatabaseuchungen
     */
    public ExpenseObject combineExpenses(List<ExpenseObject> expenses) {
        ExpenseObject dummyParentExpense = ExpenseObject.createDummyExpense();

        for (ExpenseObject expense : expenses) {
            if (expense.isParent()) {

                dummyParentExpense.addChildren(expense.getChildren());
                try {
                    for (ExpenseObject child : expense.getChildren())
                        delete(child);
                } catch (CannotDeleteChildExpenseException e) {

                    // TODO: Was soll passieren wenn ein Kind nicht gelöscht werden kann?
                }
            } else {

                try {
                    dummyParentExpense.addChild(expense);
                    mBookingRepo.delete(expense);
                } catch (CannotDeleteExpenseException e) {

                    // TODO: Kann eine ParentExpense nicht gefunden werden muss der gesamte vorgang abgebrochen werden
                    //Beispiel: https://stackoverflow.com/questions/6909221/android-sqlite-rollback
                }
            }
        }

        return mBookingRepo.insert(dummyParentExpense);
    }

    public ExpenseObject extractChildFromBooking(ExpenseObject childExpense) throws ChildExpenseNotFoundException {
        if (!exists(childExpense))
            throw new ChildExpenseNotFoundException(childExpense.getIndex());

        try {
            if (isLastChildOfParent(childExpense)) {
                ExpenseObject parentExpense = getParent(childExpense);

                delete(childExpense);
                parentExpense.removeChild(childExpense);
                childExpense.setExpenseType(ExpenseObject.EXPENSE_TYPES.NORMAL_EXPENSE);
                mBookingRepo.delete(parentExpense);
            } else {

                delete(childExpense);
                childExpense.setExpenseType(ExpenseObject.EXPENSE_TYPES.NORMAL_EXPENSE);
            }

            return mBookingRepo.insert(childExpense);
        } catch (Exception e) {

            // TODO: Was soll passieren, wenn das Kind nicht gelöscht werden kann?
            return null;
        }
    }

    public ExpenseObject get(long expenseId) throws ChildExpenseNotFoundException {
        Cursor c = executeRaw(new GetChildBookingQuery(expenseId));

        if (!c.moveToFirst()) {
            throw new ChildExpenseNotFoundException(expenseId);
        }

        return transformer.transform(c);
    }

    public List<ExpenseObject> getAll(long parentId) {
        Cursor c = executeRaw(new GetAllChildBookingsQuery(parentId));

        ArrayList<ExpenseObject> childBookings = new ArrayList<>();
        while (c.moveToNext())
            childBookings.add(transformer.transform(c));

        return childBookings;
    }

    public ExpenseObject insert(ExpenseObject parentExpense, ExpenseObject childExpense) {
        ContentValues values = new ContentValues();
        values.put(ExpensesDbHelper.BOOKINGS_COL_EXPENSE_TYPE, childExpense.getExpenseType().name());
        values.put(ExpensesDbHelper.BOOKINGS_COL_PRICE, childExpense.getUnsignedPrice());
        values.put(ExpensesDbHelper.BOOKINGS_COL_PARENT_ID, parentExpense.getIndex());
        values.put(ExpensesDbHelper.BOOKINGS_COL_CATEGORY_ID, childExpense.getCategory().getIndex());
        values.put(ExpensesDbHelper.BOOKINGS_COL_EXPENDITURE, childExpense.isExpenditure());
        values.put(ExpensesDbHelper.BOOKINGS_COL_TITLE, childExpense.getTitle());
        values.put(ExpensesDbHelper.BOOKINGS_COL_DATE, childExpense.getDate().getTimeInMillis());
        values.put(ExpensesDbHelper.BOOKINGS_COL_NOTICE, childExpense.getNotice());
        values.put(ExpensesDbHelper.BOOKINGS_COL_ACCOUNT_ID, childExpense.getAccountId());
        values.put(ExpensesDbHelper.BOOKINGS_COL_CURRENCY_ID, childExpense.getCurrency().getIndex());
        values.put(ExpensesDbHelper.BOOKINGS_COL_HIDDEN, 0);

        long insertedChildId = mDatabase.insert(ExpensesDbHelper.TABLE_BOOKINGS, null, values);

        try {
            updateAccountBalance(
                    childExpense.getAccountId(),
                    childExpense.getSignedPrice()
            );
        } catch (AccountNotFoundException e) {
            //Kann nicht passieren, da der User bei der Buchungserstellung nur aus Konten auswählen kann die bereits existieren
        }

        return ExpenseObject.copyWithNewIndex(childExpense, insertedChildId);
    }

    public void update(ExpenseObject childExpense) throws ChildExpenseNotFoundException {
        ContentValues updatedChild = new ContentValues();
        updatedChild.put(ExpensesDbHelper.BOOKINGS_COL_EXPENSE_TYPE, childExpense.getExpenseType().name());
        updatedChild.put(ExpensesDbHelper.BOOKINGS_COL_PRICE, childExpense.getUnsignedPrice());
        updatedChild.put(ExpensesDbHelper.BOOKINGS_COL_CATEGORY_ID, childExpense.getCategory().getIndex());
        updatedChild.put(ExpensesDbHelper.BOOKINGS_COL_EXPENDITURE, childExpense.isExpenditure());
        updatedChild.put(ExpensesDbHelper.BOOKINGS_COL_TITLE, childExpense.getTitle());
        updatedChild.put(ExpensesDbHelper.BOOKINGS_COL_DATE, childExpense.getDate().getTimeInMillis());
        updatedChild.put(ExpensesDbHelper.BOOKINGS_COL_NOTICE, childExpense.getNotice());
        updatedChild.put(ExpensesDbHelper.BOOKINGS_COL_ACCOUNT_ID, childExpense.getAccountId());
        updatedChild.put(ExpensesDbHelper.BOOKINGS_COL_CURRENCY_ID, childExpense.getCurrency().getIndex());

        try {
            ExpenseObject oldExpense = get(childExpense.getIndex());

            updateAccountBalance(
                    childExpense.getAccountId(),
                    childExpense.getSignedPrice() - oldExpense.getSignedPrice()
            );

            int affectedRows = mDatabase.update(ExpensesDbHelper.TABLE_BOOKINGS, updatedChild, ExpensesDbHelper.BOOKINGS_COL_ID + " = ?", new String[]{childExpense.getIndex() + ""});

            if (affectedRows == 0)
                throw new ChildExpenseNotFoundException(childExpense.getIndex());
        } catch (ChildExpenseNotFoundException e) {

            throw new ChildExpenseNotFoundException(childExpense.getIndex());
        } catch (AccountNotFoundException e) {

            // TODO: Was sollte passieren?
        }
    }

    public void delete(ExpenseObject childExpense) throws CannotDeleteChildExpenseException {

        if (!isParentRecurringOrTemplate(childExpense)) {
            try {
                hide(childExpense);
            } catch (ChildExpenseNotFoundException e) {

                // TODO Was soll passieren, wenn das Kind nicht gefunden wurde?
            }
            return;
        }

        if (isLastChildOfParent(childExpense)) {
            try {
                ExpenseObject parentExpense = getParent(childExpense);

                mDatabase.delete(ExpensesDbHelper.TABLE_BOOKINGS, ExpensesDbHelper.BOOKINGS_COL_ID + " = ?", new String[]{"" + childExpense.getIndex()});
                parentExpense.removeChild(childExpense);
                updateAccountBalance(
                        childExpense.getAccountId(),
                        -childExpense.getSignedPrice()
                );
                mBookingRepo.delete(parentExpense);
            } catch (Exception e) {

                throw CannotDeleteChildExpenseException.RelatedExpenseNotFound(childExpense);
            }

            return;
        }


        try {
            mDatabase.delete(ExpensesDbHelper.TABLE_BOOKINGS, ExpensesDbHelper.BOOKINGS_COL_ID + " = ?", new String[]{"" + childExpense.getIndex()});
            updateAccountBalance(
                    childExpense.getAccountId(),
                    -childExpense.getSignedPrice()
            );

        } catch (AccountNotFoundException e) {

            //sollte nicht passieren können, da Konten erst gelöscht werden können wenn es keine Buchungen mehr mit diesem Konto gibt
        }
    }

    public void hide(ExpenseObject childExpense) throws ChildExpenseNotFoundException {
        // REFACTOR: Kann durch die Methode des parents ersetzt werden.

        try {
            if (isLastVisibleChildOfParent(childExpense)) {

                ExpenseObject parentExpense = getParent(childExpense);
                mBookingRepo.hide(parentExpense);
            }

            ContentValues values = new ContentValues();
            values.put(ExpensesDbHelper.BOOKINGS_COL_HIDDEN, 1);

            int affectedRows = mDatabase.update(ExpensesDbHelper.TABLE_BOOKINGS, values, ExpensesDbHelper.BOOKINGS_COL_ID + " = ?", new String[]{"" + childExpense.getIndex()});

            if (affectedRows == 0)
                throw new ChildExpenseNotFoundException(childExpense.getIndex());

            try {
                updateAccountBalance(
                        childExpense.getAccountId(),
                        -childExpense.getSignedPrice()
                );
            } catch (AccountNotFoundException e) {

                // TODO: Wenn der Kontostand nicht geupdated werden kann muss die gesamte Transaktion zurückgenommen werden
            }
        } catch (ExpenseNotFoundException e) {

            // TODO: Dem aufrufenden Code mitteilen dass die Buchung nicht versteckt werden konnte
        }
    }

    public void closeDatabase() {
        //3 Mal weil 3 Datenbankverbindungen (ChildExpenseRepo, AccountRepo, BookingTagRepo, BookingRepo) geöffnet werden

        DatabaseManager.getInstance().closeDatabase();
        DatabaseManager.getInstance().closeDatabase();
        DatabaseManager.getInstance().closeDatabase();
        DatabaseManager.getInstance().closeDatabase();
    }

    public ExpenseObject getParent(ExpenseObject childExpense) throws ChildExpenseNotFoundException, ExpenseNotFoundException {
        if (!exists(childExpense)) {
            throw new ChildExpenseNotFoundException(childExpense.getIndex());
        }

        Cursor c = executeRaw(new GetParentBookingQuery(childExpense));

        if (!c.moveToFirst())
            throw ExpenseNotFoundException.parentExpenseNotFoundException(childExpense);

        return bookingTransformer.transform(c);
    }

    private Cursor executeRaw(QueryInterface query) {
        return mDatabase.rawQuery(String.format(
                query.sql(),
                query.values()
        ), null);
    }

    private boolean isParentRecurringOrTemplate(ExpenseObject expense) {
        try {
            ExpenseObject parentExpense = getParent(expense);

            return !mBookingRepo.isRecurringBooking(parentExpense)
                    || !mBookingRepo.isTemplateBooking(parentExpense);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isLastChildOfParent(ExpenseObject childExpense) {
        Cursor c = executeRaw(new IsChildBookingLastOfParentQuery(childExpense));

        if (c.getCount() == 1) {

            c.close();
            return true;
        }

        c.close();
        return false;
    }

    private boolean isLastVisibleChildOfParent(ExpenseObject childExpense) throws ChildExpenseNotFoundException, ExpenseNotFoundException {
        ExpenseObject parentExpense = getParent(childExpense);

        Cursor c = executeRaw(new IsChildBookingLastVisibleOfParentQuery(parentExpense));

        if (c.getCount() == 1) {

            c.close();
            return true;
        }

        c.close();
        return false;
    }

    /**
     * Methode um den Kontostand anzupassen.
     *
     * @param accountId Konto welches angepasst werden soll
     * @param amount    Betrag der angezogen oder hinzugefügt werden soll
     */
    private void updateAccountBalance(long accountId, double amount) throws AccountNotFoundException {

        Account account = mAccountRepo.get(accountId);
        double newBalance = account.getBalance().getSignedValue() + amount;
        account.setBalance(new Price(newBalance, account.getBalance().getCurrency()));
        mAccountRepo.update(account);
    }
}
