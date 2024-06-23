package com.socksapp.missedconnection.model;

import java.util.Date;

public class ChatMessage {
    public int viewType;
    public String senderId, receiverId, message, dateTime;
    public Date dateObject;
    public String conversionId,conversionName,conversionImage;

    public Boolean loadMoreMessages;

    public int getViewType() {
        return viewType;
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
    }
}
