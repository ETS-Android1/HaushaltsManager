package com.example.lucas.haushaltsmanager.Database.Repositories.ChildExpenses;

import android.content.Context;
import android.database.CursorIndexOutOfBoundsException;
import android.database.MatrixCursor;

import com.example.lucas.haushaltsmanager.Database.DatabaseManager;
import com.example.lucas.haushaltsmanager.Database.ExpensesDbHelper;
import com.example.lucas.haushaltsmanager.Database.Repositories.Accounts.AccountRepository;
import com.example.lucas.haushaltsmanager.Database.Repositories.Accounts.Exceptions.AccountNotFoundException;
import com.example.lucas.haushaltsmanager.Database.Repositories.Bookings.Exceptions.ExpenseNotFoundException;
import com.example.lucas.haushaltsmanager.Database.Repositories.Bookings.ExpenseRepository;
import com.example.lucas.haushaltsmanager.Database.Repositories.ChildCategories.ChildCategoryRepository;
import com.example.lucas.haushaltsmanager.Database.Repositories.ChildExpenses.Exceptions.AddChildToChildException;
import com.example.lucas.haushaltsmanager.Database.Repositories.ChildExpenses.Exceptions.CannotDeleteChildExpenseException;
import com.example.lucas.haushaltsmanager.Database.Repositories.ChildExpenses.Exceptions.ChildExpenseNotFoundException;
import com.example.lucas.haushaltsmanager.Database.Repositories.Currencies.CurrencyRepository;
import com.example.lucas.haushaltsmanager.Entities.Account;
import com.example.lucas.haushaltsmanager.Entities.Category;
import com.example.lucas.haushaltsmanager.Entities.Currency;
import com.example.lucas.haushaltsmanager.Entities.ExpenseObject;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ChildExpenseRepositoryTest {
    private Account account;
    private Category category;

    @Before
    public void setup() {
        Context context = RuntimeEnvironment.application;
        ExpensesDbHelper dbHelper = new ExpensesDbHelper(context);
        DatabaseManager.initializeInstance(dbHelper);

        Category parentCategory = mock(Category.class);
        when(parentCategory.getIndex()).thenReturn(107L);

        category = new Category("Kategorie", "#121212", true, new ArrayList<Category>());
        category = ChildCategoryRepository.insert(parentCategory, category);

        Currency currency = new Currency("Euro", "EUR", "€");
        currency = CurrencyRepository.insert(currency);

        account = new Account("Konto", 70, currency);
        account = AccountRepository.insert(account);
    }

    private ExpenseObject getSimpleExpense() {
        Currency currency = new Currency("Euro", "EUR", "€");
        currency = CurrencyRepository.insert(currency);

        return new ExpenseObject(
                "Ausgabe",
                new Random().nextInt(1000),
                false,
                category,
                account.getIndex(),
                currency
        );
    }

    private ExpenseObject getParentExpenseWithChildren() {
        ExpenseObject parentExpense = getSimpleExpense();
        parentExpense.setPrice(0);

        parentExpense.addChild(getSimpleExpense());
        parentExpense.addChild(getSimpleExpense());

        return parentExpense;
    }

    @Test
    public void testExistsWithValidChildExpenseShouldSucceed() {
        ExpenseObject parentExpense = getParentExpenseWithChildren();
        ExpenseObject childExpense = ChildExpenseRepository.insert(parentExpense, parentExpense.getChildren().get(0));

        boolean exists = ChildExpenseRepository.exists(childExpense);
        assertTrue("Kindbuchung wurde nicht gefunden", exists);
    }

    @Test
    public void testExistsWithInvalidChildExpenseShouldFail() {
        ExpenseObject childExpense = getSimpleExpense();

        boolean exists = ChildExpenseRepository.exists(childExpense);
        assertFalse("Nicht existierende KindBuchung wurde gefunden", exists);

    }

    @Test
    public void testAddChildToBookingWithExistingParentThatHasChildrenShouldSucceed() {
        ExpenseObject parentExpense = ExpenseRepository.insert(getParentExpenseWithChildren());

        ExpenseObject childExpense = getSimpleExpense();
        childExpense.setExpenditure(false);
        childExpense.setPrice(133);

        try {
            ExpenseObject actualExpense = ChildExpenseRepository.addChildToBooking(childExpense, parentExpense);

            assertEquals(parentExpense.getChildren().get(0), actualExpense.getChildren().get(0));
            assertEquals(parentExpense.getChildren().get(1), actualExpense.getChildren().get(1));
            assertEquals(parentExpense.getChildren().get(2), actualExpense.getChildren().get(2));
            assertEqualAccountBalance(
                    account.getBalance() + parentExpense.getChildren().get(0).getSignedPrice() + parentExpense.getChildren().get(1).getSignedPrice() + childExpense.getSignedPrice(),
                    account.getIndex()
            );
        } catch (AddChildToChildException e) {

            Assert.fail("KindBuchung konnte nicht zu einem Parent hinzugefügt werden");
        }
    }

    @Test
    public void testAddChildToBookingWithExistingParentThatHasNoChildrenShouldSucceed() {
        ExpenseObject parentExpense = getSimpleExpense();
        parentExpense.setExpenditure(true);
        parentExpense.setPrice(144);
        parentExpense = ExpenseRepository.insert(parentExpense);

        ExpenseObject childExpense = getSimpleExpense();
        childExpense.setExpenditure(true);
        childExpense.setPrice(177);

        try {
            ExpenseObject actualParentExpense = ChildExpenseRepository.addChildToBooking(childExpense, parentExpense);

            assertEquals(parentExpense, actualParentExpense.getChildren().get(0));
            assertEquals(childExpense, actualParentExpense.getChildren().get(1));
            assertEqualAccountBalance(
                    account.getBalance() + parentExpense.getSignedPrice() + childExpense.getSignedPrice(),
                    actualParentExpense.getChildren().get(0).getAccountId()
            );

        } catch (AddChildToChildException e) {

            Assert.fail("ParentBuchung ist keine KindBuchung");
        }
    }

    @Test
    public void testAddChildToBookingWithParentBookingIsChildShouldThrowAddChildToExpenseException() {
        ExpenseObject parentExpense = ExpenseRepository.insert(getParentExpenseWithChildren());
        ExpenseObject childExpense = getSimpleExpense();

        try {
            ChildExpenseRepository.addChildToBooking(childExpense, parentExpense.getChildren().get(0));
            Assert.fail("KindBuchung konnte zu einer KindBuchung hinzugefügt werden");

        } catch (AddChildToChildException e) {

            assertEquals("It is not possible to add children to a ChildExpense.", e.getMessage());
        }
    }

    @Test
    public void testCombineExpensesWithParentExpensesShouldSucceed() {
        ArrayList<ExpenseObject> expenses = new ArrayList<>();
        expenses.add(ExpenseRepository.insert(getSimpleExpense()));
        expenses.add(ExpenseRepository.insert(getSimpleExpense()));

        ExpenseObject parentExpense = ChildExpenseRepository.combineExpenses(expenses);

        assertTrue("ParentExpense wurde nicht erstellt", ExpenseRepository.exists(parentExpense));
        assertTrue("ChildExpense 1 wurde nicht erstellt", ChildExpenseRepository.exists(parentExpense.getChildren().get(0)));
        assertTrue("ChildExpense 2 wurde nicht erstellt", ChildExpenseRepository.exists(parentExpense.getChildren().get(1)));
        assertEqualAccountBalance(
                account.getBalance() + expenses.get(0).getSignedPrice() + expenses.get(1).getSignedPrice(),
                account.getIndex()
        );
    }

    @Test
    public void testExtractChildFromBookingShouldSucceed() {
        ExpenseObject parentExpense = ExpenseRepository.insert(getParentExpenseWithChildren());

        try {
            ExpenseObject extractedChildExpense = ChildExpenseRepository.extractChildFromBooking(parentExpense.getChildren().get(0));

            assertTrue("Die KindBuchung wurde nicht zu einer ParentBuchung konvertiert", ExpenseRepository.exists(extractedChildExpense));
            assertFalse("Die extrahierte KindBuchung wurde nicht gelöscht", ChildExpenseRepository.exists(extractedChildExpense));
            assertEqualAccountBalance(
                    account.getBalance() + parentExpense.getChildren().get(0).getSignedPrice() + parentExpense.getChildren().get(1).getSignedPrice(),
                    account.getIndex()
            );

        } catch (ChildExpenseNotFoundException e) {

            Assert.fail("Existierende KindBuchung konnt enicht extrahiert werden");
        }
    }

    @Test
    public void testExtractLastChildFromBookingShouldSucceedAndParentBookingShouldBeRemoved() {
        ExpenseObject parentExpense = getParentExpenseWithChildren();
        parentExpense.removeChild(parentExpense.getChildren().get(1));
        parentExpense = ExpenseRepository.insert(parentExpense);

        try {
            ExpenseObject extractedChildExpense = ChildExpenseRepository.extractChildFromBooking(parentExpense.getChildren().get(0));

            assertTrue("KindBuchung wurde nicht in eine ParentBuchung konvertiert", ExpenseRepository.exists(extractedChildExpense));
            assertFalse("ParentBuchung ohne Kinder wurde nicht gelöscht", ExpenseRepository.exists(parentExpense));
            assertEqualAccountBalance(
                    account.getBalance() + parentExpense.getChildren().get(0).getSignedPrice(),
                    account.getIndex()
            );

        } catch (ChildExpenseNotFoundException e) {

            Assert.fail("KindBuchung konnte nicht gelöscht werden");
        }
    }

    @Test
    public void testExtractChildFromBookingWithNotExistingChildBookingShouldThrowChildExpenseNotFoundException() {
        ExpenseObject childExpense = getSimpleExpense();

        try {
            ChildExpenseRepository.extractChildFromBooking(childExpense);
            Assert.fail("Nicht existierende KindBuchung wurde gefunden");

        } catch (ChildExpenseNotFoundException e) {

            assertEquals(String.format("Could not find Child Expense with id %s.", childExpense.getIndex()), e.getMessage());
            assertEqualAccountBalance(
                    account.getBalance(),
                    account.getIndex()
            );
        }
    }

    @Test
    public void testGetWithExistingChildExpenseShouldSucceed() {
        ExpenseObject parentExpense = getParentExpenseWithChildren();
        ExpenseObject expectedChildExpense = ChildExpenseRepository.insert(parentExpense, parentExpense.getChildren().get(0));

        try {
            ExpenseObject actualChildExpense = ChildExpenseRepository.get(expectedChildExpense.getIndex());
            assertEquals(expectedChildExpense, actualChildExpense);

        } catch (ChildExpenseNotFoundException e) {

            Assert.fail("KindBuchung konnte nicht gefunden werden");
        }
    }

    @Test
    public void testGetWithNotExistingChildExpenseShouldThrowChildExpenseNotFoundException() {
        long notExistingChildExpenseId = 524L;

        try {
            ChildExpenseRepository.get(notExistingChildExpenseId);
            Assert.fail("Nicht existierende KindBuchung wurde gefunden");

        } catch (ChildExpenseNotFoundException e) {

            assertEquals(String.format("Could not find Child Expense with id %s.", notExistingChildExpenseId), e.getMessage());
        }
    }

    @Test
    public void testGetAllShouldOnlyReturnChildrenThatAreNotHidden() {
        ExpenseObject parentExpense = getParentExpenseWithChildren();
        parentExpense = ExpenseRepository.insert(parentExpense);

        try {
            ChildExpenseRepository.hide(parentExpense.getChildren().get(0));
            List<ExpenseObject> fetchedChildren = ChildExpenseRepository.getAll(parentExpense.getIndex());

            assertEquals("Es wurden zu viele Kinder aus der Datenbank geholt", 1, fetchedChildren.size());
            assertFalse("Ein verstecktes Kind wurde aus der Datenbank geholt", fetchedChildren.contains(parentExpense.getChildren().get(0)));
            assertTrue("Ein nicht verstecktes Kind wurde aus der Datenbank geholt", fetchedChildren.contains(parentExpense.getChildren().get(1)));
        } catch (ChildExpenseNotFoundException e) {

            Assert.fail("Ausgabe konnte nicht gefunden werden");
        }
    }

    @Test
    public void testUpdateWithExistingChildExpenseShouldSucceed() {
        ExpenseObject parentExpense = getParentExpenseWithChildren();
        ExpenseObject expectedChildExpense = ChildExpenseRepository.insert(parentExpense, parentExpense.getChildren().get(0));

        try {
            expectedChildExpense.setPrice(13);
            expectedChildExpense.setExpenditure(true);

            ChildExpenseRepository.update(expectedChildExpense);
            ExpenseObject actualChildExpense = ChildExpenseRepository.get(expectedChildExpense.getIndex());

            assertEquals(expectedChildExpense, actualChildExpense);
            assertEqualAccountBalance(
                    account.getBalance() + expectedChildExpense.getSignedPrice(),
                    account.getIndex()
            );

        } catch (ChildExpenseNotFoundException e) {

            Assert.fail("KindBuchung konnte nicht gefunden werden");
        }
    }

    @Test
    public void testUpdateWithNotExistingChildExpenseShouldThrowChildExpenseNotFoundException() {
        ExpenseObject childExpense = getSimpleExpense();

        try {
            ChildExpenseRepository.update(childExpense);
            Assert.fail("Nicht existierende KindBuchung konnte geupdated werden");

        } catch (ChildExpenseNotFoundException e) {

            assertEquals(String.format("Could not find Child Expense with id %s.", childExpense.getIndex()), e.getMessage());
        }
    }

    @Test
    public void testDeleteWithExistingChildExpenseShouldSucceed() {
        ExpenseObject parentExpense = ExpenseRepository.insert(getParentExpenseWithChildren());
        ExpenseObject childExpense = parentExpense.getChildren().get(0);

        try {
            ChildExpenseRepository.delete(childExpense);

            assertFalse("Buchung wurde nicht gelöscht", ChildExpenseRepository.exists(childExpense));
            assertEqualAccountBalance(
                    account.getBalance() + parentExpense.getChildren().get(1).getSignedPrice(),
                    childExpense.getAccountId()
            );

        } catch (CannotDeleteChildExpenseException e) {

            Assert.fail("KindBuchung konnte nicht gelöscht werden");
        }
    }

    @Test
    public void testDeleteWithNotExistingChildExpenseShouldSucceed() {
        ExpenseObject childExpense = getSimpleExpense();

        try {
            ChildExpenseRepository.delete(childExpense);
            assertFalse("Nicht existierende KindBuchung wurde gefunden", ChildExpenseRepository.exists(childExpense));

        } catch (Exception e) {

            Assert.fail("Nicht existierende KindBuchung konnte nicht gelöscht werden");
        }
    }

    @Test
    public void testDeleteWithChildExpenseIsLastOfParentShouldDeleteParentAsWell() {
        ExpenseObject parentExpense = getParentExpenseWithChildren();
        parentExpense.getChildren().remove(parentExpense.getChildren().get(1));
        parentExpense = ExpenseRepository.insert(parentExpense);
        assertTrue("ParentExpense wurde nicht erstellt", ExpenseRepository.exists(parentExpense));

        try {
            ChildExpenseRepository.delete(parentExpense.getChildren().get(0));

            assertFalse("KindBuchung wurde nicht gelöscht", ChildExpenseRepository.exists(parentExpense.getChildren().get(0)));
            assertFalse("ParentBuchung wurde nicht gelöscht", ExpenseRepository.exists(parentExpense));

        } catch (Exception e) {

            Assert.fail("KindBuchung konnte nicht gelöscht werden");
        }
    }

    @Test
    public void testGetParentWithExistingChildExpenseShouldSucceed() {
        ExpenseObject expectedParentExpense = ExpenseRepository.insert(getParentExpenseWithChildren());

        try {
            ExpenseObject actualParentExpense = ChildExpenseRepository.getParent(expectedParentExpense.getChildren().get(0));
            assertEquals(expectedParentExpense, actualParentExpense);

        } catch (Exception e) {

            Assert.fail("Existierende ParentBuchung wurde nicht gefunden");
        }
    }

    @Test
    public void testGetParentWithNotExistingChildExpenseShouldThrowChildExpenseNotFoundException() {
        ExpenseObject childExpense = getSimpleExpense();

        try {
            ChildExpenseRepository.getParent(childExpense);
            Assert.fail("Konnte einen Parent zu einer nicht existierenden KindBuchung finden");

        } catch (ExpenseNotFoundException e) {

            Assert.fail("Existierende ParentBuchung konnte nicht gefunden werden");
        } catch (ChildExpenseNotFoundException e) {

            assertEquals(String.format("Could not find Child Expense with id %s.", childExpense.getIndex()), e.getMessage());
        }
    }

    @Test
    public void testGetParentWithNotExistingParentExpenseShouldThrowExpenseNotFoundException() {
        ExpenseObject parentExpense = getParentExpenseWithChildren();
        ExpenseObject childExpense = ChildExpenseRepository.insert(parentExpense, parentExpense.getChildren().get(0));

        try {
            ChildExpenseRepository.getParent(childExpense);
            Assert.fail("Ein nicht existierenden Parent wurde gefunden");

        } catch (ChildExpenseNotFoundException e) {

            Assert.fail("KindBuchung wurde nicht gefunden");
        } catch (ExpenseNotFoundException e) {

            assertEquals(String.format("Could not find ParentExpense for ChildExpense %s.", childExpense.getTitle()), e.getMessage());
        }
    }

    @Test
    public void testCursorToChildBookingWithValidCursorShouldSucceed() {
        ExpenseObject expectedChildExpense = getSimpleExpense();
        expectedChildExpense.setExpenseType(ExpenseObject.EXPENSE_TYPES.CHILD_EXPENSE);

        String[] columns = new String[]{
                ExpensesDbHelper.BOOKINGS_COL_ID,
                ExpensesDbHelper.BOOKINGS_COL_DATE,
                ExpensesDbHelper.BOOKINGS_COL_TITLE,
                ExpensesDbHelper.BOOKINGS_COL_PRICE,
                ExpensesDbHelper.BOOKINGS_COL_EXPENDITURE,
                ExpensesDbHelper.BOOKINGS_COL_NOTICE,
                ExpensesDbHelper.BOOKINGS_COL_ACCOUNT_ID,
                ExpensesDbHelper.BOOKINGS_COL_CURRENCY_ID,
                ExpensesDbHelper.CURRENCIES_COL_NAME,
                ExpensesDbHelper.CURRENCIES_COL_SHORT_NAME,
                ExpensesDbHelper.CURRENCIES_COL_SYMBOL,
                ExpensesDbHelper.CATEGORIES_COL_ID,
                ExpensesDbHelper.CATEGORIES_COL_NAME,
                ExpensesDbHelper.CATEGORIES_COL_COLOR,
                ExpensesDbHelper.CATEGORIES_COL_DEFAULT_EXPENSE_TYPE
        };

        MatrixCursor cursor = new MatrixCursor(columns);
        cursor.addRow(new Object[]{
                expectedChildExpense.getIndex(),
                expectedChildExpense.getDateTime().getTimeInMillis(),
                expectedChildExpense.getTitle(),
                expectedChildExpense.getUnsignedPrice(),
                expectedChildExpense.isExpenditure() ? 1 : 0,
                expectedChildExpense.getNotice(),
                expectedChildExpense.getAccountId(),
                expectedChildExpense.getCurrency().getIndex(),
                expectedChildExpense.getCurrency().getName(),
                expectedChildExpense.getCurrency().getShortName(),
                expectedChildExpense.getCurrency().getSymbol(),
                expectedChildExpense.getCategory().getIndex(),
                expectedChildExpense.getCategory().getTitle(),
                expectedChildExpense.getCategory().getColorString(),
                expectedChildExpense.getCategory().getDefaultExpenseType() ? 1 : 0
        });
        cursor.moveToFirst();

        try {
            ExpenseObject actualChildExpense = ChildExpenseRepository.cursorToChildBooking(cursor);
            assertEquals(expectedChildExpense, actualChildExpense);

        } catch (CursorIndexOutOfBoundsException e) {

            Assert.fail("Ausgabe konnte nicht aus einem vollständigen Cursor wiederhergestellt werden");
        }
    }

    @Test
    public void testCursorToChildBookingWithInvalidCursorShouldThrowCursorIndexOutOfBoundsException() {
        ExpenseObject expectedChildExpense = getSimpleExpense();
        expectedChildExpense.setExpenseType(ExpenseObject.EXPENSE_TYPES.CHILD_EXPENSE);

        String[] columns = new String[]{
                ExpensesDbHelper.BOOKINGS_COL_ID,
                ExpensesDbHelper.BOOKINGS_COL_DATE,
                ExpensesDbHelper.BOOKINGS_COL_TITLE,
                //Der Preis der KindBuchung wurde nicht mit abgefragt
                ExpensesDbHelper.BOOKINGS_COL_EXPENDITURE,
                ExpensesDbHelper.BOOKINGS_COL_NOTICE,
                ExpensesDbHelper.BOOKINGS_COL_ACCOUNT_ID,
                ExpensesDbHelper.BOOKINGS_COL_CURRENCY_ID,
                ExpensesDbHelper.CURRENCIES_COL_NAME,
                ExpensesDbHelper.CURRENCIES_COL_SHORT_NAME,
                ExpensesDbHelper.CURRENCIES_COL_SYMBOL,
                ExpensesDbHelper.CATEGORIES_COL_ID,
                ExpensesDbHelper.CATEGORIES_COL_NAME,
                ExpensesDbHelper.CATEGORIES_COL_COLOR,
                ExpensesDbHelper.CATEGORIES_COL_DEFAULT_EXPENSE_TYPE
        };

        MatrixCursor cursor = new MatrixCursor(columns);
        cursor.addRow(new Object[]{
                expectedChildExpense.getIndex(),
                expectedChildExpense.getDateTime().getTimeInMillis(),
                expectedChildExpense.getTitle(),
                expectedChildExpense.isExpenditure() ? 1 : 0,
                expectedChildExpense.getNotice(),
                expectedChildExpense.getAccountId(),
                expectedChildExpense.getCurrency().getIndex(),
                expectedChildExpense.getCurrency().getName(),
                expectedChildExpense.getCurrency().getShortName(),
                expectedChildExpense.getCurrency().getSymbol(),
                expectedChildExpense.getCategory().getIndex(),
                expectedChildExpense.getCategory().getTitle(),
                expectedChildExpense.getCategory().getColorString(),
                expectedChildExpense.getCategory().getDefaultExpenseType() ? 1 : 0
        });
        cursor.moveToFirst();

        try {
            ChildExpenseRepository.cursorToChildBooking(cursor);
            Assert.fail("KindBuchung konnte trotz eines Fehlerhaften Cursor widerhergestellt werden");

        } catch (CursorIndexOutOfBoundsException e) {

            //do nothing
        }
    }

    @Test
    public void testHideWithExistingChildExpenseShouldSucceed() {
        ExpenseObject parentExpense = ExpenseRepository.insert(getParentExpenseWithChildren());

        try {
            ExpenseObject hiddenChild = parentExpense.getChildren().get(0);
            ChildExpenseRepository.hide(hiddenChild);

            assertTrue("KindBuchung wurde gelöscht", ChildExpenseRepository.exists(hiddenChild));
            assertFalse("Versteckte Buchung wurde aus der Datenbank geholt", ChildExpenseRepository.getAll(parentExpense.getIndex()).contains(hiddenChild));
            assertEqualAccountBalance(
                    account.getBalance() + parentExpense.getChildren().get(1).getSignedPrice(),
                    account.getIndex()
            );

        } catch (ChildExpenseNotFoundException e) {

            Assert.fail("Kind Buchung wurde nicht gefunden");
        }
    }

    @Test
    public void testHideLastChildOfParentShouldHideParentAsWell() {
        ExpenseObject parentExpense = getParentExpenseWithChildren();
        parentExpense.getChildren().remove(1);
        parentExpense = ExpenseRepository.insert(parentExpense);

        try {
            ExpenseObject hiddenChildExpense = parentExpense.getChildren().get(0);
            ChildExpenseRepository.hide(hiddenChildExpense);

            assertTrue("ParentBuchung wurde nicht versteckt", ExpenseRepository.isHidden(parentExpense));
            assertFalse("KindBuchung wurde nicht versteckt", ChildExpenseRepository.getAll(parentExpense.getIndex()).contains(hiddenChildExpense));
            assertEqualAccountBalance(
                    account.getBalance(),
                    account.getIndex()
            );
        } catch (ChildExpenseNotFoundException e) {

            Assert.fail("KindBuchung wurde nicht gefunden");
        } catch (ExpenseNotFoundException e) {

            Assert.fail("ParetnBuchung wurde nicht gefunden");
        }
    }

    @Test
    public void testHideWithNotExistingChildExpenseShouldThrowChildExpenseNotFoundException() {
        ExpenseObject childExpense = getSimpleExpense();

        try {
            ChildExpenseRepository.hide(childExpense);
            Assert.fail("Nicht existierende ChildExpense konnte versteckt werden");

        } catch (ChildExpenseNotFoundException e) {

            assertEquals(String.format("Could not find Child Expense with id %s.", childExpense.getIndex()), e.getMessage());
            assertEqualAccountBalance(
                    account.getBalance(),
                    account.getIndex()
            );
        }
    }

    @Test
    public void testIsHiddenWithExistingChildExpenseShouldSucceed() {
        ExpenseObject parentExpense = ExpenseRepository.insert(getParentExpenseWithChildren());

        try {
            ExpenseObject hiddenChildExpense = parentExpense.getChildren().get(0);

            boolean isHidden = ExpenseRepository.isHidden(hiddenChildExpense);
            assertFalse("ChildExpense ist versteckt", isHidden);

            ChildExpenseRepository.hide(hiddenChildExpense);
            isHidden = ExpenseRepository.isHidden(hiddenChildExpense);
            assertTrue("ChildExpense ist nicht versteckt", isHidden);

        } catch (ChildExpenseNotFoundException e) {

            Assert.fail("ChildExpense wurde nicht gefunden");
        } catch (ExpenseNotFoundException e) {

            Assert.fail("ParentExpense wurde nicht gefunden");
        }
    }

    @Test
    public void testIsHiddenWithNotExistingChildExpenseShouldThrowExpenseNotFoundException() {
        ExpenseObject childExpense = getSimpleExpense();

        try {
            ChildExpenseRepository.isHidden(childExpense);
            Assert.fail("ChildExpense wurde gefunden");

        } catch (ChildExpenseNotFoundException e) {

            assertEquals(String.format("Could not find Child Expense with id %s.", childExpense.getIndex()), e.getMessage());
            assertEqualAccountBalance(
                    account.getBalance(),
                    account.getIndex()
            );
        }
    }

    private void assertEqualAccountBalance(double expectedAmount, long accountId) {

        try {
            double actualBalance = AccountRepository.get(accountId).getBalance();
            assertEquals("Konto wurde nicht geupdated", expectedAmount, actualBalance);

        } catch (AccountNotFoundException e) {

            Assert.fail("Konto wurde nicht gefunden");
        }
    }
}