package com.socksapp.missedconnection.FCM;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FCMNotificationSender {
    private String userFcmToken;
    private String title;
    private String body;
    private Context context;
    private String postUrl = "https://fcm.googleapis.com/v1/projects/missedconnection-c000f/messages:send";

    public FCMNotificationSender(String userFcmToken,String title,String body,Context context){
        this.userFcmToken = userFcmToken;
        this.title = title;
        this.body = body;
        this.context = context;
    }

    public void SendNotification(){
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JSONObject mainObj = new JSONObject();
        try {
            JSONObject messageObject = new JSONObject();

            JSONObject notificationObject = new JSONObject();
            notificationObject.put("title",title);
            notificationObject.put("body",body);

            messageObject.put("token",userFcmToken);
            messageObject.put("notification",notificationObject);

            mainObj.put("message",messageObject);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,postUrl,mainObj, response -> {

            }, volleyError -> {

            }) {
                @NonNull
                @Override
                public Map<String, String> getHeaders(){
                    AccessToken accessToken = new AccessToken();
                    String accessKey = accessToken.getAccessToken();
                    Map<String,String> header = new HashMap<>();
                    header.put("content-type","application/json");
                    header.put("authorization","Bearer " + accessKey);
                    return  header;
                }
            };

            requestQueue.add(request);

        }catch (Exception e){
            e.printStackTrace();
        }

    }

}
