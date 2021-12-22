package com.example.lucas.haushaltsmanager.Activities.MainTab;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.lucas.haushaltsmanager.Activities.AboutUsActivity;
import com.example.lucas.haushaltsmanager.Activities.BackupActivity;
import com.example.lucas.haushaltsmanager.Activities.CategoryList;
import com.example.lucas.haushaltsmanager.Activities.ImportExportActivity;
import com.example.lucas.haushaltsmanager.Activities.RecurringBookingList;
import com.example.lucas.haushaltsmanager.Activities.Settings;
import com.example.lucas.haushaltsmanager.Dialogs.ChangeAccounts.ChooseAccountsDialogFragment;
import com.example.lucas.haushaltsmanager.R;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import java.util.UUID;

public class ParentActivity extends AppCompatActivity implements ChooseAccountsDialogFragment.OnSelectedAccount {
    private TabLayout mTabLayout;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tab_main_mit_nav_drawer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the mReportAdapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections mReportAdapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mTabLayout = findViewById(R.id.tabs);
        mTabLayout.setupWithViewPager(mViewPager);


        DrawerLayout drawer = findViewById(R.id.drawer_layout_2);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view_2);
        navigationView.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {

                case R.id.categories:

                    Intent categoryIntent = new Intent(ParentActivity.this, CategoryList.class);
                    ParentActivity.this.startActivity(categoryIntent);
                    break;
                case R.id.standing_orders:

                    Intent recurringBookingIntent = new Intent(ParentActivity.this, RecurringBookingList.class);
                    ParentActivity.this.startActivity(recurringBookingIntent);
                    break;
                case R.id.backup:

                    Intent backupIntent = new Intent(ParentActivity.this, BackupActivity.class);
                    ParentActivity.this.startActivity(backupIntent);
                    break;
                case R.id.import_export:

                    Intent importExportIntent = new Intent(ParentActivity.this, ImportExportActivity.class);
                    ParentActivity.this.startActivity(importExportIntent);
                    break;
                case R.id.preferences:

                    Intent settingsIntent = new Intent(ParentActivity.this, Settings.class);
                    ParentActivity.this.startActivity(settingsIntent);
                    break;
                case R.id.about:

                    Intent aboutUsIntent = new Intent(ParentActivity.this, AboutUsActivity.class);
                    ParentActivity.this.startActivity(aboutUsIntent);
                    break;
                default:

                    Toast.makeText(ParentActivity.this, R.string.not_implemented, Toast.LENGTH_SHORT).show();
            }

            drawer.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout_2);

        if (drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity_tab, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.choose_account:

                ChooseAccountsDialogFragment chooseAccountFragment = new ChooseAccountsDialogFragment();
                chooseAccountFragment.setOnAccountSelectedListener(this);
                chooseAccountFragment.show(getFragmentManager(), "alterVisibleAccounts");
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAccountSelected(UUID accountId, boolean isChecked) {
        updateVisibleTab();
    }

    /**
     * Method to refresh data in the currently visible Tab.
     * Source: https://stackoverflow.com/a/27211004
     */
    private void updateVisibleTab() {

        int visibleTabPosition = mTabLayout.getSelectedTabPosition();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("android:switcher:" + mViewPager.getId() + ":" + visibleTabPosition);

        if (fragment != null) {

            switch (visibleTabPosition) {

                case 0:
                    ((TabOneBookings) fragment).updateView(fragment.getView());
                    break;
                case 1:
                    ((TabTwoMonthlyReports) fragment).updateView(fragment.getView());
                    break;
                case 2:
                    ((TabThreeYearlyReports) fragment).updateView(fragment.getView());
                    break;
            }
        }
    }

    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new TabOneBookings();
                case 1:
                    return new TabTwoMonthlyReports();
                case 2:
                    return new TabThreeYearlyReports();
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
}
