package com.chiara.accessibilityservices;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "[Chiara_SettingsActivity]";
    private static final int PERMISSION_REQUEST_CODE_NOTIFICATIONS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        requestNotificationPermissionIfNeeded();

        Button btnOpenAccessibility = findViewById(R.id.btn_open_accessibility);
        btnOpenAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                ComponentName componentName = new ComponentName(getPackageName(), MainService.class.getName());
                String serviceId = componentName.flattenToString();
                intent.putExtra(":settings:fragment_args_key", serviceId);
                intent.putExtra(":settings:show_fragment_args", true);
                startActivity(intent);
            }
        });

        Button btnOpenVolume = findViewById(R.id.btn_open_volume);
        btnOpenVolume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                        ? new Intent(Settings.Panel.ACTION_VOLUME)
                        : new Intent(Settings.ACTION_SOUND_SETTINGS);
                startActivity(intent);
            }
        });

        Button btnTestCrash = findViewById(R.id.btn_test_crash);
        btnTestCrash.setOnClickListener(v -> {
            throw new RuntimeException("Test Crash - Firebase Crashlytics Verification");
        });
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE_NOTIFICATIONS
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "POST_NOTIFICATIONS permission granted.");
            } else {
                Log.w(TAG, "POST_NOTIFICATIONS permission denied.");
            }
        }
    }
}
