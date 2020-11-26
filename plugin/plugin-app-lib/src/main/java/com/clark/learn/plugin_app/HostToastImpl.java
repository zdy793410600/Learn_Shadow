package com.clark.learn.plugin_app;

import android.widget.Toast;

import com.clark.learn.host_lib.IHostToast;

public class HostToastImpl implements IHostToast {

    @Override
    public void showToast(String s) {
        Toast.makeText(PluginApplication.getApp(), "plugin show:" + s, Toast.LENGTH_SHORT).show();
    }

}
