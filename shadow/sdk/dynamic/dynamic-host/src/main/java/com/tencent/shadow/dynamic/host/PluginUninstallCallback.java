package com.tencent.shadow.dynamic.host;

public interface PluginUninstallCallback {

    void onSuccess();

    void onFail(String errMsg);

}
