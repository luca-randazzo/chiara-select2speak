package com.chiara.accessibilityservices;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaProjectionService extends Service {
    private static final String DEBUG_TAG = "[Chiara_MediaProjectionService]";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "MediaProjectionServiceChannel";
    
    public static final String EXTRA_RESULT_CODE = "RESULT_CODE";
    public static final String EXTRA_DATA = "DATA";

    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private ImageReader mImageReader;
    private VirtualDisplay mVirtualDisplay;
    private Handler mHandler;
    private HandlerThread mHandlerThread;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandlerThread = new HandlerThread("ProjectionThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.screenshot_service_title))
                .setContentText(getString(R.string.screenshot_service_text))
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        // Android 14+ requires starting foreground BEFORE getting the projection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        if (intent != null && intent.hasExtra(EXTRA_RESULT_CODE)) {
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
            Intent data;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data = intent.getParcelableExtra(EXTRA_DATA, Intent.class);
            } else {
                data = intent.getParcelableExtra(EXTRA_DATA);
            }

            if (resultCode != 0 && data != null) {
                try {
                    mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
                    if (mMediaProjection != null) {
                        mHandler.post(this::startProjection);
                    } else {
                        Log.e(DEBUG_TAG, "Failed to get MediaProjection");
                        stopSelf();
                    }
                } catch (SecurityException e) {
                    Log.e(DEBUG_TAG, "SecurityException while getting projection: " + e.getMessage());
                    // Clear the cached permission as it is no longer valid
                    MainService.mProjectionData = null;
                    MainService.mProjectionResultCode = Activity.RESULT_CANCELED;
                    stopSelf();
                }
            }
        }

        return START_NOT_STICKY;
    }

    private void startProjection() {
        try {
            // MUST register callback BEFORE starting capture on Android 14+
            mMediaProjection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    cleanup();
                }
            }, mHandler);

            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getRealMetrics(metrics);
            Point size = new Point();
            display.getRealSize(size);
            
            int width = size.x;
            int height = size.y;
            int density = metrics.densityDpi;

            mImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
            mImageReader.setOnImageAvailableListener(new ImageAvailableListener(width, height), mHandler);

            mVirtualDisplay = mMediaProjection.createVirtualDisplay("screencap",
                    width, height, density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    mImageReader.getSurface(), null, mHandler);

        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Error starting projection: " + e.getMessage());
            stopSelf();
        }
    }

    private void cleanup() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mImageReader != null) {
            mImageReader.setOnImageAvailableListener(null, null);
            mImageReader = null;
        }
        if (mMediaProjection != null) {
            mMediaProjection = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanup();
        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.screenshot_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        private final int mWidth;
        private final int mHeight;

        ImageAvailableListener(int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            Bitmap bitmap = null;
            Bitmap croppedBitmap = null;
            FileOutputStream fos = null;

            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mWidth;

                    // Create bitmap with padding
                    bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);

                    // Crop to actual screen size to remove row padding
                    croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, mWidth, mHeight);

                    File externalFilesDir = getExternalFilesDir(null);
                    if (externalFilesDir != null) {
                        String filename = externalFilesDir.getAbsolutePath() + "/0.png";
                        fos = new FileOutputStream(filename);
                        croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.flush();
                        fos.close();
                        fos = null;
                        
                        Log.i(DEBUG_TAG, "Screenshot saved to " + filename);
                        
                        // Notify MainService
                        Intent readyIntent = new Intent("com.chiara.accessibilityservices.SCREENSHOT_READY");
                        readyIntent.setPackage(getPackageName());
                        sendBroadcast(readyIntent);
                    }

                    if (mMediaProjection != null) {
                        mMediaProjection.stop();
                    }
                    stopSelf();
                }
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "Error in onImageAvailable: " + e.getMessage());
            } finally {
                if (fos != null) try { fos.close(); } catch (IOException ignored) {}
                if (croppedBitmap != null) croppedBitmap.recycle();
                if (bitmap != null) bitmap.recycle();
                if (image != null) image.close();
            }
        }
    }
}
