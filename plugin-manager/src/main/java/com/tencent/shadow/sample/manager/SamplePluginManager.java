/*
 * Tencent is pleased to support the open source community by making Tencent Shadow available.
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.tencent.shadow.sample.manager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.clark.learn.host_constant.Constant;
import com.tencent.shadow.core.manager.installplugin.InstalledPlugin;
import com.tencent.shadow.dynamic.host.EnterCallback;
import com.tencent.shadow.dynamic.host.PluginInstallCallback;
import com.tencent.shadow.dynamic.host.PluginUninstallCallback;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SamplePluginManager extends FastPluginManager {

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Map<String, InstalledPlugin> mInstalledPlugin = new HashMap<>();

    private Context mCurrentContext;

    public SamplePluginManager(Context context) {
        super(context);
        mCurrentContext = context;
    }

    /**
     * @return PluginManager实现的别名，用于区分不同PluginManager实现的数据存储路径
     */
    @Override
    protected String getName() {
        return "test-dynamic-manager";
    }

    /**
     * @return 宿主so的ABI。插件必须和宿主使用相同的ABI。
     */
    @Override
    public String getAbi() {
        return "";
    }

    /**
     * @return 宿主中注册的PluginProcessService实现的类名
     */
    @Override
    protected String getPluginProcessServiceName(String partKey) {
        if ("plugin-app".equals(partKey)) {
            return "com.clark.learn.learn_shadow.plugigservice.PluginProcessPPS";
        } else {
            //如果有默认PPS，可用return代替throw
            throw new IllegalArgumentException("unexpected plugin load request: " + partKey);
        }
    }

    @Override
    public void installPlugin(Context context, final String zip, final String hash, final boolean odex, final PluginInstallCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    InstalledPlugin installedPlugin = installPluginFromZip(zip, hash, odex);
                    if (installedPlugin == null) {
                        if (callback != null) {
                            callback.onFail("SamplePluginManager.installPlugin() -> error:installedPlugin==null");
                        }
                    } else {
                        mInstalledPlugin.put(installedPlugin.UUID, installedPlugin);
                        if (callback != null) {
                            callback.onSuccess(installedPlugin.UUID);
                        }
                        //安装完成之后，删除之前安装的plugin插件。
                        //deleteOldPlugin(installedPlugin.UUID);
                    }
                } catch (Exception e) {
                    if (callback != null) {
                        callback.onFail("SamplePluginManager.installPlugin() -> error:e=" + e.getLocalizedMessage());
                    }
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void uninstallPlugin(final String uuid, final PluginUninstallCallback uninstallCallback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                InstalledPlugin installedPlugin = getInstalledPlugin(uuid);
                if (installedPlugin != null) {
                    try {
                        //删除runtime
                        if (installedPlugin.runtimeFile.libraryDir.exists()) {
                            FileUtils.deleteDirectory(installedPlugin.runtimeFile.libraryDir);
                        }
                        if (installedPlugin.runtimeFile.oDexDir.exists()) {
                            FileUtils.deleteDirectory(installedPlugin.runtimeFile.oDexDir);
                        }
                        //删除loader
                        if (installedPlugin.pluginLoaderFile.libraryDir.exists()) {
                            FileUtils.deleteDirectory(installedPlugin.runtimeFile.libraryDir);
                        }
                        if (installedPlugin.pluginLoaderFile.oDexDir.exists()) {
                            FileUtils.deleteDirectory(installedPlugin.runtimeFile.oDexDir);
                        }
                        //删除插件
                        for (InstalledPlugin.PluginPart pluginPart : installedPlugin.plugins.values()) {
                            if (pluginPart == null) {
                                continue;
                            }
                            String absolutePath = pluginPart.pluginFile.getAbsolutePath();
                            String pluginFileDir = absolutePath.substring(0, absolutePath.lastIndexOf("/"));
                            pluginFileDir = absolutePath.substring(0, pluginFileDir.lastIndexOf("/"));

                            File file = new File(pluginFileDir);
                            if (file.exists()) {
                                FileUtils.deleteDirectory(file);
                            }
                            //删除完文件，再从数据库中删除
                            deleteInstalledPlugin(uuid);
                            mInstalledPlugin.remove(uuid);
                            if (uninstallCallback != null) {
                                uninstallCallback.onSuccess();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (uninstallCallback != null) {
                            uninstallCallback.onFail(e.getLocalizedMessage());
                        }
                    }
                }
            }
        });
    }

    @Override
    public void enter(final Context context, String uuid, long fromId, Bundle bundle, final EnterCallback callback) {
        if (fromId == Constant.FROM_ID_NOOP) {
            //do nothing.
        } else if (fromId == Constant.FROM_ID_START_ACTIVITY) {
            onStartActivity(context, uuid, bundle, callback);
        } else {
            throw new IllegalArgumentException("不认识的fromId==" + fromId);
        }
    }

    /**
     * 删除老的插件
     *
     * @param newPluginUUID
     */
    private void deleteOldPlugin(String newPluginUUID) {
        List<InstalledPlugin> installedPlugins = getInstalledPlugins(100);
        if (installedPlugins != null) {
            for (InstalledPlugin installedPlugin : installedPlugins) {
                if (installedPlugin == null) {
                    continue;
                }
                if (installedPlugin.UUID.equals(newPluginUUID)) {
                    continue;
                }
                uninstallPlugin(installedPlugin.UUID, null);
            }
        }
    }

    private void onStartActivity(final Context context, final String uuid, Bundle bundle, final EnterCallback callback) {
        final String partKey = bundle.getString(Constant.KEY_PLUGIN_PART_KEY);
        final String className = bundle.getString(Constant.KEY_ACTIVITY_CLASSNAME);
        if (className == null) {
            throw new NullPointerException("className == null");
        }
        final Bundle extras = bundle.getBundle(Constant.KEY_EXTRAS);

        if (callback != null) {
            final View view = LayoutInflater.from(mCurrentContext).inflate(R.layout.activity_load_plugin, null);
            callback.onShowLoadingView(view);
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    InstalledPlugin installedPlugin = mInstalledPlugin.get(uuid);
                    if (installedPlugin != null) {
                        Intent pluginIntent = new Intent();
                        pluginIntent.setClassName(
                                context.getPackageName(),
                                className
                        );
                        if (extras != null) {
                            pluginIntent.replaceExtras(extras);
                        }
                        startPluginActivity(installedPlugin, partKey, pluginIntent);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (callback != null) {
                    callback.onCloseLoadingView();
                }
            }
        });
    }
}
