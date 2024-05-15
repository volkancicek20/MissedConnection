package com.socksapp.missedconnection.model;

public class ChatsId {
    private String mail;
    private String id;

    public ChatsId(String mail, String id) {
        this.mail = mail;
        this.id = id;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

