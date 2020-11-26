package com.clark.learn.learn_shadow;

import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.clark.learn.host_constant.Constant;
import com.clark.learn.host_lib.LoadPluginCallback;
import com.clark.learn.host_lib.IHostToast;
import com.tencent.shadow.dynamic.host.EnterCallback;

public class MainActivity extends FragmentActivity {

    private static final String TAG = "HOST_MAIN";

    private ClassLoader mPluginClassLoader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LoadPluginCallback.setCallback(new LoadPluginCallback.Callback() {
            @Override
            public void beforeLoadPlugin(String partKey) {
                Log.d(TAG, "beforeLoadPlugin(" + partKey + ")");
            }

            @Override
            public void afterLoadPlugin(String partKey, ApplicationInfo applicationInfo, ClassLoader pluginClassLoader, Resources pluginResources) {
                mPluginClassLoader = pluginClassLoader;
                Log.d(TAG, "afterLoadPlugin(" + partKey + "," + applicationInfo.className + "{metaData=" + applicationInfo.metaData + "}" + "," + pluginClassLoader + ")");
            }
        });

        findViewById(R.id.btn1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PluginHelper.getInstance().singlePool.execute(new Runnable() {
                    @Override
                    public void run() {
                        HostApplication.getApp().loadPluginManager(PluginHelper.getInstance().pluginManagerFile);


                        Bundle bundle = new Bundle();
                        bundle.putString(Constant.KEY_PLUGIN_ZIP_PATH, PluginHelper.getInstance().pluginZipFile.getAbsolutePath());
                        bundle.putString(Constant.KEY_PLUGIN_PART_KEY, Constant.PART_KEY_PLUGIN_MAIN_APP);
                        bundle.putString(Constant.KEY_ACTIVITY_CLASSNAME, "com.clark.learn.plugin_app.MainActivity");

                        HostApplication.getApp().getPluginManager()
                                .enter(MainActivity.this, Constant.FROM_ID_START_ACTIVITY, bundle, new EnterCallback() {
                                    @Override
                                    public void onShowLoadingView(final View view) {
                                    }

                                    @Override
                                    public void onCloseLoadingView() {
                                    }

                                    @Override
                                    public void onEnterComplete() {
                                    }
                                });
                    }
                });
            }
        });

        findViewById(R.id.btn2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mPluginClassLoader!=null){
                    try {
                        Class<?> aClass = mPluginClassLoader.loadClass("com.clark.learn.plugin_app.HostToastImpl");
                        IHostToast iHostToast = aClass.asSubclass(IHostToast.class).newInstance();
                        iHostToast.showToast("我是宿主要弹的东西");
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }
}
