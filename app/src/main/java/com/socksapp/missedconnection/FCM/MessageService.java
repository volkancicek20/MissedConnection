package com.socksapp.missedconnection.FCM;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Vibrator;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MessageService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "Notification";
    private static final int NOTIFICATION_ID = 100;
    private NotificationManager notificationManager;
    private Map<String, NotificationCompat.Builder> groupedNotifications;

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        updateNewToken(token);
    }


    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        if (!isAppInForeground(this)) {

//            if (groupedNotifications == null) {
//                groupedNotifications = new HashMap<>();
//            }

            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            long[] pattern = {0, 10, 100, 200};
            vibrator.vibrate(pattern,-1);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this,CHANNEL_ID);

            Intent resultIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent;
            pendingIntent = PendingIntent.getActivity(this,1,resultIntent,PendingIntent.FLAG_IMMUTABLE); //özel bir yere gitmek için kullan
            builder.setContentTitle(Objects.requireNonNull(message.getNotification()).getTitle());
            builder.setContentText(message.getNotification().getBody());
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message.getNotification().getBody()));
            builder.setAutoCancel(true);
            builder.setSmallIcon(R.drawable.icon_notifications_base);
            builder.setVibrate(pattern);
            builder.setPriority(Notification.PRIORITY_MAX);

            notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
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
            }
            notificationManager.notify(NOTIFICATION_ID,builder.build());
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
