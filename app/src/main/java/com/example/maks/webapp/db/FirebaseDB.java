package com.example.maks.webapp.db;

import com.example.maks.webapp.listeners.OnConfigChangeListener;
import com.example.maks.webapp.listeners.OnDatabaseDataChangeListener;
import com.example.maks.webapp.listeners.OnForbiddenFileExtensionChangeListener;
import com.example.maks.webapp.listeners.OnPackagesChangeListener;
import com.example.maks.webapp.models.IpRange;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FirebaseDB implements ValueEventListener {

    private List<String> whitelist_geoIps;
    private List<String> whitelist_localizations;
    private List<String> otherIps;
    private List<String> blackReferer;
    private List<IpRange> ipsRange;
    private List<String> packages;
    private List<String> fileExtensions;
    private ArrayList<String> rules;

    private OnPackagesChangeListener onPackagesChangeListener;
    private OnConfigChangeListener onConfigChangeListener;
    private OnDatabaseDataChangeListener onDatabaseDataChangeListener;
    private OnForbiddenFileExtensionChangeListener onForbiddenFileExtensionChangeListener;

    public FirebaseDB() {
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference();
        myRef.addValueEventListener(this);

        whitelist_geoIps = new ArrayList<>();
        whitelist_localizations = new ArrayList<>();
        otherIps = new ArrayList<>();
        ipsRange = new ArrayList<>();
        packages = new ArrayList<>();
        fileExtensions = new ArrayList<>();
        rules = new ArrayList<>();
        blackReferer = new ArrayList<>();
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        String refer = dataSnapshot.child("refer").getValue(String.class);
        String link = dataSnapshot.child("link").getValue(String.class);
        String gameLink = dataSnapshot.child("appGameLink").getValue(String.class);
        String internalLink = dataSnapshot.child("appInternalLink").getValue(String.class);

        Boolean isLinkEnabled = dataSnapshot.child("isLinkEnabled").getValue(Boolean.class);
        Boolean hideStatus = dataSnapshot.child("hideStatus").getValue(Boolean.class);
        Integer startActivity = dataSnapshot.child("startActivity").getValue(Integer.class);
        String allIps = dataSnapshot.child("whiteListGeoIp").getValue(String.class);
        String allLocalization = dataSnapshot.child("whiteListLocalization").getValue(String.class);
        DataSnapshot otherIp = dataSnapshot.child("otherIp");
        DataSnapshot ipRanges = dataSnapshot.child("blackListIp");
        DataSnapshot packagesFB = dataSnapshot.child("packages");
        DataSnapshot cuttingRules = dataSnapshot.child("cuttingRules");
        DataSnapshot extensions = dataSnapshot.child("forbiddenFileExtensions");
        DataSnapshot blackRefer = dataSnapshot.child("blackListReferer");

        whitelist_geoIps.addAll(Arrays.asList(allIps.split("/")));
        whitelist_localizations.addAll(Arrays.asList(allLocalization.split("/")));

        for(DataSnapshot data : otherIp.getChildren()){
            otherIps.add(data.getValue(String.class));
        }

        for(DataSnapshot data : blackRefer.getChildren()){
            blackReferer.add(data.getValue(String.class));
        }

        for(DataSnapshot data : packagesFB.getChildren()){
            packages.add(data.getValue(String.class));
        }

        for(DataSnapshot data : extensions.getChildren()) {
            fileExtensions.add(data.getValue(String.class));
        }

        for(DataSnapshot data : cuttingRules.getChildren()) {
            rules.add(data.getValue(String.class));
        }

        for(DataSnapshot data : ipRanges.getChildren()){
            GenericTypeIndicator<Map<String, String>> indicator = new GenericTypeIndicator<Map<String, String>>() {};
            Map<String, String> object = data.getValue(indicator);
            ipsRange.add(new IpRange(object.get("start"), object.get("end")));
        }

        if(onDatabaseDataChangeListener != null)
            onDatabaseDataChangeListener.updateDate(whitelist_geoIps, whitelist_localizations,
                    otherIps, blackReferer,rules, ipsRange, link, internalLink, gameLink, refer, isLinkEnabled);

        if(onConfigChangeListener != null)
            onConfigChangeListener.onConfigChanged(startActivity);

        if(onPackagesChangeListener != null)
            onPackagesChangeListener.onPackagesChanged(packages, hideStatus);

        if(onForbiddenFileExtensionChangeListener != null)
            onForbiddenFileExtensionChangeListener.onForbiddenFileExtensionChange(fileExtensions);
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {}

    public void setOnDatabaseDataChangeListener(OnDatabaseDataChangeListener listener){
        this.onDatabaseDataChangeListener = listener;
    }

    public void setOnConfigChangeListener(OnConfigChangeListener onConfigChangeListener) {
        this.onConfigChangeListener = onConfigChangeListener;
    }

    public void setOnPackagesChangeListener(OnPackagesChangeListener onPackagesChangeListener) {
        this.onPackagesChangeListener = onPackagesChangeListener;
    }

    public void setOnForbiddenFileExtensionChangeListener(OnForbiddenFileExtensionChangeListener onForbiddenFileExtensionChangeListener) {
        this.onForbiddenFileExtensionChangeListener = onForbiddenFileExtensionChangeListener;
    }
}
