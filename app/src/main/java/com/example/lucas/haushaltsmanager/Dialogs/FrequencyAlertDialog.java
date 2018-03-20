package com.example.lucas.haushaltsmanager.Dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.lucas.haushaltsmanager.Activities.ExpenseScreenActivity;
import com.example.lucas.haushaltsmanager.R;

public class FrequencyAlertDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final Activity activity = getActivity();
        final ExpenseScreenActivity expenseScreen = (ExpenseScreenActivity) getActivity();


        LayoutInflater inflater = activity.getLayoutInflater();
        final View frequencyInput = inflater.inflate(R.layout.frequency_input, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity.getBaseContext());

        builder.setTitle("Wie häufig soll das Ereigniss eintreten?");

        builder.setView(frequencyInput);

        builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                EditText numberInput = (EditText) frequencyInput.findViewById(R.id.input_number);
                String frequency = numberInput.getText().toString();

                expenseScreen.frequency = convertToHours(frequency);
                Button freButton = (Button) activity.findViewById(R.id.expense_screen_recurring_frequency);
                freButton.setText(String.format("%s Tage", frequency));
                dismiss();
            }
        });

        builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dismiss();
            }
        });

        return builder.create();
    }

    private int convertToHours(String timeString) {

        return Integer.parseInt(timeString) * 24;
    }
}