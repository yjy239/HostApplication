package com.yjy.hostapplication;

import android.app.Application;

/**
 * <pre>
 *     author : yjy
 *     e-mail : yujunyu12@gmail.com
 *     time   : 2018/08/17
 *     desc   :
 *     version: 1.0
 * </pre>
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        HookManager.init(this);
        HookManager.loadPlugin(this);
        HookManager.hookLaunchActivity();
    }
}
