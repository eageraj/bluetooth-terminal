package com.aes_pl.simple_bluetooth_terminal;

class Constants {

    // values have to be globally unique
    static final String INTENT_ACTION_DISCONNECT = BuildConfig.APPLICATION_ID + ".Disconnect";
    static final String NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID + ".Channel";
    static final String INTENT_CLASS_MAIN_ACTIVITY = BuildConfig.APPLICATION_ID + ".MainActivity";

    static final String CLOSE_RELAY =  "A0 01 01 A2";
    static final String OPEN_RELAY = "A0 01 00 A1";

    // values have to be unique within each app
    static final int NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001;
    static final long PERIODIC_INTERVAL_MS = 15 * 60 * 1000L;

    private Constants() {}
}
