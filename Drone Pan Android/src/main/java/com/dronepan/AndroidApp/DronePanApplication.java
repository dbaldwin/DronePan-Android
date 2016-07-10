package com.dronepan.AndroidApp;

import android.app.Application;

import timber.log.Timber;

/**
 * Created by jason on 7/10/16.
 */

public class DronePanApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }


}
