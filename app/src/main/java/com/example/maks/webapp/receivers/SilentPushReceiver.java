package com.example.maks.webapp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.yandex.metrica.push.YandexMetricaPush;

public class SilentPushReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String payload = intent.getStringExtra(YandexMetricaPush.EXTRA_PAYLOAD);
    }
}
