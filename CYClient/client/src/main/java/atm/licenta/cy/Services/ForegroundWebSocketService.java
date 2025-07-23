package atm.licenta.cy.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import atm.licenta.cy.R;
import atm.licenta.cy.WebSockets.WebSocketClientManager;

public class ForegroundWebSocketService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannels(this);

        Notification notification = new NotificationCompat.Builder(this, "foreground_service")
                .setSmallIcon(R.drawable.ic_chat_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSilent(true)
                .build();

        startForeground(1, notification);

        String uid = intent.getStringExtra("uid");
        WebSocketClientManager.getInstance().init(this);
        WebSocketClientManager.getInstance().connect(uid);

        return START_STICKY;
    }

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            NotificationChannelGroup group = new NotificationChannelGroup(
                    "hidden_service_group", "Hidden Background Group");
            manager.createNotificationChannelGroup(group);

            NotificationChannel foregroundChannel = new NotificationChannel(
                    "foreground_service",
                    "Background Connection",
                    NotificationManager.IMPORTANCE_HIGH
            );
            foregroundChannel.setDescription("Used internally for WebSocket service");
            foregroundChannel.setGroup("hidden_service_group");
            manager.createNotificationChannel(foregroundChannel);

            NotificationChannel chatChannel = new NotificationChannel(
                    "chat_channel",
                    "Chat Messages",
                    NotificationManager.IMPORTANCE_HIGH
            );
            chatChannel.setDescription("CY Message Notification");
            chatChannel.enableLights(true);
            chatChannel.enableVibration(true);
            manager.createNotificationChannel(chatChannel);
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
