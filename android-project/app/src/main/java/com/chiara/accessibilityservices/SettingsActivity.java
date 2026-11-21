package com.chiara.accessibilityservices;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "[Chiara_SettingsActivity]";
    private static final int PERMISSION_REQUEST_CODE_NOTIFICATIONS = 101;

    private TextView tvAuthStatus;
    private Button btnGoogleSignIn;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();

        tvAuthStatus = findViewById(R.id.tv_auth_status);
        btnGoogleSignIn = findViewById(R.id.btn_google_sign_in);

        btnGoogleSignIn.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                signOut();
            } else {
                startGoogleBrowserSignIn();
            }
        });

        // The activity may have been recreated (e.g. killed by the system) while the
        // browser sign-in flow was running: resume a pending result if one exists.
        resumePendingAuthResultIfNeeded();
        updateAuthStatusUI();
        requestNotificationPermissionIfNeeded();

        Button btnOpenAccessibility = findViewById(R.id.btn_open_accessibility);
        btnOpenAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            ComponentName componentName = new ComponentName(getPackageName(), MainService.class.getName());
            String serviceId = componentName.flattenToString();
            intent.putExtra(":settings:fragment_args_key", serviceId);
            intent.putExtra(":settings:show_fragment_args", true);
            startActivity(intent);
        });

        Button btnOpenVolume = findViewById(R.id.btn_open_volume);
        btnOpenVolume.setOnClickListener(v -> {
            Intent intent = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ? new Intent(Settings.Panel.ACTION_VOLUME)
                    : new Intent(Settings.ACTION_SOUND_SETTINGS);
            startActivity(intent);
        });

        Button btnTestCrash = findViewById(R.id.btn_test_crash);
        btnTestCrash.setOnClickListener(v -> {
            throw new RuntimeException("Test Crash - Firebase Crashlytics Verification");
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateAuthStatusUI();
    }

    private void updateAuthStatusUI() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String label = user.getEmail() != null ? user.getEmail() : user.getDisplayName();
            if (label == null || label.isEmpty()) label = user.getUid();
            tvAuthStatus.setText(getString(R.string.auth_status_signed_in, label));
            btnGoogleSignIn.setText(R.string.google_sign_out);
        } else {
            tvAuthStatus.setText(R.string.auth_status_not_signed_in);
            btnGoogleSignIn.setText(R.string.google_sign_in);
        }
    }

    /**
     * Starts the Google sign-in flow through the browser (Chrome Custom Tab) using
     * Firebase's generic OAuth sign-in. This does not require any Google account to be
     * registered on the device, nor any OAuth client ID in the app code.
     */
    private void startGoogleBrowserSignIn() {
        OAuthProvider.Builder provider = OAuthProvider.newBuilder("google.com");

        // Always show the Google account chooser page in the browser.
        provider.addCustomParameter("prompt", "select_account");

        List<String> scopes = new ArrayList<>();
        scopes.add("email");
        scopes.add("profile");
        provider.setScopes(scopes);

        mAuth.startActivityForSignInWithProvider(/* activity= */ this, provider.build())
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "signInWithProvider:success");
                    showToast(getString(R.string.google_sign_in_success));
                    updateAuthStatusUI();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "signInWithProvider:failure", e);
                    try {
                        FirebaseCrashlytics.getInstance().recordException(e);
                    } catch (Exception ignored) {
                    }
                    showToast(getString(R.string.google_sign_in_failed,
                            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
                    updateAuthStatusUI();
                });
    }

    /**
     * Resolves a sign-in that was already started before the activity got recreated.
     */
    private void resumePendingAuthResultIfNeeded() {
        Task<AuthResult> pendingResultTask = mAuth.getPendingAuthResult();
        if (pendingResultTask == null) return;

        pendingResultTask
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "pendingSignInWithProvider:success");
                    showToast(getString(R.string.google_sign_in_success));
                    updateAuthStatusUI();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "pendingSignInWithProvider:failure", e);
                    try {
                        FirebaseCrashlytics.getInstance().recordException(e);
                    } catch (Exception ignored) {
                    }
                    updateAuthStatusUI();
                });
    }

    private void signOut() {
        mAuth.signOut();
        showToast(getString(R.string.google_sign_out_success));
        updateAuthStatusUI();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
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
