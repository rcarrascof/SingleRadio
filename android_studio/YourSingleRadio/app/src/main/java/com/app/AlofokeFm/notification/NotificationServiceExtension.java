package com.app.AlofokeFm.notification;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;

import com.app.AlofokeFm.R;
import com.onesignal.OSMutableNotification;
import com.onesignal.OSNotification;
import com.onesignal.OSNotificationReceivedEvent;
import com.onesignal.OneSignal;

@SuppressWarnings("unused")
public class NotificationServiceExtension implements OneSignal.OSRemoteNotificationReceivedHandler {

    Activity activity;
    public static final int NOTIFICATION_ID = 2;
    private NotificationManager mNotificationManager;
    String message, bigpicture, title, cname, url;
    long nid;
    private String NOTIFICATION_CHANNEL_ID = "your_radio_app_notification_channel_001";

    @Override
    public void remoteNotificationReceived(Context context, OSNotificationReceivedEvent notificationReceivedEvent) {
        OneSignal.onesignalLog(OneSignal.LOG_LEVEL.VERBOSE, "OSRemoteNotificationReceivedHandler fired!" +
                " with OSNotificationReceived: " + notificationReceivedEvent.toString());

        OSNotification notification = notificationReceivedEvent.getNotification();

        if (notification.getActionButtons() != null) {
            for (OSNotification.ActionButton button : notification.getActionButtons()) {
                OneSignal.onesignalLog(OneSignal.LOG_LEVEL.VERBOSE, "ActionButton: " + button.toString());
            }
        }

        OSMutableNotification mutableNotification = notification.mutableCopy();
        mutableNotification.setExtender(builder -> builder.setColor(context.getResources().getColor(R.color.colorPrimary)));

        // If complete isn't call within a time period of 25 seconds, OneSignal internal logic will show the original notification
        notificationReceivedEvent.complete(mutableNotification);
    }

}