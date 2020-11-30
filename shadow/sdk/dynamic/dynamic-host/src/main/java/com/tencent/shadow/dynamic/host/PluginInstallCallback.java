package com.tencent.shadow.dynamic.host;

public interface PluginInstallCallback {

    void onSuccess(String uuid);

    void onFail(String errMsg);
}
