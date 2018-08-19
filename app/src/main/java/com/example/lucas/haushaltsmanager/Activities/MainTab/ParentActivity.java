package com.example.lucas.haushaltsmanager.Activities.MainTab;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.lucas.haushaltsmanager.Activities.CategoryListActivity;
import com.example.lucas.haushaltsmanager.Activities.CourseActivity;
import com.example.lucas.haushaltsmanager.Activities.CreateBackupActivity;
import com.example.lucas.haushaltsmanager.Activities.ImportExportActivity;
import com.example.lucas.haushaltsmanager.Activities.RecurringBookingsActivity;
import com.example.lucas.haushaltsmanager.Activities.TestActivity;
import com.example.lucas.haushaltsmanager.Database.Repositories.Accounts.AccountRepository;
import com.example.lucas.haushaltsmanager.Database.Repositories.Bookings.Exceptions.CannotDeleteExpenseException;
import com.example.lucas.haushaltsmanager.Database.Repositories.Bookings.ExpenseRepository;
import com.example.lucas.haushaltsmanager.Database.Repositories.ChildExpenses.ChildExpenseRepository;
import com.example.lucas.haushaltsmanager.Database.Repositories.ChildExpenses.Exceptions.CannotDeleteChildExpenseException;
import com.example.lucas.haushaltsmanager.Dialogs.ChangeAccounts.ChooseAccountsDialogFragment;
import com.example.lucas.haushaltsmanager.Entities.Account;
import com.example.lucas.haushaltsmanager.Entities.ExpenseObject;
import com.example.lucas.haushaltsmanager.MockDataCreator;
import com.example.lucas.haushaltsmanager.MyAlarmReceiver;
import com.example.lucas.haushaltsmanager.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParentActivity extends AppCompatActivity implements ChooseAccountsDialogFragment.OnSelectedAccount {
    private static final String TAG = ParentActivity.class.getSimpleName();

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private List<ExpenseObject> mExpenses = new ArrayList<>();
    private List<Long> mActiveAccounts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tab_main_mit_nav_drawer);

        setSharedPreferencesProperties();

        setActiveAccounts();
        updateExpenses();

        //Methode die jeden Tag einmal den BackupService laufen lässt
        scheduleBackupServiceAlarm();


        //TODO den test button entfernen
        FloatingActionButton testService = (FloatingActionButton) findViewById(R.id.service_fab);
        testService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MockDataCreator test = new MockDataCreator();
                test.createBookings(100);
            }
        });


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the mReportAdapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections mReportAdapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        mTabLayout.setupWithViewPager(mViewPager);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_2);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view_2);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // Handle navigation view item clicks here.

                switch (item.getItemId()) {

                    case R.id.categories:

                        Intent categoryIntent = new Intent(ParentActivity.this, CategoryListActivity.class);
                        ParentActivity.this.startActivity(categoryIntent);
                        break;
                    case R.id.course:

                        Intent courseIntent = new Intent(ParentActivity.this, CourseActivity.class);
                        ParentActivity.this.startActivity(courseIntent);
                        break;
                    case R.id.standing_orders:

                        Intent recurringBookingIntent = new Intent(ParentActivity.this, RecurringBookingsActivity.class);
                        ParentActivity.this.startActivity(recurringBookingIntent);
                        break;
                    case R.id.backup:

                        Intent backupIntent = new Intent(ParentActivity.this, CreateBackupActivity.class);
                        ParentActivity.this.startActivity(backupIntent);
                        break;
                    case R.id.import_export:

                        Intent importExportIntent = new Intent(ParentActivity.this, ImportExportActivity.class);
                        ParentActivity.this.startActivity(importExportIntent);
                        break;
                    case R.id.preferences:

                        //todo show preferences activity
                        break;
                    case R.id.about:

                        Intent testPieIntent = new Intent(ParentActivity.this, TestActivity.class);//todo zeige die AboutActivity
                        ParentActivity.this.startActivity(testPieIntent);
                        break;
                    default:

                        //todo übersetzung
                        Toast.makeText(ParentActivity.this, "Ups, da hast du wohl etwas entdeckt was du eigentlich noch gar nicht sehen solltest.", Toast.LENGTH_SHORT).show();
                }

                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_2);
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
        });
    }

    /**
     * Methode um die Hauptwährung und das Symbol der Hauptwährung in die SharedPreferences zu schreiben.
     */
    private void setSharedPreferencesProperties() {
        SharedPreferences preferences = this.getSharedPreferences("UserSettings", Context.MODE_PRIVATE);
        preferences.edit().putInt("maxBackupCount", 20).apply();
        preferences.edit().putLong("mainCurrencyIndex", 32L).apply();
    }

    @Override
    public void onBackPressed() {

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_2);
        if (drawer.isDrawerOpen(GravityCompat.START)) {

            drawer.closeDrawer(GravityCompat.START);
        } else {

            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_activity_tab, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.choose_account:

                ChooseAccountsDialogFragment chooseAccountFragment = new ChooseAccountsDialogFragment();
                chooseAccountFragment.show(getFragmentManager(), "alterVisibleAccounts");
                break;

            default:

                Toast.makeText(this, "This should never happen!", Toast.LENGTH_SHORT).show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {

            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    return new TabOneBookings();
                case 1:
                    return new TabTwoMonthlyReports();
                case 2:
                    return new TabThree();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {

            return 3;//show 3 pages
        }

        @Override
        public CharSequence getPageTitle(int position) {

            switch (position) {

                case 0:

                    return getString(R.string.tab_one_title);
                case 1:

                    return getString(R.string.tab_two_title);
                case 2:

                    return getString(R.string.tab_three_title);
            }
            return null;
        }
    }

    /**
     * /**
     * Methode um die Daten des aktuell sichtbaren Tabs upzudaten.
     * Quelle: https://stackoverflow.com/a/27211004
     */
    public void updateChildView() {

        int visibleTabPosition = mTabLayout.getSelectedTabPosition();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("android:switcher:" + mViewPager.getId() + ":" + visibleTabPosition);

        if (fragment != null) {

            switch (visibleTabPosition) {

                case 0:
                    ((TabOneBookings) fragment).updateView();
                    break;
                case 1:
                    ((TabTwoMonthlyReports) fragment).updateView();
                    break;
                case 3:
                    ((TabThree) fragment).updateView();
                    break;
            }
        }
    }

    /**
     * Methode um meinen BackupService periodische jeden Tag einmal laufen zu lassen.
     * <p>
     * Anleitung siehe: https://guides.codepath.com/android/Starting-Background-Services#using-with-alarmmanager-for-periodic-tasks
     *///todo es sollte nicht jedes mal ein backup erstellt werden wenn die app aufgerufen wird
    private void scheduleBackupServiceAlarm() {

        Intent backupServiceIntent = new Intent(getApplicationContext(), MyAlarmReceiver.class);

        final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, MyAlarmReceiver.REQUEST_CODE, backupServiceIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        long startInMillis = System.currentTimeMillis();

        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, startInMillis, AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    //ab hier werden die aktiven konten gesetzt

    /**
     * Methode um die mActiveAccounts liste zu initialisieren
     */
    private void setActiveAccounts() {
        Log.d(TAG, "setActiveAccounts: Erneuere aktive Kontenliste");

        SharedPreferences preferences = getSharedPreferences("ActiveAccounts", Context.MODE_PRIVATE);

        for (Account account : getAllAccounts()) {
            if (preferences.getBoolean(account.getTitle(), false))
                mActiveAccounts.add(account.getIndex());
        }
    }

    /**
     * Methode um alle verfügbaren Konten aus der Datenbank zu holen
     *
     * @return Liste alles verfügbaren Konten
     */
    private List<Account> getAllAccounts() {
        return AccountRepository.getAll();
    }

    List<Long> getActiveAccounts() {
        return mActiveAccounts;
    }

    /**
     * Anleitung von: https://stackoverflow.com/questions/27204409/android-calling-a-function-inside-a-fragment-from-a-custom-action-bar
     * <p>
     * Der User hat im ChooseAccountDialogFragment ein Konto angewählt.
     *
     * @param accountId Id des angewählten Kontos.
     * @param isChecked Status des Kontos (aktiv - true oder inaktiv - false)
     */
    public void onAccountSelected(long accountId, boolean isChecked) {
        if (mActiveAccounts.contains(accountId) == isChecked)
            return;

        if (mActiveAccounts.contains(accountId) && !isChecked)
            mActiveAccounts.remove(accountId);
        else
            mActiveAccounts.add(accountId);

        updateChildView();
    }

    //ab hier wird die Buchungsliste erstellt

    /**
     * Methode um die Liste der Buchungen zu erneuern.
     */
    void updateExpenses() {
        mExpenses = ExpenseRepository.getAll();
    }

    /**
     * Methode für die KindFragments um alle anzuzeigenden Buchungen zu erhalten
     *
     * @return Anzuzeigende Buchungen
     */
    List<ExpenseObject> getExpenses() {
        return mExpenses;
    }

    /**
     * Methode für die KindFragmente um alle Buchungen in einem bestimmten Zeitraum zu bekommen.
     *
     * @param from Start des Zeitraums
     * @param to   Ende des Zeitraums
     * @return Buchungen innerhalb des angegebenen Zeitraums
     */
    ArrayList<ExpenseObject> getExpenses(Calendar from, Calendar to) {

        ArrayList<ExpenseObject> bookingsWithinTimeFrame = new ArrayList<>();
        for (ExpenseObject expense : mExpenses) {

            if (expense.getDateTime().after(from) && expense.getDateTime().before(to)) {
                bookingsWithinTimeFrame.add(expense);
            }
        }

        return bookingsWithinTimeFrame;
    }

    /**
     * Methode um eine Group- oder Parentbuchung aus der Liste der Buchungen zu löschen.
     *
     * @param expense Zu löschende Buchung.
     */
    void deleteGroupBooking(ExpenseObject expense) {
        mExpenses.remove(expense);
    }

    void deleteBookings(List<ExpenseObject> expenses) {
        for (ExpenseObject expense : expenses) {
            if (expense.isParent()) {
                deleteChildren(expense.getChildren());
            } else {
                try {
                    ExpenseRepository.delete(expense);
                } catch (CannotDeleteExpenseException e) {
                    //do nothing
                }
            }
        }

        updateExpenses();
    }

    private void deleteChildren(List<ExpenseObject> children) {
        for (ExpenseObject child : children) {
            try {
                ChildExpenseRepository.delete(child);
            } catch (CannotDeleteChildExpenseException e) {
                //do nothing
            }
        }
    }

    /**
     * Methode um mehrere Group- oder Parentbuchungen aus der Liste der Buchungen zu löschen.
     *
     * @param expenses Zu löschende Buchungen
     */
    void deleteGroupBookings(ArrayList<ExpenseObject> expenses) {
        for (ExpenseObject expense : expenses) {
            deleteGroupBooking(expense);
        }
    }

    /**
     * Methode um ein Group- oder Parentbuchung in die Liste der Buchungen einzufügen.
     *
     * @param expense Hinzuzufügende Buchung
     */
    void addGroupBooking(ExpenseObject expense) {

        mExpenses.add(expense);
    }

    /**
     * Methode um mehrere Group- oder Parentbuchungen in die Liste der Buchungen einzufügen.
     *
     * @param expenses Hinzuzufügenden Buchungen
     */
    void addGroupBookings(ArrayList<ExpenseObject> expenses) {

        for (ExpenseObject expense : expenses) {
            addGroupBooking(expense);
        }
    }

    /**
     * Methode um eine bestimmte KindBuchung zu löchen.
     *
     * @param indexOfParent ParentBuchung des Kindes
     * @param child         Zu löschendes Kind
     */
    void deleteChildBooking(int indexOfParent, ExpenseObject child) {

        ExpenseObject parentExpense = mExpenses.get(indexOfParent);
        parentExpense.removeChild(child);

        mExpenses.set(indexOfParent, parentExpense);
    }

    /**
     * Methode um bestimmte Kindbuchungen aus der Liste der Buchungen zu löschen.
     * Dabei ist KEY = ParentBuchung und VALUE = KindBuchung.
     *
     * @param children Zu löschende Kindbuchungen
     */
    void deleteChildBookings(HashMap<Long, ExpenseObject> children) {

        for (Map.Entry<Long, ExpenseObject> entry : children.entrySet()) {
            deleteChildBooking(
                    entry.getKey().intValue(),
                    entry.getValue()
            );
        }
    }

    /**
     * Methode um ein neues Kind zu einer bestehenden Buchung hinzufügen soll.
     *
     * @param indexOfParent ParentBuchung zu der das Kind hinzugefügt werden soll
     * @param child         Hinzuzufügende KindBuchung
     */
    void addChildBooking(int indexOfParent, ExpenseObject child) {

        ExpenseObject parentExpense = mExpenses.get(indexOfParent);
        parentExpense.addChild(child);

        mExpenses.set(indexOfParent, parentExpense);
    }

    /**
     * Methode um bestimmte KindBuchungen zu bestimmten ParentBuchungen hinzuzufügen.
     * Dabei ist KEY = ParentBuchung und VALUE = KindBuchung.
     *
     * @param children Hinzuzufügende KindBuchungen
     */
    void addChildBookings(HashMap<Long, ExpenseObject> children) {

        for (Map.Entry<Long, ExpenseObject> entry : children.entrySet()) {
            addChildBooking(
                    entry.getKey().intValue(),
                    entry.getValue()
            );
        }
    }
}
