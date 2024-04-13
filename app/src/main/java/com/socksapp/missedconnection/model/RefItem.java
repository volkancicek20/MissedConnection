package com.socksapp.missedconnection.model;

public class RefItem {
    private String ref;
    private String mail;

    public RefItem(String ref, String mail) {
        this.ref = ref;
        this.mail = mail;
    }

    public String getMail() {
        return mail;
    }

    public String getRef() {
        return ref;
    }
}

