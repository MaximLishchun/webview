package com.example.maks.webapp.models;

public class IpRange {

    private String start;
    private String end;

    public IpRange(String start, String end) {
        this.start = start;
        this.end = end;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }
}
