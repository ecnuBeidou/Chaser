package com.agenthun.chaser;

import android.app.Application;

import com.agenthun.chaser.utils.update.UpdateConfig;


/**
 * @project ESeal
 * @authors agenthun
 * @date 16/3/4 上午6:48.
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        UpdateConfig.initGet(this);
    }

}
