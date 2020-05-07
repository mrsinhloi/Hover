/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mattcarroll.hover.window;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import io.mattcarroll.hover.HoverView;
import io.mattcarroll.hover.OnExitListener;
import io.mattcarroll.hover.R;
import io.mattcarroll.hover.SideDock;

/**
 * {@code Service} that presents a {@link HoverView} within a {@code Window}.
 * <p>
 * The {@code HoverView} is displayed whenever any Intent is received by this {@code Service}. The
 * {@code HoverView} is removed and destroyed whenever this {@code Service} is destroyed.
 * <p>
 * A {@link Service} is required for displaying a {@code HoverView} in a {@code Window} because there
 * is no {@code Activity} to associate with the {@code HoverView}'s UI. This {@code Service} is the
 * application's link to the device's {@code Window} to display the {@code HoverView}.
 */
public abstract class HoverMenuService extends Service {

    private static final String TAG = "HoverMenuService";

    private HoverView mHoverView;
    private boolean mIsRunning;
    private OnExitListener mOnMenuOnExitListener = new OnExitListener() {
        @Override
        public void onExit() {
            Log.d(TAG, "Menu exit requested. Exiting.");
            mHoverView.removeFromWindow();
            onHoverMenuExitingByUserRequest();
            close();
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        initNotification();
    }


    public void close() {
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Stop and return immediately if we don't have permission to display things above other
        // apps.
        /*if (!OverlayPermission.hasRuntimePermissionToDrawOverlay(getApplicationContext())) {
            Log.e(TAG, "Cannot display a Hover menu in a Window without the draw overlay permission.");
            close();
            return START_NOT_STICKY;
        }

        if (null == intent) {
            Log.e(TAG, "Received null Intent. Not creating Hover menu.");
            close();
            return START_NOT_STICKY;
        }*/

        if (!mIsRunning) {
            Log.d(TAG, "onStartCommand() - showing Hover menu.");
            mIsRunning = true;
            initHoverMenu(intent);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        if (mIsRunning) {
            mHoverView.removeFromWindow();
            mIsRunning = false;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initHoverMenu(@NonNull Intent intent) {
        mHoverView = HoverView.createForWindow(
                this,
                new WindowViewController((WindowManager) getSystemService(Context.WINDOW_SERVICE)),
                new SideDock.SidePosition(SideDock.SidePosition.RIGHT, 0.5f)
        );
        mHoverView.setOnExitListener(mOnMenuOnExitListener);
        mHoverView.addToWindow();

        onHoverMenuLaunched(intent, mHoverView);
    }

    /**
     * Hook for subclasses to return a custom Context to be used in the creation of the {@code HoverMenu}.
     * For example, subclasses might choose to provide a ContextThemeWrapper.
     *
     * @return context for HoverMenu initialization
     */
    protected Context getContextForHoverMenu() {
        return this;
    }

    @NonNull
    protected HoverView getHoverView() {
        return mHoverView;
    }


    protected void onHoverMenuLaunched(@NonNull Intent intent, @NonNull HoverView hoverView) {
        // Hook for subclasses.
    }

    /**
     * Hook method for subclasses to take action when the user exits the HoverMenu. This method runs
     * just before this {@code HoverMenuService} calls {@code stopSelf()}.
     */
    protected void onHoverMenuExitingByUserRequest() {
        // Hook for subclasses.
    }

    //region NOTIFICATION IN ANDROID 8 - ANDROID O
    public void initNotification() {
        Notification foregroundNotification = getForegroundNotification();
        if (null != foregroundNotification) {
            int notificationId = getForegroundNotificationId();
            startForeground(notificationId, foregroundNotification);
        }
    }

    protected int getForegroundNotificationId() {
        // Subclasses should provide their own notification ID if using a notification.
        return 1234567089;
    }

    public static final String NOTIFY_GROUP_BUBBLE = "com.workchat.NOTIFY_GROUP_BUBBLE";

    protected Notification getForegroundNotification() {
        // If subclass returns a non-null Notification then the Service will be run in
        // the foreground.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String NOTIFICATION_CHANNEL_ID = getPackageName() + ".channel.bubble";
            String channelName = "Workchat Background Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.ic_workchat_notify)
                    .setContentTitle(getString(R.string.background_notify_chathead))
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setGroup(NOTIFY_GROUP_BUBBLE)
                    .build();
            return notification;
        } else {
            return new Notification();
        }
    }
    //endregion NOTIFICATION IN ANDROID 8 - ANDROID O


}
