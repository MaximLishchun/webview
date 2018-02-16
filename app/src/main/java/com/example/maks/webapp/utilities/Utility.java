package com.example.maks.webapp.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;

public class Utility {

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    public static boolean isSimCardExist(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm != null && tm.getSimState() != TelephonyManager.SIM_STATE_ABSENT;
    }

    public static String getCountryCode(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm != null ? tm.getSimCountryIso() : "";
    }
}
