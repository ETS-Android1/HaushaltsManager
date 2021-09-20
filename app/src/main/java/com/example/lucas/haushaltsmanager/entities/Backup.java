package com.example.lucas.haushaltsmanager.entities;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

public class Backup {
    public static final String BACKUP_FILE_EXTENSION = "sdf"; //.SaveDataFile
    private static final Time executionTime = new Time(23, 59);

    private final String title;

    public Backup(@Nullable String title) {
        this.title = title;
    }

    public String getTitle() {
        return String.format("%s.%s",
                title == null ? getDefaultBackupName() : title,
                BACKUP_FILE_EXTENSION
        );
    }

    public Delay getDelay() {
        return new Delay(
                TimeUnit.MILLISECONDS,
                timeUntilExecution()
        );
    }

    private String getDefaultBackupName() {
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(Calendar.getInstance().getTime()) + "_Backup";
    }

    private long timeUntilExecution() {
        long currentReminderTime = executionTime.toMillis();
        long now = System.currentTimeMillis();

        if (backupOccursToday(currentReminderTime)) {
            return currentReminderTime - now;
        }

        long nextBackupTime = getNextBackupTime(currentReminderTime);
        return nextBackupTime - now;
    }

    private long getNextBackupTime(long reminderTime) {
        return reminderTime + TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS);
    }

    private boolean backupOccursToday(long reminderTime) {
        return System.currentTimeMillis() < reminderTime;
    }
}
