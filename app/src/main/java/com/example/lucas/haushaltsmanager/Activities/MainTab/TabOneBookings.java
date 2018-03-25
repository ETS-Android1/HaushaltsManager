package com.example.lucas.haushaltsmanager.Activities.MainTab;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.example.lucas.haushaltsmanager.Activities.ExpenseScreenActivity;
import com.example.lucas.haushaltsmanager.Database.ExpensesDataSource;
import com.example.lucas.haushaltsmanager.Entities.Account;
import com.example.lucas.haushaltsmanager.Entities.Category;
import com.example.lucas.haushaltsmanager.Entities.Currency;
import com.example.lucas.haushaltsmanager.Entities.ExpenseObject;
import com.example.lucas.haushaltsmanager.ExpandableListAdapter;
import com.example.lucas.haushaltsmanager.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class TabOneBookings extends Fragment {

    ExpandableListAdapter mListAdapter;
    ExpandableListView mExpListView;
    List<ExpenseObject> mListDataHeader;
    HashMap<ExpenseObject, List<ExpenseObject>> mListDataChild;
    String TAG = TabOneBookings.class.getSimpleName();

    ExpensesDataSource mDatabase;
    ArrayList<ExpenseObject> mExpenses;
    List<Long> mActiveAccounts;

    FloatingActionButton fabMainAction, fabDelete, fabCombine;
    Animation openFabAnim, closeFabAnim, rotateForwardAnim, rotateBackwardAnim;
    boolean combOpen = false, delOpen = false, fabOpen = false;
    boolean mSelectionMode = false;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mListDataHeader = new ArrayList<>();
        mActiveAccounts = new ArrayList<>();
        mListDataChild = new HashMap<>();
        mExpenses = new ArrayList<>();

        mDatabase = new ExpensesDataSource(getContext());
        setActiveAccounts();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mDatabase.isOpen())
            mDatabase.close();
    }

    /**
     * https://www.captechconsulting.com/blogs/android-expandablelistview-magic
     * Anleitung um eine ExpandableListView ohne indicators zu machen
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstances) {

        View rootView = inflater.inflate(R.layout.tab_one_bookings, container, false);

        //get ListView
        mExpListView = (ExpandableListView) rootView.findViewById(R.id.lvExp);
        mExpListView.setBackgroundColor(Color.WHITE);

        updateExpListView();
        //prepareListDataOld();

        final Activity mainTab = getActivity();

        // Animated Floating Action Buttons
        fabMainAction = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fabMainAction.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (delOpen || combOpen) {

                    mListAdapter.deselectAll();
                    mSelectionMode = false;
                    closeDelete();
                    closeCombine();
                    animateIconClose();

                    updateExpListView();
                } else {

                    Intent createNewBookingIntent = new Intent(mainTab, ExpenseScreenActivity.class);
                    createNewBookingIntent.putExtra("mode", "createBooking");
                    mainTab.startActivity(createNewBookingIntent);
                }
            }
        });

        fabCombine = (FloatingActionButton) rootView.findViewById(R.id.fab_combine);
        fabCombine.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (mListAdapter.getSelectedCount() > 1) {

                    //todo bevor die Buchungen zusammengefügt werden sollte ein alert dialog den user nach einem namen für die KombiBuchung fragen
                    ExpenseObject parentBooking = mDatabase.createChildBooking(mListAdapter.getSelectedGroupData());
                    mExpenses.removeAll(mListAdapter.getSelectedGroupData());
                    mExpenses.add(0, parentBooking);
                    mListAdapter.deselectAll();
                    updateExpListView();
                    animateFabs(mListAdapter.getSelectedCount());
                } else {

                    //wenn zu einer buchung eine Kindbuchung hinzugefügt werden soll, dann muss die id des Parents mit übergeben werden
                    long parentExpenseId = mListAdapter.getSelectedBookingIds()[0];
                    Intent createChildToBookingIntent = new Intent(mainTab, ExpenseScreenActivity.class);
                    createChildToBookingIntent.putExtra("mode", "addChild");
                    createChildToBookingIntent.putExtra("parentBooking", parentExpenseId);

                    mListAdapter.deselectAll();
                    mainTab.startActivity(createChildToBookingIntent);
                }

                //todo snackbar einfügen, die es ermöglicht die aktion wieder rückgängig zu machen
            }
        });

        fabDelete = (FloatingActionButton) rootView.findViewById(R.id.fab_delete);
        fabDelete.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                mDatabase.deleteBookings(mListAdapter.getSelectedBookingIds());
                mExpenses.removeAll(mListAdapter.getSelectedGroupData());
                mListAdapter.deselectAll();

                updateExpListView();
                animateFabs(mListAdapter.getSelectedCount());

                //todo snackbar einfügen die es ermöglicht die aktion wieder rückgängig zu machen
            }
        });


        openFabAnim = AnimationUtils.loadAnimation(mainTab, R.anim.fab_open);
        closeFabAnim = AnimationUtils.loadAnimation(mainTab, R.anim.fab_close);

        rotateForwardAnim = AnimationUtils.loadAnimation(mainTab, R.anim.rotate_forward);
        rotateBackwardAnim = AnimationUtils.loadAnimation(mainTab, R.anim.rotate_backward);


        //OnClickMethods for ExpandableListView
        //ExpandableListView Group click listener
        mExpListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {

            @Override
            public boolean onGroupClick(ExpandableListView parent, View view, int groupPosition, long id) {

                //get expense
                ExpenseObject expense = (ExpenseObject) mListAdapter.getGroup(groupPosition);

                //if the user clicks on date divider nothing should happen
                if (expense.getExpenseType() == ExpenseObject.EXPENSE_TYPES.DATE_PLACEHOLDER)
                    return true;

                //if user clicks on parent the default behaviour should happen
                if (expense.isParent())
                    return false;

                if (!mSelectionMode) {

                    Intent updateParentExpenseIntent = new Intent(getContext(), ExpenseScreenActivity.class);
                    updateParentExpenseIntent.putExtra("mode", "updateParent");
                    updateParentExpenseIntent.putExtra("updateParentExpense", expense);
                    //updateParentExpenseIntent.putExtra("parentExpense", expense.getIndex());
                    startActivity(updateParentExpenseIntent);
                } else {

                    if (mListAdapter.isSelected(groupPosition)) {

                        mListAdapter.removeGroupFromList(groupPosition);
                        view.setBackgroundColor(Color.WHITE);

                        if (mListAdapter.getSelectedCount() == 0)
                            mSelectionMode = false;
                    } else {

                        mListAdapter.selectGroup(groupPosition);
                        view.setBackgroundColor(getResources().getColor(R.color.highlighted_item_color));
                    }

                    animateFabs(mListAdapter.getSelectedCount());
                }

                return true;
            }
        });


        //ExpandableListView Child click listener
        mExpListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

                if (mSelectionMode)
                    return true;

                mListAdapter.clearSelected();

                //get expense
                ExpenseObject expense = (ExpenseObject) mListAdapter.getChild(groupPosition, childPosition);

                Log.d(TAG, "onChildClick: " + expense.getName() + " " + expense.getIndex());

                //start expenseScreen with selected expense
                Intent updateChildExpenseIntent = new Intent(getContext(), ExpenseScreenActivity.class);
                updateChildExpenseIntent.putExtra("mode", "updateChild");
                updateChildExpenseIntent.putExtra("updateChildExpense", expense);
                //updateChildExpenseIntent.putExtra("childExpense", expense.getIndex());
                startActivity(updateChildExpenseIntent);
                return true;
            }
        });


        //ExpandableListView Long click listener for selecting multiple groups
        mExpListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                //if selection mode is enabled do not make long clicks anymore
                if (mSelectionMode)
                    return true;

                int groupPosition = ExpandableListView.getPackedPositionGroup(id);
                int childPosition = ExpandableListView.getPackedPositionChild(id);

                if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {

                    ExpenseObject expense = mListAdapter.getExpense(groupPosition);

                    if (expense.isValidExpense()) {

                        mListAdapter.selectGroup(groupPosition);
                        view.setBackgroundColor(getResources().getColor(R.color.highlighted_item_color));

                        mSelectionMode = true;
                        animateFabs(mListAdapter.getSelectedCount());
                        return true;
                    }

                    return false;
                } else if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {

                    //if long click is on child element
                    Toast.makeText(getContext(), "CHILD", Toast.LENGTH_SHORT).show();
                    return true;
                }

                return false;
            }
        });
        return rootView;
    }

    /**
     * Methode um die mActiveAccounts liste zu initialisieren
     */
    private void setActiveAccounts() {

        Log.d(TAG, "setActiveAccounts: erneuere aktiven Kontenliste");

        if (!mDatabase.isOpen())
            mDatabase.open();

        if (!mActiveAccounts.isEmpty())
            mActiveAccounts.clear();

        SharedPreferences preferences = getContext().getSharedPreferences("ActiveAccounts", Context.MODE_PRIVATE);

        for (Account account : mDatabase.getAllAccounts()) {

            if (preferences.getBoolean(account.getName(), false))
                mActiveAccounts.add(account.getIndex());
        }
    }

    /**
     * Methode um die ExpandableListView items vorzubereiten.
     * Beim vorbereiten wird die mActiveAccounts liste mit einbezogen,
     * ist das Konto einer Buchung nicht in der aktiven Kontoliste wird die Buchung auch nicht angezeigt.
     * <p>
     * todo wenn ein Konto abgewählt wird und bei einer parentBuchung ein oder mehrere (aber nicht alle) Buchungen nicht mehr angezeigt werden,
     * muss auch der angezeigte Preis der parentBuchung angepasst werden
     */
    //jedes mal wenn ich von tab 3 auf den ersten tab wechsle wird die funktion prepareListData ausgeführt
    private void prepareListData() {

        Log.d(TAG, "prepareListData: erstelle neue Listen daten");

        if (mListDataHeader.size() > 0)
            mListDataHeader.clear();

        if (!mListDataChild.isEmpty())
            mListDataChild.clear();

        if (mExpenses.isEmpty()) {//wenn die Liste noch nicht erstellt wurde

            if (!mDatabase.isOpen())
                mDatabase.open();

            //erzeuge den ersten Tag der Monats (um nur die Buchugen des aktuellen Monats anzuzeigen)
            Calendar firstOfMonth = Calendar.getInstance();
            firstOfMonth.set(Calendar.DAY_OF_MONTH, 1);

            //erzeuge den letzten Tag des Monats (um nur die Buchungen des aktuellen Monats anzuzeigen)
            int lastDayMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
            Calendar lastOfMonth = Calendar.getInstance();
            lastOfMonth.set(Calendar.DAY_OF_MONTH, lastDayMonth);
            mExpenses = mDatabase.getBookings(firstOfMonth.getTimeInMillis(), lastOfMonth.getTimeInMillis());
            Log.d(TAG, "prepareListData: hole mir neue daten aus der datenbank");
        }


        String separatorDate = "";

        for (int i = 0; i < mExpenses.size(); ) {

            //wenn das Datum der neuen Buchung ungleich das der alten Buchung ist muss ein DatumsSeperator eingefügt werden
            //wird ein DatumsSeperator eingefügt wird der counter nicht um eins erhöht
            if (!mExpenses.get(i).getDate().equals(separatorDate)) {

                separatorDate = mExpenses.get(i).getDate();

                Account account = new Account(8888, "", 0, Currency.createDummyCurrency(getContext()));

                ExpenseObject dateSeparator = new ExpenseObject(-1, "", 0, mExpenses.get(i).getDateTime(), true, Category.createDummyCategory(getContext()), null, account, null, ExpenseObject.EXPENSE_TYPES.DATE_PLACEHOLDER);

                mListDataHeader.add(dateSeparator);
                mListDataChild.put(dateSeparator, new ArrayList<ExpenseObject>());
            } else {

                ExpenseObject expense = mExpenses.get(i);

                if (expense.isParent()) {

                    ArrayList<ExpenseObject> allowedBookings = new ArrayList<>();

                    for (ExpenseObject childExpense : expense.getChildren()) {

                        if (mActiveAccounts.contains(childExpense.getAccount().getIndex()))
                            allowedBookings.add(childExpense);
                    }

                    //wenn kein Kind erlaubt ist muss der Parent nicht angezeigt werden
                    if (allowedBookings.size() > 0) {

                        mListDataHeader.add(expense);
                        mListDataChild.put(expense, allowedBookings);
                    }
                }

                //wenn expense keine kinder hat
                if (expense.getExpenseType() == ExpenseObject.EXPENSE_TYPES.DATE_PLACEHOLDER || mActiveAccounts.contains(expense.getAccount().getIndex())) {

                    mListDataHeader.add(expense);
                    mListDataChild.put(expense, expense.getChildren());//sollte leer sein
                }
                i++;
            }
        }
    }

    /**
     * Ersatz für die prepareListData methode, da diese nicht in der lage ist die Datumstrenner aus der liste zu nehmen.
     * Da diese Funktion aber noch Probleme mit der HasMap klasse hat wird sie noch nicht eingesetzt
     */
    private void prepareListData2() {//todo unerwartetes verhalten der HasMap

        Log.d(TAG, "prepareListData: erstelle neue Listen daten");

        if (mListDataHeader.size() > 0)
            mListDataHeader.clear();

        if (!mListDataChild.isEmpty())
            mListDataChild.clear();

        if (mExpenses.isEmpty()) {//wenn die Liste noch nicht erstellt wurde

            Log.d(TAG, "prepareListData2: Hole die Buchungen aus der Datenbank");
            if (!mDatabase.isOpen())
                mDatabase.open();

            //erzeuge den ersten Tag der Monats (um nur die Buchugen des aktuellen Monats anzuzeigen)
            Calendar firstOfMonth = Calendar.getInstance();
            firstOfMonth.set(Calendar.DAY_OF_MONTH, 1);

            //erzeuge den letzten Tag des Monats (um nur die Buchungen des aktuellen Monats anzuzeigen)
            int lastDayMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
            Calendar lastOfMonth = Calendar.getInstance();
            lastOfMonth.set(Calendar.DAY_OF_MONTH, lastDayMonth);
            mExpenses = mDatabase.getBookings(firstOfMonth.getTimeInMillis(), lastOfMonth.getTimeInMillis());
        }

        String separatorDate = "";
        ArrayList<ExpenseObject> tempGroupData = new ArrayList<>();
        HashMap<ExpenseObject, List<ExpenseObject>> tempChildData = new HashMap<>();
        ArrayList<ExpenseObject> childrenToDisplay = new ArrayList<>();

        for (int i = 0; i < mExpenses.size(); ) {

            if (mExpenses.get(i).getDate().equals(separatorDate) && i + 1 != mExpenses.size()) {

                ExpenseObject expense = mExpenses.get(i);
                childrenToDisplay.clear();

                for (ExpenseObject childExpense : expense.getChildren()) {

                    if (mActiveAccounts.contains(childExpense.getAccount().getIndex()))
                        childrenToDisplay.add(childExpense);
                }

                //muss childrenToDisplay.size() nicht == 0 sein???
                //wenn es kein/-e Kind/-er zum anzeigen gibt
                if (childrenToDisplay.size() > 0 || !expense.isParent()) {

                    tempGroupData.add(expense);
                    tempChildData.put(expense, childrenToDisplay);//bei jeder iteration werden die kinder der buchungen alle  auf den gleichn wert gesetzt und die Group buhungen in der HasMAp sind auch nicht geordnet
                }
                i++;
            } else {

                if (tempGroupData.size() > 0) {

                    ExpenseObject dateSeparator = ExpenseObject.createDummyExpense(getContext());
                    dateSeparator.setExpenseType(ExpenseObject.EXPENSE_TYPES.DATE_PLACEHOLDER);
                    dateSeparator.setDateTime(tempGroupData.get(0).getDateTime());

                    mListDataHeader.add(dateSeparator);
                    mListDataChild.put(dateSeparator, new ArrayList<ExpenseObject>());

                    mListDataHeader.addAll(tempGroupData);
                    mListDataChild.putAll(tempChildData);
                }

                tempGroupData.clear();
                tempChildData.clear();
                separatorDate = mExpenses.get(i).getDate();

                if (i == mExpenses.size() - 1)
                    break;
            }
        }
    }

    /**
     * Methode um die ein Konto in der aktiven Kontoliste zu aktivieren oder deaktivieren
     * !Nachdem Änderungen an der aktiven Kontoliste gemacht wurden wird die ExpandableListView neu instanziiert
     *
     * @param accountId AccountId des zu ändernden Kontos
     * @param isChecked status des Kontos
     */
    public void refreshListOnAccountSelected(long accountId, boolean isChecked) {

        if (mActiveAccounts.contains(accountId) == isChecked)
            return;

        if (mActiveAccounts.contains(accountId) && !isChecked)
            mActiveAccounts.remove(accountId);
        else
            mActiveAccounts.add(accountId);

        updateExpListView();
    }

    /**
     * Methode um die ExpandableListView nach einer Änderung neu anzuzeigen.
     */
    public void updateExpListView() {

        prepareListData();

        mListAdapter = new ExpandableListAdapter(getActivity(), mListDataHeader, mListDataChild);

        mExpListView.setAdapter(mListAdapter);

        mListAdapter.notifyDataSetChanged();
    }

    /**
     * animating the FloatingActionButtons
     * todo die ganzen animations methoden noch einmal neu schreiben da ich mit den aktuellen nicht zufrieden bin
     *
     * @param selectedCount number of selected entries
     */
    private void animateFabs(int selectedCount) {

        switch (selectedCount) {

            case 0:// beide buttons müssen nicht funktional und nicht sichtbar sein
                closeCombine();
                closeDelete();
                animateIconClose();
                break;
            case 1:// beide buttons müssen sichtbar sein und auf dem combineButton muss das addChild icon zu sehen sein
                fabCombine.setImageResource(R.drawable.ic_add_child_white);
                openDelete();
                openCombine();
                animateIconOpen();
                break;
            default:// beide buttons müssen sichtbar und funktional sein und auf dem combineButton muss das combineBookings icon sichtbar sein
                fabCombine.setImageResource(R.drawable.ic_combine_white);
                openCombine();
                openDelete();
                animateIconOpen();
                break;
        }
    }

    /**
     * Methode die das plus auf dem Button animiert.
     * Wird diese Animation getriggert dreht sich das Pluszeichen um 45°.
     */
    public void animateIconOpen() {

        if (!fabOpen) {

            fabMainAction.startAnimation(rotateForwardAnim);
            fabOpen = true;
        }
    }

    /**
     * Methode die das plus auf dem Button animiert.
     * Wird diese Animation getriggert dreht sich das Pluszeichen um -45°.
     */
    public void animateIconClose() {

        if (fabOpen) {

            fabMainAction.startAnimation(rotateBackwardAnim);
            fabOpen = false;
        }
    }

    /**
     * Methode die den LöschFab sichtbar und anklickbar macht.
     */
    public void openDelete() {

        if (!delOpen) {

            fabDelete.startAnimation(openFabAnim);
            fabDelete.setClickable(true);

            delOpen = true;
        }
    }

    /**
     * Methode die den LöschFab unsichtbar und nicht mehr anklickbar macht.
     */
    public void closeDelete() {

        if (delOpen) {

            fabDelete.startAnimation(closeFabAnim);
            fabDelete.setClickable(false);

            delOpen = false;
        }
    }

    /**
     * Methode die den KombinierFab sichtbar und anklickbar macht.
     */
    public void openCombine() {

        if (!combOpen) {

            fabCombine.startAnimation(openFabAnim);
            fabCombine.setClickable(true);

            combOpen = true;
        }
    }

    /**
     * Methode die den KombinierFab sichtbar und anklickbar macht.
     */
    public void closeCombine() {

        if (combOpen) {

            fabCombine.startAnimation(closeFabAnim);
            fabCombine.setClickable(false);

            combOpen = false;
        }
    }
}