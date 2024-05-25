package com.socksapp.missedconnection.model;

import com.google.firebase.Timestamp;

public class PostNotification {
    public static final int LAYOUT_ONE = 1;
    public static final int LAYOUT_EMPTY = 2;
    public int viewType;
    public String imageUrl;
    public String galleryUrl;
    public String name;
    public String other_name;
    public String action_explain;
    public String mail;
    public String city;
    public String district;
    public String explain;
    public Timestamp timestamp;
    public Timestamp timestamp2;

    public PostNotification(){

    }

    public int getViewType() {
        return viewType;
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getGalleryUrl() {
        return galleryUrl;
    }

    public void setGalleryUrl(String galleryUrl) {
        this.galleryUrl = galleryUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOther_name() {
        return other_name;
    }

    public void setOther_name(String other_name) {
        this.other_name = other_name;
    }

    public String getAction_explain() {
        return action_explain;
    }

    public void setAction_explain(String action_explain) {
        this.action_explain = action_explain;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
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

    public Timestamp getTimestamp2() {
        return timestamp2;
    }

    public void setTimestamp2(Timestamp timestamp2) {
        this.timestamp2 = timestamp2;
    }
}
