package com.aes_pl.simple_bluetooth_terminal;

import static java.lang.Thread.sleep;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class PeriodicSender {

    private static final long INTERVAL_MS = 1 * 60 * 1000L;

    private final SerialService service;
    private final Runnable onSent;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> future;

    PeriodicSender(SerialService service, Runnable onSent) {
        this.service = service;
        this.onSent = onSent;
    }

    void start() {
        future = scheduler.scheduleWithFixedDelay(this::send, initialDelay(), INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private long initialDelay() {
        long now = System.currentTimeMillis();
        return INTERVAL_MS - (now % INTERVAL_MS);
    }

    void stop() {
        if (future != null) future.cancel(false);
    }

    private void send() {
        long next = INTERVAL_MS - (System.currentTimeMillis() % INTERVAL_MS);
        try {
            service.write(hexToBytes(Constants.CLOSE_RELAY));
            sleep(1000);
            service.write(hexToBytes(Constants.OPEN_RELAY));
            if (onSent != null) onSent.run();
        } catch (Exception ignored) {
        }
        // reschedule: cancel current and restart with corrected delay
        if (future != null) future.cancel(false);
        future = scheduler.scheduleWithFixedDelay(this::send, next, INTERVAL_MS, TimeUnit.MILLISECONDS);
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
