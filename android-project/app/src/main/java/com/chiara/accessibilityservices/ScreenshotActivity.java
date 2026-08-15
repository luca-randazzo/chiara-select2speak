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

public class ScreenshotActivity extends Activity {

    private static final String DEBUG_TAG = "[Chiara_ScreenshotActivity]";
    private static final int REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Call for the projection manager
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        // Start projection intent to ask for permission
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Log.i(DEBUG_TAG, "User granted screen capture permission");
                
                // Store the permission result in MainService for future reuse
                MainService.mProjectionResultCode = resultCode;
                MainService.mProjectionData = data;
                
                // Start the foreground service and pass the permission result
                Intent serviceIntent = new Intent(this, MediaProjectionService.class);
                serviceIntent.putExtra(MediaProjectionService.EXTRA_RESULT_CODE, resultCode);
                serviceIntent.putExtra(MediaProjectionService.EXTRA_DATA, data);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            } else {
                Log.e(DEBUG_TAG, "User denied screen capture permission or data is null");
            }
            
            // Finish the activity regardless of the result
            finish();
        }
    }
}
