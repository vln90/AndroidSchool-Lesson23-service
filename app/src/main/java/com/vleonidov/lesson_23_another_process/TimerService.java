package com.vleonidov.lesson_23_another_process;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class TimerService extends Service {

    private static final String TAG = "TimerService";

    private static final String CHANNEL_ID = "channel_id_2";

    private static final long TIME_COUNTDOWN = 1000 * 600L;
    private static final long TIMER_PERIOD = 1000L;

    private static final int NOTIFICATION_ID = 1;

    private static int sNotificationID = 0;

    public static final String ACTION_CLOSE = "TIMER_SERVICE_ACTION_CLOSE";

    public static final int MSG_START_TIMER = 1;
    public static final int MSG_STOP_TIMER = 2;
    public static final String BUNDLE_KEY_TIME = "BUNDLE_KEY_TIME";

    public static final int MSG_TIMER_CALLED = 3;
    public static final String BUNDLE_CURRENT_TIME = "BUNDLE_CURRENT_TIME";

    private CountDownTimer mCountDownTimer;


    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        Log.d(TAG, "onCreate() called");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called with: intent = [" + intent + "], flags = [" + flags + "], startId = [" + startId + "]");

        if (ACTION_CLOSE.equals(intent.getAction())) {
            stopSelf();
        } else {
            startCountdownTimer(TIME_COUNTDOWN, TIMER_PERIOD);

            startForeground(NOTIFICATION_ID, createNotification(1000));
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy() called");

        stopCountdownTimer();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind() called with: intent = [" + intent + "]");

        return mTimerServiceAIDL.asBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind() called with: intent = [" + intent + "]");

        return super.onUnbind(intent);
    }


    public void startCountdownTimer(long time, long period) {
        mCountDownTimer = new CountDownTimer(time, period) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "onTick() called with: millisUntilFinished = [" + millsToSeconds(millisUntilFinished) + "]");


                startForeground(NOTIFICATION_ID, createNotification(millsToSeconds(millisUntilFinished)));
//                updateNotification(createNotification(millsToSeconds(millisUntilFinished)));
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "onFinish() called");

                stopSelf();
            }
        };

        mCountDownTimer.start();
    }


    private Notification createNotification(long currentTime) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Intent intentCloseService = new Intent(this, TimerService.class);
        intentCloseService.setAction(ACTION_CLOSE);
        PendingIntent pendingIntentCloseService = PendingIntent.getService(this, 0, intentCloseService, 0);

        builder.setContentTitle(getString(R.string.timer_service_content_title))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(getString(R.string.timer_service_content_description) + currentTime)
                .setOnlyAlertOnce(true)
                .addAction(0, getString(R.string.button_stop_service), pendingIntentCloseService)
                .setContentIntent(pendingIntent);

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notification_channel_name);
            String description = getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void updateNotification(@NonNull Notification notification) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void stopCountdownTimer() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
    }

    private long millsToSeconds(long time) {
        return time / 1000L;
    }

    private Handler mHandler = new Handler();

    private ITimerServiceAIDL.Stub mTimerServiceAIDL = new ITimerServiceAIDL.Stub() {
        @Override
        public void startTimer(final long time, final long period) throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    startCountdownTimer(time, period);
                }
            });
        }
    };
}
