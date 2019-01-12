package com.example.lucas.haushaltsmanager.Database.Repositories.Accounts;

import android.database.CursorIndexOutOfBoundsException;
import android.database.MatrixCursor;

import com.example.lucas.haushaltsmanager.Database.DatabaseManager;
import com.example.lucas.haushaltsmanager.Database.ExpensesDbHelper;
import com.example.lucas.haushaltsmanager.Database.Repositories.Accounts.Exceptions.AccountNotFoundException;
import com.example.lucas.haushaltsmanager.Database.Repositories.Accounts.Exceptions.CannotDeleteAccountException;
import com.example.lucas.haushaltsmanager.Database.Repositories.Bookings.ExpenseRepository;
import com.example.lucas.haushaltsmanager.Database.Repositories.ChildExpenses.ChildExpenseRepository;
import com.example.lucas.haushaltsmanager.Entities.Account;
import com.example.lucas.haushaltsmanager.Entities.Category;
import com.example.lucas.haushaltsmanager.Entities.Currency;
import com.example.lucas.haushaltsmanager.Entities.Expense.ExpenseObject;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class AccountRepositoryTest {
    private AccountRepositoryInterface mAccountRepo;

    private ChildExpenseRepository mChildExpenseRepo;
    private ExpenseRepository mBookingRepo;

    /**
     * Manager welcher die Datenbank verbindungen hält
     */
    private DatabaseManager mDatabaseManagerInstance;

    @Before
    public void setup() {
        mAccountRepo = new AccountRepository(RuntimeEnvironment.application);
        mDatabaseManagerInstance = DatabaseManager.getInstance();


        mChildExpenseRepo = new ChildExpenseRepository(RuntimeEnvironment.application);
        mBookingRepo = new ExpenseRepository(RuntimeEnvironment.application);
    }

    @After
    public void teardown() {

        mAccountRepo.closeDatabase();
        mDatabaseManagerInstance.closeDatabase();
    }

    public Account getSimpleAccount() {
        Currency localCurrency = mock(Currency.class);

        return new Account(
                "Konto",
                7653,
                localCurrency
        );
    }

    @Test
    public void testExistsWithExistingAccountShouldSucceed() {
        Account account = mAccountRepo.create(getSimpleAccount());

        boolean exists = mAccountRepo.exists(account);
        assertTrue("Das Konto konnte nicht in der Datenbank gefunden werrden", exists);
    }

    @Test
    public void testExistsWithNotExistingAccountShouldSucceed() {
        Account account = getSimpleAccount();

        boolean exists = mAccountRepo.exists(account);
        assertFalse("Nicht existierendes Konto konnte in der Datenbank gefunden werden", exists);
    }

    @Test
    public void testGetWithExistingAccountShouldSucceed() {
        Account expectedAccount = mAccountRepo.create(getSimpleAccount());

        try {
            Account fetchedAccount = mAccountRepo.get(expectedAccount.getIndex());
            assertEquals(expectedAccount, fetchedAccount);

        } catch (AccountNotFoundException e) {

            Assert.fail("Konto wurde nicht gefunden");
        }
    }

    @Test
    public void testGetWithNotExistingAccountShouldThrowAccountNotFoundException() {
        long notExistingAccountId = 1337;

        try {
            mAccountRepo.get(notExistingAccountId);
            Assert.fail("Nicht existierendes Kont konnte gefunden werden");

        } catch (AccountNotFoundException e) {

            assertEquals(String.format("Could not find Account with id %s.", notExistingAccountId), e.getMessage());
        }
    }

    @Test
    public void testInsertWithValidAccountShouldSucceed() {
        Account expectedAccount = mAccountRepo.create(getSimpleAccount());

        try {
            Account fetchedAccount = mAccountRepo.get(expectedAccount.getIndex());
            assertEquals(expectedAccount, fetchedAccount);

        } catch (AccountNotFoundException e) {

            Assert.fail("Konto wurde nicht gefunden");
        }
    }

    @Test
    public void testDeleteWithWithExistingAccountShouldSucceed() {
        Account account = mAccountRepo.create(getSimpleAccount());

        try {
            mAccountRepo.delete(account);
            assertFalse("Konto wurde nicht gelöscht", mAccountRepo.exists(account));

        } catch (CannotDeleteAccountException e) {

            Assert.fail("Konto, welches keiner Buchung zugeordnet ist, konnte nicht gelöscht werden");
        }
    }

    @Test
    public void testDeleteWithExistingAccountAttachedToParentExpenseShouldFailWithCannotDeleteAccountException() {
        Account account = mAccountRepo.create(getSimpleAccount());

        Category mockCategory = mock(Category.class);
        Currency mockCurrency = mock(Currency.class);

        ExpenseObject parentExpense = new ExpenseObject("Ausgabe", 0, false, mockCategory, account.getIndex(), mockCurrency);
        mBookingRepo.insert(parentExpense);

//        ExpenseRepository mockExpenseRepo = mock(ExpenseRepository.class);
//        when(mockExpenseRepo.exists()).thenReturn(true);
//        injectMock(mAccountRepo, mockExpenseRepo, "mBookingRepo");

        try {
            mAccountRepo.delete(account);
            Assert.fail("Konto konnte gelöscht werden obwohl es eine ParentBuchung mit diesem Konto gibt");

        } catch (CannotDeleteAccountException e) {

            assertTrue("Konto wurde gelöscht", mAccountRepo.exists(account));
            assertEquals(String.format("Account %s cannot be deleted.", account.getTitle()), e.getMessage());
        }
    }

    @Test
    public void testDeleteWithExistingAccountAttachedToChildExpenseShouldFailWithCannotDeleteAccountException() {
        Account account = mAccountRepo.create(getSimpleAccount());

        ExpenseObject mockParentExpense = mock(ExpenseObject.class);
        Category mockCategory = mock(Category.class);
        Currency mockCurrency = mock(Currency.class);

        ExpenseObject childExpense = new ExpenseObject("Ausgabe", 0, false, mockCategory, account.getIndex(), mockCurrency);
        mChildExpenseRepo.insert(mockParentExpense, childExpense);

//        ChildExpenseRepository mockChildExpenseRepo = mock(ChildExpenseRepository.class);
//        when(mockChildExpenseRepo.exists()).thenReturn(true);
//        injectMock(mAccountRepo, mockChildExpenseRepo, "mChildExpenseRepo");

        try {
            mAccountRepo.delete(account);
            Assert.fail("Konto konnte gelöscht werden obwohl es eine KindBuchung mit diesem Konto gibt");

        } catch (CannotDeleteAccountException e) {

            assertTrue("Konto wurde gelöscht", mAccountRepo.exists(account));
            assertEquals(String.format("Account %s cannot be deleted.", account.getTitle()), e.getMessage());
        }
    }

    @Test
    public void testDeleteWithNotExistingAccountShouldSucceed() {
        Account account = getSimpleAccount();

        try {
            mAccountRepo.delete(account);
            assertFalse("Konto wurde in der Datenbank gefunden", mAccountRepo.exists(account));

        } catch (CannotDeleteAccountException e) {

            Assert.fail("Nicht existierendes Konto konnte nicht gelöscht werden");
        }

    }

    @Test
    public void testUpdateWithWithExistingAccountShouldSucceed() {
        Account expectedAccount = mAccountRepo.create(getSimpleAccount());

        try {
            expectedAccount.setName("New Account Name");
            mAccountRepo.update(expectedAccount);
            Account fetchedAccount = mAccountRepo.get(expectedAccount.getIndex());

            assertEquals(expectedAccount, fetchedAccount);

        } catch (AccountNotFoundException e) {

            Assert.fail("Gerade erstelltes Konto konnte nicht gefunden werden");
        }
    }

    @Test
    public void testUpdateWithNotExistingAccountShouldThrowAccountNotFoundException() {
        Account account = getSimpleAccount();

        try {
            mAccountRepo.update(account);
            Assert.fail("Nicht existierendes Konto konnte geupdated werden");

        } catch (AccountNotFoundException e) {

            assertEquals(String.format("Could not find Account with id %s.", account.getIndex()), e.getMessage());
        }
    }

    @Test
    public void testCursorToAccountWithValidCursorShouldSucceed() {
        Account expectedAccount = getSimpleAccount();

        String[] columns = new String[]{
                ExpensesDbHelper.ACCOUNTS_COL_ID,
                ExpensesDbHelper.ACCOUNTS_COL_NAME,
                ExpensesDbHelper.ACCOUNTS_COL_BALANCE,
                ExpensesDbHelper.CURRENCIES_COL_ID,
                ExpensesDbHelper.CURRENCIES_COL_NAME,
                ExpensesDbHelper.CURRENCIES_COL_SHORT_NAME,
                ExpensesDbHelper.CURRENCIES_COL_SYMBOL
        };

        MatrixCursor cursor = new MatrixCursor(columns);
        cursor.addRow(new Object[]{expectedAccount.getIndex(), expectedAccount.getTitle(), expectedAccount.getBalance(), expectedAccount.getCurrency().getIndex(), expectedAccount.getCurrency().getName(), expectedAccount.getCurrency().getShortName(), expectedAccount.getCurrency().getSymbol()});
        cursor.moveToFirst();

        try {
            Account fetchedAccount = mAccountRepo.fromCursor(cursor);
            assertEquals(expectedAccount, fetchedAccount);

        } catch (CursorIndexOutOfBoundsException e) {

            Assert.fail("Konto konnte nicht aus einem Cursor hergestellt werden");
        }
    }

    @Test
    public void testCursorToAccountWithInvalidCursorShouldThrowCursorIndexOutOfBoundsException() {
        Account expectedAccount = getSimpleAccount();

        String[] columns = new String[]{
                ExpensesDbHelper.ACCOUNTS_COL_ID,
                ExpensesDbHelper.ACCOUNTS_COL_NAME,
                //Der Kontostand ist nicht mit im Cursor
                ExpensesDbHelper.ACCOUNTS_COL_CURRENCY_ID
        };

        MatrixCursor cursor = new MatrixCursor(columns);
        cursor.addRow(new Object[]{expectedAccount.getIndex(), expectedAccount.getTitle(), expectedAccount.getCurrency().getIndex()});
        cursor.moveToFirst();

        try {
            mAccountRepo.fromCursor(cursor);
            Assert.fail("Konto konnte aus einem Fehlerhaften Cursor wiederhergestellt werden");

        } catch (CursorIndexOutOfBoundsException e) {

            //do nothing
        }
    }

    /**
     * Methode um ein Feld einer Klasse durch ein anderes, mit injection, auszutauschen.
     *
     * @param obj       Objekt welches angepasst werden soll
     * @param value     Neuer Wert des Felds
     * @param fieldName Name des Feldes
     */
    private void injectMock(Object obj, Object value, String fieldName) {
        try {
            Class cls = obj.getClass();

            Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {

            Assert.fail(String.format("Could not find field %s in class %s", fieldName, obj.getClass().getSimpleName()));
        }
    }
}
