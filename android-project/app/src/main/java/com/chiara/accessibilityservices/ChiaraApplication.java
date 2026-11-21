package com.chiara.accessibilityservices;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChiaraApplication extends Application {
    private static final String TAG = "[Chiara_Application]";
    private static ChiaraApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        setupUncaughtExceptionHandler();
        initCrashlyticsKeys();
        Log.i(TAG, "ChiaraApplication initialized on Android SDK " + Build.VERSION.SDK_INT);
    }

    public static ChiaraApplication getInstance() {
        return sInstance;
    }

    private void initCrashlyticsKeys() {
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCustomKey("sdk_version", Build.VERSION.SDK_INT);
            crashlytics.setCustomKey("device_model", Build.MODEL);
            crashlytics.log("ChiaraApplication started");
        } catch (Exception e) {
            Log.w(TAG, "Crashlytics notice: " + e.getMessage());
        }
    }

    private void setupUncaughtExceptionHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception caught in thread " + thread.getName(), throwable);
            logCrashToFile(thread, throwable);

            try {
                FirebaseCrashlytics.getInstance().recordException(throwable);
            } catch (Exception ignored) {}

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });
    }

    public void logCrashToFile(Thread thread, Throwable throwable) {
        File extDir = getExternalFilesDir("logs");
        File logDir = (extDir != null) ? extDir : getFilesDir();

        if (logDir == null) {
            Log.e(TAG, "Cannot access log directory to save crash log.");
            return;
        }

        File crashFile = new File(logDir, "crash_log.txt");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        String timestamp = sdf.format(new Date());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("Timestamp: ").append(timestamp).append("\n");
        sb.append("Thread: ").append(thread.getName()).append(" (ID: ").append(thread.getId()).append(")\n");
        sb.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        sb.append("Android SDK: ").append(Build.VERSION.SDK_INT).append(" (Release: ").append(Build.VERSION.RELEASE).append(")\n");
        sb.append("Stack trace:\n").append(stackTrace).append("\n");
        sb.append("========================================\n\n");

        try (FileWriter fw = new FileWriter(crashFile, true)) {
            fw.write(sb.toString());
            fw.flush();
            Log.i(TAG, "Crash log successfully saved to: " + crashFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Failed to write crash log to file", e);
        }
    }
}
