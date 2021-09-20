package com.example.lucas.haushaltsmanager.Activities.MainTab;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.cardview.widget.CardView;

import com.example.lucas.haushaltsmanager.CardPopulator.LineChartCardPopulator;
import com.example.lucas.haushaltsmanager.CardPopulator.PieChartCardPopulator;
import com.example.lucas.haushaltsmanager.CardPopulator.TimeFrameCardPopulator;
import com.example.lucas.haushaltsmanager.Database.Repositories.Bookings.ExpenseRepository;
import com.example.lucas.haushaltsmanager.R;
import com.example.lucas.haushaltsmanager.Utils.CalendarUtils;
import com.example.lucas.haushaltsmanager.Utils.ExpenseUtils.ExpenseGrouper;
import com.example.lucas.haushaltsmanager.Utils.ExpenseUtils.ExpenseSum;
import com.example.lucas.haushaltsmanager.entities.Booking.IBooking;
import com.example.lucas.haushaltsmanager.entities.Report;

import java.util.HashMap;
import java.util.List;

public class TabThreeYearlyReports extends AbstractTab {
    private LineChartCardPopulator mLineChartPopulator;
    private PieChartCardPopulator mIncomeCardPopulator, mExpenseCardPopulator;
    private TimeFrameCardPopulator mTimeFrameCardPopulator;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstances) {
        View rootView = inflater.inflate(R.layout.tab_three_yearly_reports, container, false);

        List<IBooking> bookings = getVisibleExpenses();

        mTimeFrameCardPopulator = new TimeFrameCardPopulator(
                (CardView) rootView.findViewById(R.id.tab_three_timeframe_report_card),
                getResources()
        );
        mTimeFrameCardPopulator.setData(createReport(
                getStringifiedYear(),
                bookings
        ));

        mIncomeCardPopulator = new PieChartCardPopulator(
                (CardView) rootView.findViewById(R.id.tab_three_income_card)
        );
        mIncomeCardPopulator.showIncome();
        mIncomeCardPopulator.setData(createReport(
                getString(R.string.income),
                bookings
        ));

        mExpenseCardPopulator = new PieChartCardPopulator(
                (CardView) rootView.findViewById(R.id.tab_three_expense_card)
        );
        mExpenseCardPopulator.showExpense();
        mExpenseCardPopulator.setData(createReport(
                getString(R.string.expense),
                bookings
        ));

        mLineChartPopulator = new LineChartCardPopulator(
                (CardView) rootView.findViewById(R.id.tab_three_line_chart),
                getLastYearAccountBalance(CalendarUtils.getCurrentYear(), bookings)
        );
        mLineChartPopulator.setResources(getResources(), CalendarUtils.getCurrentYear());
        mLineChartPopulator.setData(createReport(
                getString(R.string.account_balance),
                bookings
        ));

        return rootView;
    }

    public void updateView(View rootView) {
        Report report = createReport("", getVisibleExpenses());

        report.setTitle(getStringifiedYear());
        mTimeFrameCardPopulator.setData(report);

        report.setTitle(getString(R.string.account_balance));
        mLineChartPopulator.setData(report);

        report.setTitle(getString(R.string.income));
        mIncomeCardPopulator.setData(report);

        report.setTitle(getString(R.string.expense));
        mExpenseCardPopulator.setData(report);
    }

    private List<IBooking> getVisibleExpenses() {
        ExpenseRepository repository = new ExpenseRepository(getContext());

        return repository.getAll();
    }

    private double getLastYearAccountBalance(int currentYear, List<IBooking> bookings) {
        HashMap<Integer, Double> mAccountBalanceYear = new ExpenseSum().byYear(bookings);

        int lastYear = currentYear - 1;

        if (mAccountBalanceYear.containsKey(lastYear)) {
            return mAccountBalanceYear.get(lastYear);
        }

        return 0d;
    }

    private Report createReport(String title, List<IBooking> expenses) {
        return new Report(
                title,
                filterByYear(expenses, CalendarUtils.getCurrentYear())
        );
    }

    private List<IBooking> filterByYear(List<IBooking> expenses, int year) {
        return new ExpenseGrouper().byYearNew(expenses, year);
    }

    private String getStringifiedYear() {
        return String.valueOf(CalendarUtils.getCurrentYear());
    }
}
