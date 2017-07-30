package com.example.lucas.haushaltsmanager;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.Button;

public class AccountPickerDialogFragment extends DialogFragment {

    public interface NoticeDialogListener {

        void onItemSelected(DialogFragment dialog);
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {

            mListener = (AccountPickerDialogFragment.NoticeDialogListener) activity;
        } catch (ClassCastException e) {

            throw new ClassCastException(activity.toString() + "must implement NoticeDialogListener");
        }
    }

    NoticeDialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final Bundle args = getArguments();
        final Activity activity = getActivity();
        final Button btn = (Button) activity.findViewById(R.id.expense_screen_account);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(args.getString("original_title"));

        builder.setSingleChoiceItems(R.array.dummy_accounts, -1, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int selectedAccount) {

                mListener.onItemSelected(AccountPickerDialogFragment.this);
                btn.setText(getResources().getStringArray(R.array.dummy_accounts)[selectedAccount]);
            }
        });

        return builder.create();
    }
}
