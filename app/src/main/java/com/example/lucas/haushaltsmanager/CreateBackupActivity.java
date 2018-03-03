package com.example.lucas.haushaltsmanager;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CreateBackupActivity extends AppCompatActivity implements DirectoryPickerDialogFragment.OnDirectorySelected, BasicTextInputDialog.BasicDialogCommunicator {

    String TAG = CreateBackupActivity.class.getSimpleName();

    FloatingActionButton mCreateBackupFab;
    Button mChooseDirectoryBtn;
    File mBackupDirectory;
    ListView mListView;
    ArrayAdapter<String> mListViewAdapter;
    List<String> mOldBackups;

    //.SavedDataFile
    final String mBackupExtension = ".sdf";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_backup);

        //Initialisierung des Backup Ordners
        mBackupDirectory = new File(getFilesDir().toString() + "/Backups");
        if (!mBackupDirectory.exists())
            mBackupDirectory.mkdir();

        mChooseDirectoryBtn = (Button) findViewById(R.id.create_backup_directory_btn);
        mListView = (ListView) findViewById(R.id.create_backup_list_view);
        //mListView.setEmptyView(); todo wenn keine alten Backups vorhanden sind soll "Keine Existierenden Backups vorhanden" angezeigt werden

        refreshListView();

        mCreateBackupFab = (FloatingActionButton) findViewById(R.id.create_backup_create_backup_btn);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mChooseDirectoryBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Bundle bundle = new Bundle();
                bundle.putString("title", getResources().getString(R.string.choose_directory));

                DirectoryPickerDialogFragment directoryPicker = new DirectoryPickerDialogFragment();
                directoryPicker.setArguments(bundle);
                directoryPicker.show(getFragmentManager(), "choose_directory");
            }
        });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                showConfirmationDialog(new File(mBackupDirectory + "/" + mListView.getItemAtPosition(position)));
            }
        });

        mCreateBackupFab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Bundle bundle = new Bundle();
                bundle.putString("title", getResources().getString(R.string.choose_new_backup_name));

                BasicTextInputDialog basicDialog = new BasicTextInputDialog();
                basicDialog.setArguments(bundle);
                basicDialog.show(getFragmentManager(), "create_backup_name");
            }
        });
    }

    /**
     * Methode um die ListView neu zu laden, wenn sich die Anzahl der Backups geändert hat.
     */
    private void refreshListView() {

        mOldBackups = getBackupsInDirectory(mBackupDirectory);

        mListViewAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mOldBackups);

        mListView.setAdapter(mListViewAdapter);

        mListViewAdapter.notifyDataSetChanged();
    }

    /**
     * Methode um ein Backup zu erstellen
     */
    private void createBackup(@Nullable String backupName) {

        Intent backupServiceIntent = new Intent(this, BackupService.class);
        backupServiceIntent.putExtra("backup_directory", mBackupDirectory.toString());
        if (backupName != null)
            backupServiceIntent.putExtra("backup_name", backupName);

        startService(backupServiceIntent);

        Toast.makeText(this, R.string.creating_backup, Toast.LENGTH_SHORT).show();

        refreshListView();
    }

    /**
     * Methode die das angegeben Verzeichniss prüft
     *
     * @param directory Verzeichniss
     * @return Ist das Verzeichniss benutzbar
     */
    private boolean validateDirectory(File directory) {

        //gucke ob das verzeichniss benutzbar ist
        if (!directory.exists())
            return false;

        if (directory.isFile())
            return false;

        return true;
    }//todo

    /**
     * Methode um das vom User ausgewählte Backup wiederherzustellen
     *
     * @param fileName Backup das wiederhergestellt werden soll
     */
    private void restoreAppState(File fileName) {

        try {

            BackupService.copy(fileName, getDatabasePath("expenses.db"));
        } catch (IOException e) {

            Log.e(TAG, "restoreAppState: ", e);
        }
    }

    /**
     * Methode um einen ConfirmationDialog zu zeigen der den User fragt, ob er sich sicher ist, dass er das Backup wiederherstellen möchte.
     *
     * @param file Das Backup
     */
    private void showConfirmationDialog(final File file) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.restoreBackup);

        builder.setMessage(getResources().getString(R.string.restore_backup_confirmation) + ": " + file.getName());

        builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                restoreAppState(file);
            }
        });

        builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                //do nothing
            }
        });

        builder.create();

        builder.show();
    }

    /**
     * Methode um die Namen aller App eigenen Backups (.sdf) Datein aus einem Verzeichniss zu erhalten
     *
     * @param directory Verzeichniss, in dem gesucht werden soll.
     * @return Dateinamen der Backups
     */
    private List<String> getBackupsInDirectory(File directory) {

        List<String> backups = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files != null && files.length != 0) {

            for (File file : files) {

                if (file.getName().contains(mBackupExtension))
                    backups.add(file.getName());
            }
        }

        return backups;
    }

    @Override
    public void onDirectorySelected(File file, String tag) {

        if (tag.equals("choose_directory")) {

            if (validateDirectory(file))
                mBackupDirectory = file;
        }
    }

    @Override
    public void onTextInput(String data, String tag) {

        if (tag.equals("create_backup_name")) {

            createBackup(data);
        }
    }
}
