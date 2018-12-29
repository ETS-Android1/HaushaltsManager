package com.example.lucas.haushaltsmanager.Worker;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

abstract class AbstractWorker extends Worker {
    private static final int DELAY_IN_HOURS = 24;
    private static final TimeUnit DELAY_TIME_UNIT = TimeUnit.HOURS;

    AbstractWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    final int getNotificationId() {
        return new Random().nextInt();
    }

    /**
     * Methode um einen neuen NotificationWorker zu starten.
     * Da man mit dem aktuellen WorkManager noch keine Wiederkehrenden Worker erstellen kann,
     * bei denen der erste Worker zu einer bestimmten Zeit ausgelöst wird,ist das hier der Workaround.
     */
    final void scheduleNewWorker() {
        // REFACTOR: Wenn man den ersten Worker schedulen kann diese Methodik entfernen
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest
                .Builder(this.getClass())
                .setInitialDelay(DELAY_IN_HOURS, DELAY_TIME_UNIT)
                .build();

        saveWorkerId(workRequest.getId().toString());

        WorkManager.getInstance().enqueue(workRequest);

        Log.i(this.getClass().getSimpleName(), String.format("Next Job will be executed in %d %s", DELAY_IN_HOURS, DELAY_TIME_UNIT.toString()));
    }

    abstract void saveWorkerId(String workerId);
}
