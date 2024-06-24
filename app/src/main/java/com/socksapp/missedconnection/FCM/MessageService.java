package com.socksapp.missedconnection.FCM;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.myclass.SharedPreferencesHelperMessage;
import com.socksapp.missedconnection.myclass.SharedPreferencesHelperPost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class MessageService extends FirebaseMessagingService {
    private final static String CHANNEL_ID = "default";
    public static final String NOTIFICATION_ID = "10001";

    NotificationManagerCompat notificationManagerCompat;


    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        updateNewToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        String title = Objects.requireNonNull(message.getNotification()).getTitle();
        String body = message.getNotification().getBody();
        String senderId = message.getData().get("senderId");

        if (!isAppInForeground(this)) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            long[] pattern = {0, 10, 100, 200};
            vibrator.vibrate(pattern,-1);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this,CHANNEL_ID);

            if(title != null && !title.isEmpty()) {
                builder.setContentTitle(title);
            }else {
                builder.setContentTitle(null);
            }
            builder.setContentText(body);
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(body));
            builder.setGroup(senderId);
            builder.setGroupSummary(true);
            builder.setAutoCancel(true);
            builder.setSmallIcon(R.drawable.icon_notifications_base);
            builder.setVibrate(pattern);
            builder.setPriority(Notification.PRIORITY_MAX);

            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

            String channelID = CHANNEL_ID;
            NotificationChannel channel = new NotificationChannel(
                    channelID,"Coding",NotificationManager.IMPORTANCE_HIGH
            );
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setVibrationPattern(pattern);
            channel.canBypassDnd();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                channel.canBubble();
            }
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(channelID);

            notificationManager.notify((int) System.currentTimeMillis(),builder.build());
        }

    }

    private boolean isAppInForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    processInfo.processName.equals(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private void updateNewToken(String token){

    }
}
