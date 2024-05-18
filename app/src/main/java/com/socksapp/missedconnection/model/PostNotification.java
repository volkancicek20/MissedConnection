package com.socksapp.missedconnection.model;

import com.google.firebase.Timestamp;

public class PostNotification {
    public static final int LAYOUT_ONE = 1;
    public static final int LAYOUT_EMPTY = 2;
    public int viewType;
    public String name;
    public String explain;
    public Timestamp timestamp;
    public String city;
    public String district;
    public String explain_post;

    public int getViewType() {
        return viewType;
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExplain() {
        return explain;
    }

    public void setExplain(String explain) {
        this.explain = explain;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getExplain_post() {
        return explain_post;
    }

    public void setExplain_post(String explain_post) {
        this.explain_post = explain_post;
    }
}
