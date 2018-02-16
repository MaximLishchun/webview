package com.example.maks.webapp;

import android.support.multidex.MultiDexApplication;
import android.webkit.CookieSyncManager;

import com.onesignal.OneSignal;

import com.yandex.metrica.YandexMetrica;
import com.yandex.metrica.push.YandexMetricaPush;

public class App extends MultiDexApplication {

    public final static String API_key = "eb82b622-6b31-4c0b-826a-f896801af56d";

    @Override
    public void onCreate() {
        super.onCreate();
        YandexMetrica.activate(getApplicationContext(), API_key);
        YandexMetrica.enableActivityAutoTracking(this);
        // push metrica
        YandexMetricaPush.init(getApplicationContext());
        // onesignal
        OneSignal.startInit(getApplicationContext());
        // cookie
        CookieSyncManager.createInstance(getApplicationContext());
    }
}
