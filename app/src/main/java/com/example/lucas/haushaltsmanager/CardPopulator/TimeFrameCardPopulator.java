package com.example.lucas.haushaltsmanager.CardPopulator;

import android.content.res.Resources;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.cardview.widget.CardView;

import com.example.lucas.haushaltsmanager.App.app;
import com.example.lucas.haushaltsmanager.entities.Category;
import com.example.lucas.haushaltsmanager.entities.Booking.Booking;
import com.example.lucas.haushaltsmanager.entities.Booking.IBooking;
import com.example.lucas.haushaltsmanager.entities.Booking.ParentBooking;
import com.example.lucas.haushaltsmanager.entities.Price;
import com.example.lucas.haushaltsmanager.entities.Report.ReportInterface;
import com.example.lucas.haushaltsmanager.R;
import com.example.lucas.haushaltsmanager.Utils.ExpenseUtils.ExpenseSum;
import com.example.lucas.haushaltsmanager.Views.MoneyTextView;
import com.example.lucas.haushaltsmanager.Views.RoundedTextView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeFrameCardPopulator {
    private ViewHolder mViewHolder;
    private final Resources mResources;

    public TimeFrameCardPopulator(CardView rootView, Resources resources) {
        initializeViewHolder(rootView);

        mResources = resources;
    }

    public void setData(ReportInterface report) {
        setCardTitle(report.getCardTitle());

        setIncome(new Price(report.getIncoming()));

        setOutgoing(new Price(report.getOutgoing()));

        setTotal(new Price(report.getTotal()));

        setTotalBookingsCount(report.getBookingCount());

        setCategory(report.getMostStressedCategory());

        setPieChart(report);
    }

    private void setCardTitle(String title) {
        mViewHolder.mTitle.setText(title);
    }

    private void setIncome(Price income) {
        mViewHolder.income.bind(income);
    }

    private void setOutgoing(Price outgoing) {
        mViewHolder.expense.bind(outgoing);
    }

    private void setTotal(Price total) {
        mViewHolder.total.bind(total);
    }

    private void setTotalBookingsCount(int bookingsCount) {
        mViewHolder.mBookingsCount.setText(String.format("%s %s", bookingsCount, getString(R.string.bookings)));
    }

    private void setCategory(Category category) {
        mViewHolder.mCategoryColor.setCircleColor(category.getColor().getColorString());
        mViewHolder.mCategoryTitle.setText(category.getName());
    }

    private void setPieChart(ReportInterface report) {
        mViewHolder.mPieChart.setData(preparePieData(report));
        mViewHolder.mPieChart.setDrawHoleEnabled(false);
        mViewHolder.mPieChart.getLegend().setEnabled(false);
        mViewHolder.mPieChart.getDescription().setEnabled(false);
        mViewHolder.mPieChart.setNoDataText(getString(R.string.no_bookings_in_year));
        mViewHolder.mPieChart.setNoDataTextColor(getColor(R.color.text_color_alert));
        mViewHolder.mPieChart.setRotationEnabled(false);
        mViewHolder.mPieChart.setHighlightPerTapEnabled(true); // Muss aktiviert sein, sonst kann ich den Listener nicht setzen
        mViewHolder.mPieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                Toast.makeText(app.getContext(), "" + e.getY(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected() {
                // Do nothing
            }
        });
    }

    private PieData preparePieData(ReportInterface report) {
        if (report.getBookingCount() == 0) {
            return null;
        }

        List<PieEntry> pieData = new ArrayList<>();
        List<Booking> expenses = flattenExpenses(report.getExpenses());
        for (Map.Entry<Boolean, Double> entry : sumByExpenseType(expenses).entrySet()) {
            pieData.add(dataSetFrom(entry));
        }

        PieDataSet pds = new PieDataSet(pieData, "");
        pds.setColors(getColor(R.color.booking_income), getColor(R.color.booking_expense));
        pds.setDrawValues(false);

        return new PieData(pds);
    }

    private PieEntry dataSetFrom(Map.Entry<Boolean, Double> entry) {
        float value = Math.abs(entry.getValue().floatValue());

        return new PieEntry(
                value,
                "" // Es sollen keine Labels angezeigt werden
        );
    }

    @ColorInt
    private int getColor(@ColorRes int color) {
        return mResources.getColor(color);
    }

    private String getString(@StringRes int string) {
        return mResources.getString(string);
    }

    private List<Booking> flattenExpenses(List<IBooking> bookings) {
        List<Booking> extractedChildren = new ArrayList<>();

        for (IBooking booking : bookings) {
            if (booking instanceof ParentBooking)
                extractedChildren.addAll(((ParentBooking) booking).getChildren());
            else
                extractedChildren.add((Booking) booking);
        }

        return extractedChildren;
    }

    private HashMap<Boolean, Double> sumByExpenseType(List<Booking> expenses) {
        ExpenseSum expenseSum = new ExpenseSum();

        HashMap<Boolean, Double> summedExpenses = new HashMap<>();
        summedExpenses.put(true, expenseSum.byExpenditureType(true, expenses));
        summedExpenses.put(false, expenseSum.byExpenditureType(false, expenses));

        return summedExpenses;
    }

    private void initializeViewHolder(CardView rootView) {
        mViewHolder = new ViewHolder();

        mViewHolder.mTitle = rootView.findViewById(R.id.timeframe_report_card_title);

        mViewHolder.income = rootView.findViewById(R.id.time_frame_report_card_income);

        mViewHolder.expense = rootView.findViewById(R.id.time_frame_report_card_expense);

        mViewHolder.total = rootView.findViewById(R.id.time_frame_report_card_total);

        mViewHolder.mBookingsCount = rootView.findViewById(R.id.timeframe_report_card_total_bookings);

        mViewHolder.mCategoryColor = rootView.findViewById(R.id.timeframe_report_card_category_color);
        mViewHolder.mCategoryTitle = rootView.findViewById(R.id.timeframe_report_card_category_title);

        mViewHolder.mPieChart = rootView.findViewById(R.id.timeframe_report_card_pie_chart);
    }

    private static class ViewHolder {
        TextView mTitle;
        MoneyTextView income;
        MoneyTextView expense;
        MoneyTextView total;
        TextView mBookingsCount;
        RoundedTextView mCategoryColor;
        TextView mCategoryTitle;
        PieChart mPieChart;
    }
}
