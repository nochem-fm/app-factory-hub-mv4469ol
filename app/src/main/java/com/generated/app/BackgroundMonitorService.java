package com.generated.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackgroundMonitorService extends Service {

    public static final String CHANNEL_ID = "app_factory_bg_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long CHECK_INTERVAL_MS = 60000; // Check every 60 seconds

    private Handler handler;
    private Runnable monitorRunnable;
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        handler = new Handler(Looper.getMainLooper());

        monitorRunnable = new Runnable() {
            @Override
            public void run() {
                checkServerStatus();
                if (isRunning) {
                    handler.postDelayed(this, CHECK_INTERVAL_MS);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = buildForegroundNotification(getString(R.string.notif_text));
        startForeground(NOTIFICATION_ID, notification);

        if (!isRunning) {
            isRunning = true;
            handler.post(monitorRunnable);
        }

        return START_STICKY;
    }

    private void checkServerStatus() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://app-factory-nt5l.onrender.com/");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestMethod("GET");
                    int code = conn.getResponseCode();
                    InputStream is = conn.getInputStream();
                    if (is != null) {
                        is.close();
                    }
                    conn.disconnect();

                    if (code == 200) {
                        updateNotification("האתר מקוון ופעיל כעת");
                    } else {
                        updateNotification("סטטוס שרת: " + code);
                    }
                } catch (Exception ignored) {
                    updateNotification("ממתין לחיבור עם השרת...");
                }
            }
        }).start();
    }

    private void updateNotification(final String statusText) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) {
                    nm.notify(NOTIFICATION_ID, buildForegroundNotification(statusText));
                }
            }
        });
    }

    private Notification buildForegroundNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle(getString(R.string.notif_title))
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.notification_channel_desc));
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (handler != null && monitorRunnable != null) {
            handler.removeCallbacks(monitorRunnable);
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
