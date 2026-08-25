package com.chiara.accessibilityservices;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button btnOpenAccessibility = findViewById(R.id.btn_open_accessibility);
        btnOpenAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                
                // Construct the component name for the accessibility service
                ComponentName componentName = new ComponentName(getPackageName(), MainService.class.getName());
                String serviceId = componentName.flattenToString();
                
                // These extras are often used by the Settings app to highlight or jump to a specific service
                intent.putExtra(":settings:fragment_args_key", serviceId);
                intent.putExtra(":settings:show_fragment_args", true);
                
                // Fallback for some Android versions to open the specific service settings
                // However, ACTION_ACCESSIBILITY_SETTINGS is safer to ensure they see the toggle.

                startActivity(intent);
            }
        });
    }
}
