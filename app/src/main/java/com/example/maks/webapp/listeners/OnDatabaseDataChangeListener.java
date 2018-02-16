package com.example.maks.webapp.listeners;

import com.example.maks.webapp.models.IpRange;

import java.util.ArrayList;
import java.util.List;

public interface OnDatabaseDataChangeListener {

    void updateDate(List<String> whitelist_geoIps, List<String> whitelist_localizations, List<String> otherIps, List<String> blackRefer, ArrayList<String> rules,
                    List<IpRange> ipsRange,  String link, String internalLink, String gameLink, String refer, Boolean isLinkEnabled);
}
