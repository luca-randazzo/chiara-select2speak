/*
Copyright (C) Unknown
*/

package com.chiara.accessibilityservices;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class ScreenshotActivity extends Activity {

    private static final String DEBUG_TAG = "[Chiara_ScreenshotActivity]";
    private static final int REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            if (projectionManager != null) {
                startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            } else {
                Log.e(DEBUG_TAG, "MediaProjectionManager is null");
                FirebaseCrashlytics.getInstance().log("MediaProjectionManager is null in ScreenshotActivity");
                finish();
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Error starting screen capture intent: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Log.i(DEBUG_TAG, "User granted screen capture permission");
                FirebaseCrashlytics.getInstance().log("User granted screen capture permission");

                // Store the permission result in MainService for future reuse
                MainService.mProjectionResultCode = resultCode;
                MainService.mProjectionData = data;
                
                // Start the foreground service and pass the permission result
                Intent serviceIntent = new Intent(this, MediaProjectionService.class);
                serviceIntent.putExtra(MediaProjectionService.EXTRA_RESULT_CODE, resultCode);
                serviceIntent.putExtra(MediaProjectionService.EXTRA_DATA, data);
                
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    } else {
                        startService(serviceIntent);
                    }
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "Failed to start MediaProjectionService: " + e.getMessage());
                    FirebaseCrashlytics.getInstance().recordException(e);
                    MainService.mProjectionData = null;
                    MainService.mProjectionResultCode = RESULT_CANCELED;
                }
            } else {
                Log.e(DEBUG_TAG, "User denied screen capture permission or data is null");
                FirebaseCrashlytics.getInstance().log("User denied screen capture permission or data is null");
            }
            
            // Finish the activity regardless of the result
            finish();
        }
    }
}
