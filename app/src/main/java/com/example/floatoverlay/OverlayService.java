package com.example.floatoverlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class OverlayService extends Service {
    private static final String CHANNEL_ID = "float_overlay_channel";
    private static final int NOTIFICATION_ID = 29;
    private static final String PREFS_NAME = "overlay_preferences";
    private static final String PREF_ADDRESS = "float_address";
    private static final long REFRESH_INTERVAL_MS = 500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private View overlayView;
    private TextView addressView;
    private TextView valueView;
    private SharedPreferences preferences;

    private final Runnable refreshTask = new Runnable() {
        @Override
        public void run() {
            refreshValue();
            handler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        startInForeground();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        showOverlay();
        handler.post(refreshTask);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startInForeground() {
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.notification_title));
            manager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(this, MainActivity.class);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openIntent, pendingFlags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        Notification notification = builder
                .setContentTitle(getString(R.string.notification_title))
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void showOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(235, 30, 32, 34));
        background.setCornerRadius(dp(8));
        background.setStroke(dp(1), Color.rgb(77, 182, 172));
        panel.setBackground(background);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(header, new LinearLayout.LayoutParams(
                dp(220), dp(38)));

        TextView title = new TextView(this);
        title.setText("FLOAT MONITOR");
        title.setTextColor(Color.WHITE);
        title.setTextSize(14f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        title.setGravity(Gravity.CENTER_VERTICAL);

        Button closeButton = new Button(this);
        closeButton.setText("X");
        closeButton.setTextColor(Color.WHITE);
        closeButton.setTextSize(13f);
        closeButton.setPadding(0, 0, 0, 0);
        closeButton.setMinWidth(0);
        closeButton.setMinHeight(0);
        closeButton.setBackgroundColor(Color.TRANSPARENT);
        closeButton.setOnClickListener(view -> stopSelf());
        header.addView(closeButton, new LinearLayout.LayoutParams(dp(38), dp(38)));

        addressView = new TextView(this);
        addressView.setText("Address: --");
        addressView.setTextColor(Color.rgb(189, 193, 198));
        addressView.setTextSize(12f);
        addressView.setTypeface(Typeface.MONOSPACE);
        addressView.setSingleLine(true);
        panel.addView(addressView, new LinearLayout.LayoutParams(dp(220), dp(28)));
        addressView.setGravity(Gravity.CENTER_VERTICAL);

        valueView = new TextView(this);
        valueView.setText("Value: --");
        valueView.setTextColor(Color.rgb(128, 203, 196));
        valueView.setTextSize(20f);
        valueView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        valueView.setSingleLine(true);
        panel.addView(valueView, new LinearLayout.LayoutParams(dp(220), dp(42)));
        valueView.setGravity(Gravity.CENTER_VERTICAL);

        int windowType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                dp(244),
                WindowManager.LayoutParams.WRAP_CONTENT,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(16);
        params.y = dp(96);

        attachDragHandler(panel, params);
        windowManager.addView(panel, params);
        overlayView = panel;
    }

    private void attachDragHandler(View view, WindowManager.LayoutParams params) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float touchX;
            private float touchY;

            @Override
            public boolean onTouch(View touchedView, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + Math.round(event.getRawX() - touchX);
                        params.y = initialY + Math.round(event.getRawY() - touchY);
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        touchedView.performClick();
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private void refreshValue() {
        if (addressView == null || valueView == null) {
            return;
        }

        long address = preferences.getLong(PREF_ADDRESS, 0L);
        if (address == 0L) {
            addressView.setText("Address: --");
            valueView.setText("Value: --");
            return;
        }

        addressView.setText("0x" + Long.toHexString(address).toUpperCase(Locale.US));
        try {
            float value = MemoryReader.readFloat(address);
            valueView.setText(String.format(Locale.US, "Value: %.6f", value));
        } catch (IllegalArgumentException exception) {
            valueView.setText("Value: read error");
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(refreshTask);
        if (windowManager != null && overlayView != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (IllegalArgumentException ignored) {
                // The view was already detached by the window manager.
            }
        }
        overlayView = null;
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
