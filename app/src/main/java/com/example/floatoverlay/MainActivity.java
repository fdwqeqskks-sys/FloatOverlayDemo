package com.example.floatoverlay;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int OVERLAY_PERMISSION_REQUEST = 1001;
    private static final String PREFS_NAME = "overlay_preferences";
    private static final String PREF_ADDRESS = "float_address";

    private EditText addressInput;
    private TextView resultView;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        setContentView(createContentView());

        long savedAddress = preferences.getLong(PREF_ADDRESS, 0L);
        if (savedAddress != 0L) {
            addressInput.setText(formatAddress(savedAddress));
        }
    }

    private ScrollView createContentView() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(245, 247, 248));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(24), dp(40), dp(24), dp(32));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("Float Overlay");
        title.setTextSize(28f);
        title.setTextColor(Color.rgb(32, 33, 36));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(title, matchWrap(dp(0)));

        TextView addressLabel = new TextView(this);
        addressLabel.setText("Address (hex)");
        addressLabel.setTextSize(14f);
        addressLabel.setTextColor(Color.rgb(80, 86, 90));
        content.addView(addressLabel, matchWrap(dp(32)));

        addressInput = new EditText(this);
        addressInput.setSingleLine(true);
        addressInput.setHint("0x00000000");
        addressInput.setTextSize(18f);
        addressInput.setTypeface(Typeface.MONOSPACE);
        addressInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        content.addView(addressInput, matchWrap(dp(4)));

        Button sampleButton = createButton("Create demo address");
        sampleButton.setOnClickListener(view -> createDemoAddress());
        content.addView(sampleButton, matchWrap(dp(20)));

        Button applyButton = createButton("Apply address");
        applyButton.setOnClickListener(view -> saveAddress());
        content.addView(applyButton, matchWrap(dp(8)));

        Button readButton = createButton("Read once");
        readButton.setOnClickListener(view -> readOnce());
        content.addView(readButton, matchWrap(dp(8)));

        resultView = new TextView(this);
        resultView.setText("Value: --");
        resultView.setTextSize(22f);
        resultView.setTextColor(Color.rgb(0, 105, 92));
        resultView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        resultView.setGravity(Gravity.CENTER);
        resultView.setPadding(dp(8), dp(20), dp(8), dp(20));
        content.addView(resultView, matchWrap(dp(8)));

        Button startButton = createButton("Start overlay");
        startButton.setOnClickListener(view -> requestPermissionAndStart());
        content.addView(startButton, matchWrap(dp(8)));

        Button stopButton = createButton("Stop overlay");
        stopButton.setOnClickListener(view -> stopService(
                new Intent(this, OverlayService.class)));
        content.addView(stopButton, matchWrap(dp(8)));

        return scrollView;
    }

    private Button createButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(15f);
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        return button;
    }

    private LinearLayout.LayoutParams matchWrap(int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = topMargin;
        return params;
    }

    private void createDemoAddress() {
        long address = MemoryReader.createSample(123.456f);
        addressInput.setText(formatAddress(address));
        preferences.edit().putLong(PREF_ADDRESS, address).apply();
        readAddress(address);
    }

    private void saveAddress() {
        try {
            long address = parseAddress();
            preferences.edit().putLong(PREF_ADDRESS, address).apply();
            Toast.makeText(this, "Address applied", Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException exception) {
            showInputError(exception.getMessage());
        }
    }

    private void readOnce() {
        try {
            long address = parseAddress();
            preferences.edit().putLong(PREF_ADDRESS, address).apply();
            readAddress(address);
        } catch (IllegalArgumentException exception) {
            showInputError(exception.getMessage());
        }
    }

    private void readAddress(long address) {
        try {
            float value = MemoryReader.readFloat(address);
            resultView.setText(String.format(Locale.US, "Value: %.6f", value));
        } catch (IllegalArgumentException exception) {
            resultView.setText("Value: read error");
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private long parseAddress() {
        String text = addressInput.getText().toString().trim();
        if (text.startsWith("0x") || text.startsWith("0X")) {
            text = text.substring(2);
        }
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Enter a hexadecimal address");
        }

        try {
            long address = Long.parseLong(text, 16);
            if (address <= 0L) {
                throw new NumberFormatException();
            }
            return address;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid hexadecimal address");
        }
    }

    private void showInputError(String message) {
        addressInput.setError(message);
        addressInput.requestFocus();
    }

    private void requestPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST);
            return;
        }
        startOverlayService();
    }

    private void startOverlayService() {
        Intent intent = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                    || Settings.canDrawOverlays(this)) {
                startOverlayService();
            } else {
                Toast.makeText(this, "Overlay permission was not granted",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private String formatAddress(long address) {
        return "0x" + Long.toHexString(address).toUpperCase(Locale.US);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
