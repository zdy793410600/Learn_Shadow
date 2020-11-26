package com.clark.learn.plugin_app;

import android.app.Application;

public class PluginApplication extends Application {

    private static PluginApplication sApp;

    @Override
    public void onCreate() {
        super.onCreate();
        sApp = this;
    }

    public static PluginApplication getApp() {
        return sApp;
    }

}
