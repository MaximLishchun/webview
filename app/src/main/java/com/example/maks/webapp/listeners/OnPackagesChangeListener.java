package com.example.maks.webapp.listeners;

import java.util.List;

public interface OnPackagesChangeListener {

    void onPackagesChanged(List<String> packages, boolean hideStatus);
}
