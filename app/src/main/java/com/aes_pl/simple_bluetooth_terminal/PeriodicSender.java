package com.aes_pl.simple_bluetooth_terminal;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;

import androidx.core.content.ContextCompat;

import java.util.concurrent.Executors;

class PeriodicSender {

    private static final String ACTION_SEND = BuildConfig.APPLICATION_ID + ".PeriodicSend";

    private final Context context;
    private final SerialService service;
    private final Runnable onSent;
    private final AlarmManager alarmManager;
    private final PendingIntent pendingIntent;
    private final PowerManager.WakeLock wakeLock;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            wakeLock.acquire(10_000L); // 10s max, covers write + sleep(1000) + write
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    service.write(hexToBytes(Constants.CLOSE_RELAY));
                    Thread.sleep(1000);
                    service.write(hexToBytes(Constants.OPEN_RELAY));
                    if (onSent != null) onSent.run();
                } catch (Exception ignored) {
                } finally {
                    if (wakeLock.isHeld()) wakeLock.release();
                }
                scheduleNext();
            });
        }
    };

    PeriodicSender(Context context, SerialService service, Runnable onSent) {
        this.context = context;
        this.service = service;
        this.onSent = onSent;
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ACTION_SEND).setPackage(context.getPackageName());
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        wakeLock = ((PowerManager) context.getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, BuildConfig.APPLICATION_ID + ":PeriodicSender");
    }

    void start() {
        ContextCompat.registerReceiver(context, receiver, new IntentFilter(ACTION_SEND), ContextCompat.RECEIVER_NOT_EXPORTED);
        scheduleNext();
    }

    void stop() {
        alarmManager.cancel(pendingIntent);
        try { context.unregisterReceiver(receiver); } catch (Exception ignored) {}
        if (wakeLock.isHeld()) wakeLock.release();
    }

    private void scheduleNext() {
        long now = System.currentTimeMillis();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        long interval = (hour >= 23 || hour < 8) ? Constants.NIGHTTIME_PERIODIC_INTERVAL_MS : Constants.DAYTIME_PERIODIC_INTERVAL_MS;
        long next = now - (now % interval) + interval;
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pendingIntent);
    }

    private static byte[] hexToBytes(String inhex) {
        String hex = inhex.toUpperCase().replace(" ", "");
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) | Character.digit(hex.charAt(i + 1), 16));
        return data;
    }
}
