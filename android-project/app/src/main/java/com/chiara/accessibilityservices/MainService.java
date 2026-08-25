/*
Copyright (C) 2020 Luca Randazzo

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, version 3 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details here:
<https://www.gnu.org/licenses/>.
*/

package com.chiara.accessibilityservices;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/* This service provides an entry point to the Chiara_Select2Speak service. */
public class MainService extends AccessibilityService implements View.OnTouchListener {
    // service name used inside logs
    private static final String DEBUG_TAG = "[Chiara_MainService]";

    private static final int SUCCESS = 1;
    private static final int ERROR = -1;

    // MediaProjection permission storage
    public static Intent mProjectionData;
    public static int mProjectionResultCode = Activity.RESULT_CANCELED;

    // service status variables
    private boolean service_active = false;
    private boolean speech_active = false;
    private boolean first_setup = true;

    // service path
    private static String PATH;

    // layout
    FrameLayout mLayout;
    // area selection
    int previous_action=0, current_action=0;
    int x0, x1, y0, y1, current_x, current_y;
    // GUI
    ImageView image_view;
    Bitmap bitmapDrawingPane;
    Canvas canvasDrawingPane;
    Paint paint;

    // screenshot
    Bitmap latest_screenshot_bitmap;

    // drag variables
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    private int lastX = 100, lastY = 100;
    private boolean buttonMoved;
    private int buttonMarginX = Integer.MIN_VALUE;
    private int buttonMarginY = Integer.MIN_VALUE;

    // TextRecognizer
    TextRecognizer text_recognizer;

    // TextToSpeech engine
    private TextToSpeech tts;
    boolean remove_newlines = true;
    private final Map<String, List<SpokenWord>> spokenWords = new HashMap<>();
    private int utteranceCounter = 0;
    private Paint highlightPaint;

    // MyLog class
    MyLog my_log;

    // goodies
    String tts_welcome_message = "Ciao scimmiotta, ti voglio bene da Luca";

    // debug
    boolean verbose_ontouch = false;
    boolean lovely_start    = false;

    private final BroadcastReceiver screenshotReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(DEBUG_TAG, "Received screenshot ready broadcast from MediaProjectionService");
            // Only clear and reload if we haven't already got a bitmap (e.g. from native API)
            onScreenshotReady();
        }
    };

    @Override protected void onServiceConnected() {
        // ---------------------------------------------------------------
        // Setup PATH and MyLog
        // ---------------------------------------------------------------
        File externalFilesDir = getExternalFilesDir(null);
        if (externalFilesDir != null) {
            PATH = externalFilesDir.getAbsolutePath() + "/";
            my_log = new MyLog(PATH);

            Log.i(DEBUG_TAG, "[onServiceConnected] PATH initialized to: " + PATH);
            my_log.i(DEBUG_TAG, "[onServiceConnected] PATH initialized to: " + PATH);
        }
        else {
            Log.e(DEBUG_TAG, "[onServiceConnected] Failed to create file storage directory, getExternalFilesDir() returned null.");
            return;
        }

        // Register receiver for screenshot ready signal (fallback for older Android versions)
        IntentFilter filter = new IntentFilter("com.chiara.accessibilityservices.SCREENSHOT_READY");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenshotReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(screenshotReceiver, filter);
        }

        // ---------------------------------------------------------------
        // Say hello :)
        // ---------------------------------------------------------------
        Log.i(DEBUG_TAG, "[onServiceConnected] Hello world! Setting-up...");
        my_log.i(DEBUG_TAG, "[onServiceConnected] Hello world! Setting-up...");


        // ---------------------------------------------------------------
        // Setup OCR
        // ---------------------------------------------------------------
        text_recognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!text_recognizer.isOperational()) {
            Log.e(DEBUG_TAG, "[onServiceConnected] Detector dependencies are not available");
            my_log.e(DEBUG_TAG, "[onServiceConnected] Detector dependencies are not available");

            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, "[onServiceConnected] Low storage space available", Toast.LENGTH_LONG).show();
                Log.e(DEBUG_TAG, "[onServiceConnected] Low storage space available");
                my_log.e(DEBUG_TAG, "[onServiceConnected] Low storage space available");
            }
        }
        else {
            Log.i(DEBUG_TAG, "[onServiceConnected] OCR correctly setup");
            my_log.i(DEBUG_TAG, "[onServiceConnected] OCR correctly setup");
        }


        // ---------------------------------------------------------------
        // Setup Screenshoter
        // ---------------------------------------------------------------
        // delete previous screenshots
        deletePNGFilesInFolder(PATH);


        // ---------------------------------------------------------------
        // Setup Layout and GUI
        // ---------------------------------------------------------------
        // create layout and set Overlay properties
        mLayout = new FrameLayout(this);
        setOverlayProperties(false);

        // setup the buttons
        LayoutInflater inflater = LayoutInflater.from(MainService.this);
        inflater.inflate(R.layout.ui, mLayout);
        // hide stop button
        final Button button_stop = (Button) mLayout.findViewById(R.id.stop);
        button_stop.setVisibility(View.GONE);
        Log.i(DEBUG_TAG, "[onServiceConnected] mLayout setup");
        my_log.i(DEBUG_TAG, "[onServiceConnected] mLayout setup");

        // setup images
        image_view = (ImageView) mLayout.findViewById(R.id.image_view);
        image_view.setVisibility(View.GONE);
        Log.i(DEBUG_TAG, "[onServiceConnected] Images setup");
        my_log.i(DEBUG_TAG, "[onServiceConnected] Images setup");

        // configure buttons
        configureButtons();
        Log.i(DEBUG_TAG, "[onServiceConnected] Buttons setup");
        my_log.i(DEBUG_TAG, "[onServiceConnected] Buttons setup");


        // ---------------------------------------------------------------
        // Setup OnTouchListener
        // ---------------------------------------------------------------
        mLayout.setOnTouchListener(this);
        Log.i(DEBUG_TAG, "[onServiceConnected] OnTouchListener setup");
        my_log.i(DEBUG_TAG, "[onServiceConnected] OnTouchListener setup");


        // ---------------------------------------------------------------
        // Setup TTS
        // ---------------------------------------------------------------
        // Set up the Text To Speech engine.
        TextToSpeech.OnInitListener listener = new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(final int status) {
                if (status == TextToSpeech.SUCCESS) {
                    Log.i(DEBUG_TAG, "[onServiceConnected] Text to speech engine started successfully");
                    my_log.i(DEBUG_TAG, "[onServiceConnected] Text to speech engine started successfully");

                    tts.setLanguage(Locale.ITALIAN);

                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            new Handler(Looper.getMainLooper()).post(() -> setButtonSpeaking(true));
                        }

                        @Override
                        public void onRangeStart(String utteranceId, int start, int end, int frame) {
                            List<SpokenWord> words = spokenWords.get(utteranceId);
                            if (words == null) return;

                            for (SpokenWord word : words) {
                                if (start < word.end && end > word.start) {
                                    new Handler(Looper.getMainLooper()).post(() -> drawSpokenWord(word.bounds));
                                    return;
                                }
                            }
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (tts != null && !tts.isSpeaking()) {
                                    finishSpeechOverlay();
                                }
                            }, 100);
                        }

                        @Override
                        public void onError(String utteranceId) {
                            new Handler(Looper.getMainLooper()).post(() -> finishSpeechOverlay());
                        }
                    });
                }
                else {
                    Log.e(DEBUG_TAG, "[onServiceConnected] Error starting the Text to speech engine");
                    my_log.e(DEBUG_TAG, "[onServiceConnected] Error starting the Text to speech engine");
                }
            }
        };
        tts = new TextToSpeech(this.getApplicationContext(), listener);


        // ---------------------------------------------------------------
        // Finish setup
        // ---------------------------------------------------------------
        Log.i(DEBUG_TAG, "[onServiceConnected] Setup done");
        my_log.i(DEBUG_TAG, "[onServiceConnected] Setup done");

        Toast.makeText(getBaseContext(),"Chiara_Select2Speak active!", Toast.LENGTH_SHORT).show();

        if (lovely_start) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    tts.speak(tts_welcome_message, TextToSpeech.QUEUE_ADD, null, "DEFAULT");
                }
            }, 1000);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(screenshotReceiver);
        } catch (Exception ignored) {}
    }

    private void onScreenshotReady() {
        // If we already have the bitmap (from native API), don't reload from disk
        if (latest_screenshot_bitmap == null) {
            latest_screenshot_bitmap = loadScreenshotBitmap();
        }

        if (latest_screenshot_bitmap != null) {
            Log.i(DEBUG_TAG, "Screenshot processed and loaded successfully");
            
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(getBaseContext(),"GO :)", Toast.LENGTH_SHORT).show();
                
                // Show the overlay view for selection
                setupServiceStatus(true);
                
                // Prepare drawing panes
                Bitmap.Config config = latest_screenshot_bitmap.getConfig() != null ? 
                        latest_screenshot_bitmap.getConfig() : Bitmap.Config.ARGB_8888;

                bitmapDrawingPane = Bitmap.createBitmap(
                        latest_screenshot_bitmap.getWidth(),
                        latest_screenshot_bitmap.getHeight(),
                        config);
                canvasDrawingPane = new Canvas(bitmapDrawingPane);
                
                // image_view will show the drawing pane, 
                // the screenshot itself is NOT displayed as the background to allow seeing the screen below
                image_view.setImageBitmap(bitmapDrawingPane);
                
                paint = new Paint();
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(ContextCompat.getColor(this, R.color.selection_rect));
                paint.setStrokeWidth(10);

                highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                highlightPaint.setStyle(Paint.Style.FILL);
                highlightPaint.setColor(Color.argb(160, 255, 214, 0));
            });
        } else {
            Log.e(DEBUG_TAG, "Failed to load screenshot even after signal");
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(getBaseContext(), "Errore: screenshot non pronto", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void configureButtons() {
        final ImageButton button_start = (ImageButton) mLayout.findViewById(R.id.start);
        final Button button_stop    = (Button) mLayout.findViewById(R.id.stop);

        button_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(DEBUG_TAG, "[configureButtons::button_start::onClick] Pressed 'Start'");
                my_log.i(DEBUG_TAG, "[configureButtons::button_start::onClick] Pressed 'Start'");

                latest_screenshot_bitmap = null; // Clear previous
                deletePNGFilesInFolder(PATH);
                takeScreenshot();
            }
        });

        button_start.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (service_active) return false;

                WindowManager.LayoutParams params = (WindowManager.LayoutParams) mLayout.getLayoutParams();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        buttonMoved = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(event.getRawX() - initialTouchX) >= 10 ||
                                Math.abs(event.getRawY() - initialTouchY) >= 10) {
                            buttonMoved = true;
                        }
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        lastX = params.x;
                        lastY = params.y;
                        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                        wm.updateViewLayout(mLayout, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!buttonMoved) {
                            v.performClick();
                        }
                        return true;
                }
                return false;
            }
        });

        button_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(DEBUG_TAG, "[configureButtons::button_stop::onClick] Pressed 'Stop'");
                if (tts.isSpeaking()) {
                    tts.stop();
                    setButtonSpeaking(false);
                }
            }
        });
    }

    private void setupServiceStatus(boolean status) {
        final ImageButton button_start = (ImageButton) mLayout.findViewById(R.id.start);

        service_active = status;

        if (service_active) {
            button_start.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_bg_active)));
            setOverlayProperties(true);
            if (canvasDrawingPane != null) {
                canvasDrawingPane.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            }
            image_view.setVisibility(View.VISIBLE);
            Log.i(DEBUG_TAG, "[setupServiceStatus] Service set to active");
        }
        else {
            button_start.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_bg_inactive)));
            setOverlayProperties(false);
            if (canvasDrawingPane != null) {
                canvasDrawingPane.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            }
            image_view.setVisibility(View.GONE);
            Log.i(DEBUG_TAG, "[setupServiceStatus] Service set to not active");
        }
    }

    private void setButtonSpeaking(boolean speaking) {
        if (mLayout == null) return;
        final ImageButton button_start = (ImageButton) mLayout.findViewById(R.id.start);
        if (button_start == null) return;

        if (speaking) {
            button_start.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccent)));
        } else {
            button_start.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_bg_inactive)));
        }
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override public void onInterrupt() { }

    @Override public boolean onTouch(View v, MotionEvent event) {
        if (speech_active) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                tts.stop();
                finishSpeechOverlay();
            }
            return true;
        }
        if (!service_active) return false;
        processMotionEvent(event, verbose_ontouch, v);
        return true; // Consume the event when active
    }

    private void finishSpeechOverlay() {
        speech_active = false;
        spokenWords.clear();
        setButtonSpeaking(false);
        if (mLayout != null && !first_setup) {
            setOverlayProperties(false);
            image_view.setVisibility(View.GONE);
        }
    }

    private void rememberButtonPosition(ImageButton button_start) {
        if (button_start == null || first_setup) return;

        int[] location = new int[2];
        button_start.getLocationOnScreen(location);
        lastX = location[0] - buttonMarginX;
        lastY = location[1] - buttonMarginY;
    }

    void setOverlayProperties(boolean fullscreen) {
        final ImageButton button_start = (ImageButton) mLayout.findViewById(R.id.start);
        WindowManager.LayoutParams lp;
        RelativeLayout.LayoutParams buttonParams = null;

        if (button_start != null) {
            buttonParams = (RelativeLayout.LayoutParams) button_start.getLayoutParams();
            if (buttonMarginX == Integer.MIN_VALUE) {
                buttonMarginX = buttonParams.leftMargin;
                buttonMarginY = buttonParams.topMargin;
            }
        }

        rememberButtonPosition(button_start);

        if (first_setup) {
            lp = new WindowManager.LayoutParams();
            lp.type     = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
            lp.format   = PixelFormat.TRANSLUCENT;
            lp.flags    |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            lp.gravity  = Gravity.TOP | Gravity.LEFT;
            lp.x = lastX;
            lp.y = lastY;
        }
        else {
            lp = (WindowManager.LayoutParams) mLayout.getLayoutParams();
        }

        if (fullscreen) {
            lp.height   = WindowManager.LayoutParams.MATCH_PARENT;
            lp.width    = WindowManager.LayoutParams.MATCH_PARENT;
            lp.x = 0;
            lp.y = 0;
            if (button_start != null) {
                buttonParams.leftMargin = lastX + buttonMarginX;
                buttonParams.topMargin = lastY + buttonMarginY;
                button_start.setLayoutParams(buttonParams);
            }
        }
        else {
            lp.width    = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.height   = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.x = lastX;
            lp.y = lastY;
            if (button_start != null) {
                buttonParams.leftMargin = buttonMarginX;
                buttonParams.topMargin = buttonMarginY;
                button_start.setLayoutParams(buttonParams);
            }
        }

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (first_setup) {
            wm.addView(mLayout, lp);
        }
        else {
            wm.updateViewLayout(mLayout, lp);
        }
        first_setup = false;
    }

    void processMotionEvent (MotionEvent event, boolean print, View view) {
        if (print) printMotionEvent(event);

        previous_action = current_action;
        current_action = event.getAction();
        current_x = (int) event.getX();
        current_y = (int) event.getY();

        if (current_x < 0) current_x = 0;
        if (current_y < 0) current_y = 0;

        switch (current_action) {
            case MotionEvent.ACTION_DOWN:
                x0 = current_x;
                y0 = current_y;
                break;

            case MotionEvent.ACTION_MOVE:
                drawRectangle();
                break;

            case MotionEvent.ACTION_UP :
                if (previous_action == MotionEvent.ACTION_MOVE) {
                    x1 = current_x;
                    y1 = current_y;
                    drawRectangle();
                    speech_active = true;

                    if (latest_screenshot_bitmap != null) {
                        try {
                            Bitmap screenshot_bitmap_resized = resizeBitmap(latest_screenshot_bitmap);
                            bitmapToSpeech(screenshot_bitmap_resized);
                        } catch (Exception e) {
                            Log.e(DEBUG_TAG, "Error resizing or processing bitmap: " + e.getMessage());
                            tts.speak("Errore elaborazione", TextToSpeech.QUEUE_ADD, null, "DEFAULT");
                        }
                    }
                    else {
                        Log.e(DEBUG_TAG, "[processMotionEvent] null bitmap");
                        tts.speak("Nessuno screenshot caricato", TextToSpeech.QUEUE_ADD, null, "DEFAULT");
                    }
                    setupServiceStatus(false);
                    setOverlayProperties(true);
                }
                break;

            default:
                break;
        }
    }

    void printMotionEvent(MotionEvent event) {
        int action = event.getAction();
        String actionStr = (action == MotionEvent.ACTION_DOWN) ? "DOWN" : 
                           (action == MotionEvent.ACTION_UP) ? "UP" : 
                           (action == MotionEvent.ACTION_MOVE) ? "MOVE" : String.valueOf(action);
        Log.i(DEBUG_TAG, "[printMotionEvent] " + actionStr + " - x " + String.format("%.2f", event.getRawX()) + ", y " + String.format("%.2f", event.getRawY()) );
    }

    int deletePNGFilesInFolder(String folder) {
        int count = 0;
        File dir = new File(folder);

        if ( dir.isDirectory() ) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    if (child.endsWith(".png")) {
                        new File(dir, child).delete();
                        count++;
                    }
                }
            }
        }
        return (count > 0) ? SUCCESS : ERROR;
    }

    void takeScreenshot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.i(DEBUG_TAG, "Taking screenshot using AccessibilityService API (API 30+)");
            takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(), new TakeScreenshotCallback() {
                @Override
                public void onSuccess(@NonNull ScreenshotResult screenshotResult) {
                    Log.i(DEBUG_TAG, "Screenshot captured successfully via native API");
                    try {
                        Bitmap bitmap = Bitmap.wrapHardwareBuffer(screenshotResult.getHardwareBuffer(), screenshotResult.getColorSpace());
                        if (bitmap != null) {
                            // Convert hardware bitmap to software bitmap for OCR
                            latest_screenshot_bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                            bitmap.recycle();
                            onScreenshotReady();
                        } else {
                            Log.e(DEBUG_TAG, "Failed to wrap hardware buffer");
                            tts.speak("Errore cattura", TextToSpeech.QUEUE_ADD, null, "DEFAULT");
                        }
                    } catch (Exception e) {
                        Log.e(DEBUG_TAG, "Error processing native screenshot: " + e.getMessage());
                    } finally {
                        if (screenshotResult.getHardwareBuffer() != null) {
                            screenshotResult.getHardwareBuffer().close();
                        }
                    }
                }

                @Override
                public void onFailure(int errorCode) {
                    Log.e(DEBUG_TAG, "Native screenshot failed with error code: " + errorCode);
                    // Fallback to MediaProjection for modern devices if native fails
                    takeScreenshotFallback();
                }
            });
        } else {
            takeScreenshotFallback();
        }
    }

    private void takeScreenshotFallback() {
        if (mProjectionData != null) {
            Log.i(DEBUG_TAG, "Reusing stored MediaProjection permission");
            Intent serviceIntent = new Intent(this, MediaProjectionService.class);
            serviceIntent.putExtra(MediaProjectionService.EXTRA_RESULT_CODE, mProjectionResultCode);
            serviceIntent.putExtra(MediaProjectionService.EXTRA_DATA, mProjectionData);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } else {
            Log.i(DEBUG_TAG, "Taking screenshot using MediaProjection fallback (asking permission)");
            Intent dialogIntent = new Intent(this, ScreenshotActivity.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(dialogIntent);
        }
    }

    Bitmap loadScreenshotBitmap() {
        File dir = new File(PATH);
        if ( dir.isDirectory() ) {
            String screenshot_filename = PATH + "0.png";
            File screenshot_file = new File(screenshot_filename);

            if( screenshot_file.exists() ) {
                Log.i(DEBUG_TAG, "[loadScreenshotBitmap] Loading from file: " + screenshot_filename);
                Bitmap bitmap = BitmapFactory.decodeFile(screenshot_file.getAbsolutePath());
                if (bitmap == null) {
                    Log.e(DEBUG_TAG, "Decoded bitmap is null");
                }
                return bitmap;
            }
            else {
                Log.w(DEBUG_TAG, "[loadScreenshotBitmap] Screenshot file not found at " + screenshot_filename);
            }
        }
        return null;
    }

    Bitmap resizeBitmap(Bitmap original) {
        int x0_crop = Math.min(x0, x1);
        int y0_crop = Math.min(y0, y1);
        int width_crop = Math.abs(x1 - x0);
        int height_crop = Math.abs(y1 - y0);

        // Basic bounds checking
        if (x0_crop < 0) x0_crop = 0;
        if (y0_crop < 0) y0_crop = 0;
        if (x0_crop >= original.getWidth()) x0_crop = original.getWidth() - 1;
        if (y0_crop >= original.getHeight()) y0_crop = original.getHeight() - 1;

        if (x0_crop + width_crop > original.getWidth()) width_crop = original.getWidth() - x0_crop;
        if (y0_crop + height_crop > original.getHeight()) height_crop = original.getHeight() - y0_crop;
        
        if (width_crop <= 0) width_crop = 1;
        if (height_crop <= 0) height_crop = 1;

        return Bitmap.createBitmap(original, x0_crop, y0_crop, width_crop, height_crop);
    }

    int bitmapToSpeech(Bitmap screenshot_bitmap) {
        if (screenshot_bitmap == null) return ERROR;

        Frame screenshot_frame = new Frame.Builder().setBitmap(screenshot_bitmap).build();
        SparseArray<TextBlock> text = text_recognizer.detect(screenshot_frame);

        if (text.size() > 0) {
            for (int i=0; i<text.size(); ++i) {
                TextBlock item = text.valueAt(i);
                if  (item != null && item.getValue() != null) {
                    String current_string = item.getValue();
                    if (remove_newlines) {
                        current_string = current_string.replace("\n", " ");
                    }
                    String utteranceId = "ocr-" + utteranceCounter++;
                    List<SpokenWord> words = getSpokenWords(item, current_string);
                    if (!words.isEmpty()) {
                        spokenWords.put(utteranceId, words);
                    }
                    tts.speak(current_string, TextToSpeech.QUEUE_ADD, null, utteranceId);
                    Log.i(DEBUG_TAG, "[bitmapToSpeech] Text: " + current_string);
                }
            }
            return SUCCESS;
        }
        else {
            Log.w(DEBUG_TAG, "[bitmapToSpeech] No text found");
            tts.speak("Nessun testo trovato", TextToSpeech.QUEUE_ADD, null, "DEFAULT");
            return ERROR;
        }
    }

    void drawRectangle() {
        if (canvasDrawingPane == null) return;
        canvasDrawingPane.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        canvasDrawingPane.drawRoundRect(x0, y0, current_x, current_y, 25, 25, paint);
        image_view.invalidate();
    }

    private List<SpokenWord> getSpokenWords(TextBlock block, String text) {
        List<SpokenWord> words = new ArrayList<>();
        int searchStart = 0;

        for (com.google.android.gms.vision.text.Text line : block.getComponents()) {
            for (com.google.android.gms.vision.text.Text element : line.getComponents()) {
                String value = element.getValue();
                if (value == null || value.isEmpty()) continue;

                int start = text.indexOf(value, searchStart);
                if (start < 0) continue;
                int end = start + value.length();
                Rect bounds = new Rect(element.getBoundingBox());
                bounds.offset(Math.min(x0, x1), Math.min(y0, y1));
                words.add(new SpokenWord(start, end, bounds));
                searchStart = end;
            }
        }
        return words;
    }

    private void drawSpokenWord(Rect bounds) {
        if (!speech_active || canvasDrawingPane == null || highlightPaint == null) return;
        canvasDrawingPane.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        canvasDrawingPane.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom,
                18, 18, highlightPaint);
        image_view.setVisibility(View.VISIBLE);
        image_view.invalidate();
    }

    private static class SpokenWord {
        final int start;
        final int end;
        final Rect bounds;

        SpokenWord(int start, int end, Rect bounds) {
            this.start = start;
            this.end = end;
            this.bounds = bounds;
        }
    }
}

class MyLog {
    File log_file;
    FileWriter log_file_writer;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");

    MyLog(String PATH) {
        log_file = new File(PATH, dateFormat.format( new Date() ) + "_MyLog.txt");
        try {
            log_file_writer = new FileWriter(log_file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void i(String tag, String msg) {
        log(tag, "I", msg);
    }
    void w(String tag, String msg) {
        log(tag, "W", msg);
    }
    void e(String tag, String msg) {
        log(tag, "E", msg);
    }
    private void log(String tag, String level, String msg) {
        try {
            if (log_file_writer == null) return;
            log_file_writer.append(dateFormat.format(new Date())).append(" ").append(level).append("/").append(tag).append(": ").append(msg).append("\n");
            log_file_writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
