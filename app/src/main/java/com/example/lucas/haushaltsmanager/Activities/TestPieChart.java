package com.example.lucas.haushaltsmanager.Activities;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.lucas.haushaltsmanager.CustomViews.PieChart;
import com.example.lucas.haushaltsmanager.R;

public class TestPieChart extends AppCompatActivity {

    protected void onCreate(Bundle savedInstances) {
        super.onCreate(savedInstances);
        setContentView(R.layout.test_pie_chart);


        PieChart test = (PieChart) findViewById(R.id.test_chart);

        float[] pieData = {30, 20, 19, 15, 7, 4, 4, 1};
        int[] sliceColors = {Color.YELLOW, Color.GREEN, Color.RED, Color.MAGENTA, Color.BLACK, Color.BLUE, Color.GRAY, Color.RED};
        String[] sliceLabels = {"Label", "Label", "Label", "Label", "Label", "Label", "Label", "Label"};

        test.setPieData(pieData, sliceColors, sliceLabels);
        test.setSliceColors(sliceColors);
    }
}
