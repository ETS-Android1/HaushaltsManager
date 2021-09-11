package com.example.lucas.haushaltsmanager.RecyclerView.AdditionalFunctionality.SelectionRules;

import com.example.lucas.haushaltsmanager.Entities.Category;
import com.example.lucas.haushaltsmanager.Entities.Color;
import com.example.lucas.haushaltsmanager.Entities.Booking.Booking;
import com.example.lucas.haushaltsmanager.Entities.Booking.ExpenseType;
import com.example.lucas.haushaltsmanager.Entities.Booking.ParentBooking;
import com.example.lucas.haushaltsmanager.Entities.Price;
import com.example.lucas.haushaltsmanager.RecyclerView.Items.Booking.ChildBookingItem.ChildExpenseItem;
import com.example.lucas.haushaltsmanager.RecyclerView.Items.DateItem.DateItem;
import com.example.lucas.haushaltsmanager.RecyclerView.Items.Booking.BookingItem.ExpenseItem;
import com.example.lucas.haushaltsmanager.RecyclerView.Items.IRecyclerItem;
import com.example.lucas.haushaltsmanager.RecyclerView.Items.Booking.ParentBookingItem.ParentBookingItem;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class ExpenseListSelectionRulesTest {
    private ExpenseListSelectionRules selectionRules;

    @Before
    public void setUp() {
        selectionRules = new ExpenseListSelectionRules();
    }

    @Test
    public void parentCategoryItemCannotBeSelected() {
        ParentBookingItem parentCategoryItem = new ParentBookingItem(getDummyParentExpense(), getDummyDateItem());

        boolean parentCanBeSelected = selectionRules.canBeSelected(parentCategoryItem, new ArrayList<IRecyclerItem>());

        assertFalse(parentCanBeSelected);
    }

    @Test
    public void dateItemCannotBeSelected() {
        DateItem dateItem = getDummyDateItem();

        boolean dateItemCanBeSelected = selectionRules.canBeSelected(dateItem, new ArrayList<IRecyclerItem>());

        assertFalse(dateItemCanBeSelected);
    }

    @Test
    public void alreadySelectedItemCannotBeSelected() {
        List<IRecyclerItem> selectedItems = new ArrayList<>();

        ChildExpenseItem selectedItem = new ChildExpenseItem(getDummyExpense(), getDummyParentItem());
        selectedItems.add(selectedItem);

        boolean canBeSelected = selectionRules.canBeSelected(selectedItem, selectedItems);

        assertFalse(canBeSelected);
    }

    @Test
    public void expenseItemCanBeSelected() {
        ExpenseItem expenseItem = new ExpenseItem(getDummyExpense(), getDummyDateItem());

        boolean expenseItemCanBeSelected = selectionRules.canBeSelected(expenseItem, new ArrayList<IRecyclerItem>());

        assertTrue(expenseItemCanBeSelected);
    }

    @Test
    public void childExpenseItemCanBeSelected() {
        ChildExpenseItem childExpenseItem = new ChildExpenseItem(getDummyExpense(), getDummyParentItem());

        boolean childCanBeSelected = selectionRules.canBeSelected(childExpenseItem, new ArrayList<IRecyclerItem>());

        assertTrue(childCanBeSelected);
    }

    @Test
    public void expenseItemWithADifferentParentCanBeSelected() {
        List<IRecyclerItem> selectedItems = new ArrayList<>();
        selectedItems.add(new ExpenseItem(getDummyExpense(), getDummyDateItem()));

        ExpenseItem expenseItem = new ExpenseItem(getDummyExpense(), getDummyDateItem());
        boolean canBeSelected = selectionRules.canBeSelected(expenseItem, selectedItems);

        assertTrue(canBeSelected);
    }

    @Test
    public void childItemWithADifferentParentCannotBeSelected() {
        List<IRecyclerItem> selectedItems = new ArrayList<>();
        selectedItems.add(new ChildExpenseItem(getDummyExpense(), getDummyParentItem()));

        ChildExpenseItem child = new ChildExpenseItem(getDummyExpense(), getDummyParentItem());
        boolean canBeSelected = selectionRules.canBeSelected(child, selectedItems);

        assertFalse(canBeSelected);
    }

    @Test
    public void cannotSelectExpenseItemWhenChildIsSelected() {
        List<IRecyclerItem> selectedItems = new ArrayList<>();
        selectedItems.add(new ChildExpenseItem(getDummyExpense(), getDummyParentItem()));

        ExpenseItem expenseItem = new ExpenseItem(getDummyExpense(), getDummyDateItem());
        boolean canBeSelected = selectionRules.canBeSelected(expenseItem, selectedItems);

        assertFalse(canBeSelected);
    }

    private ParentBookingItem getDummyParentItem() {
        return new ParentBookingItem(getDummyParentExpense(), getDummyDateItem());
    }

    private Booking getDummyExpense() {
        return new Booking(
                "Ausgabe",
                new Price(100),
                new Category("Kategorie", new Color(Color.BLACK), ExpenseType.expense()),
                UUID.randomUUID()
        );
    }

    private ParentBooking getDummyParentExpense() {
        return new ParentBooking(
                UUID.randomUUID(),
                "Parent Ausgabe",
                Calendar.getInstance(),
                new ArrayList<Booking>()
        );
    }

    private DateItem getDummyDateItem() {
        return new DateItem(
                Calendar.getInstance()
        );
    }
}
