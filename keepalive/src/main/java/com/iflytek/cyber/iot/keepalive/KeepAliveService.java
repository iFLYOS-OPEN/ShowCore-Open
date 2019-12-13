package com.iflytek.cyber.iot.keepalive;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import java.util.concurrent.TimeUnit;

public class KeepAliveService extends Service {
    public static final String KEEP_ALIVE_ACTION = "com.iflytek.cyber.iot.keepalive.action.KEEP_ALIVE";

    private static final String MAIN_APP_PKG = "com.iflytek.cyber.iot.show.core";
    private static final String MAIN_APP_CLS = "com.iflytek.cyber.iot.show.core.EvsLauncherActivity";

    private CountDownHandler handler = new CountDownHandler();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler.setOnDeadCallback(new OnDeadCallback() {
            @Override
            public void onDead() {
                Intent intent = getPackageManager().getLaunchIntentForPackage(MAIN_APP_PKG);
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    Intent mainApp = new Intent();
                    mainApp.setClassName(MAIN_APP_PKG, MAIN_APP_CLS);
                    mainApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(mainApp);
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "evs_keep_alive_channel";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "EVS 保活服务",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();

            startForeground(1, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && KEEP_ALIVE_ACTION.equals(intent.getAction())) {
            handler.keepAlive();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    static class CountDownHandler extends Handler {
        private Long current = 0L;

        private OnDeadCallback onDeadCallback;

        public void setOnDeadCallback(OnDeadCallback onDeadCallback) {
            this.onDeadCallback = onDeadCallback;
        }

        public void keepAlive() {
            Message msg = Message.obtain();
            Long current = System.currentTimeMillis();
            msg.obj = current;
            this.current = current;
            sendMessageDelayed(msg, TimeUnit.SECONDS.toMillis(15));
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                if (msg.obj == current) {
                    // 每一个 keep-alive 的消息都应该被下一个的置换掉，若未置换则说明主程序发送 keep-alive 超时
                    if (onDeadCallback != null) {
                        onDeadCallback.onDead();
                    }
                }
            }
        }
    }

    interface OnDeadCallback {
        void onDead();
    }
}
